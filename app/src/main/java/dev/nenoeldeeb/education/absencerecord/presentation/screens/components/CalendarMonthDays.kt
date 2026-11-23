package dev.nenoeldeeb.education.absencerecord.presentation.screens.components

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

internal fun calculateDaysInMonth(yearMonth: LocalDate): List<LocalDate?> {
    val firstDayOfMonth = LocalDate(yearMonth.year, yearMonth.month, 1)
    val nextMonth = firstDayOfMonth.plus(1, DateTimeUnit.MONTH)
    val lastDayOfMonth = nextMonth.minus(1, DateTimeUnit.DAY)

    // Calculate the first day offset (Saturday = 0, Sunday = 1, ..., Friday = 6)
    val firstDayOfWeekCalendar = DayOfWeek.SATURDAY
    var startOffset = firstDayOfMonth.dayOfWeek.isoDayNumber - firstDayOfWeekCalendar.isoDayNumber
    if (startOffset < 0) startOffset += 7

    val days = mutableListOf<LocalDate?>()
    repeat(startOffset) { days.add(null) }
    (1..lastDayOfMonth.day).forEach { dayOfMonth ->
        days.add(LocalDate(yearMonth.year, yearMonth.month, dayOfMonth))
    }
    while (days.size % 7 != 0) {
        days.add(null)
    }
    return days
}