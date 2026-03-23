package dev.nenoeldeeb.education.absencerecord.presentation.screens.report

import androidx.compose.runtime.Stable
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.SortType
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.usecases.AttendanceUseCases
import dev.nenoeldeeb.education.absencerecord.domain.usecases.ClassManagementUseCases
import dev.nenoeldeeb.education.absencerecord.domain.usecases.ReportUseCases
import dev.nenoeldeeb.education.absencerecord.domain.usecases.StudentManagementUseCases
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText
import dev.nenoeldeeb.education.absencerecord.presentation.utils.applyClassFilter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

@OptIn(ExperimentalCoroutinesApi::class)
@Stable
class ReportViewModel(
    private val studentManagementUseCases: StudentManagementUseCases,
    private val attendanceUseCases: AttendanceUseCases,
    private val reportUseCases: ReportUseCases,
    private val classManagementUseCases: ClassManagementUseCases
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReportScreenState())
    val uiState: StateFlow<ReportScreenState> = _uiState.asStateFlow()

    /** Unfiltered student list; re-filtered cheaply on class selection. */
    private var rawStudents: List<Student> = emptyList()

    init {
        initializeStudents()
        initializeClasses()
        initializeAvailableMonths()
        initializeStudentHistory()
    }

    private fun initializeStudents() {
        viewModelScope.launch {
            combine(_uiState.map { it.sortType }, _uiState.map { it.selectedMonth }) { type, month ->
                Pair(type, month)
            }.flatMapLatest { (currentSortType, currentSelectedMonth) ->
                studentManagementUseCases.getAllStudentsUseCase(
                    currentSortType,
                    currentSelectedMonth
                )
            }.collectLatest { result ->
                result.onSuccess { students ->
                    rawStudents = students
                    _uiState.update { state ->
                        state.copy(
                            allStudents =
                                students.applyClassFilter(
                                    state.selectedClassFilter
                                )
                        )
                    }
                }.onFailure { e ->
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

    private fun initializeClasses() {
        viewModelScope.launch {
            classManagementUseCases.getAllClassesUseCase().collectLatest { result ->
                result.onSuccess { classes ->
                    _uiState.update { it.copy(availableClasses = classes) }
                }
            }
        }
    }

    private fun initializeAvailableMonths() {
        viewModelScope.launch {
            attendanceUseCases.getAvailableMonthsUseCase().collectLatest { result ->
                result.onSuccess { months ->
                    _uiState.update { it.copy(availableMonths = months) }
                }.onFailure { e ->
                    _uiState.update {
                        it.copy(
                            error =
                                UiText.StringResource(
                                    R.string.error_loading_available_months,
                                    e.message ?: "Unknown error"
                                )
                        )
                    }
                }
            }
        }
    }

    private fun initializeStudentHistory() {
        viewModelScope.launch {
            _uiState.map { it.selectedStudentForHistory }.filterNotNull().flatMapLatest { student ->
                attendanceUseCases.getStudentAttendanceDatesUseCase(student.id)
            }.collectLatest { result ->
                result.onSuccess { historyDates ->
                    val history =
                        historyDates.groupBy { LocalDate(it.year, it.month, 1) }.map { entry ->
                            Pair(entry.key, entry.value.sorted())
                        }.sortedByDescending { it.first }
                    _uiState.update { it.copy(studentHistory = history) }
                }.onFailure { e ->
                    _uiState.update {
                        it.copy(
                            error =
                                UiText.StringResource(
                                    R.string.error_loading_student_history,
                                    e.message ?: "Unknown error"
                                )
                        )
                    }
                }
            }
        }
    }

    fun onEvent(event: ReportScreenEvent) {
        when (event) {
            is ReportScreenEvent.UpdateSelectedMonth -> updateSelectedMonth(event.month)
            is ReportScreenEvent.ToggleSortType -> toggleSortType()
            is ReportScreenEvent.ClearMonthFilter -> clearMonthFilter()
            is ReportScreenEvent.SelectStudentForHistory -> selectStudentForHistory(event.student)
            is ReportScreenEvent.PrepareCalendarImageForSharing ->
                prepareCalendarImageForSharing(
                    event.month,
                    event.studentId,
                    event.studentName
                )

            is ReportScreenEvent.ConsumeShareFileUri -> _uiState.update { it.copy(shareFileUri = null) }

            is ReportScreenEvent.ShowToast -> showToast(event.message)
            is ReportScreenEvent.ConsumeToastMessage -> _uiState.update { it.copy(toastMessage = null) }

            is ReportScreenEvent.ShowHistoryDialog -> _uiState.update { it.copy(showHistoryDialog = event.show) }

            is ReportScreenEvent.ShowCalendarPreviewDialog ->
                _uiState.update {
                    it.copy(
                        showCalendarPreviewDialog = event.show
                    )
                }

            is ReportScreenEvent.SelectMonthYearForCalendarPreview ->
                _uiState.update {
                    it.copy(
                        selectedMonthYearForCalendarPreview = event.month
                    )
                }

            is ReportScreenEvent.ToggleMonthDropdown -> _uiState.update { it.copy(monthDropdownExpanded = event.expanded) }

            is ReportScreenEvent.SelectClassFilter ->
                _uiState.update { state ->
                    state.copy(
                        selectedClassFilter = event.filter,
                        classDropdownExpanded = false,
                        allStudents = rawStudents.applyClassFilter(event.filter)
                    )
                }

            is ReportScreenEvent.ToggleClassFilterVisibility ->
                _uiState.update {
                    it.copy(
                        isClassFilterVisible = !it.isClassFilterVisible
                    )
                }

            is ReportScreenEvent.ToggleSortComponentsVisibility ->
                _uiState.update {
                    it.copy(isSortComponentsVisible = !it.isSortComponentsVisible)
                }

            is ReportScreenEvent.ToggleClassDropdown -> _uiState.update { it.copy(classDropdownExpanded = event.expanded) }
        }
    }

    private fun updateSelectedMonth(month: LocalDate?) {
        _uiState.update { it.copy(selectedMonth = month) }
    }

    private fun toggleSortType() {
        _uiState.update {
            it.copy(
                sortType =
                    if (it.sortType == SortType.ByName) {
                        SortType.ByAttendance
                    } else {
                        SortType.ByName
                    }
            )
        }
    }

    private fun clearMonthFilter() {
        _uiState.update { it.copy(selectedMonth = null) }
    }

    private fun selectStudentForHistory(student: Student?) {
        _uiState.update { it.copy(selectedStudentForHistory = student) }
    }

    private fun showToast(message: UiText) {
        _uiState.update { it.copy(toastMessage = message) }
    }

    private fun prepareCalendarImageForSharing(
        month: LocalDate,
        studentId: Int,
        studentName: String
    ) {
        if (_uiState.value.selectedStudentForHistory?.id != studentId) {
            onEvent(
                ReportScreenEvent.ShowToast(
                    UiText.StringResource(R.string.error_student_not_selected_history)
                )
            )
            return
        }
        viewModelScope.launch {
            try {
                val startOfMonth = LocalDate(month.year, month.month, 1)
                val endOfMonth = startOfMonth.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)

                attendanceUseCases.getAttendanceHistoryForDateRangeUseCase(
                    studentId,
                    startOfMonth,
                    endOfMonth
                ).collectLatest { result ->
                    result.onSuccess { items ->
                        reportUseCases.shareReportUseCase(
                            studentName,
                            month,
                            items.map { it.date }
                        ).onSuccess { uri ->
                            _uiState.update {
                                it.copy(shareFileUri = uri.toUri())
                            }
                        }.onFailure { e ->
                            onEvent(
                                ReportScreenEvent.ShowToast(
                                    UiText.StringResource(
                                        R.string.error_preparing_image,
                                        e.message ?: ""
                                    )
                                )
                            )
                        }
                    }.onFailure { e ->
                        onEvent(
                            ReportScreenEvent.ShowToast(
                                UiText.StringResource(
                                    R.string.error_fetching_history,
                                    e.message ?: ""
                                )
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                onEvent(
                    ReportScreenEvent.ShowToast(
                        UiText.StringResource(
                            R.string.error_preparing_image,
                            e.message ?: ""
                        )
                    )
                )
            }
        }
    }
}