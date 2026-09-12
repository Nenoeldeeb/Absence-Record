package dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.presentation.screens.components.ComposeCalendar
import dev.nenoeldeeb.education.absencerecord.presentation.screens.components.EmptyStateMessage
import dev.nenoeldeeb.education.absencerecord.presentation.utils.DateFormatter.toMonthYearUiText
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AttendanceReportSection(
    attendanceDates: List<LocalDate>,
    selectedMonth: LocalDate?,
    onMonthSelected: (LocalDate) -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentMonth = remember { todayMonth() }
    val displayMonth = selectedMonth ?: currentMonth
    val monthSummaries =
        remember(attendanceDates) {
            attendanceDates
                .groupingBy { LocalDate(it.year, it.month, 1) }
                .eachCount()
                .map { (month, count) -> MonthPresentSummary(month = month, presentDays = count) }
                .sortedByDescending { it.month }
        }
    val markedDates =
        remember(attendanceDates, displayMonth) {
            attendanceDates
                .filter { it.year == displayMonth.year && it.month == displayMonth.month }
                .toSet()
        }
    val displaySummary = monthSummaries.firstOrNull { it.month == displayMonth }
    var dropdownExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExposedDropdownMenuBox(
                expanded = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = it },
                modifier = Modifier.weight(1f)
            ) {
                val fieldLabel = summaryLabel(displaySummary, displayMonth)
                val selectMonthHint = stringResource(R.string.student_detail_select_month)
                OutlinedTextField(
                    value = fieldLabel,
                    onValueChange = {},
                    readOnly = true,
                    enabled = monthSummaries.isNotEmpty(),
                    singleLine = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                    },
                    modifier =
                        Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                            .semantics {
                                contentDescription = "$selectMonthHint: $fieldLabel"
                            }
                )
                ExposedDropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    monthSummaries.forEach { summary ->
                        MonthDropdownItem(
                            summary = summary,
                            isSelected = summary.month == displayMonth,
                            onClick = {
                                dropdownExpanded = false
                                onMonthSelected(summary.month)
                            }
                        )
                    }
                }
            }
            IconButton(onClick = onShare) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.outline_share_24),
                    contentDescription = stringResource(R.string.student_detail_share)
                )
            }
        }

        if (monthSummaries.isEmpty()) {
            EmptyStateMessage(
                message = R.string.no_attendance_records,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
            )
        }

        HorizontalDivider()

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            key(displayMonth) {
                ComposeCalendar(
                    initialMonth = displayMonth,
                    markedDates = markedDates,
                    interactive = false,
                    showHeader = false,
                    onDateSelected = {},
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun MonthDropdownItem(
    summary: MonthPresentSummary,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val summaryDescription = summaryLabel(summary)
    DropdownMenuItem(
        text = { Text(text = monthLabel(summary.month)) },
        onClick = onClick,
        modifier =
            modifier.semantics {
                contentDescription = summaryDescription
            },
        leadingIcon =
            if (isSelected) {
                {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.outline_check_24),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                null
            },
        trailingIcon = {
            Text(text = presentDaysLabel(summary.presentDays))
        }
    )
}

@Composable
private fun summaryLabel(
    summary: MonthPresentSummary?,
    date: LocalDate
): String =
    if (summary != null) {
        summaryLabel(summary)
    } else {
        monthLabel(date)
    }

@Composable
private fun summaryLabel(summary: MonthPresentSummary): String =
    "${monthLabel(summary.month)}, ${presentDaysLabel(summary.presentDays)}"

@Composable
private fun monthLabel(date: LocalDate): String = date.toMonthYearUiText(fullName = true).asString()

@Composable
private fun presentDaysLabel(count: Int): String =
    pluralStringResource(
        R.plurals.present_days_count,
        count,
        count
    )

private data class MonthPresentSummary(
    val month: LocalDate,
    val presentDays: Int
)

private fun todayMonth(): LocalDate {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    return LocalDate(now.year, now.month, 1)
}