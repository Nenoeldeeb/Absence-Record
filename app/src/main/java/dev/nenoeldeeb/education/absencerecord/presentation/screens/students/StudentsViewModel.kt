package dev.nenoeldeeb.education.absencerecord.presentation.screens.students

import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentClass
import dev.nenoeldeeb.education.absencerecord.domain.usecases.ClassManagementUseCases
import dev.nenoeldeeb.education.absencerecord.domain.usecases.StudentManagementUseCases
import dev.nenoeldeeb.education.absencerecord.presentation.screens.components.ClassFilter
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.AddClass
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.AddStudent
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.CloseImportSelectionDialog
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.ConsumeToastMessage
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.DeleteClass
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.DeleteSelectedStudents
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.DismissBulkDeleteDialog
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.ExportAndDeleteSelectedStudents
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.ExportSelectedStudents
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.PerformImport
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.PrepareImportSelectionDialog
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.RenameClass
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.SelectClassFilter
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.ShowBulkDeleteDialog
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.ShowManageClassesDialog
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.ShowStudentDialog
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.ToggleClassDropdown
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.ToggleImportSelection
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.ToggleSelectionMode
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.ToggleStudentSelection
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.ToggleStudentsSelection
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.UpdateNewStudentName
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.UpdateStudent
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.delegates.ImportExportDelegate
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.delegates.SelectionStateDelegate
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Stable
open class StudentsViewModel(
    private val studentManagementUseCases: StudentManagementUseCases,
    private val classManagementUseCases: ClassManagementUseCases,
    private val selectionDelegate: SelectionStateDelegate = SelectionStateDelegate(),
    private val importExportDelegate: ImportExportDelegate = ImportExportDelegate()
) : ViewModel() {
    private val _uiState = MutableStateFlow(StudentsScreenState())
    val uiState: StateFlow<StudentsScreenState> = _uiState.asStateFlow()

    /** Unfiltered student list; used to cheaply re-apply filters without a new DB query. */
    private var _rawStudents: List<Student> = emptyList()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                studentManagementUseCases.getAllStudentsUseCase(),
                classManagementUseCases.getAllClassesUseCase()
            ) { studentsResult, classesResult -> Pair(studentsResult, classesResult) }
                .collectLatest { (studentsResult, classesResult) ->
                    studentsResult.onSuccess { students ->
                        _rawStudents = students
                        _uiState.update { state ->
                            state.copy(
                                allStudents = applyFilter(students, state.selectedClassFilter)
                            )
                        }
                    }.onFailure { e ->
                        _uiState.update {
                            it.copy(
                                error = UiText.StringResource(
                                    R.string.error_loading_students,
                                    e.message ?: "Unknown error"
                                )
                            )
                        }
                    }
                    classesResult.onSuccess { classes ->
                        _uiState.update { it.copy(availableClasses = classes) }
                    }
                }
        }
    }

    private fun applyFilter(students: List<Student>, filter: ClassFilter): List<Student> =
        when (filter) {
            ClassFilter.All -> students
            ClassFilter.Unassigned -> students.filter { it.classId == null }
            is ClassFilter.ByClass -> students.filter { it.classId == filter.studentClass.id }
        }

    open fun onEvent(event: StudentsScreenEvent) {
        when (event) {
            is UpdateNewStudentName -> _uiState.update { it.copy(newStudentName = event.name) }
            is AddStudent -> addStudent(event.name, event.classId)
            is UpdateStudent -> updateStudent(event.student, event.newName, event.newClassId)
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
                        importSelectionMap = importExportDelegate.toggleImportSelection(
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
                        selectedStudentIds = selectionDelegate.toggleSelection(
                            state.selectedStudentIds,
                            event.studentId
                        )
                    )
                }

            is ToggleSelectionMode ->
                _uiState.update { state ->
                    val (newMode, newSelection) = selectionDelegate.toggleMode(
                        state.isMultiSelectionMode,
                        state.selectedStudentIds
                    )
                    state.copy(isMultiSelectionMode = newMode, selectedStudentIds = newSelection)
                }

            is ToggleStudentsSelection ->
                _uiState.update { state ->
                    state.copy(
                        selectedStudentIds =
                            if (state.selectedStudentIds.size < state.allStudents.size)
                                selectionDelegate.selectAll(state.allStudents)
                            else selectionDelegate.clearSelection()
                    )
                }

            is DeleteSelectedStudents -> deleteSelectedStudents()
            is ExportSelectedStudents -> exportSelectedStudents(event.uri)
            is ExportAndDeleteSelectedStudents -> exportAndDeleteSelectedStudents(event.uri)
            is ShowBulkDeleteDialog -> _uiState.update { it.copy(showBulkDeleteDialog = true) }
            is DismissBulkDeleteDialog -> _uiState.update { it.copy(showBulkDeleteDialog = false) }
            // Class filter events
            is SelectClassFilter ->
                _uiState.update { state ->
                    state.copy(
                        selectedClassFilter = event.filter,
                        classDropdownExpanded = false,
                        allStudents = applyFilter(_rawStudents, event.filter)
                    )
                }

            is ToggleClassDropdown ->
                _uiState.update { it.copy(classDropdownExpanded = event.expanded) }

            is ShowManageClassesDialog ->
                _uiState.update { it.copy(showManageClassesDialog = event.show) }

            is AddClass -> addClass(event.name)
            is RenameClass -> renameClass(event.studentClass, event.newName)
            is DeleteClass -> deleteClass(event.studentClass)
        }
    }

    // ── Student CRUD ─────────────────────────────────────────────────────────

    private fun addStudent(name: String, classId: Int?) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            _uiState.update {
                it.copy(toastMessage = UiText.StringResource(R.string.student_name_cannot_be_empty))
            }
            return
        }
        viewModelScope.launch {
            studentManagementUseCases
                .addStudentUseCase(Student(name = trimmed, classId = classId))
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            newStudentName = "",
                            showAddStudentDialog = false,
                            toastMessage = UiText.StringResource(R.string.student_added, trimmed)
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            error = UiText.StringResource(
                                R.string.error_adding_student,
                                e.message ?: "Unknown error"
                            )
                        )
                    }
                }
        }
    }

    private fun updateStudent(student: Student, newName: String, newClassId: Int?) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) {
            _uiState.update {
                it.copy(toastMessage = UiText.StringResource(R.string.student_name_cannot_be_empty))
            }
            return
        }
        viewModelScope.launch {
            studentManagementUseCases
                .updateStudentUseCase(student.copy(name = trimmed, classId = newClassId))
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            showEditDialog = null,
                            toastMessage = UiText.StringResource(R.string.student_updated, trimmed)
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            error = UiText.StringResource(
                                R.string.error_updating_student,
                                e.message ?: "Unknown error"
                            )
                        )
                    }
                }
        }
    }

    // ── Import / Export ──────────────────────────────────────────────────────

    private fun prepareImportSelectionDialog(uri: Uri?) {
        if (uri == null) {
            _uiState.update {
                it.copy(toastMessage = UiText.StringResource(R.string.file_selection_cancelled))
            }
            return
        }
        viewModelScope.launch {
            studentManagementUseCases.importStudentsUseCase.parseFile(uri.toString())
                .onSuccess { parsedData ->
                    val (data, selectionMap, showDialog) =
                        importExportDelegate.prepareImportDialog(parsedData)
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
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            error = UiText.StringResource(
                                R.string.error_reading_or_parsing_file,
                                e.message ?: "Unknown error"
                            )
                        )
                    }
                }
        }
    }

    private fun performImport() {
        val parsedStudents = _uiState.value.parsedStudentsFromFile ?: return
        val selectionMap = _uiState.value.importSelectionMap
        viewModelScope.launch {
            studentManagementUseCases.importStudentsUseCase.performImport(parsedStudents, selectionMap)
                .onSuccess { importResult ->
                    _uiState.update {
                        it.copy(
                            showImportSelectionDialog = false,
                            toastMessage = importExportDelegate.buildImportResultMessage(importResult)
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            error = UiText.StringResource(
                                R.string.error_during_import,
                                e.message ?: "Unknown error"
                            )
                        )
                    }
                }
        }
    }

    // ── Bulk operations ──────────────────────────────────────────────────────

    private fun deleteSelectedStudents() {
        val selectedIds = _uiState.value.selectedStudentIds
        if (selectedIds.isEmpty()) return
        val studentsToDelete = _uiState.value.allStudents.filter { it.id in selectedIds }
        viewModelScope.launch {
            studentManagementUseCases.deleteStudentsUseCase(studentsToDelete)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            showBulkDeleteDialog = false,
                            isMultiSelectionMode = false,
                            selectedStudentIds = emptySet(),
                            toastMessage = UiText.StringResource(R.string.students_deleted_successfully)
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            error = UiText.StringResource(
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
        viewModelScope.launch {
            studentManagementUseCases.exportStudentsUseCase(
                uri.toString(), selectedIds, _uiState.value.allStudents
            ).onSuccess {
                _uiState.update {
                    it.copy(
                        isMultiSelectionMode = false,
                        selectedStudentIds = emptySet(),
                        toastMessage = UiText.StringResource(R.string.data_exported_successfully)
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        error = UiText.StringResource(
                            R.string.error_creating_export_data,
                            e.message ?: "Unknown error"
                        )
                    )
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
            studentManagementUseCases.exportStudentsUseCase(uri.toString(), selectedIds, allStudents)
                .onSuccess {
                    studentManagementUseCases.deleteStudentsUseCase(studentsToDelete)
                        .onSuccess {
                            _uiState.update {
                                it.copy(
                                    showBulkDeleteDialog = false,
                                    isMultiSelectionMode = false,
                                    selectedStudentIds = emptySet(),
                                    toastMessage = UiText.StringResource(R.string.data_exported_and_deleted_successfully)
                                )
                            }
                        }
                        .onFailure { e ->
                            _uiState.update {
                                it.copy(
                                    error = UiText.StringResource(
                                        R.string.error_deleting_student,
                                        e.message ?: "Unknown error"
                                    )
                                )
                            }
                        }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            error = UiText.StringResource(
                                R.string.error_creating_export_data,
                                e.message ?: "Unknown error"
                            )
                        )
                    }
                }
        }
    }

    // ── Class CRUD ────────────────────────────────────────────────────────────

    private fun addClass(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            _uiState.update {
                it.copy(toastMessage = UiText.StringResource(R.string.class_name_cannot_be_empty))
            }
            return
        }
        viewModelScope.launch {
            classManagementUseCases.addClassUseCase(StudentClass(name = trimmed))
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            toastMessage = UiText.StringResource(R.string.class_added, trimmed)
                        )
                    }
                }
                .onFailure { e ->
                    val message = if (e.message == "DUPLICATE_CLASS_NAME") {
                        UiText.StringResource(R.string.error_class_name_already_exists)
                    } else {
                        UiText.StringResource(R.string.error_adding_class, e.message ?: "Unknown error")
                    }
                    _uiState.update {
                        it.copy(toastMessage = message)
                    }
                }
        }
    }

    private fun renameClass(studentClass: StudentClass, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) {
            _uiState.update {
                it.copy(toastMessage = UiText.StringResource(R.string.class_name_cannot_be_empty))
            }
            return
        }
        viewModelScope.launch {
            classManagementUseCases.updateClassUseCase(studentClass.copy(name = trimmed))
                .onSuccess {
                    val filter = _uiState.value.selectedClassFilter
                    if (filter is ClassFilter.ByClass && filter.studentClass.id == studentClass.id) {
                        _uiState.update {
                            it.copy(
                                selectedClassFilter = ClassFilter.ByClass(
                                    studentClass.copy(name = trimmed)
                                ),
                                toastMessage = UiText.StringResource(R.string.class_updated, trimmed)
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(toastMessage = UiText.StringResource(R.string.class_updated, trimmed))
                        }
                    }
                }
                .onFailure { e ->
                    val message = if (e.message == "DUPLICATE_CLASS_NAME") {
                        UiText.StringResource(R.string.error_class_name_already_exists)
                    } else {
                        UiText.StringResource(R.string.error_updating_class, e.message ?: "Unknown error")
                    }
                    _uiState.update {
                        it.copy(toastMessage = message)
                    }
                }
        }
    }

    private fun deleteClass(studentClass: StudentClass) {
        viewModelScope.launch {
            classManagementUseCases.deleteClassUseCase(studentClass)
                .onSuccess {
                    val filter = _uiState.value.selectedClassFilter
                    if (filter is ClassFilter.ByClass && filter.studentClass.id == studentClass.id) {
                        _uiState.update {
                            it.copy(
                                selectedClassFilter = ClassFilter.All,
                                allStudents = applyFilter(_rawStudents, ClassFilter.All),
                                toastMessage = UiText.StringResource(R.string.class_deleted)
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(toastMessage = UiText.StringResource(R.string.class_deleted))
                        }
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            toastMessage = UiText.StringResource(
                                R.string.error_deleting_class,
                                e.message ?: "Unknown error"
                            )
                        )
                    }
                }
        }
    }
}
