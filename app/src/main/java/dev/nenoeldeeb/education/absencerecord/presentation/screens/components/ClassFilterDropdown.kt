package dev.nenoeldeeb.education.absencerecord.presentation.screens.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentClass

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassFilterDropdown(
    selectedFilter: ClassFilter,
    availableClasses: List<StudentClass>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onFilterSelected: (ClassFilter) -> Unit,
    modifier: Modifier = Modifier,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    val label =
        when (selectedFilter) {
            ClassFilter.All -> stringResource(R.string.class_filter_all)
            ClassFilter.Unassigned -> stringResource(R.string.class_filter_unassigned)
            is ClassFilter.ByClass -> selectedFilter.studentClass.name
        }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = onExpandedChange,
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = label,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.class_filter_label)) },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                textStyle = TextStyle(textDirection = TextDirection.Content),
                modifier =
                    Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) }
            ) {
                // All
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.class_filter_all)) },
                    onClick = { onFilterSelected(ClassFilter.All) },
                    leadingIcon =
                        if (selectedFilter == ClassFilter.All) {
                            {
                                Icon(
                                    imageVector =
                                        ImageVector.vectorResource(
                                            R.drawable.outline_check_24
                                        ),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            null
                        }
                )
                // Unassigned
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.class_filter_unassigned)) },
                    onClick = { onFilterSelected(ClassFilter.Unassigned) },
                    leadingIcon =
                        if (selectedFilter == ClassFilter.Unassigned) {
                            {
                                Icon(
                                    imageVector =
                                        ImageVector.vectorResource(
                                            R.drawable.outline_check_24
                                        ),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            null
                        }
                )
                // Named classes
                availableClasses.forEach { cls ->
                    val isSelected = selectedFilter is ClassFilter.ByClass && selectedFilter.studentClass.id == cls.id
                    DropdownMenuItem(
                        text = { Text(text = cls.name) },
                        onClick = { onFilterSelected(ClassFilter.ByClass(cls)) },
                        leadingIcon =
                            if (isSelected) {
                                {
                                    Icon(
                                        imageVector =
                                            ImageVector.vectorResource(
                                                R.drawable.outline_check_24
                                            ),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else {
                                null
                            }
                    )
                }
            }
        }

        trailingIcon?.invoke()
    }
}