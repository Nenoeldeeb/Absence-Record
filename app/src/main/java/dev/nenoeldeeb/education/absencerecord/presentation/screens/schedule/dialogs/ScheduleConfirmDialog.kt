package dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule.dialogs

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import dev.nenoeldeeb.education.absencerecord.R

@Composable
fun ScheduleConfirmDialog(
    hourLabel: String,
    assignedStudentNames: List<String>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val message =
        if (assignedStudentNames.isEmpty()) {
            stringResource(R.string.schedule_delete_hour_message_empty)
        } else {
            pluralStringResource(
                R.plurals.schedule_delete_hour_message_with_students,
                assignedStudentNames.size,
                assignedStudentNames.size,
                assignedStudentNames.joinToString(", ")
            )
        }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                ImageVector.vectorResource(id = R.drawable.outline_warning_24),
                contentDescription = null
            )
        },
        title = { Text(stringResource(R.string.schedule_delete_hour_named, hourLabel)) },
        text = { Text(message) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                colors =
                    androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
            ) {
                Text(stringResource(R.string.action_delete))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        modifier = modifier
    )
}