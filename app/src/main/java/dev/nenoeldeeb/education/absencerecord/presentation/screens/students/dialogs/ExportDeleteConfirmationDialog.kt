package dev.nenoeldeeb.education.absencerecord.presentation.screens.students.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.presentation.screens.components.ConfirmActionDialog

@Composable
internal fun ExportDeleteConfirmationDialog(
    selectedCount: Int,
    modifier: Modifier = Modifier,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ConfirmActionDialog(
        title = stringResource(R.string.export_delete_confirmation_title),
        body =
            pluralStringResource(
                R.plurals.export_delete_confirmation_message,
                selectedCount,
                selectedCount
            ),
        confirmLabel = stringResource(R.string.action_export_delete),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        modifier = modifier
    )
}