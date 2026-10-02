package dev.nenoeldeeb.education.absencerecord.presentation.screens.students

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.repositories.ClassFilterRepository
import dev.nenoeldeeb.education.absencerecord.domain.usecases.AttendanceUseCases
import dev.nenoeldeeb.education.absencerecord.domain.usecases.ClassManagementUseCases
import dev.nenoeldeeb.education.absencerecord.domain.usecases.StudentManagementUseCases
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.AddClass
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.AddStudent
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.ConsumeToastMessage
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.ConsumeUndoBackup
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.DeleteClass
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.DismissBulkDeleteDialog
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.DismissExportDeleteDialog
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.RenameClass
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.ShowBulkDeleteDialog
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.ShowExportDeleteDialog
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.ShowManageClassesDialog
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.ShowStudentDialog
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.ToggleClassDropdown
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.ToggleClassFilter
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.ToggleClassFilterVisibility
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.ToggleMonthDropdown
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.ToggleSelectionMode
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.ToggleSortPanelVisible
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.ToggleStudentSelection
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.ToggleStudentsSelection
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.UndoDeleteStudents
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.UpdateNewStudentName
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.UpdateSelectedMonth
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenEvent.UpdateSortType
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.delegates.ClassActionDelegate
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.delegates.ImportExportDelegate
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.delegates.SelectionStateDelegate
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.delegates.StudentActionDelegate
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.handlers.BulkActionHandler
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.handlers.ImportExportHandler
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.handlers.PanelVisibilityHandler
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.handlers.StudentClassHandler
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.handlers.StudentSearchHandler
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.handlers.UndoDeleteHandler
import dev.nenoeldeeb.education.absencerecord.presentation.utils.applyStudentListFilters
import dev.nenoeldeeb.education.absencerecord.presentation.utils.toUiText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
open class StudentsViewModel(
    private val studentManagementUseCases: StudentManagementUseCases,
    private val classManagementUseCases: ClassManagementUseCases,
    private val selectionDelegate: SelectionStateDelegate = SelectionStateDelegate(),
    private val importExportDelegate: ImportExportDelegate = ImportExportDelegate(),
    private val studentActionDelegate: StudentActionDelegate =
        StudentActionDelegate(studentManagementUseCases, importExportDelegate),
    private val classActionDelegate: ClassActionDelegate = ClassActionDelegate(classManagementUseCases),
    private val importExportHandler: ImportExportHandler =
        ImportExportHandler(studentActionDelegate, importExportDelegate),
    private val bulkActionHandler: BulkActionHandler = BulkActionHandler(studentActionDelegate),
    private val classFilterRepository: ClassFilterRepository,
    private val attendanceUseCases: AttendanceUseCases,
    private val undoDeleteHandler: UndoDeleteHandler =
        UndoDeleteHandler(studentManagementUseCases, attendanceUseCases),
    private val searchHandler: StudentSearchHandler = StudentSearchHandler(),
    private val studentClassHandler: StudentClassHandler =
        StudentClassHandler(classActionDelegate, classFilterRepository),
    private val panelVisibilityHandler: PanelVisibilityHandler = PanelVisibilityHandler()
) : ViewModel() {
    private val _uiState = MutableStateFlow(StudentsScreenState())
    val uiState: StateFlow<StudentsScreenState> = _uiState.asStateFlow()
    private var rawStudents: List<Student> = emptyList()

    init {
        loadData()
        observeClasses()
        observeClassFilter()
        observeAvailableMonths()
    }

    private fun observeAvailableMonths() {
        viewModelScope.launch {
            attendanceUseCases.getAvailableMonthsUseCase().collectLatest { result ->
                result
                    .onSuccess { months ->
                        _uiState.update { it.copy(availableMonths = months) }
                    }
                    .onFailure { e ->
                        _uiState.update { it.copy(error = e.toUiText()) }
                    }
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                _uiState.map { it.sortType }.distinctUntilChanged(),
                _uiState.map { it.selectedMonth }.distinctUntilChanged()
            ) { sortType, month -> sortType to month }
                .flatMapLatest { (sortType, month) ->
                    studentManagementUseCases.getAllStudentsUseCase(sortType, month)
                }
                .collectLatest { result ->
                    result.onSuccess { students ->
                        rawStudents = students
                        val scope = _uiState.value
                        _uiState.update { state ->
                            state.copy(
                                allStudents =
                                    students.applyStudentListFilters(
                                        scope.selectedClassIds,
                                        scope.isMultiSelectionMode,
                                        scope.searchQuery
                                    ),
                                totalStudentsCount = students.size
                            )
                        }
                    }.onFailure { e ->
                        _uiState.update {
                            it.copy(error = e.toUiText())
                        }
                    }
                }
        }
    }

    private fun observeClasses() {
        viewModelScope.launch {
            classManagementUseCases.getAllClassesUseCase().collectLatest { result ->
                result.onSuccess { classes ->
                    _uiState.update { it.copy(availableClasses = classes) }
                }.onFailure { e ->
                    _uiState.update { it.copy(error = e.toUiText()) }
                }
            }
        }
    }

    private fun observeClassFilter() {
        viewModelScope.launch {
            classFilterRepository.selectedClassIds.collectLatest { ids ->
                _uiState.update { state ->
                    state.copy(
                        selectedClassIds = ids,
                        allStudents =
                            rawStudents.applyStudentListFilters(
                                ids,
                                state.isMultiSelectionMode,
                                state.searchQuery
                            ),
                        totalStudentsCount = rawStudents.size
                    )
                }
            }
        }
    }

    open fun onEvent(event: StudentsScreenEvent) {
        when (event) {
            is UpdateNewStudentName -> _uiState.update { it.copy(newStudentName = event.name) }
            is StudentsScreenEvent.UpdateSearchQuery ->
                searchHandler.handleUpdateQuery(event.query, rawStudents, _uiState)
            is StudentsScreenEvent.ToggleSearch ->
                searchHandler.handleToggleSearch(event.active, rawStudents, _uiState)
            is UpdateSortType -> _uiState.update { it.copy(sortType = event.sortType) }
            is ToggleSortPanelVisible ->
                panelVisibilityHandler.handleToggleSortPanel(event.show, _uiState)
            is UpdateSelectedMonth -> _uiState.update { it.copy(selectedMonth = event.month, isMonthDropdownExpanded = false) }
            is ToggleMonthDropdown -> _uiState.update { it.copy(isMonthDropdownExpanded = event.expanded) }
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

            is StudentsScreenEvent.PrepareImportSelectionDialog ->
                importExportHandler.handlePrepareImportSelectionDialog(
                    event.uri,
                    _uiState,
                    viewModelScope
                )
            is StudentsScreenEvent.CloseImportSelectionDialog -> importExportHandler.handleCloseImportSelectionDialog(_uiState)
            is StudentsScreenEvent.ToggleImportSelection ->
                importExportHandler.handleToggleImportSelection(
                    event.parsedStudentId,
                    _uiState
                )
            is StudentsScreenEvent.PerformImport -> importExportHandler.handlePerformImport(_uiState, viewModelScope)

            is StudentsScreenEvent.ConsumeError -> _uiState.update { it.copy(error = null) }
            is ConsumeToastMessage -> _uiState.update { it.copy(toastMessage = null) }
            is ShowStudentDialog ->
                _uiState.update {
                    it.copy(
                        showAddStudentDialog = event.show,
                        newStudentName = if (event.show) "" else it.newStudentName
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
                    val query = if (newMode) "" else state.searchQuery
                    state.copy(
                        isMultiSelectionMode = newMode,
                        selectedStudentIds = newSelection,
                        isSearchActive = if (newMode) false else state.isSearchActive,
                        searchQuery = query,
                        allStudents =
                            rawStudents.applyStudentListFilters(state.selectedClassIds, newMode, query)
                    )
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

            is StudentsScreenEvent.DeleteSelectedStudents ->
                bulkActionHandler.handleDeleteSelectedStudents(
                    _uiState,
                    viewModelScope,
                    undoDeleteHandler
                )
            is StudentsScreenEvent.ExportSelectedStudents ->
                bulkActionHandler.handleExportSelectedStudents(
                    event.uri.toString(),
                    _uiState,
                    viewModelScope
                )
            is StudentsScreenEvent.ExportAndDeleteSelectedStudents ->
                bulkActionHandler.handleExportAndDeleteSelectedStudents(
                    event.uri.toString(),
                    _uiState,
                    viewModelScope,
                    undoDeleteHandler
                )

            is ShowBulkDeleteDialog -> _uiState.update { it.copy(showBulkDeleteDialog = true) }
            is DismissBulkDeleteDialog -> _uiState.update { it.copy(showBulkDeleteDialog = false) }
            is ShowExportDeleteDialog -> _uiState.update { it.copy(showExportDeleteDialog = true) }
            is DismissExportDeleteDialog -> _uiState.update { it.copy(showExportDeleteDialog = false) }
            is UndoDeleteStudents -> undoDeleteHandler.handleUndo(_uiState, viewModelScope)
            is ConsumeUndoBackup -> undoDeleteHandler.consumeBackup(_uiState)
            is ToggleClassFilter -> {
                classFilterRepository.toggleClass(event.classId)
            }
            is StudentsScreenEvent.ClearClassFilter -> {
                classFilterRepository.clearFilter()
            }

            is ToggleClassFilterVisibility ->
                panelVisibilityHandler.handleToggleClassFilterVisibility(_uiState)
            is ToggleClassDropdown -> _uiState.update { it.copy(classDropdownExpanded = event.expanded) }
            is ShowManageClassesDialog -> _uiState.update { it.copy(showManageClassesDialog = event.show) }
            is AddClass -> studentClassHandler.handleAddClass(event.name, _uiState, viewModelScope)
            is RenameClass ->
                studentClassHandler.handleRenameClass(event.studentClass, event.newName, _uiState, viewModelScope)
            is DeleteClass -> studentClassHandler.handleDeleteClass(event.studentClass, _uiState, viewModelScope)
        }
    }
}