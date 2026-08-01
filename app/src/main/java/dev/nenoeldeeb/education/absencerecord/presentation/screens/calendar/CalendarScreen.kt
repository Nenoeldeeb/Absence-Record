@file:OptIn(ExperimentalMaterial3Api::class)

package dev.nenoeldeeb.education.absencerecord.presentation.screens.calendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.app.AppViewModelProvider
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentAttendance
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentClass
import dev.nenoeldeeb.education.absencerecord.presentation.screens.components.ClassCheckboxFilter
import dev.nenoeldeeb.education.absencerecord.presentation.screens.components.ComposeCalendar
import dev.nenoeldeeb.education.absencerecord.presentation.theme.green30
import dev.nenoeldeeb.education.absencerecord.presentation.utils.DateFormatter.toUiText
import dev.nenoeldeeb.education.absencerecord.presentation.utils.SoundPlayer
import kotlinx.datetime.LocalDate

@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = viewModel(factory = AppViewModelProvider.factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it.asString(context))
            viewModel.onEvent(CalendarScreenEvent.ConsumeError)
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            ComposeCalendar(
                modifier = Modifier.weight(1f),
                onDateSelected = { date ->
                    viewModel.onEvent(CalendarScreenEvent.SelectDateForDialog(date))
                }
            )
        }
    }

    uiState.selectedDateForDialog?.let {
        AttendanceDialog(
            allStudents = uiState.allStudents,
            studentsForSelectedDate = uiState.studentsForSelectedDate,
            selectedDate = it,
            availableClasses = uiState.availableClasses,
            selectedClassIds = uiState.selectedClassIds,
            filterDropdownExpanded = uiState.filterDropdownExpanded,
            onDismiss = { viewModel.onEvent(CalendarScreenEvent.SelectDateForDialog(null)) },
            onToggleAttendance = { student, date, isPresent ->
                if (isPresent) {
                    viewModel.onEvent(
                        CalendarScreenEvent.DeleteStudentAttendance(student.id, date)
                    )
                    SoundPlayer.playRemoveAttendanceSound()
                } else {
                    viewModel.onEvent(
                        CalendarScreenEvent.MarkStudentAttendance(student.id, date)
                    )
                    SoundPlayer.playAddAttendanceSound()
                }
            },
            onToggleClassSelection = { classId ->
                viewModel.onEvent(CalendarScreenEvent.ToggleClassSelection(classId))
            },
            onToggleFilterDropdown = {
                viewModel.onEvent(CalendarScreenEvent.ToggleFilterDropdown)
            }
        )
    }
}

@Composable
internal fun AttendanceDialog(
    modifier: Modifier = Modifier,
    allStudents: List<Student>,
    studentsForSelectedDate: List<StudentAttendance>,
    selectedDate: LocalDate,
    availableClasses: List<StudentClass>,
    selectedClassIds: Set<Int>,
    filterDropdownExpanded: Boolean,
    onDismiss: () -> Unit,
    onToggleAttendance: (Student, LocalDate, Boolean) -> Unit,
    onToggleClassSelection: (Int) -> Unit,
    onToggleFilterDropdown: () -> Unit
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = {
            DialogTitle(
                selectedDate = selectedDate,
                totalStudents = allStudents.size,
                presentCount = studentsForSelectedDate.size
            )
        },
        text = {
            Column {
                ClassCheckboxFilter(
                    availableClasses = availableClasses,
                    selectedClassIds = selectedClassIds,
                    expanded = filterDropdownExpanded,
                    onExpandedChange = { onToggleFilterDropdown() },
                    onClassToggle = onToggleClassSelection,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                DialogContent(
                    modifier = Modifier.fillMaxWidth(),
                    allStudents = allStudents,
                    studentsForSelectedDate = studentsForSelectedDate,
                    selectedDate = selectedDate,
                    onToggleAttendance = onToggleAttendance
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        }
    )
}

@Composable
internal fun DialogTitle(
    selectedDate: LocalDate,
    totalStudents: Int,
    presentCount: Int
) {
    val month = selectedDate.month.toUiText(fullName = false).asString()
    val dayName = selectedDate.dayOfWeek.toUiText(fullName = true).asString()

    val formattedDate =
        rememberSaveable(selectedDate) {
            "${selectedDate.year} $month ${selectedDate.day} $dayName"
        }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.mark_attendance_title, formattedDate))
        Text(stringResource(R.string.attendance_count, totalStudents, presentCount))
    }
}

@Composable
internal fun DialogContent(
    modifier: Modifier = Modifier,
    allStudents: List<Student>,
    studentsForSelectedDate: List<StudentAttendance>,
    selectedDate: LocalDate,
    onToggleAttendance: (Student, LocalDate, Boolean) -> Unit
) {
    when {
        allStudents.isEmpty() -> {
            Text(
                stringResource(R.string.no_students_for_attendance),
                textAlign = TextAlign.Center,
                modifier = modifier
            )
        }

        else -> {
            LazyColumn(modifier = modifier, contentPadding = PaddingValues(vertical = 8.dp)) {
                items(allStudents, key = { it.id }) { student ->
                    val isPresent =
                        studentsForSelectedDate.any {
                            it.studentId == student.id && it.date == selectedDate
                        }
                    StudentListItem(
                        modifier =
                            Modifier.clickable {
                                onToggleAttendance(student, selectedDate, isPresent)
                            },
                        student,
                        isPresent
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
internal fun StudentListItem(
    modifier: Modifier = Modifier,
    student: Student,
    isPresent: Boolean
) {
    val statusText =
        stringResource(
            if (isPresent) {
                R.string.content_description_present
            } else {
                R.string.content_description_absent
            }
        )
    val contentDescription = stringResource(R.string.student_attendance_status, student.name, statusText)
    val listItemColors =
        ListItemDefaults.colors(
            containerColor =
                if (isPresent) {
                    green30.copy(alpha = 0.5f)
                } else {
                    ListItemDefaults.colors().containerColor
                }
        )

    ListItem(
        headlineContent = {
            Text(
                text = student.name,
                textAlign = TextAlign.Start,
                style =
                    MaterialTheme.typography.bodyLarge.copy(
                        textDirection = TextDirection.Content
                    ),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .wrapContentWidth()
            )
        },
        leadingContent = {
            Icon(
                imageVector =
                    ImageVector.vectorResource(
                        if (isPresent) {
                            R.drawable.outline_check_24
                        } else {
                            R.drawable.outline_close_24
                        }
                    ),
                contentDescription = null,
                tint =
                    if (isPresent) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    }
            )
        },
        colors = listItemColors,
        modifier = modifier.semantics { this.contentDescription = contentDescription }
    )
}