package dev.nenoeldeeb.education.absencerecord.presentation.screens.students

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.app.AppViewModelProvider
import dev.nenoeldeeb.education.absencerecord.presentation.screens.components.EmptyStateMessage
import dev.nenoeldeeb.education.absencerecord.presentation.screens.components.StudentList
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.components.MultiSelectionHeader
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.dialogs.BulkDeleteConfirmationDialog
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.dialogs.ImportSelectionDialog
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.dialogs.StudentDialog

@Composable
fun StudentsScreen(
    modifier: Modifier = Modifier, viewModel: StudentsViewModel = viewModel(factory = AppViewModelProvider.factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let {
            Toast.makeText(context, it.asString(context), Toast.LENGTH_LONG).show()
            viewModel.onEvent(StudentsScreenEvent.ConsumeToastMessage)
        }
    }

    BackHandler(uiState.isMultiSelectionMode) {
        viewModel.onEvent(StudentsScreenEvent.ToggleSelectionMode)
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"), onResult = { uri ->
            uri?.let {
                viewModel.onEvent(StudentsScreenEvent.ExportSelectedStudents(it))
            }
        })

    val exportAndDeleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"), onResult = { uri ->
            if (uri != null) {
                viewModel.onEvent(
                    StudentsScreenEvent.ExportAndDeleteSelectedStudents(uri)
                )
            }
        })

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(), onResult = { uri ->
            viewModel.onEvent(StudentsScreenEvent.PrepareImportSelectionDialog(uri))
        })

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            if (uiState.isMultiSelectionMode) {
                MultiSelectionHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    selectedStudentIds = uiState.selectedStudentIds,
                    onCloseSelectionMode = {
                        viewModel.onEvent(StudentsScreenEvent.ToggleSelectionMode)
                    },
                    onToggleSelection = {
                        viewModel.onEvent(StudentsScreenEvent.ToggleStudentsSelection)
                    },
                    onExport = { fileName ->
                        exportLauncher.launch(fileName)
                    },
                    onExportAndDelete = { fileName ->
                        exportAndDeleteLauncher.launch(fileName)
                    },
                    onShowDeleteDialog = {
                        viewModel.onEvent(StudentsScreenEvent.ShowBulkDeleteDialog)
                    })
            }
            if (!uiState.isMultiSelectionMode) {
                Text(
                    stringResource(R.string.students_title, uiState.allStudents.size),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            if (uiState.allStudents.isEmpty()) {
                EmptyStateMessage(
                    message = R.string.no_students_message,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                StudentList(
                    allStudents = uiState.allStudents,
                    selectedStudentIds = uiState.selectedStudentIds,
                    onStudentClick = { student ->
                        if (uiState.isMultiSelectionMode) {
                            viewModel.onEvent(
                                StudentsScreenEvent.ToggleStudentSelection(student.id)
                            )
                        } else {
                            viewModel.onEvent(
                                StudentsScreenEvent.ShowStudentDialog(student)
                            )
                        }
                    },
                    onStudentLongClick = { studentId ->
                        if (!uiState.isMultiSelectionMode) {
                            viewModel.onEvent(StudentsScreenEvent.ToggleSelectionMode)
                            viewModel.onEvent(
                                StudentsScreenEvent.ToggleStudentSelection(studentId)
                            )
                        }
                    })
            }
        }

        FloatingActionButton(
            onClick = { viewModel.onEvent(StudentsScreenEvent.ShowStudentDialog(null, true)) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.outline_add_24),
                contentDescription = stringResource(R.string.new_student_label)
            )
        }
    }

    if (uiState.showEditDialog != null || uiState.showAddStudentDialog) {
        StudentDialog(
            showEditDialog = uiState.showEditDialog,
            newStudentName = uiState.newStudentName,
            onStudentNameChange = { name ->
                viewModel.onEvent(StudentsScreenEvent.UpdateNewStudentName(name))
            },
            onSave = { student, name ->
                if (student != null) {
                    viewModel.onEvent(StudentsScreenEvent.UpdateStudent(student, name))
                } else {
                    viewModel.onEvent(StudentsScreenEvent.AddStudent(name))
                }
            },
            onImportClick = {
                importLauncher.launch(arrayOf("application/json", "*/*"))
            },
            onDismiss = {
                viewModel.onEvent(StudentsScreenEvent.ShowStudentDialog(null, false))
            })
    }

    if (uiState.showImportSelectionDialog) {
        ImportSelectionDialog(
            parsedStudentsFromFile = uiState.parsedStudentsFromFile,
            importSelectionMap = uiState.importSelectionMap,
            onToggleSelection = { itemId ->
                viewModel.onEvent(StudentsScreenEvent.ToggleImportSelection(itemId))
            },
            onSelectAll = { shouldSelect ->
                val items = uiState.parsedStudentsFromFile ?: emptyList()
                items.forEach { item ->
                    if (uiState.importSelectionMap[item.id] != shouldSelect) {
                        viewModel.onEvent(StudentsScreenEvent.ToggleImportSelection(item.id))
                    }
                }
            },
            onImport = {
                viewModel.onEvent(StudentsScreenEvent.PerformImport)
                viewModel.onEvent(StudentsScreenEvent.ShowStudentDialog(null, false))
            },
            onDismiss = {
                viewModel.onEvent(StudentsScreenEvent.CloseImportSelectionDialog)
            })
    }

    if (uiState.showBulkDeleteDialog) {
        BulkDeleteConfirmationDialog(selectedStudentIds = uiState.selectedStudentIds, onConfirmDelete = {
            viewModel.onEvent(StudentsScreenEvent.DeleteSelectedStudents)
        }, onDismiss = {
            viewModel.onEvent(StudentsScreenEvent.DismissBulkDeleteDialog)
        })
    }
}
