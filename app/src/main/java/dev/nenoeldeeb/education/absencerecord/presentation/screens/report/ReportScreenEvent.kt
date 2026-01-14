package dev.nenoeldeeb.education.absencerecord.presentation.screens.report

import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.presentation.util.UiText
import kotlinx.datetime.LocalDate

sealed interface ReportScreenEvent {
    data class UpdateSelectedMonth(val month: LocalDate?) : ReportScreenEvent

    object ToggleSortType : ReportScreenEvent

    object ClearMonthFilter : ReportScreenEvent

    data class SelectStudentForHistory(val student: Student?) : ReportScreenEvent

    data class PrepareCalendarImageForSharing(
        val month: LocalDate,
        val studentId: Int,
        val studentName: String
    ) : ReportScreenEvent

    object ConsumeShareFileUri : ReportScreenEvent

    data class ShowToast(val message: UiText) : ReportScreenEvent

    object ConsumeToastMessage : ReportScreenEvent

    data class ShowHistoryDialog(val show: Boolean) : ReportScreenEvent

    data class ShowCalendarPreviewDialog(val show: Boolean) : ReportScreenEvent

    data class SelectMonthYearForCalendarPreview(val month: LocalDate?) : ReportScreenEvent

    data class ToggleMonthDropdown(val expanded: Boolean) : ReportScreenEvent

    object CloseMonthDropdown : ReportScreenEvent
}