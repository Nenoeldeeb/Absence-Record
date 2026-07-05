package dev.nenoeldeeb.education.absencerecord.presentation.screens.calendar

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentAttendance
import dev.nenoeldeeb.education.absencerecord.domain.usecases.AttendanceUseCases
import dev.nenoeldeeb.education.absencerecord.domain.usecases.ClassManagementUseCases
import dev.nenoeldeeb.education.absencerecord.domain.usecases.StudentManagementUseCases
import dev.nenoeldeeb.education.absencerecord.presentation.utils.applyMultiClassFilter
import dev.nenoeldeeb.education.absencerecord.presentation.utils.toUiText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

@Stable
class CalendarViewModel(
    private val attendanceUseCases: AttendanceUseCases,
    private val studentManagementUseCases: StudentManagementUseCases,
    private val classManagementUseCases: ClassManagementUseCases
) : ViewModel() {
    private val _uiState = MutableStateFlow(CalendarScreenState())
    val uiState: StateFlow<CalendarScreenState> = _uiState.asStateFlow()

    private var rawStudents: List<Student> = emptyList()

    init {
        initializeStudents()
        initializeClasses()
        setupAttendanceListener()
    }

    private fun initializeStudents() {
        viewModelScope.launch {
            studentManagementUseCases.getAllStudentsUseCase().collectLatest { result ->
                result
                    .onSuccess { students ->
                        rawStudents = students
                        _uiState.update { state ->
                            val filteredStudents =
                                students.applyMultiClassFilter(state.selectedClassIds)
                            val visibleIds = filteredStudents.map { it.id }.toSet()
                            state.copy(
                                allStudents = filteredStudents,
                                studentsForSelectedDate =
                                    state.studentsForSelectedDate.filter {
                                        it.studentId in visibleIds
                                    }
                            )
                        }
                    }
                    .onFailure { e ->
                        _uiState.update {
                            it.copy(error = e.toUiText())
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
                    .onFailure { e ->
                        _uiState.update { it.copy(error = e.toUiText()) }
                    }
            }
        }
    }

    fun onEvent(event: CalendarScreenEvent) {
        when (event) {
            is CalendarScreenEvent.MarkStudentAttendance ->
                markStudentAttendance(event.studentId, event.date)

            is CalendarScreenEvent.DeleteStudentAttendance ->
                deleteStudentAttendance(event.studentId, event.date)

            is CalendarScreenEvent.SelectDateForDialog ->
                _uiState.update {
                    it.copy(
                        selectedDateForDialog = event.date,
                        studentsForSelectedDate =
                            if (event.date == null) {
                                emptyList()
                            } else {
                                it.studentsForSelectedDate
                            }
                    )
                }

            is CalendarScreenEvent.ToggleClassSelection ->
                toggleClassSelection(event.classId)

            is CalendarScreenEvent.ToggleFilterDropdown ->
                _uiState.update { it.copy(filterDropdownExpanded = !it.filterDropdownExpanded) }

            is CalendarScreenEvent.ConsumeError ->
                _uiState.update { it.copy(error = null) }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun setupAttendanceListener() {
        viewModelScope.launch {
            _uiState
                .map { it.selectedDateForDialog }
                .filterNotNull()
                .flatMapLatest { date -> attendanceUseCases.getAttendanceForDateUseCase(date) }
                .collectLatest { result ->
                    result
                        .onSuccess { selectedStudentsForDate ->
                            _uiState.update { state ->
                                val visibleIds = state.allStudents.map { it.id }.toSet()
                                state.copy(
                                    studentsForSelectedDate =
                                        selectedStudentsForDate.filter {
                                            it.studentId in visibleIds
                                        }
                                )
                            }
                        }
                        .onFailure { e ->
                            _uiState.update {
                                it.copy(error = e.toUiText())
                            }
                        }
                }
        }
    }

    private fun toggleClassSelection(classId: Int) {
        viewModelScope.launch {
            val updatedIds = _uiState.value.selectedClassIds.toMutableSet()
            if (classId in updatedIds) {
                updatedIds.remove(classId)
            } else {
                updatedIds.add(classId)
            }

            val filteredStudents = rawStudents.applyMultiClassFilter(updatedIds)

            _uiState.update { state ->
                val visibleIds = filteredStudents.map { it.id }.toSet()
                state.copy(
                    selectedClassIds = updatedIds,
                    allStudents = filteredStudents,
                    studentsForSelectedDate =
                        state.studentsForSelectedDate.filter {
                            it.studentId in visibleIds
                        }
                )
            }
        }
    }

    private fun markStudentAttendance(
        studentId: Int,
        date: LocalDate
    ) {
        viewModelScope.launch {
            attendanceUseCases.recordStudentAttendanceUseCase(
                StudentAttendance(studentId = studentId, date = date)
            )
                .onFailure { e ->
                    _uiState.update {
                        it.copy(error = e.toUiText())
                    }
                }
        }
    }

    private fun deleteStudentAttendance(
        studentId: Int,
        date: LocalDate
    ) {
        viewModelScope.launch {
            attendanceUseCases.deleteStudentAttendanceUseCase(studentId, date).onFailure { e ->
                _uiState.update {
                    it.copy(error = e.toUiText())
                }
            }
        }
    }
}