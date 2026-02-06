package dev.nenoeldeeb.education.absencerecord.domain.usecases

import dev.nenoeldeeb.education.absencerecord.domain.repositories.ReportRepository
import dev.nenoeldeeb.education.absencerecord.domain.usecases.report.ShareReportUseCase

data class ReportUseCases(private val reportRepository: ReportRepository) {
    val shareReportUseCase: ShareReportUseCase = ShareReportUseCase(reportRepository)
}