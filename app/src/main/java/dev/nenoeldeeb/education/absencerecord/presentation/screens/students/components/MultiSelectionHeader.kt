package dev.nenoeldeeb.education.absencerecord.presentation.screens.students.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.nenoeldeeb.education.absencerecord.R

@Composable
internal fun MultiSelectionHeader(
    selectedStudentIds: Set<Int>,
    modifier: Modifier = Modifier,
    totalCount: Int = 0,
    onCloseSelectionMode: () -> Unit,
    onToggleSelection: () -> Unit,
    onExport: (String) -> Unit,
    onExportAndDelete: () -> Unit,
    onShowDeleteDialog: () -> Unit
) {
    val res = LocalResources.current
    val hasSelection = selectedStudentIds.isNotEmpty()
    val countText =
        if (totalCount > 0) {
            stringResource(
                R.string.selection_count_selected_with_total,
                selectedStudentIds.size,
                totalCount
            )
        } else {
            stringResource(R.string.selection_count_selected, selectedStudentIds.size)
        }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f, fill = false),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onCloseSelectionMode,
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.outline_close_24),
                    contentDescription = stringResource(R.string.action_close)
                )
            }
            Text(
                text = countText,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier =
                    Modifier
                        .weight(1f, fill = false)
                        .padding(start = 8.dp)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onToggleSelection,
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.outline_select_all_24),
                    contentDescription = stringResource(R.string.toggle_students_selection)
                )
            }
            IconButton(
                enabled = hasSelection,
                onClick = {
                    onExport(defaultExportFileName(res))
                },
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.outline_file_upload_24),
                    contentDescription = stringResource(R.string.action_export)
                )
            }
            var overflowExpanded by remember { mutableStateOf(false) }
            Box {
                IconButton(
                    enabled = hasSelection,
                    onClick = { overflowExpanded = true },
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                ) {
                    Icon(
                        imageVector =
                            ImageVector.vectorResource(id = R.drawable.outline_more_vert_24),
                        contentDescription = stringResource(R.string.more_options_description)
                    )
                }
                DropdownMenu(
                    expanded = overflowExpanded,
                    onDismissRequest = { overflowExpanded = false }
                ) {
                    DropdownMenuItem(
                        enabled = hasSelection,
                        text = { Text(stringResource(R.string.action_export_delete)) },
                        leadingIcon = {
                            Icon(
                                imageVector =
                                    ImageVector.vectorResource(
                                        id = R.drawable.outline_person_remove_24
                                    ),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            overflowExpanded = false
                            onExportAndDelete()
                        }
                    )
                    DropdownMenuItem(
                        enabled = hasSelection,
                        text = { Text(stringResource(R.string.action_delete)) },
                        leadingIcon = {
                            Icon(
                                imageVector =
                                    ImageVector.vectorResource(id = R.drawable.outline_delete_24),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            overflowExpanded = false
                            onShowDeleteDialog()
                        }
                    )
                }
            }
        }
    }
}