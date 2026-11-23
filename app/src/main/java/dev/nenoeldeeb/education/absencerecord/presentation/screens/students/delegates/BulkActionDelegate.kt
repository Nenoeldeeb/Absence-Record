package dev.nenoeldeeb.education.absencerecord.presentation.screens.students.delegates

import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenState
import dev.nenoeldeeb.education.absencerecord.presentation.utils.toUiText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Bulk multi-selection actions (delete / export / export-and-delete).
 * Reads the current snapshot from [uiState] — a read-only param per the
 * delegate contract — and mutates state only through `updateState`.
 */
class BulkActionDelegate(
    private val studentActionDelegate: StudentActionDelegate,
    private val undoDeleteDelegate: UndoDeleteStudentsDelegate,
    private val uiState: StateFlow<StudentsScreenState>,
    private val scope: CoroutineScope,
    private val updateState: ((StudentsScreenState) -> StudentsScreenState) -> Unit
) {
    fun deleteSelectedStudents() {
        scope.launch {
            val snapshot = uiState.value
            val toDelete = snapshot.allStudents.filter { it.id in snapshot.selectedStudentIds }
            val backup = undoDeleteDelegate.buildBackup(toDelete)
            studentActionDelegate.deleteSelectedStudents(snapshot.selectedStudentIds, snapshot.allStudents)
                .onSuccess { toast ->
                    updateState { state ->
                        state.copy(
                            toastMessage = toast,
                            showBulkDeleteDialog = false,
                            isMultiSelectionMode = false,
                            selectedStudentIds = emptySet(),
                            lastDeletedBackup = backup
                        )
                    }
                }
                .onFailure { error -> updateState { state -> state.copy(error = error.toUiText()) } }
        }
    }

    fun exportSelectedStudents(uriString: String) {
        scope.launch {
            val state = uiState.value
            studentActionDelegate.exportSelectedStudents(uriString, state.selectedStudentIds, state.allStudents)
                .onSuccess { toast ->
                    updateState { current ->
                        current.copy(
                            toastMessage = toast,
                            isMultiSelectionMode = false,
                            selectedStudentIds = emptySet()
                        )
                    }
                }
                .onFailure { error -> updateState { current -> current.copy(error = error.toUiText()) } }
        }
    }

    fun exportAndDeleteSelectedStudents(uriString: String) {
        scope.launch {
            val state = uiState.value
            val toDelete = state.allStudents.filter { it.id in state.selectedStudentIds }
            val backup = undoDeleteDelegate.buildBackup(toDelete)
            studentActionDelegate.exportAndDeleteSelectedStudents(uriString, state.selectedStudentIds, state.allStudents)
                .onSuccess { toast ->
                    updateState { current ->
                        current.copy(
                            toastMessage = toast,
                            showBulkDeleteDialog = false,
                            showExportDeleteDialog = false,
                            isMultiSelectionMode = false,
                            selectedStudentIds = emptySet(),
                            lastDeletedBackup = backup
                        )
                    }
                }
                .onFailure { error -> updateState { current -> current.copy(error = error.toUiText()) } }
        }
    }
}