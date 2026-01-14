package dev.nenoeldeeb.education.absencerecord.presentation.screens.calendar

import kotlinx.datetime.LocalDate

sealed interface CalendarScreenEvent {
    data class UpdateSelectedMonth(val month: LocalDate?) : CalendarScreenEvent

    data object ClearMonthFilter : CalendarScreenEvent

    data class GetStudentsForDate(val date: LocalDate) : CalendarScreenEvent

    data class MarkStudentAttendance(val studentId: Int, val date: LocalDate) : CalendarScreenEvent

    data class DeleteStudentAttendance(val studentId: Int, val date: LocalDate) : CalendarScreenEvent

    data class SelectDateForDialog(val date: LocalDate?) : CalendarScreenEvent
}