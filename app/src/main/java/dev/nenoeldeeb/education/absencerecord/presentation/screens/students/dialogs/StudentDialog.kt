package dev.nenoeldeeb.education.absencerecord.presentation.screens.students.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.Student

@Composable
internal fun StudentDialog(
    showEditDialog: Student?,
    newStudentName: String,
    modifier: Modifier = Modifier,
    onStudentNameChange: (String) -> Unit,
    onSave: (Student?, String) -> Unit,
    onImportClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val isEditMode = showEditDialog != null
    val dialogTitle = if (isEditMode) {
        stringResource(R.string.edit_student)
    } else {
        stringResource(R.string.new_student_label)
    }

    AlertDialog(
        onDismissRequest = onDismiss, title = { Text(dialogTitle) }, text = {
            Column {
                OutlinedTextField(
                    value = newStudentName,
                    onValueChange = onStudentNameChange,
                    label = { Text(stringResource(R.string.student_name_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done
                    ),
                    textStyle = TextStyle(textDirection = TextDirection.Content),
                    modifier = Modifier.fillMaxWidth()
                )

                if (!isEditMode) {
                    Button(
                        onClick = onImportClick, modifier = Modifier
                            .padding(top = 16.dp)
                            .fillMaxWidth()
                            .wrapContentWidth()
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.outline_file_upload_24),
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(stringResource(R.string.import_students_action))
                    }
                }
            }
        }, confirmButton = {
            Button(
                enabled = newStudentName.isNotBlank(), onClick = {
                    onSave(showEditDialog, newStudentName)
                }) {
                Text(
                    stringResource(
                        if (isEditMode) R.string.action_save else R.string.action_add
                    )
                )
            }
        }, dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }, modifier = modifier
    )
}
