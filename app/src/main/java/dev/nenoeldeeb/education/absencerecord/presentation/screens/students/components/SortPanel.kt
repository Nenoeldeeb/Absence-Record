package dev.nenoeldeeb.education.absencerecord.presentation.screens.students.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.SortType
import dev.nenoeldeeb.education.absencerecord.presentation.utils.DateFormatter.toMonthYearUiText
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SortPanel(
    sortType: SortType,
    selectedMonth: LocalDate?,
    availableMonths: List<LocalDate>,
    monthDropdownExpanded: Boolean,
    onMonthDropdownExpandedChange: (Boolean) -> Unit,
    onMonthSelected: (LocalDate?) -> Unit,
    onToggleSortType: () -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.Center,
        maxItemsInEachRow = 2
    ) {
        Row(
            modifier = Modifier.weight(1f, fill = true),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f, fill = true)) {
                val label =
                    selectedMonth?.let { it.toMonthYearUiText(fullName = true).asString() }
                        ?: stringResource(R.string.all_months)
                val selectMonthHint = stringResource(R.string.sort_select_month)
                val monthButtonModifier =
                    Modifier
                        .fillMaxWidth()
                        .sizeIn(minHeight = 48.dp)
                        .semantics(mergeDescendants = true) {
                            contentDescription = "$selectMonthHint: $label"
                            selected = selectedMonth != null
                        }
                // Same active grammar as the top bar: tonal fill means
                // "a narrowing choice is on". All-months stays outlined.
                if (selectedMonth != null) {
                    FilledTonalButton(
                        onClick = { onMonthDropdownExpandedChange(!monthDropdownExpanded) },
                        modifier = monthButtonModifier
                    ) {
                        Text(
                            text = label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Icon(
                            imageVector =
                                ImageVector.vectorResource(R.drawable.outline_expand_more_24),
                            contentDescription = null
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = { onMonthDropdownExpandedChange(!monthDropdownExpanded) },
                        modifier = monthButtonModifier
                    ) {
                        Text(
                            text = label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Icon(
                            imageVector =
                                ImageVector.vectorResource(R.drawable.outline_expand_more_24),
                            contentDescription = null
                        )
                    }
                }
                DropdownMenu(
                    expanded = monthDropdownExpanded,
                    onDismissRequest = { onMonthDropdownExpandedChange(false) }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.all_months)) },
                        onClick = { onMonthSelected(null) },
                        modifier = Modifier.semantics { selected = selectedMonth == null },
                        leadingIcon =
                            if (selectedMonth == null) {
                                {
                                    Icon(
                                        imageVector =
                                            ImageVector.vectorResource(R.drawable.outline_check_24),
                                        contentDescription = null
                                    )
                                }
                            } else {
                                null
                            }
                    )
                    availableMonths.forEach { month ->
                        DropdownMenuItem(
                            text = {
                                Text(text = month.toMonthYearUiText(fullName = true).asString())
                            },
                            onClick = { onMonthSelected(month) },
                            modifier = Modifier.semantics { selected = month == selectedMonth },
                            leadingIcon =
                                if (month == selectedMonth) {
                                    {
                                        Icon(
                                            imageVector =
                                                ImageVector.vectorResource(R.drawable.outline_check_24),
                                            contentDescription = null
                                        )
                                    }
                                } else {
                                    null
                                }
                        )
                    }
                }
            }
            if (selectedMonth != null) {
                IconButton(
                    onClick = { onMonthSelected(null) },
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                ) {
                    Icon(
                        imageVector =
                            ImageVector.vectorResource(R.drawable.outline_close_24),
                        contentDescription = stringResource(R.string.clear_month_filter)
                    )
                }
            }
        }
        val sortLabel =
            stringResource(
                if (sortType == SortType.ByName) {
                    R.string.sort_by_name
                } else {
                    R.string.sort_by_attendance
                }
            )
        val sortDescription =
            stringResource(R.string.sort_toggle_description, sortLabel)
        OutlinedButton(
            onClick = onToggleSortType,
            modifier =
                Modifier
                    .weight(1f, fill = true)
                    .sizeIn(minHeight = 48.dp)
                    .semantics(mergeDescendants = true) {
                        contentDescription = sortDescription
                    }
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.outline_sort_24),
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = sortLabel,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}