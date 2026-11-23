package dev.nenoeldeeb.education.absencerecord.presentation.screens.components

import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.ui.unit.dp
import dev.nenoeldeeb.education.absencerecord.R

/**
 * Reusable confirmation dialog for delete/destructive actions.
 * Encapsulates the warning icon, dialog layout, button styling, and 48dp minimum touch target.
 */
@Composable
fun ConfirmActionDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    useTextButtonForConfirm: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                ImageVector.vectorResource(id = R.drawable.outline_warning_24),
                contentDescription = null
            )
        },
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            if (useTextButtonForConfirm) {
                TextButton(
                    onClick = onConfirm,
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                    colors =
                        ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                ) {
                    Text(confirmLabel)
                }
            } else {
                Button(
                    onClick = onConfirm,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                ) {
                    Text(
                        confirmLabel,
                        color = MaterialTheme.colorScheme.onError
                    )
                }
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