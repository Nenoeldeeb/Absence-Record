package dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.presentation.screens.components.ComposeCalendar
import dev.nenoeldeeb.education.absencerecord.presentation.screens.components.EmptyStateMessage
import kotlinx.datetime.LocalDate

private val noopOnDateSelected: (LocalDate) -> Unit = {}

private fun monthKey(date: LocalDate): LocalDate = LocalDate(date.year, date.month, 1)

private fun groupAttendanceByMonth(
    attendanceDates: List<LocalDate>
): Pair<List<MonthPresentSummary>, Map<LocalDate, Set<LocalDate>>> {
    if (attendanceDates.isEmpty()) return emptyList<MonthPresentSummary>() to emptyMap()
    val grouped = LinkedHashMap<LocalDate, MutableSet<LocalDate>>()
    for (date in attendanceDates) {
        grouped.getOrPut(monthKey(date)) { LinkedHashSet() }.add(date)
    }
    val summaries =
        grouped.map { (month, dates) ->
            MonthPresentSummary(month = month, presentDays = dates.size)
        }.sortedByDescending { it.month }
    return summaries to grouped
}

@Composable
internal fun AttendanceReportSection(
    attendanceDates: List<LocalDate>,
    selectedMonth: LocalDate?,
    onMonthSelected: (LocalDate) -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
    // Hoisted from StudentDetailBody: this section previously ran its own
    // BoxWithConstraints, nesting three measure passes deep (Body > section >
    // ComposeCalendar). Reusing the parent's orientation removes one pass.
    landscape: Boolean = false
) {
    val currentMonth = remember { todayMonth() }
    // Single pass builds both the dropdown summaries and the per-month lookup,
    // so switching months is an O(1) map get instead of an O(N) filter.
    val (monthSummaries, datesByMonth) =
        remember(attendanceDates) {
            groupAttendanceByMonth(attendanceDates)
        }
    val displayMonth = selectedMonth ?: monthSummaries.firstOrNull()?.month ?: currentMonth
    val markedDates =
        remember(datesByMonth, displayMonth) {
            datesByMonth[monthKey(displayMonth)] ?: emptySet()
        }
    val displaySummary = monthSummaries.firstOrNull { it.month == displayMonth }

    if (landscape) {
        Row(modifier = modifier.fillMaxSize()) {
            Column(
                modifier =
                    Modifier
                        .weight(0.42f)
                        .fillMaxHeight()
                        .padding(start = 16.dp, top = 4.dp, bottom = 4.dp)
            ) {
                MonthControlsRow(
                    displaySummary = displaySummary,
                    displayMonth = displayMonth,
                    monthSummaries = monthSummaries,
                    onMonthSelected = onMonthSelected,
                    shareEnabled = markedDates.isNotEmpty(),
                    onShare = onShare,
                    modifier = Modifier.fillMaxWidth()
                )
                if (monthSummaries.isEmpty()) {
                    EmptyStateMessage(
                        message = R.string.no_attendance_records,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                    )
                }
            }
            if (monthSummaries.isNotEmpty()) {
                Box(
                    modifier =
                        Modifier
                            .weight(0.58f)
                            .fillMaxHeight()
                ) {
                    ComposeCalendar(
                        initialMonth = displayMonth,
                        markedDates = markedDates,
                        interactive = false,
                        showHeader = false,
                        onDateSelected = noopOnDateSelected,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    } else {
        Column(modifier = modifier.fillMaxSize()) {
            MonthControlsRow(
                displaySummary = displaySummary,
                displayMonth = displayMonth,
                monthSummaries = monthSummaries,
                onMonthSelected = onMonthSelected,
                shareEnabled = markedDates.isNotEmpty(),
                onShare = onShare,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
            )

            if (monthSummaries.isEmpty()) {
                EmptyStateMessage(
                    message = R.string.no_attendance_records,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                )
            } else {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    ComposeCalendar(
                        initialMonth = displayMonth,
                        markedDates = markedDates,
                        interactive = false,
                        showHeader = false,
                        onDateSelected = noopOnDateSelected,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}