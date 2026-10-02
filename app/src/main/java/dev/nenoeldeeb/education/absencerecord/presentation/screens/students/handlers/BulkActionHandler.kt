package dev.nenoeldeeb.education.absencerecord.presentation.screens.students.handlers

import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenState
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.delegates.StudentActionDelegate
import dev.nenoeldeeb.education.absencerecord.presentation.utils.toUiText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BulkActionHandler(
    private val studentActionDelegate: StudentActionDelegate
) {
    fun handleDeleteSelectedStudents(
        _uiState: MutableStateFlow<StudentsScreenState>,
        viewModelScope: CoroutineScope,
        undoDeleteHandler: UndoDeleteHandler? = null
    ) {
        viewModelScope.launch {
            val snapshot = _uiState.value
            val toDelete = snapshot.allStudents.filter { it.id in snapshot.selectedStudentIds }
            val backup = undoDeleteHandler?.buildBackup(toDelete).orEmpty()
            studentActionDelegate.deleteSelectedStudents(snapshot.selectedStudentIds, snapshot.allStudents)
                .onSuccess { toast ->
                    _uiState.update { state ->
                        state.copy(
                            toastMessage = toast,
                            showBulkDeleteDialog = false,
                            isMultiSelectionMode = false,
                            selectedStudentIds = emptySet(),
                            lastDeletedBackup = backup
                        )
                    }
                }
                .onFailure { error -> _uiState.update { state -> state.copy(error = error.toUiText()) } }
        }
    }

    fun handleExportSelectedStudents(
        uriString: String,
        _uiState: MutableStateFlow<StudentsScreenState>,
        viewModelScope: CoroutineScope
    ) {
        viewModelScope.launch {
            val state = _uiState.value
            studentActionDelegate.exportSelectedStudents(uriString, state.selectedStudentIds, state.allStudents)
                .onSuccess { toast ->
                    _uiState.update { state ->
                        state.copy(toastMessage = toast, isMultiSelectionMode = false, selectedStudentIds = emptySet())
                    }
                }
                .onFailure { error -> _uiState.update { state -> state.copy(error = error.toUiText()) } }
        }
    }

    fun handleExportAndDeleteSelectedStudents(
        uriString: String,
        _uiState: MutableStateFlow<StudentsScreenState>,
        viewModelScope: CoroutineScope,
        undoDeleteHandler: UndoDeleteHandler? = null
    ) {
        viewModelScope.launch {
            val state = _uiState.value
            val toDelete = state.allStudents.filter { it.id in state.selectedStudentIds }
            val backup = undoDeleteHandler?.buildBackup(toDelete).orEmpty()
            studentActionDelegate.exportAndDeleteSelectedStudents(uriString, state.selectedStudentIds, state.allStudents)
                .onSuccess { toast ->
                    _uiState.update { updateState ->
                        updateState.copy(
                            toastMessage = toast,
                            showBulkDeleteDialog = false,
                            showExportDeleteDialog = false,
                            isMultiSelectionMode = false,
                            selectedStudentIds = emptySet(),
                            lastDeletedBackup = backup
                        )
                    }
                }
                .onFailure { error -> _uiState.update { state -> state.copy(error = error.toUiText()) } }
        }
    }
}