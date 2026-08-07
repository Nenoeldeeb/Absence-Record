package dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import dev.nenoeldeeb.education.absencerecord.R

@Composable
internal fun DeleteConfirmationDialog(
    modifier: Modifier = Modifier,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                ImageVector.vectorResource(id = R.drawable.outline_warning_24),
                stringResource(R.string.student_detail_delete_title)
            )
        },
        title = { Text(stringResource(R.string.student_detail_delete_title)) },
        text = { Text(stringResource(R.string.student_detail_delete_message)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
            ) {
                Text(
                    stringResource(R.string.action_delete),
                    color = MaterialTheme.colorScheme.onError
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        modifier = modifier
    )
}