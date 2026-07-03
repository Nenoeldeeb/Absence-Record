package dev.nenoeldeeb.education.absencerecord.presentation.screens.students.handlers

import android.net.Uri
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenState
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.delegates.ImportExportDelegate
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.delegates.StudentActionDelegate
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText
import dev.nenoeldeeb.education.absencerecord.presentation.utils.toUiText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ImportExportHandler(
    private val studentActionDelegate: StudentActionDelegate,
    private val importExportDelegate: ImportExportDelegate
) {
    fun handlePrepareImportSelectionDialog(
        uri: Uri?,
        _uiState: MutableStateFlow<StudentsScreenState>,
        viewModelScope: CoroutineScope
    ) {
        val resolvedUri =
            uri ?: run {
                _uiState.update { it.copy(error = StudentError.Cancelled.toUiText()) }
                return
            }
        viewModelScope.launch {
            studentActionDelegate.prepareImportSelectionDialog(resolvedUri.toString())
                .onSuccess { parsedData ->
                    val (data, selectionMap, showDialog) = importExportDelegate.prepareImportDialog(parsedData)
                    _uiState.update {
                        if (data == null) {
                            it.copy(
                                parsedStudentsFromFile = null,
                                importSelectionMap = emptyMap(),
                                toastMessage = UiText.StringResource(R.string.no_students_found_in_file)
                            )
                        } else {
                            it.copy(
                                parsedStudentsFromFile = data,
                                importSelectionMap = selectionMap,
                                showImportSelectionDialog = showDialog
                            )
                        }
                    }
                }
                .onFailure { error -> _uiState.update { state -> state.copy(error = error.toUiText()) } }
        }
    }

    fun handleCloseImportSelectionDialog(_uiState: MutableStateFlow<StudentsScreenState>) {
        _uiState.update {
            it.copy(showImportSelectionDialog = false, parsedStudentsFromFile = null, importSelectionMap = emptyMap())
        }
    }

    fun handleToggleImportSelection(
        parsedStudentId: Int,
        _uiState: MutableStateFlow<StudentsScreenState>
    ) {
        _uiState.update {
            it.copy(
                importSelectionMap = importExportDelegate.toggleImportSelection(it.importSelectionMap, parsedStudentId)
            )
        }
    }

    fun handlePerformImport(
        _uiState: MutableStateFlow<StudentsScreenState>,
        viewModelScope: CoroutineScope
    ) {
        viewModelScope.launch {
            val parsedStudents = _uiState.value.parsedStudentsFromFile ?: return@launch
            studentActionDelegate.performImport(parsedStudents, _uiState.value.importSelectionMap)
                .onSuccess { toast ->
                    _uiState.update { state ->
                        state.copy(
                            showImportSelectionDialog = false,
                            parsedStudentsFromFile = null,
                            importSelectionMap = emptyMap(),
                            toastMessage = toast
                        )
                    }
                }
                .onFailure { error -> _uiState.update { state -> state.copy(error = error.toUiText()) } }
        }
    }
}