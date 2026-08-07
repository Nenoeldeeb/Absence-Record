package dev.nenoeldeeb.education.absencerecord.presentation.screens.students.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.SortType
import dev.nenoeldeeb.education.absencerecord.presentation.utils.DateFormatter.toMonthYearUiText
import kotlinx.datetime.LocalDate

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
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            val label =
                selectedMonth?.let { it.toMonthYearUiText(fullName = true).asString() }
                    ?: stringResource(R.string.all_months)
            val selectMonthHint = stringResource(R.string.sort_select_month)
            OutlinedButton(
                onClick = { onMonthDropdownExpandedChange(!monthDropdownExpanded) },
                modifier =
                    Modifier.semantics {
                        contentDescription = "$selectMonthHint: $label"
                    }
            ) {
                Text(text = label)
            }
            DropdownMenu(
                expanded = monthDropdownExpanded,
                onDismissRequest = { onMonthDropdownExpandedChange(false) }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.all_months)) },
                    onClick = { onMonthSelected(null) },
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
        Spacer(modifier = Modifier.width(8.dp))
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
                Modifier.semantics {
                    contentDescription = sortDescription
                }
        ) {
            Text(text = sortLabel, textAlign = TextAlign.Center)
        }
    }
}