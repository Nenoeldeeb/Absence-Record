package dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.presentation.utils.DateFormatter.toMonthYearUiText
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

internal data class MonthPresentSummary(
    val month: LocalDate,
    val presentDays: Int
)

@Composable
internal fun summaryLabel(
    summary: MonthPresentSummary?,
    date: LocalDate
): String =
    if (summary != null) {
        summaryLabel(summary)
    } else {
        monthLabel(date)
    }

@Composable
internal fun summaryLabel(summary: MonthPresentSummary): String =
    "${monthLabel(summary.month)}, ${presentDaysLabel(summary.presentDays)}"

@Composable
internal fun monthLabel(date: LocalDate): String = date.toMonthYearUiText(fullName = true).asString()

@Composable
internal fun presentDaysLabel(count: Int): String =
    pluralStringResource(
        R.plurals.present_days_count,
        count,
        count
    )

internal fun todayMonth(): LocalDate {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    return LocalDate(now.year, now.month, 1)
}