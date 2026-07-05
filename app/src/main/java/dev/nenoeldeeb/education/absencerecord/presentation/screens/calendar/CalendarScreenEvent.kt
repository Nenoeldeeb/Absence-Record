package dev.nenoeldeeb.education.absencerecord.presentation.screens.calendar

import kotlinx.datetime.LocalDate

sealed interface CalendarScreenEvent {
    data class MarkStudentAttendance(val studentId: Int, val date: LocalDate) : CalendarScreenEvent

    data class DeleteStudentAttendance(val studentId: Int, val date: LocalDate) :
        CalendarScreenEvent

    data class SelectDateForDialog(val date: LocalDate?) : CalendarScreenEvent

    data class ToggleClassSelection(val classId: Int) : CalendarScreenEvent

    data object ToggleFilterDropdown : CalendarScreenEvent

    data object ConsumeError : CalendarScreenEvent
}