package dev.nenoeldeeb.education.absencerecord.presentation.screens.students.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import dev.nenoeldeeb.education.absencerecord.R
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

@Composable
internal fun MultiSelectionHeader(
    selectedStudentIds: Set<Int>,
    modifier: Modifier = Modifier,
    onCloseSelectionMode: () -> Unit,
    onToggleSelection: () -> Unit,
    onExport: (String) -> Unit,
    onExportAndDelete: (String) -> Unit,
    onShowDeleteDialog: () -> Unit
) {
    val res = LocalResources.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onCloseSelectionMode) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.outline_close_24),
                    contentDescription = stringResource(R.string.action_close)
                )
            }
            Text(
                text = "${selectedStudentIds.size}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        Row {
            IconButton(
                onClick = onToggleSelection
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.outline_select_all_24),
                    contentDescription = stringResource(R.string.toggle_students_selection)
                )
            }
            IconButton(
                enabled = selectedStudentIds.isNotEmpty(),
                onClick = {
                    val defaultFileName =
                        res.getString(
                            R.string.absencerecord_export_json,
                            timestamp()
                        )
                    onExport(defaultFileName)
                }
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.outline_file_upload_24),
                    contentDescription = stringResource(R.string.action_export)
                )
            }
            IconButton(
                enabled = selectedStudentIds.isNotEmpty(),
                onClick = {
                    val defaultFileName =
                        res.getString(
                            R.string.absencerecord_export_json,
                            timestamp()
                        )
                    onExportAndDelete(defaultFileName)
                }
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.outline_file_upload_24),
                    contentDescription = stringResource(R.string.action_export_delete),
                    tint =
                        if (selectedStudentIds.isNotEmpty()) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        }
                )
            }
            IconButton(
                enabled = selectedStudentIds.isNotEmpty(),
                onClick = onShowDeleteDialog
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.outline_delete_24),
                    contentDescription = stringResource(R.string.action_delete),
                    tint =
                        if (selectedStudentIds.isNotEmpty()) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        }
                )
            }
        }
    }
}

private fun timestamp(): String {
    return Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).run {
        "${year}${
            month.number.toString().padStart(2, '0')
        }${day.toString().padStart(2, '0')}"
    }
}