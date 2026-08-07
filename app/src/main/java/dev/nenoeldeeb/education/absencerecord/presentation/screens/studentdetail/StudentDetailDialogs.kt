package dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail

import androidx.compose.runtime.Composable
import dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.dialogs.ChangeClassDialog
import dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.dialogs.DeleteConfirmationDialog
import dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.dialogs.EditStudentNameDialog

@Composable
internal fun StudentDetailDialogs(
    uiState: StudentDetailScreenState,
    onEvent: (StudentDetailScreenEvent) -> Unit
) {
    if (uiState.isDeleteConfirmationDialogOpen) {
        DeleteConfirmationDialog(
            onConfirm = {
                onEvent(StudentDetailScreenEvent.ConfirmDeleteStudent)
            },
            onDismiss = {
                onEvent(StudentDetailScreenEvent.ToggleDeleteConfirmationDialog)
            }
        )
    }

    val student = uiState.student
    if (uiState.isEditNameDialogOpen && student != null) {
        EditStudentNameDialog(
            currentName = student.name,
            onConfirm = { name ->
                onEvent(StudentDetailScreenEvent.UpdateStudentName(name))
            },
            onDismiss = {
                onEvent(StudentDetailScreenEvent.ToggleEditNameDialog)
            }
        )
    }

    if (uiState.isChangeClassDialogOpen) {
        ChangeClassDialog(
            availableClasses = uiState.availableClasses,
            currentClassId = uiState.student?.classId,
            onConfirm = { classId ->
                onEvent(StudentDetailScreenEvent.UpdateStudentClass(classId))
            },
            onDismiss = {
                onEvent(StudentDetailScreenEvent.ToggleChangeClassDialog)
            }
        )
    }
}