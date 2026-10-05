package dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.nenoeldeeb.education.absencerecord.R
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MonthControlsRow(
    displaySummary: MonthPresentSummary?,
    displayMonth: LocalDate,
    monthSummaries: List<MonthPresentSummary>,
    onMonthSelected: (LocalDate) -> Unit,
    shareEnabled: Boolean,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Local to this row so expanding the menu never recomposes the calendar grid.
    var dropdownExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = modifier,
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
                label = { Text(selectMonthHint) },
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
        IconButton(
            onClick = onShare,
            enabled = shareEnabled
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.outline_share_24),
                contentDescription = stringResource(R.string.student_detail_share)
            )
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
            Text(
                text = presentDaysLabel(summary.presentDays),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
    )
}