package dev.nenoeldeeb.education.absencerecord.presentation.screens.calendar

import dev.nenoeldeeb.education.absencerecord.presentation.screens.components.ClassFilter
import kotlinx.datetime.LocalDate

sealed interface CalendarScreenEvent {
    data class MarkStudentAttendance(val studentId: Int, val date: LocalDate) : CalendarScreenEvent

    data class DeleteStudentAttendance(val studentId: Int, val date: LocalDate) :
        CalendarScreenEvent

    data class SelectDateForDialog(val date: LocalDate?) : CalendarScreenEvent

    data class SelectClassFilter(val filter: ClassFilter) : CalendarScreenEvent

    data class ToggleClassDropdown(val expanded: Boolean) : CalendarScreenEvent
}
