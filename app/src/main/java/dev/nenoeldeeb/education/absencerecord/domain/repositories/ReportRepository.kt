package dev.nenoeldeeb.education.absencerecord.domain.repositories

import kotlinx.datetime.LocalDate

interface ReportRepository {
    suspend fun generateAndSaveReport(
        studentName: String,
        month: LocalDate,
        attendance: List<LocalDate>
    ): Result<String>
}