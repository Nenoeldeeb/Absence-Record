package dev.nenoeldeeb.education.absencerecord.presentation.screens.calendar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.presentation.screens.calendar.dialogs.AttendanceDialog
import dev.nenoeldeeb.education.absencerecord.presentation.utils.DateFormatter
import kotlinx.datetime.LocalDate

/**
 * Aggregator for all dialogs on the Calendar screen.
 */
@Composable
internal fun CalendarDialogs(
    uiState: CalendarScreenState,
    onEvent: (CalendarScreenEvent) -> Unit
) {
    uiState.selectedDateForDialog?.let { dialogDate ->
        val today = DateFormatter.today()
        val onToggleAttendance: (Student, LocalDate, Boolean) -> Unit =
            remember(today) {
                { student: Student, date: LocalDate, isPresent: Boolean ->
                    if (date <= today) {
                        if (isPresent) {
                            onEvent(CalendarScreenEvent.DeleteStudentAttendance(student.id, date))
                        } else {
                            onEvent(CalendarScreenEvent.MarkStudentAttendance(student.id, date))
                        }
                    }
                }
            }
        AttendanceDialog(
            allStudents = uiState.allStudents,
            studentsForSelectedDate = uiState.studentsForSelectedDate,
            selectedDate = dialogDate,
            availableClasses = uiState.availableClasses,
            selectedClassIds = uiState.selectedClassIds,
            filterDropdownExpanded = uiState.filterDropdownExpanded,
            isFutureDate = dialogDate > today,
            onDismiss = { onEvent(CalendarScreenEvent.SelectDateForDialog(null)) },
            onToggleAttendance = onToggleAttendance,
            onToggleClassSelection = { classId ->
                onEvent(CalendarScreenEvent.ToggleClassSelection(classId))
            },
            onToggleFilterDropdown = { onEvent(CalendarScreenEvent.ToggleFilterDropdown) },
            onClearClassFilter = { onEvent(CalendarScreenEvent.ClearClassFilter) }
        )
    }
}