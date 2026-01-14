package dev.nenoeldeeb.education.absencerecord.domain.usecases.report

import dev.nenoeldeeb.education.absencerecord.domain.repositories.ReportRepository
import kotlinx.datetime.LocalDate

class ShareReportUseCase(
    private val reportRepository: ReportRepository
) {
    suspend operator fun invoke(
        studentName: String,
        month: LocalDate,
        attendance: List<LocalDate>
    ): Result<String> {
        return reportRepository.generateAndSaveReport(studentName, month, attendance)
    }
}