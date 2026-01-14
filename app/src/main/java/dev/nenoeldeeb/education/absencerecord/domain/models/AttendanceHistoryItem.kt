package dev.nenoeldeeb.education.absencerecord.domain.models

import kotlinx.datetime.LocalDate

data class AttendanceHistoryItem(
    val date: LocalDate,
    val name: String
)