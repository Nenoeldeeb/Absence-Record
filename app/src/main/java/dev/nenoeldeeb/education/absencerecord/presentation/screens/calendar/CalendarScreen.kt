package dev.nenoeldeeb.education.absencerecord.presentation.screens.calendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.app.AppViewModelProvider
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.presentation.screens.components.ComposeCalendar
import dev.nenoeldeeb.education.absencerecord.presentation.theme.green30
import dev.nenoeldeeb.education.absencerecord.presentation.utils.DateFormatter.toUiText
import dev.nenoeldeeb.education.absencerecord.presentation.utils.SoundPlayer
import kotlinx.datetime.LocalDate

@Composable
fun CalendarScreen(viewModel: CalendarViewModel = viewModel(factory = AppViewModelProvider.factory)) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        ComposeCalendar(
            modifier = Modifier.weight(1f),
            onDateSelected = { date ->
                viewModel.onEvent(CalendarScreenEvent.SelectDateForDialog(date))
                viewModel.onEvent(CalendarScreenEvent.GetStudentsForDate(date))
            },
        )
    }

    uiState.selectedDateForDialog?.let {
        AttendanceDialog(
            uiState = uiState,
            selectedDate = it,
            onDismiss = { viewModel.onEvent(CalendarScreenEvent.SelectDateForDialog(null)) },
            onToggleAttendance = { student, date, isPresent ->
                if (isPresent) {
                    SoundPlayer.playRemoveAttendanceSound()
                } else {
                    SoundPlayer.playAddAttendanceSound()
                }
                val event =
                    if (isPresent) {
                        CalendarScreenEvent.DeleteStudentAttendance(student.id, date)
                    } else {
                        CalendarScreenEvent.MarkStudentAttendance(student.id, date)
                    }
                viewModel.onEvent(event)
                viewModel.onEvent(CalendarScreenEvent.GetStudentsForDate(date))
            }
        )
    }
}

@Composable
internal fun AttendanceDialog(
    uiState: CalendarScreenState,
    selectedDate: LocalDate,
    onDismiss: () -> Unit,
    onToggleAttendance: (Student, LocalDate, Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { DialogTitle(uiState, selectedDate) },
        text = { DialogContent(uiState, selectedDate, onToggleAttendance) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        }
    )
}

@Composable
internal fun DialogTitle(
    uiState: CalendarScreenState,
    selectedDate: LocalDate
) {
    val month = selectedDate.month.toUiText(fullName = false).asString()
    val dayName = selectedDate.dayOfWeek.toUiText(fullName = true).asString()

    val formattedDate =
        rememberSaveable(selectedDate) {
            "${selectedDate.year} $month ${selectedDate.day} $dayName"
        }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.mark_attendance_title, formattedDate))
        Text(
            stringResource(
                R.string.attendance_count,
                uiState.allStudents.size,
                uiState.studentsForSelectedDate.size
            )
        )
    }
}

@Composable
internal fun DialogContent(
    uiState: CalendarScreenState,
    selectedDate: LocalDate,
    onToggleAttendance: (Student, LocalDate, Boolean) -> Unit
) {
    when {
        uiState.error != null -> {
            Text(
                text = stringResource(R.string.error_display, uiState.error.asString()),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.error
            )
        }

        uiState.allStudents.isEmpty() -> {
            Text(
                stringResource(R.string.no_students_for_attendance),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        else -> {
            LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                items(uiState.allStudents, key = { it.id }) { student ->
                    val isPresent =
                        uiState.studentsForSelectedDate.any {
                            it.studentId == student.id && it.date == selectedDate
                        }
                    StudentListItem(student, isPresent) {
                        onToggleAttendance(student, selectedDate, isPresent)
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
internal fun StudentListItem(
    student: Student,
    isPresent: Boolean,
    onClick: () -> Unit
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
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentWidth()
            )
        },
        colors = listItemColors,
        modifier =
            Modifier
                .clickable(onClick = onClick)
                .semantics {
                    this.contentDescription = contentDescription
                }
    )
}