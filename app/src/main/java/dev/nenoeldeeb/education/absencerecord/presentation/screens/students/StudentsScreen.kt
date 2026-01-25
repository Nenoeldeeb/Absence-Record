@file:OptIn(ExperimentalTime::class)

package dev.nenoeldeeb.education.absencerecord.presentation.screens.students

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.nenoeldeeb.education.absencerecord.R
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Composable
fun StudentsScreen(viewModel: StudentsViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let {
            Toast.makeText(context, it.asString(context), Toast.LENGTH_LONG).show()
            viewModel.onEvent(StudentsScreenEvent.ConsumeToastMessage)
        }
    }

    BackHandler(uiState.isMultiSelectionMode) {
        viewModel.onEvent(StudentsScreenEvent.ClearSelection)
    }

    val exportLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/json"),
            onResult = { uri ->
                uri?.let {
                    viewModel.onEvent(StudentsScreenEvent.ExportSelectedStudents(it))
                }
            }
        )

    val exportAndDeleteLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/json"),
            onResult = { uri ->
                if (uri != null) {
                    viewModel.onEvent(
                        StudentsScreenEvent.ExportAndDeleteSelectedStudents(uri)
                    )
                }
            }
        )

    val importLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
            onResult = { uri ->
                viewModel.onEvent(StudentsScreenEvent.PrepareImportSelectionDialog(uri))
            }
        )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(8.dp)
        ) {
            if (uiState.isMultiSelectionMode) {
                MultiSelectionHeader(
                    uiState,
                    viewModel,
                    context,
                    { exportLauncher.launch(it) },
                    { exportAndDeleteLauncher.launch(it) }
                )
            }
            if (!uiState.isMultiSelectionMode) {
                Text(
                    stringResource(R.string.students_title, uiState.allStudents.size),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            if (uiState.allStudents.isEmpty()) {
                EmptyStateMessage()
            } else {
                StudentList(uiState, viewModel)
            }
        }

        FloatingActionButton(
            onClick = { viewModel.onEvent(StudentsScreenEvent.ShowStudentDialog(null, true)) },
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.outline_add_24),
                contentDescription = stringResource(R.string.new_student_label)
            )
        }
    }

    if (uiState.showEditDialog != null || uiState.showAddStudentDialog) {
        StudentDialog(uiState, viewModel, { importLauncher.launch(it) })
    }

    if (uiState.showImportSelectionDialog) {
        ImportSelectionDialog(uiState, viewModel)
    }

    if (uiState.showBulkDeleteDialog) {
        BulkDeleteConfirmationDialog(uiState, viewModel)
    }
}

@Composable
internal fun MultiSelectionHeader(
    uiState: StudentsScreenState,
    viewModel: StudentsViewModel,
    context: Context,
    onExport: (String) -> Unit,
    onExportAndDelete: (String) -> Unit
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.onEvent(StudentsScreenEvent.ClearSelection) }) {
                Icon(
                    painter = painterResource(id = R.drawable.outline_close_24),
                    contentDescription = stringResource(R.string.action_close)
                )
            }
            Text(
                text = "${uiState.selectedStudentIds.size}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        Row {
            IconButton(onClick = { viewModel.onEvent(StudentsScreenEvent.SelectAllStudents) }) {
                Icon(
                    painter = painterResource(id = R.drawable.outline_select_all_24),
                    contentDescription = stringResource(R.string.select_all_students)
                )
            }
            IconButton(
                onClick = {
                    val timestamp =
                        Clock.System.now()
                            .toLocalDateTime(TimeZone.currentSystemDefault())
                            .run {
                                "${year}${
                                    month.number.toString().padStart(2, '0')
                                }${day.toString().padStart(2, '0')}"
                            }
                    val defaultFileName =
                        context.getString(R.string.absencerecord_export_json, timestamp)
                    onExport(defaultFileName)
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.outline_file_upload_24),
                    contentDescription = stringResource(R.string.action_export)
                )
            }
            IconButton(
                onClick = {
                    val now =
                        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                    val timestamp =
                        "${now.year}${
                            now.month.number.toString().padStart(2, '0')
                        }${now.day.toString().padStart(2, '0')}_${
                            now.hour.toString().padStart(2, '0')
                        }${now.minute.toString().padStart(2, '0')}${
                            now.second.toString().padStart(2, '0')
                        }"
                    val defaultFileName =
                        context.getString(R.string.absencerecord_export_json, timestamp)
                    onExportAndDelete(defaultFileName)
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.outline_file_upload_24),
                    // Reuse export icon? Or maybe a combined one.
                    contentDescription = stringResource(R.string.action_export_delete),
                    tint = MaterialTheme.colorScheme.error
                )
            }
            IconButton(onClick = { viewModel.onEvent(StudentsScreenEvent.ShowBulkDeleteDialog) }) {
                Icon(
                    painter = painterResource(id = R.drawable.outline_delete_24),
                    contentDescription = stringResource(R.string.action_delete),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
internal fun EmptyStateMessage() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(R.string.no_students_message),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
internal fun StudentList(
    uiState: StudentsScreenState,
    viewModel: StudentsViewModel
) {
    LazyColumn {
        items(uiState.allStudents, key = { it.id }) { student ->
            val isSelected = uiState.selectedStudentIds.contains(student.id)
            ListItem(
                modifier =
                    Modifier.combinedClickable(
                        onClick = {
                            if (uiState.isMultiSelectionMode) {
                                viewModel.onEvent(
                                    StudentsScreenEvent
                                        .ToggleStudentSelection(
                                            student.id
                                        )
                                )
                            } else {
                                viewModel.onEvent(
                                    StudentsScreenEvent.ShowStudentDialog(
                                        student
                                    )
                                )
                            }
                        },
                        onLongClick = {
                            if (!uiState.isMultiSelectionMode) {
                                viewModel.onEvent(
                                    StudentsScreenEvent.ToggleSelectionMode
                                )
                                viewModel.onEvent(
                                    StudentsScreenEvent
                                        .ToggleStudentSelection(
                                            student.id
                                        )
                                )
                            }
                        }
                    ),
                headlineContent = {
                    Text(
                        student.name,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .wrapContentWidth()
                    )
                },
                leadingContent = {
                    if (uiState.isMultiSelectionMode) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = {
                                viewModel.onEvent(
                                    StudentsScreenEvent.ToggleStudentSelection(
                                        student.id
                                    )
                                )
                            }
                        )
                    }
                },
                colors =
                    ListItemDefaults.colors(
                        containerColor =
                            if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                ListItemDefaults.colors().containerColor
                            }
                    )
            )
        }
    }
}

@Composable
internal fun StudentDialog(
    uiState: StudentsScreenState,
    viewModel: StudentsViewModel,
    importLauncher: (Array<String>) -> Unit
) {
    val isEditMode = uiState.showEditDialog != null
    val dialogTitle =
        if (isEditMode) {
            stringResource(R.string.edit_student)
        } else {
            stringResource(R.string.new_student_label)
        }

    AlertDialog(
        onDismissRequest = {
            viewModel.onEvent(StudentsScreenEvent.ShowStudentDialog(null, false))
        },
        title = { Text(dialogTitle) },
        text = {
            Column {
                OutlinedTextField(
                    value = uiState.newStudentName,
                    onValueChange = {
                        viewModel.onEvent(StudentsScreenEvent.UpdateNewStudentName(it))
                    },
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

                if (!isEditMode) {
                    Button(
                        onClick = { importLauncher(arrayOf("application/json", "*/*")) },
                        modifier =
                            Modifier
                                .padding(top = 16.dp)
                                .fillMaxWidth()
                                .wrapContentWidth()
                    ) {
                        Icon(
                            painter =
                                painterResource(id = R.drawable.outline_file_upload_24),
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
                enabled = uiState.newStudentName.isNotBlank(),
                onClick = {
                    if (isEditMode) {
                        viewModel.onEvent(
                            StudentsScreenEvent.UpdateStudent(
                                uiState.showEditDialog,
                                uiState.newStudentName
                            )
                        )
                    } else {
                        viewModel.onEvent(
                            StudentsScreenEvent.AddStudent(uiState.newStudentName)
                        )
                    }
                }
            ) {
                Text(
                    stringResource(
                        if (isEditMode) R.string.action_save else R.string.action_add
                    )
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    viewModel.onEvent(StudentsScreenEvent.ShowStudentDialog(null, false))
                }
            ) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@Composable
internal fun ImportSelectionDialog(
    uiState: StudentsScreenState,
    viewModel: StudentsViewModel
) {
    val items = uiState.parsedStudentsFromFile ?: emptyList()
    val selectionMap = uiState.importSelectionMap
    val allSelected = items.isNotEmpty() && items.all { selectionMap[it.id] == true }
    val noneSelected = items.none { selectionMap[it.id] == true }

    AlertDialog(
        onDismissRequest = {
            viewModel.onEvent(StudentsScreenEvent.CloseImportSelectionDialog)
        },
        title = { Text(stringResource(R.string.select_students_import_title)) },
        text = {
            Column {
                Text(
                    pluralStringResource(
                        R.plurals.import_preview_message,
                        uiState.parsedStudentsFromFile?.size ?: 0,
                        uiState.parsedStudentsFromFile?.size ?: 0
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable(enabled = items.isNotEmpty()) {
                                val targetState = !allSelected
                                items.forEach { item ->
                                    if (selectionMap[item.id] != targetState) {
                                        viewModel.onEvent(
                                            StudentsScreenEvent
                                                .ToggleImportSelection(
                                                    item.id
                                                )
                                        )
                                    }
                                }
                            }
                            .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = allSelected,
                        onCheckedChange = { shouldBeChecked ->
                            items.forEach { item ->
                                if (selectionMap[item.id] != shouldBeChecked) {
                                    viewModel.onEvent(
                                        StudentsScreenEvent.ToggleImportSelection(
                                            item.id
                                        )
                                    )
                                }
                            }
                        },
                        enabled = items.isNotEmpty()
                    )
                    Text(
                        stringResource(R.string.select_all_students),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                HorizontalDivider()
                LazyColumn(modifier = Modifier.height(300.dp)) {
                    if (items.isEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.no_items_available),
                                modifier = Modifier.padding(16.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        items(items, key = { it.id }) { item ->
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.onEvent(
                                                StudentsScreenEvent
                                                    .ToggleImportSelection(
                                                        item.id
                                                    )
                                            )
                                        }
                                        .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = selectionMap[item.id] == true,
                                    onCheckedChange = {
                                        viewModel.onEvent(
                                            StudentsScreenEvent.ToggleImportSelection(
                                                item.id
                                            )
                                        )
                                    }
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    item.originalData.name,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    stringResource(
                                        R.string.imported_dates_count,
                                        item.originalData.dates.size
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.onEvent(StudentsScreenEvent.PerformImport)
                    viewModel.onEvent(StudentsScreenEvent.ShowStudentDialog(null, false))
                          },
                enabled = !noneSelected
            ) { Text(stringResource(R.string.import_selected)) }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    viewModel.onEvent(StudentsScreenEvent.CloseImportSelectionDialog)
                }
            ) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@Composable
internal fun BulkDeleteConfirmationDialog(
    uiState: StudentsScreenState,
    viewModel: StudentsViewModel
) {
    AlertDialog(
        onDismissRequest = { viewModel.onEvent(StudentsScreenEvent.DismissBulkDeleteDialog) },
        icon = {
            Icon(
                painterResource(id = R.drawable.outline_warning_24),
                stringResource(R.string.bulk_delete_confirmation_title)
            )
        },
        title = { Text(stringResource(R.string.bulk_delete_confirmation_title)) },
        text = {
            Text(
                pluralStringResource(
                    R.plurals.bulk_delete_confirmation_message,
                    uiState.selectedStudentIds.size,
                    uiState.selectedStudentIds.size
                )
            )
        },
        confirmButton = {
            Button(
                onClick = { viewModel.onEvent(StudentsScreenEvent.DeleteSelectedStudents) },
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
            TextButton(
                onClick = { viewModel.onEvent(StudentsScreenEvent.DismissBulkDeleteDialog) }
            ) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}