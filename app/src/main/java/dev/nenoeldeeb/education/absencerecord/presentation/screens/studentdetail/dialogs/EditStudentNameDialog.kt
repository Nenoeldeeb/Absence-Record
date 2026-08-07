package dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.dialogs

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import dev.nenoeldeeb.education.absencerecord.R

@Composable
internal fun EditStudentNameDialog(
    currentName: String,
    modifier: Modifier = Modifier,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember(currentName) { mutableStateOf(currentName) }
    val trimmedName = name.trim()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.student_detail_edit_name_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.student_name_label)) },
                singleLine = true,
                textStyle = TextStyle(textDirection = TextDirection.Content),
                keyboardOptions =
                    KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done
                    ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(trimmedName) },
                enabled = trimmedName.isNotEmpty(),
                modifier = Modifier.height(48.dp)
            ) {
                Text(stringResource(R.string.action_save))
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