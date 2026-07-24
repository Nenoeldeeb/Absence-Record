package dev.nenoeldeeb.education.absencerecord.presentation.screens.students.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentClass

@Composable
internal fun ManageClassesDialog(
    classes: List<StudentClass>,
    onAddClass: (String) -> Unit,
    onRenameClass: (StudentClass, String) -> Unit,
    onDeleteClass: (StudentClass) -> Unit,
    onDismiss: () -> Unit
) {
    var newClassName by rememberSaveable { mutableStateOf("") }
    var renamingClassId by rememberSaveable { mutableStateOf<Int?>(null) }
    var renameText by rememberSaveable { mutableStateOf("") }
    var classToDelete by remember { mutableStateOf<StudentClass?>(null) }

    if (classToDelete != null) {
        AlertDialog(
            onDismissRequest = { classToDelete = null },
            title = { Text(stringResource(R.string.delete_class_title)) },
            text = { Text(stringResource(R.string.delete_class_message, classToDelete!!.name)) },
            icon = {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.outline_delete_24),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteClass(classToDelete!!)
                        classToDelete = null
                    }
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.outline_delete_24),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.action_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { classToDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.manage_classes_title)) }, text = {
        Column {
            // ── Add new class ────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = newClassName,
                    onValueChange = { newClassName = it },
                    label = { Text(stringResource(R.string.new_class_label)) },
                    singleLine = true,
                    keyboardOptions =
                        KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Done
                        ),
                    textStyle = TextStyle(textDirection = TextDirection.Content),
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        if (newClassName.isNotBlank()) {
                            onAddClass(newClassName.trim())
                            newClassName = ""
                        }
                    },
                    enabled = newClassName.isNotBlank()
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.outline_add_24),
                        contentDescription = stringResource(R.string.action_add)
                    )
                }
            }

            if (classes.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(4.dp))

                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(classes, key = { it.id }) { cls ->
                        val isRenaming = renamingClassId == cls.id

                        if (isRenaming) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                            ) {
                                OutlinedTextField(
                                    value = renameText,
                                    onValueChange = { renameText = it },
                                    singleLine = true,
                                    keyboardOptions =
                                        KeyboardOptions(
                                            capitalization = KeyboardCapitalization.Words,
                                            imeAction = ImeAction.Done
                                        ),
                                    textStyle = TextStyle(textDirection = TextDirection.Content),
                                    modifier = Modifier.weight(1f)
                                )
                                // Confirm rename
                                IconButton(
                                    onClick = {
                                        if (renameText.isNotBlank()) {
                                            onRenameClass(cls, renameText.trim())
                                        }
                                        renamingClassId = null
                                    },
                                    enabled = renameText.isNotBlank()
                                ) {
                                    Icon(
                                        imageVector =
                                            ImageVector.vectorResource(
                                                R.drawable.outline_check_24
                                            ),
                                        contentDescription = stringResource(R.string.action_save),
                                        tint =
                                            if (renameText.isNotBlank()) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.primaryContainer
                                            },
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                // Cancel rename
                                IconButton(onClick = { renamingClassId = null }) {
                                    Icon(
                                        imageVector =
                                            ImageVector.vectorResource(
                                                R.drawable.outline_close_24
                                            ),
                                        contentDescription = stringResource(R.string.action_cancel),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                            ) {
                                Text(
                                    text = cls.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Content),
                                    modifier = Modifier.weight(1f)
                                )
                                // Edit button
                                IconButton(
                                    onClick = {
                                        renamingClassId = cls.id
                                        renameText = cls.name
                                    }
                                ) {
                                    Icon(
                                        imageVector =
                                            ImageVector.vectorResource(
                                                R.drawable.outline_edit_24
                                            ),
                                        contentDescription = stringResource(R.string.action_edit),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                // Delete button
                                IconButton(onClick = { classToDelete = cls }) {
                                    Icon(
                                        imageVector =
                                            ImageVector.vectorResource(
                                                R.drawable.outline_delete_24
                                            ),
                                        contentDescription = stringResource(R.string.action_delete),
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }, confirmButton = {}, dismissButton = {
        TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
    })
}