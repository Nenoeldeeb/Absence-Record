package dev.nenoeldeeb.education.absencerecord.data.datasources.local.entities

import kotlinx.datetime.LocalDate

data class AttendanceHistoryItemEntity(
    val date: LocalDate,
    val studentName: String
)