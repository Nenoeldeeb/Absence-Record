package dev.nenoeldeeb.education.absencerecord.presentation.screens.report.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.SortType
import dev.nenoeldeeb.education.absencerecord.presentation.utils.DateFormatter.toMonthYearUiText
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReportControls(
    selectedMonth: LocalDate?,
    availableMonths: List<LocalDate>,
    monthDropdownExpanded: Boolean,
    sortType: SortType,
    modifier: Modifier = Modifier,
    onMonthSelected: (LocalDate) -> Unit,
    onMonthCleared: () -> Unit,
    onMonthDropdownToggled: (Boolean) -> Unit,
    onSortTypeToggled: () -> Unit
) {
    Column(modifier = modifier) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ExposedDropdownMenuBox(
                expanded = monthDropdownExpanded,
                onExpandedChange = onMonthDropdownToggled,
                modifier = Modifier.weight(1.6f)
            ) {
                OutlinedTextField(
                    value =
                        selectedMonth?.toMonthYearUiText(fullName = true)?.asString()
                            ?: stringResource(R.string.all_months),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.filter_by_month)) },
                    trailingIcon = {
                        selectedMonth?.let {
                            IconButton(onClick = {
                                onMonthCleared()
                                onMonthDropdownToggled(false)
                            }) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(id = R.drawable.outline_close_24),
                                    contentDescription = stringResource(R.string.clear_month_filter)
                                )
                            }
                        } ?: ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthDropdownExpanded)
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = monthDropdownExpanded,
                    onDismissRequest = { onMonthDropdownToggled(false) }
                ) {
                    if (availableMonths.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.no_data_for_months)) },
                            onClick = {},
                            enabled = false
                        )
                    }
                    availableMonths.forEach { month ->
                        DropdownMenuItem(
                            text = { Text(month.toMonthYearUiText(fullName = true).asString()) },
                            onClick = {
                                onMonthSelected(month)
                                onMonthDropdownToggled(false)
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onSortTypeToggled,
                modifier = Modifier.weight(1f)
            ) {
                val sortActionText =
                    if (sortType == SortType.ByName) {
                        stringResource(R.string.sort_action_rate)
                    } else {
                        stringResource(R.string.sort_action_name)
                    }
                Text(sortActionText)
            }
        }
        Text(
            text =
                stringResource(
                    R.string.students_sort_title,
                    stringResource(
                        if (sortType == SortType.ByName) {
                            R.string.sort_by_name
                        } else {
                            R.string.sort_by_attendance
                        }
                    )
                ),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}