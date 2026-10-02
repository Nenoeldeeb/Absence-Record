package dev.nenoeldeeb.education.absencerecord.presentation.screens.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentClass

@Composable
internal fun ClassCheckboxFilter(
    availableClasses: List<StudentClass>,
    selectedClassIds: Set<Int>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onClassToggle: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onClearFilters: (() -> Unit)? = null
) {
    val label =
        if (availableClasses.isEmpty()) {
            stringResource(R.string.class_filter_no_classes)
        } else {
            when (selectedClassIds.size) {
                0 -> stringResource(R.string.class_filter_none)
                1 -> stringResource(R.string.class_filter_one)
                else -> stringResource(R.string.class_filter_n, selectedClassIds.size)
            }
        }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { onExpandedChange(!expanded) },
                modifier = Modifier.weight(1f).sizeIn(minHeight = 48.dp),
                enabled = availableClasses.isNotEmpty()
            ) {
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Start,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (selectedClassIds.isNotEmpty() && onClearFilters != null) {
                IconButton(
                    onClick = onClearFilters,
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.outline_close_24),
                        contentDescription = stringResource(R.string.clear_class_filter),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            if (availableClasses.isEmpty()) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.no_items_available)) },
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.sizeIn(minHeight = 48.dp)
                )
            }
            availableClasses.forEach { cls ->
                val isChecked = cls.id in selectedClassIds
                val checkboxContentDescription =
                    stringResource(
                        R.string.class_filter_checkbox_content_description,
                        cls.name,
                        stringResource(
                            if (isChecked) {
                                R.string.class_filter_checkbox_checked
                            } else {
                                R.string.class_filter_checkbox_unchecked
                            }
                        )
                    )
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = null,
                                modifier = Modifier.clearAndSetSemantics { }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = cls.name,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    onClick = { onClassToggle(cls.id) },
                    modifier =
                        Modifier.sizeIn(minHeight = 48.dp).semantics {
                            contentDescription = checkboxContentDescription
                        }
                )
            }
        }
    }
}