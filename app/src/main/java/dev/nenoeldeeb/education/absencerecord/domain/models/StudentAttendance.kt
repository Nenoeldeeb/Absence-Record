package dev.nenoeldeeb.education.absencerecord.domain.models

import kotlinx.datetime.LocalDate

data class StudentAttendance(
    val id: Int = 0,
    val studentId: Int,
    val date: LocalDate
)