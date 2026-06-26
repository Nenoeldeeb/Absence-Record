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
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
import dev.nenoeldeeb.education.absencerecord.presentation.screens.components.ComposeCalendar
import dev.nenoeldeeb.education.absencerecord.presentation.theme.green30
import dev.nenoeldeeb.education.absencerecord.presentation.utils.DateFormatter.toUiText
import dev.nenoeldeeb.education.absencerecord.presentation.utils.SoundPlayer
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText
import kotlinx.datetime.LocalDate

@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = viewModel(factory = AppViewModelProvider.factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = modifier) {
        ComposeCalendar(
            modifier = Modifier.weight(1f),
            onDateSelected = { date ->
                viewModel.onEvent(CalendarScreenEvent.SelectDateForDialog(date))
            }
        )
    }

    uiState.selectedDateForDialog?.let {
        AttendanceDialog(
            allStudents = uiState.allStudents,
            studentsForSelectedDate = uiState.studentsForSelectedDate,
            error = uiState.error,
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
    error: UiText?,
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
                    error = error,
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
    error: UiText?,
    selectedDate: LocalDate,
    onToggleAttendance: (Student, LocalDate, Boolean) -> Unit
) {
    when {
        error != null -> {
            Text(
                text = stringResource(R.string.error_display, error.asString()),
                textAlign = TextAlign.Center,
                modifier = modifier,
                color = MaterialTheme.colorScheme.error
            )
        }

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
    val contentDescription =
        stringResource(
            if (isPresent) {
                R.string.content_description_present
            } else {
                R.string.content_description_absent
            }
        )
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
        colors = listItemColors,
        modifier = modifier.semantics { this.contentDescription = contentDescription }
    )
}