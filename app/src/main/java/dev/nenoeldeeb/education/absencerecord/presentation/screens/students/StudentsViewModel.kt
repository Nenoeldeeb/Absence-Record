package dev.nenoeldeeb.education.absencerecord.presentation.screens.students

import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.usecases.StudentManagementUseCases
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.AddStudent
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.CloseImportSelectionDialog
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.ConsumeToastMessage
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.DeleteSelectedStudents
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.DismissBulkDeleteDialog
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.ExportAndDeleteSelectedStudents
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.ExportSelectedStudents
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.PerformImport
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.PrepareImportSelectionDialog
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.ToggleStudentsSelection
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.ShowBulkDeleteDialog
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.ShowStudentDialog
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.ToggleImportSelection
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.ToggleSelectionMode
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.ToggleStudentSelection
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.UpdateNewStudentName
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.UpdateStudent
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.delegates.ImportExportDelegate
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.delegates.SelectionStateDelegate
import dev.nenoeldeeb.education.absencerecord.presentation.util.UiText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@Stable
open class StudentsViewModel(
    private val studentManagementUseCases: StudentManagementUseCases,
    private val selectionDelegate: SelectionStateDelegate = SelectionStateDelegate(),
    private val importExportDelegate: ImportExportDelegate = ImportExportDelegate()
) : ViewModel() {
    private val _uiState = MutableStateFlow(StudentsScreenState())
    val uiState: StateFlow<StudentsScreenState> = _uiState.asStateFlow()

    init {
        initializeStudents()
    }

    private fun initializeStudents() {
        viewModelScope.launch {
            studentManagementUseCases.getAllStudentsUseCase()
                .collectLatest { result ->
                    result
                        .onSuccess { students ->
                            _uiState.update { it.copy(allStudents = students) }
                        }
                        .onFailure { e ->
                            _uiState.update {
                                it.copy(
                                    error =
                                        UiText.StringResource(
                                            R.string.error_loading_students,
                                            e.message ?: "Unknown error"
                                        )
                                )
                            }
                        }
                }
        }
    }

    open fun onEvent(event: StudentsScreenEvent) {
        when (event) {
            is UpdateNewStudentName -> _uiState.update { it.copy(newStudentName = event.name) }
            is AddStudent -> addStudent(event.name)
            is UpdateStudent -> updateStudent(event.student, event.newName)
            is PrepareImportSelectionDialog -> prepareImportSelectionDialog(event.uri)
            is CloseImportSelectionDialog ->
                _uiState.update {
                    it.copy(
                        showImportSelectionDialog = false,
                        parsedStudentsFromFile = null,
                        importSelectionMap = emptyMap()
                    )
                }

            is ToggleImportSelection ->
                _uiState.update {
                    it.copy(
                        importSelectionMap =
                            importExportDelegate.toggleImportSelection(
                                it.importSelectionMap,
                                event.parsedStudentId
                            )
                    )
                }

            is PerformImport -> performImport()
            is ConsumeToastMessage -> _uiState.update { it.copy(toastMessage = null) }
            is ShowStudentDialog ->
                _uiState.update {
                    it.copy(
                        showEditDialog = event.student,
                        showAddStudentDialog = event.show,
                        newStudentName = event.student?.name ?: ""
                    )
                }

            is ToggleStudentSelection ->
                _uiState.update { state ->
                    state.copy(
                        selectedStudentIds =
                            selectionDelegate.toggleSelection(
                                state.selectedStudentIds,
                                event.studentId
                            )
                    )
                }

            is ToggleSelectionMode ->
                _uiState.update { state ->
                    val (newMode, newSelection) =
                        selectionDelegate.toggleMode(
                            state.isMultiSelectionMode,
                            state.selectedStudentIds
                        )
                    state.copy(
                        isMultiSelectionMode = newMode,
                        selectedStudentIds = newSelection
                    )
                }

            is ToggleStudentsSelection ->
                _uiState.update { state ->
                    state.copy(
                        selectedStudentIds = if (state.selectedStudentIds.size < state.allStudents.size)
                            selectionDelegate.selectAll(state.allStudents)
                        else selectionDelegate.clearSelection(),
                    )
                }
            is DeleteSelectedStudents -> deleteSelectedStudents()
            is ExportSelectedStudents -> exportSelectedStudents(event.uri)
            is ExportAndDeleteSelectedStudents -> exportAndDeleteSelectedStudents(event.uri)
            is ShowBulkDeleteDialog -> _uiState.update { it.copy(showBulkDeleteDialog = true) }
            is DismissBulkDeleteDialog -> _uiState.update { it.copy(showBulkDeleteDialog = false) }
        }
    }

    private fun addStudent(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            _uiState.update {
                it.copy(toastMessage = UiText.StringResource(R.string.student_name_cannot_be_empty))
            }
            return
        }
        viewModelScope.launch {
            studentManagementUseCases
                .addStudentUseCase(Student(name = trimmed))
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            newStudentName = "",
                            showAddStudentDialog = false,
                            toastMessage =
                                UiText.StringResource(R.string.student_added, trimmed)
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            error =
                                UiText.StringResource(
                                    R.string.error_adding_student,
                                    e.message ?: "Unknown error"
                                )
                        )
                    }
                }
        }
    }

    private fun updateStudent(
        student: Student,
        newName: String
    ) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) {
            _uiState.update {
                it.copy(toastMessage = UiText.StringResource(R.string.student_name_cannot_be_empty))
            }
            return
        }
        viewModelScope.launch {
            studentManagementUseCases
                .updateStudentUseCase(student.copy(name = trimmed))
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            showEditDialog = null,
                            toastMessage =
                                UiText.StringResource(R.string.student_updated, trimmed)
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            error =
                                UiText.StringResource(
                                    R.string.error_updating_student,
                                    e.message ?: "Unknown error"
                                )
                        )
                    }
                }
        }
    }

    private fun prepareImportSelectionDialog(uri: Uri?) {
        if (uri == null) {
            _uiState.update {
                it.copy(toastMessage = UiText.StringResource(R.string.file_selection_cancelled))
            }
            return
        }
        viewModelScope.launch {
            studentManagementUseCases.importStudentsUseCase.parseFile(uri.toString()).collectLatest { result ->
                result
                    .onSuccess { parsedData ->
                        val (data, selectionMap, showDialog) =
                            importExportDelegate.prepareImportDialog(parsedData)

                        _uiState.update {
                            if (data == null) {
                                it.copy(
                                    parsedStudentsFromFile = null,
                                    importSelectionMap = emptyMap(),
                                    toastMessage =
                                        UiText.StringResource(
                                            R.string.no_students_found_in_file
                                        )
                                )
                            } else {
                                it.copy(
                                    parsedStudentsFromFile = data,
                                    importSelectionMap = selectionMap,
                                    showImportSelectionDialog = showDialog,
                                )
                            }
                        }
                    }
                    .onFailure { e ->
                        _uiState.update {
                            it.copy(
                                error =
                                    UiText.StringResource(
                                        R.string.error_reading_or_parsing_file,
                                        e.message ?: "Unknown error"
                                    )
                            )
                        }
                    }
            }
        }
    }

    private fun performImport() {
        val parsedStudents = _uiState.value.parsedStudentsFromFile
        val selectionMap = _uiState.value.importSelectionMap
        if (parsedStudents == null) return

        viewModelScope.launch {
            studentManagementUseCases.importStudentsUseCase.performImport(
                parsedStudents,
                selectionMap
            )
                .collectLatest { result ->
                    result
                        .onSuccess { importResult ->
                            val message =
                                importExportDelegate.buildImportResultMessage(
                                    importResult
                                )
                            _uiState.update {
                                it.copy(
                                    showImportSelectionDialog = false,
                                    toastMessage = message
                                )
                            }
                        }
                        .onFailure { e ->
                            _uiState.update {
                                it.copy(
                                    error =
                                        UiText.StringResource(
                                            R.string.error_during_import,
                                            e.message ?: "Unknown error"
                                        )
                                )
                            }
                        }
                }
        }
    }

    private fun deleteSelectedStudents() {
        val selectedIds = _uiState.value.selectedStudentIds
        if (selectedIds.isEmpty()) return

        val studentsToDelete = _uiState.value.allStudents.filter { it.id in selectedIds }

        viewModelScope.launch {
            studentManagementUseCases
                .deleteStudentsUseCase(studentsToDelete)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            showBulkDeleteDialog = false,
                            isMultiSelectionMode = false,
                            selectedStudentIds = emptySet(),
                            toastMessage =
                                UiText.StringResource(
                                    R.string.students_deleted_successfully
                                )
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            error =
                                UiText.StringResource(
                                    R.string.error_deleting_student,
                                    e.message ?: "Unknown error"
                                )
                        )
                    }
                }
        }
    }

    private fun exportSelectedStudents(uri: Uri) {
        val selectedIds = _uiState.value.selectedStudentIds
        if (selectedIds.isEmpty()) return
        val allStudents = _uiState.value.allStudents

        viewModelScope.launch {
            studentManagementUseCases.exportStudentsUseCase(
                uri.toString(),
                selectedIds,
                allStudents
            )
                .collectLatest { result ->
                    result
                        .onSuccess {
                            _uiState.update {
                                it.copy(
                                    isMultiSelectionMode = false,
                                    selectedStudentIds = emptySet(),
                                    toastMessage =
                                        UiText.StringResource(
                                            R.string.data_exported_successfully
                                        )
                                )
                            }
                        }
                        .onFailure { e ->
                            _uiState.update {
                                it.copy(
                                    error =
                                        UiText.StringResource(
                                            R.string.error_creating_export_data,
                                            e.message ?: "Unknown error"
                                        )
                                )
                            }
                        }
                }
        }
    }

    private fun exportAndDeleteSelectedStudents(uri: Uri) {
        val selectedIds = _uiState.value.selectedStudentIds
        if (selectedIds.isEmpty()) return
        val allStudents = _uiState.value.allStudents
        val studentsToDelete = allStudents.filter { it.id in selectedIds }

        viewModelScope.launch {
            studentManagementUseCases.exportStudentsUseCase(
                uri.toString(),
                selectedIds,
                allStudents
            )
                .collectLatest { result ->
                    result
                        .onSuccess {
                            studentManagementUseCases
                                .deleteStudentsUseCase(studentsToDelete)
                                .onSuccess {
                                    _uiState.update {
                                        it.copy(
                                            showBulkDeleteDialog = false,
                                            isMultiSelectionMode = false,
                                            selectedStudentIds = emptySet(),
                                            toastMessage =
                                                UiText.StringResource(
                                                    R.string
                                                        .data_exported_and_deleted_successfully
                                                )
                                        )
                                    }
                                }
                                .onFailure { e ->
                                    _uiState.update {
                                        it.copy(
                                            error =
                                                UiText.StringResource(
                                                    R.string
                                                        .error_deleting_student,
                                                    e.message
                                                    ?: "Unknown error"
                                                )
                                        )
                                    }
                                }
                        }
                        .onFailure { e ->
                            _uiState.update {
                                it.copy(
                                    error =
                                        UiText.StringResource(
                                            R.string.error_creating_export_data,
                                            e.message ?: "Unknown error"
                                        )
                                )
                            }
                        }
                }
        }
    }
}