package dev.nenoeldeeb.education.absencerecord.presentation.screens.report

import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.presentation.screens.components.ClassFilter
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

    data object ConsumeShareFileUri : ReportScreenEvent

    data class ShowToast(val message: UiText) : ReportScreenEvent

    data object ConsumeToastMessage : ReportScreenEvent

    data class ShowHistoryDialog(val show: Boolean) : ReportScreenEvent

    data class ShowCalendarPreviewDialog(val show: Boolean) : ReportScreenEvent

    data class SelectMonthYearForCalendarPreview(val month: LocalDate?) : ReportScreenEvent

    data class ToggleMonthDropdown(val expanded: Boolean) : ReportScreenEvent

    data class SelectClassFilter(val filter: ClassFilter) : ReportScreenEvent

    data class ToggleClassDropdown(val expanded: Boolean) : ReportScreenEvent
}