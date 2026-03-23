package dev.nenoeldeeb.education.absencerecord.presentation.screens.students.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
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
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentClass

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StudentDialog(
    showEditDialog: Student?,
    newStudentName: String,
    availableClasses: List<StudentClass>,
    modifier: Modifier = Modifier,
    onStudentNameChange: (String) -> Unit,
    onSave: (Student?, String, Int?) -> Unit,
    onImportClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val isEditMode = showEditDialog != null
    val dialogTitle =
        if (isEditMode) {
            stringResource(R.string.edit_student)
        } else {
            stringResource(R.string.new_student_label)
        }

    // Initialize class selection from the student being edited (or null = unassigned)
    var selectedClass by remember(showEditDialog) {
        mutableStateOf(availableClasses.find { it.id == showEditDialog?.classId })
    }
    var classDropdownExpanded by rememberSaveable { mutableStateOf(false) }

    val classLabel = selectedClass?.name ?: stringResource(R.string.class_unassigned_label)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dialogTitle) },
        text = {
            Column {
                OutlinedTextField(
                    value = newStudentName,
                    onValueChange = onStudentNameChange,
                    label = { Text(stringResource(R.string.student_name_label)) },
                    singleLine = true,
                    keyboardOptions =
                        KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Done
                        ),
                    textStyle = TextStyle(textDirection = TextDirection.Content),
                    modifier = Modifier.fillMaxWidth()
                )

                // Class assignment dropdown (only when classes exist)
                if (availableClasses.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = classDropdownExpanded,
                        onExpandedChange = { classDropdownExpanded = it },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                    ) {
                        OutlinedTextField(
                            value = classLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.class_filter_label)) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = classDropdownExpanded
                                )
                            },
                            modifier =
                                Modifier
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = classDropdownExpanded,
                            onDismissRequest = { classDropdownExpanded = false }
                        ) {
                            // Unassigned option
                            DropdownMenuItem(
                                text = {
                                    Text(stringResource(R.string.class_unassigned_label))
                                },
                                onClick = {
                                    selectedClass = null
                                    classDropdownExpanded = false
                                },
                                leadingIcon =
                                    if (selectedClass == null) {
                                        {
                                            Icon(
                                                imageVector =
                                                    ImageVector.vectorResource(
                                                        R.drawable.outline_check_24
                                                    ),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    } else {
                                        null
                                    }
                            )
                            availableClasses.forEach { cls ->
                                DropdownMenuItem(
                                    text = { Text(cls.name) },
                                    onClick = {
                                        selectedClass = cls
                                        classDropdownExpanded = false
                                    },
                                    leadingIcon =
                                        if (selectedClass?.id == cls.id) {
                                            {
                                                Icon(
                                                    imageVector =
                                                        ImageVector.vectorResource(
                                                            R.drawable.outline_check_24
                                                        ),
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        } else {
                                            null
                                        }
                                )
                            }
                        }
                    }
                }

                if (!isEditMode) {
                    Button(
                        onClick = onImportClick,
                        modifier =
                            Modifier
                                .padding(top = 16.dp)
                                .fillMaxWidth()
                                .wrapContentWidth()
                    ) {
                        Icon(
                            imageVector =
                                ImageVector.vectorResource(
                                    id = R.drawable.outline_file_upload_24
                                ),
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(stringResource(R.string.import_students_action))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = newStudentName.isNotBlank(),
                onClick = { onSave(showEditDialog, newStudentName, selectedClass?.id) }
            ) {
                Text(
                    stringResource(
                        if (isEditMode) R.string.action_save else R.string.action_add
                    )
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
        modifier = modifier
    )
}