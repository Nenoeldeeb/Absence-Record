package dev.nenoeldeeb.education.absencerecord.presentation.screens.report

import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText
import kotlinx.datetime.LocalDate

sealed interface ReportScreenEvent {
    data class UpdateSelectedMonth(val month: LocalDate?) : ReportScreenEvent

    data object ToggleSortType : ReportScreenEvent

    data object ClearMonthFilter : ReportScreenEvent

    data class SelectStudentForHistory(val student: Student?) : ReportScreenEvent

    data class PrepareCalendarImageForSharing(
        val month: LocalDate,
        val studentId: Int,
        val studentName: String
    ) : ReportScreenEvent

    data class ShareFileResult(val uri: android.net.Uri, val error: UiText? = null) : ReportScreenEvent

    data class ShowToast(val message: UiText) : ReportScreenEvent

    data object ConsumeToastMessage : ReportScreenEvent

    data class ShowHistoryDialog(val show: Boolean) : ReportScreenEvent

    data class ShowCalendarPreviewDialog(val show: Boolean) : ReportScreenEvent

    data class SelectMonthYearForCalendarPreview(val month: LocalDate?) : ReportScreenEvent

    data class ToggleMonthDropdown(val expanded: Boolean) : ReportScreenEvent

    data class ToggleClassFilter(val classId: Int) : ReportScreenEvent

    data object ToggleClassFilterVisibility : ReportScreenEvent

    data object ToggleSortComponentsVisibility : ReportScreenEvent

    data class ToggleClassDropdown(val expanded: Boolean) : ReportScreenEvent

    data object ConsumeError : ReportScreenEvent
}