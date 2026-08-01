package dev.nenoeldeeb.education.absencerecord.presentation.screens.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
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
    modifier: Modifier = Modifier
) {
    val label =
        when (selectedClassIds.size) {
            0 -> stringResource(R.string.class_filter_none)
            1 -> stringResource(R.string.class_filter_one)
            else -> stringResource(R.string.class_filter_n, selectedClassIds.size)
        }

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { onExpandedChange(!expanded) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Start
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
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
                                onCheckedChange = { onClassToggle(cls.id) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = cls.name)
                        }
                    },
                    onClick = { onClassToggle(cls.id) },
                    modifier =
                        Modifier.semantics {
                            contentDescription = checkboxContentDescription
                        }
                )
            }
        }
    }
}