package dev.nenoeldeeb.education.absencerecord.presentation.screens.students.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.presentation.screens.components.ConfirmActionDialog

@Composable
internal fun BulkDeleteConfirmationDialog(
    selectedStudentIds: Set<Int>,
    modifier: Modifier = Modifier,
    onConfirmDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    ConfirmActionDialog(
        title = stringResource(R.string.bulk_delete_confirmation_title),
        body =
            pluralStringResource(
                R.plurals.bulk_delete_confirmation_message,
                selectedStudentIds.size,
                selectedStudentIds.size
            ),
        confirmLabel = stringResource(R.string.action_delete),
        onConfirm = onConfirmDelete,
        onDismiss = onDismiss,
        modifier = modifier
    )
}