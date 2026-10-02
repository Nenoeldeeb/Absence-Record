package dev.nenoeldeeb.education.absencerecord.domain.models

import kotlinx.datetime.LocalDate

data class DeletedStudentBackup(
    val student: Student,
    val attendanceDates: List<LocalDate> = emptyList()
)