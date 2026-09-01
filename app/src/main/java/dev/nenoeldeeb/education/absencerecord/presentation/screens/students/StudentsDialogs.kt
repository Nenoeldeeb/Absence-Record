package dev.nenoeldeeb.education.absencerecord.presentation.screens.students

import androidx.compose.runtime.Composable
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.dialogs.BulkDeleteConfirmationDialog
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.dialogs.ImportSelectionDialog
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.dialogs.ManageClassesDialog
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.dialogs.StudentDialog

@Composable
fun StudentsDialogs(
    uiState: StudentsScreenState,
    onEvent: (StudentsScreenEvent) -> Unit,
    onImportClick: () -> Unit
) {
    if (uiState.showAddStudentDialog) {
        StudentDialog(
            newStudentName = uiState.newStudentName,
            availableClasses = uiState.availableClasses,
            onStudentNameChange = {
                onEvent(StudentsScreenEvent.UpdateNewStudentName(it))
            },
            onSave = { name, classId ->
                onEvent(StudentsScreenEvent.AddStudent(name, classId))
            },
            onImportClick = onImportClick,
            onDismiss = {
                onEvent(StudentsScreenEvent.ShowStudentDialog(false))
            }
        )
    }

    if (uiState.showImportSelectionDialog) {
        val parsedStudents = uiState.parsedImportData?.students.orEmpty()
        ImportSelectionDialog(
            parsedStudentsFromFile = parsedStudents,
            importSelectionMap = uiState.importSelectionMap,
            onToggleSelection = {
                onEvent(StudentsScreenEvent.ToggleImportSelection(it))
            },
            onSelectAll = { shouldSelect ->
                parsedStudents.forEach { item ->
                    if (uiState.importSelectionMap[item.id] != shouldSelect) {
                        onEvent(StudentsScreenEvent.ToggleImportSelection(item.id))
                    }
                }
            },
            onImport = {
                onEvent(StudentsScreenEvent.PerformImport)
                onEvent(StudentsScreenEvent.ShowStudentDialog(false))
            },
            onDismiss = { onEvent(StudentsScreenEvent.CloseImportSelectionDialog) }
        )
    }

    if (uiState.showBulkDeleteDialog) {
        BulkDeleteConfirmationDialog(
            selectedStudentIds = uiState.selectedStudentIds,
            onConfirmDelete = { onEvent(StudentsScreenEvent.DeleteSelectedStudents) },
            onDismiss = { onEvent(StudentsScreenEvent.DismissBulkDeleteDialog) }
        )
    }

    if (uiState.showManageClassesDialog) {
        ManageClassesDialog(
            classes = uiState.availableClasses,
            onAddClass = { onEvent(StudentsScreenEvent.AddClass(it)) },
            onRenameClass = { cls, name ->
                onEvent(StudentsScreenEvent.RenameClass(cls, name))
            },
            onDeleteClass = { onEvent(StudentsScreenEvent.DeleteClass(it)) },
            onDismiss = {
                onEvent(StudentsScreenEvent.ShowManageClassesDialog(false))
            }
        )
    }
}