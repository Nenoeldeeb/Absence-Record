package dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.presentation.screens.components.ConfirmActionDialog

@Composable
internal fun ScheduleConfirmDialog(
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
    ConfirmActionDialog(
        title = stringResource(R.string.schedule_delete_hour_named, hourLabel),
        body = message,
        confirmLabel = stringResource(R.string.action_delete),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        useTextButtonForConfirm = true,
        modifier = modifier
    )
}