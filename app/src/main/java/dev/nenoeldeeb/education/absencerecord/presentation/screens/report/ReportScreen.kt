package dev.nenoeldeeb.education.absencerecord.presentation.screens.report

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.app.AppViewModelProvider
import dev.nenoeldeeb.education.absencerecord.domain.models.SortType
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.presentation.screens.components.ComposeCalendar
import dev.nenoeldeeb.education.absencerecord.presentation.utils.DateFormatter.toMonthYearUiText
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(viewModel: ReportViewModel = viewModel(factory = AppViewModelProvider.factory)) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.shareFileUri) {
        uiState.shareFileUri?.let { uri ->
            val shareIntent =
                Intent(Intent.ACTION_VIEW).apply {
                    data = uri
                    flags += Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
            try {
                context.startActivity(
                    Intent.createChooser(
                        shareIntent,
                        context.applicationContext.getString(R.string.view_picture)
                    )
                )
            } catch (e: Exception) {
                viewModel.onEvent(
                    ReportScreenEvent.ShowToast(
                        UiText.StringResource(
                            R.string.sharing_app_not_found,
                            e.message ?: ""
                        )
                    )
                )
            }
            viewModel.onEvent(ReportScreenEvent.ConsumeShareFileUri)
        }
    }

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let {
            Toast.makeText(context, it.asString(context), Toast.LENGTH_LONG).show()
            viewModel.onEvent(ReportScreenEvent.ConsumeToastMessage)
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(8.dp)
    ) {
        // Sorting bar and filter UI (previously inside Scaffold's bottomBar)
        ReportControls(
            uiState = uiState,
            onEvent = viewModel::onEvent
        )

        // Main content (was inside Scaffold's content lambda)
        StudentList(
            students = uiState.allStudents,
            onStudentClick = { student ->
                viewModel.onEvent(ReportScreenEvent.SelectStudentForHistory(student))
                viewModel.onEvent(ReportScreenEvent.ShowHistoryDialog(true))
            }
        )
    }

    uiState.selectedStudentForHistory?.let { student ->
        if (uiState.showHistoryDialog) {
            StudentHistoryDialog(
                student = student,
                history = uiState.studentHistory,
                onDismiss = {
                    viewModel.onEvent(ReportScreenEvent.ShowHistoryDialog(false))
                    viewModel.onEvent(ReportScreenEvent.SelectStudentForHistory(null))
                },
                onShareMonth = { month ->
                    viewModel.onEvent(ReportScreenEvent.SelectMonthYearForCalendarPreview(month))
                    viewModel.onEvent(ReportScreenEvent.ShowCalendarPreviewDialog(true))
                }
            )
        }

        if (uiState.showCalendarPreviewDialog && uiState.selectedMonthYearForCalendarPreview != null) {
            CalendarPreviewDialog(
                student = student,
                monthToPreview = uiState.selectedMonthYearForCalendarPreview!!,
                datesForPreviewMonth =
                    uiState.studentHistory.find { it.first == uiState.selectedMonthYearForCalendarPreview }?.second
                    ?: emptyList(),
                onDismiss = { viewModel.onEvent(ReportScreenEvent.ShowCalendarPreviewDialog(false)) },
                onShare = { month, studentId, studentName ->
                    viewModel.onEvent(
                        ReportScreenEvent.PrepareCalendarImageForSharing(
                            month,
                            studentId,
                            studentName
                        )
                    )
                    viewModel.onEvent(ReportScreenEvent.ShowCalendarPreviewDialog(false))
                }
            )
        }
    }
}

@Composable
internal fun StudentHistoryDialog(
    student: Student,
    history: List<Pair<LocalDate, List<LocalDate>>>,
    onDismiss: () -> Unit,
    onShareMonth: (LocalDate) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.student_history_title, student.name)) },
        text = {
            if (history.isEmpty()) {
                Text(
                    stringResource(R.string.no_attendance_records),
                    textAlign = TextAlign.Center,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(history, key = { it.first.toString() }) { (month, dates) ->
                        ListItem(
                            headlineContent = {
                                Text(
                                    month.toMonthYearUiText(fullName = true).asString(),
                                    style = MaterialTheme.typography.titleSmall
                                )
                            },
                            supportingContent = {
                                Text(
                                    pluralStringResource(
                                        R.plurals.days_count,
                                        dates.size,
                                        dates.size
                                    )
                                )
                            },
                            trailingContent = {
                                IconButton(onClick = { onShareMonth(month) }) {
                                    Icon(
                                        painterResource(id = R.drawable.outline_share_24),
                                        stringResource(R.string.share_calendar_image)
                                    )
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        }
    )
}

@Composable
internal fun CalendarPreviewDialog(
    student: Student,
    monthToPreview: LocalDate,
    datesForPreviewMonth: List<LocalDate>,
    onDismiss: () -> Unit,
    onShare: (LocalDate, Int, String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            ComposeCalendar(
                initialMonth = monthToPreview,
                onDateSelected = {},
                markedDates = datesForPreviewMonth.toSet()
            )
        },
        confirmButton = {
            Button(onClick = { onShare(monthToPreview, student.id, student.name) }) {
                Text(stringResource(R.string.action_share_image))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReportControls(
    uiState: ReportScreenState,
    onEvent: (ReportScreenEvent) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ExposedDropdownMenuBox(
                expanded = uiState.monthDropdownExpanded,
                onExpandedChange = { onEvent(ReportScreenEvent.ToggleMonthDropdown(it)) },
                modifier = Modifier.weight(1.6f)
            ) {
                OutlinedTextField(
                    value =
                        uiState.selectedMonth?.toMonthYearUiText(fullName = true)?.asString()
                        ?: stringResource(R.string.all_months),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.filter_by_month)) },
                    trailingIcon = {
                        uiState.selectedMonth?.let {
                            IconButton(onClick = {
                                onEvent(ReportScreenEvent.ClearMonthFilter)
                                onEvent(ReportScreenEvent.ToggleMonthDropdown(false))
                            }) {
                                Icon(
                                    painterResource(id = R.drawable.outline_close_24),
                                    stringResource(R.string.clear_month_filter)
                                )
                            }
                        }
                        ?: ExposedDropdownMenuDefaults.TrailingIcon(expanded = uiState.monthDropdownExpanded)
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = uiState.monthDropdownExpanded,
                    onDismissRequest = { onEvent(ReportScreenEvent.ToggleMonthDropdown(false)) }
                ) {
                    if (uiState.availableMonths.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.no_data_for_months)) },
                            onClick = {},
                            enabled = false
                        )
                    }
                    uiState.availableMonths.forEach { month ->
                        DropdownMenuItem(
                            text = { Text(month.toMonthYearUiText(fullName = true).asString()) },
                            onClick = {
                                onEvent(ReportScreenEvent.UpdateSelectedMonth(month))
                                onEvent(ReportScreenEvent.ToggleMonthDropdown(false))
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { onEvent(ReportScreenEvent.ToggleSortType) },
                modifier = Modifier.weight(1f)
            ) {
                val sortActionText =
                    if (uiState.sortType == SortType.ByName) {
                        stringResource(R.string.sort_action_rate)
                    } else {
                        stringResource(R.string.sort_action_name)
                    }
                Text(sortActionText)
            }
        }
        Text(
            text =
                stringResource(
                    R.string.students_sort_title,
                    stringResource(
                        if (uiState.sortType == SortType.ByName) {
                            R.string.sort_by_name
                        } else {
                            R.string.sort_by_attendance
                        }
                    )
                ),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}

@Composable
internal fun StudentList(
    students: List<Student>,
    onStudentClick: (Student) -> Unit
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
    ) {
        if (students.isEmpty()) {
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(R.string.no_students_found),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(students, key = { it.id }) { student ->
                    ListItem(
                        headlineContent = {
                            Text(
                                text = student.name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .wrapContentWidth()
                            )
                        },
                        modifier = Modifier.clickable { onStudentClick(student) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}