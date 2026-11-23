package dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.presentation.screens.components.ConfirmActionDialog

@Composable
internal fun DeleteConfirmationDialog(
    modifier: Modifier = Modifier,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ConfirmActionDialog(
        title = stringResource(R.string.student_detail_delete_title),
        body = stringResource(R.string.student_detail_delete_message),
        confirmLabel = stringResource(R.string.action_delete),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        modifier = modifier
    )
}