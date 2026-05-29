package dev.nenoeldeeb.education.absencerecord.presentation.screens.students

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError
import dev.nenoeldeeb.education.absencerecord.domain.models.toUiText
import dev.nenoeldeeb.education.absencerecord.domain.usecases.ClassManagementUseCases
import dev.nenoeldeeb.education.absencerecord.domain.usecases.StudentManagementUseCases
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
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.ToggleClassFilterVisibility
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.ToggleImportSelection
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.ToggleSelectionMode
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.ToggleStudentSelection
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.ToggleStudentsSelection
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.UpdateNewStudentName
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.UpdateStudent
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.delegates.ClassActionDelegate
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.delegates.ImportExportDelegate
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.delegates.SelectionStateDelegate
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.delegates.StudentActionDelegate
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText
import dev.nenoeldeeb.education.absencerecord.presentation.utils.applyClassFilter
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
    private val importExportDelegate: ImportExportDelegate = ImportExportDelegate(),
    private val studentActionDelegate: StudentActionDelegate =
        StudentActionDelegate(studentManagementUseCases, importExportDelegate),
    private val classActionDelegate: ClassActionDelegate = ClassActionDelegate(classManagementUseCases)
) : ViewModel() {
    private val _uiState = MutableStateFlow(StudentsScreenState())
    val uiState: StateFlow<StudentsScreenState> = _uiState.asStateFlow()
    private var rawStudents: List<Student> = emptyList()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                studentManagementUseCases.getAllStudentsUseCase(),
                classManagementUseCases.getAllClassesUseCase()
            ) { s, c -> s to c }
                .collectLatest { (studentsResult, classesResult) ->
                    studentsResult.onSuccess { students ->
                        rawStudents = students
                        _uiState.update { state ->
                            state.copy(allStudents = students.applyClassFilter(state.selectedClassFilter))
                        }
                    }.onFailure { e ->
                        _uiState.update {
                            it.copy(
                                error = UiText.StringResource(R.string.error_loading_students, e.message ?: "Unknown error")
                            )
                        }
                    }
                    classesResult.onSuccess { classes -> _uiState.update { it.copy(availableClasses = classes) } }
                }
        }
    }

    open fun onEvent(event: StudentsScreenEvent) {
        when (event) {
            is UpdateNewStudentName -> _uiState.update { it.copy(newStudentName = event.name) }
            is AddStudent ->
                viewModelScope.launch {
                    studentActionDelegate.addStudent(event.name, event.classId)
                        .onSuccess { toast ->
                            _uiState.update { state ->
                                state.copy(toastMessage = toast, newStudentName = "", showAddStudentDialog = false)
                            }
                        }
                        .onFailure { error -> _uiState.update { state -> state.copy(error = error.toUiText()) } }
                }

            is UpdateStudent ->
                viewModelScope.launch {
                    studentActionDelegate.updateStudent(event.student, event.newName, event.newClassId)
                        .onSuccess { toast ->
                            _uiState.update { state -> state.copy(toastMessage = toast, showEditDialog = null) }
                        }
                        .onFailure { error -> _uiState.update { state -> state.copy(error = error.toUiText()) } }
                }

            is PrepareImportSelectionDialog -> {
                val uri =
                    event.uri ?: run {
                        _uiState.update { it.copy(error = StudentError.Cancelled.toUiText()) }
                        return
                    }
                viewModelScope.launch {
                    studentActionDelegate.prepareImportSelectionDialog(uri.toString())
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

            is CloseImportSelectionDialog ->
                _uiState.update {
                    it.copy(showImportSelectionDialog = false, parsedStudentsFromFile = null, importSelectionMap = emptyMap())
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

            is PerformImport ->
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
                        selectedStudentIds = selectionDelegate.toggleSelection(state.selectedStudentIds, event.studentId)
                    )
                }

            is ToggleSelectionMode ->
                _uiState.update { state ->
                    val (newMode, newSelection) =
                        selectionDelegate.toggleMode(
                            state.isMultiSelectionMode,
                            state.selectedStudentIds
                        )
                    state.copy(isMultiSelectionMode = newMode, selectedStudentIds = newSelection)
                }

            is ToggleStudentsSelection ->
                _uiState.update { state ->
                    state.copy(
                        selectedStudentIds =
                            if (state.selectedStudentIds.size < state.allStudents.size) {
                                selectionDelegate.selectAll(
                                    state.allStudents
                                )
                            } else {
                                selectionDelegate.clearSelection()
                            }
                    )
                }

            is DeleteSelectedStudents ->
                viewModelScope.launch {
                    studentActionDelegate.deleteSelectedStudents(_uiState.value.selectedStudentIds, _uiState.value.allStudents)
                        .onSuccess { toast ->
                            _uiState.update { state ->
                                state.copy(
                                    toastMessage = toast,
                                    showBulkDeleteDialog = false,
                                    isMultiSelectionMode = false,
                                    selectedStudentIds = emptySet()
                                )
                            }
                        }
                        .onFailure { error -> _uiState.update { state -> state.copy(error = error.toUiText()) } }
                }

            is ExportSelectedStudents ->
                viewModelScope.launch {
                    val state = _uiState.value
                    studentActionDelegate.exportSelectedStudents(
                        event.uri.toString(),
                        state.selectedStudentIds,
                        state.allStudents
                    )
                        .onSuccess { toast ->
                            _uiState.update { state ->
                                state.copy(toastMessage = toast, isMultiSelectionMode = false, selectedStudentIds = emptySet())
                            }
                        }
                        .onFailure { error -> _uiState.update { state -> state.copy(error = error.toUiText()) } }
                }

            is ExportAndDeleteSelectedStudents ->
                viewModelScope.launch {
                    val state = _uiState.value
                    studentActionDelegate.exportAndDeleteSelectedStudents(
                        event.uri.toString(),
                        state.selectedStudentIds,
                        state.allStudents
                    )
                        .onSuccess { toast ->
                            _uiState.update { updateState ->
                                updateState.copy(
                                    toastMessage = toast,
                                    showBulkDeleteDialog = false,
                                    isMultiSelectionMode = false,
                                    selectedStudentIds = emptySet()
                                )
                            }
                        }
                        .onFailure { error -> _uiState.update { state -> state.copy(error = error.toUiText()) } }
                }

            is ShowBulkDeleteDialog -> _uiState.update { it.copy(showBulkDeleteDialog = true) }
            is DismissBulkDeleteDialog -> _uiState.update { it.copy(showBulkDeleteDialog = false) }
            is SelectClassFilter ->
                _uiState.update { state ->
                    state.copy(
                        selectedClassFilter = event.filter,
                        classDropdownExpanded = false,
                        allStudents = rawStudents.applyClassFilter(event.filter)
                    )
                }

            is ToggleClassFilterVisibility -> _uiState.update { it.copy(isClassFilterVisible = !it.isClassFilterVisible) }
            is ToggleClassDropdown -> _uiState.update { it.copy(classDropdownExpanded = event.expanded) }
            is ShowManageClassesDialog -> _uiState.update { it.copy(showManageClassesDialog = event.show) }
            is AddClass ->
                viewModelScope.launch {
                    classActionDelegate.addClass(event.name)
                        .onSuccess { toast -> _uiState.update { state -> state.copy(toastMessage = toast) } }
                        .onFailure { error -> _uiState.update { state -> state.copy(error = error.toUiText()) } }
                }

            is RenameClass ->
                viewModelScope.launch {
                    classActionDelegate.renameClass(event.studentClass, event.newName)
                        .onSuccess { toast -> _uiState.update { state -> state.copy(toastMessage = toast) } }
                        .onFailure { error -> _uiState.update { state -> state.copy(error = error.toUiText()) } }
                }

            is DeleteClass ->
                viewModelScope.launch {
                    classActionDelegate.deleteClass(event.studentClass)
                        .onSuccess { toast ->
                            val newFilter =
                                classActionDelegate.onDeletedClassFilterFallback(
                                    _uiState.value.selectedClassFilter,
                                    event.studentClass
                                )
                            _uiState.update {
                                it.copy(
                                    toastMessage = toast,
                                    selectedClassFilter = newFilter,
                                    allStudents = rawStudents.applyClassFilter(newFilter)
                                )
                            }
                        }
                        .onFailure { error -> _uiState.update { state -> state.copy(error = error.toUiText()) } }
                }
        }
    }
}