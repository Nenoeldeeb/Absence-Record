package dev.nenoeldeeb.education.absencerecord.presentation.screens.calendar

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentAttendance
import dev.nenoeldeeb.education.absencerecord.domain.usecases.AttendanceUseCases
import dev.nenoeldeeb.education.absencerecord.domain.usecases.StudentManagementUseCases
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText
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
    private val studentManagementUseCases: StudentManagementUseCases
) : ViewModel() {
    private val _uiState = MutableStateFlow(CalendarScreenState())
    val uiState: StateFlow<CalendarScreenState> = _uiState.asStateFlow()

    init {
        initializeStudents()
        setupAttendanceListener()
    }

    private fun initializeStudents() {
        viewModelScope.launch {
            studentManagementUseCases.getAllStudentsUseCase()
                .collectLatest { result ->
                    result.onSuccess { students ->
                        _uiState.update { it.copy(allStudents = students) }
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

    fun onEvent(event: CalendarScreenEvent) {
        when (event) {
            is CalendarScreenEvent.MarkStudentAttendance ->
                markStudentAttendance(
                    event.studentId,
                    event.date
                )

            is CalendarScreenEvent.DeleteStudentAttendance ->
                deleteStudentAttendance(
                    event.studentId,
                    event.date
                )

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
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun setupAttendanceListener() {
        viewModelScope.launch {
            _uiState
                .map { it.selectedDateForDialog }
                .filterNotNull()
                .flatMapLatest { date ->
                    attendanceUseCases.getAttendanceForDateUseCase(date)
                }
                .collectLatest { result ->
                    result.onSuccess { selectedStudentsForDate ->
                        _uiState.update {
                            it.copy(studentsForSelectedDate = selectedStudentsForDate)
                        }
                    }.onFailure { e ->
                        _uiState.update {
                            it.copy(
                                error =
                                    UiText.StringResource(
                                        R.string.error_loading_attendance,
                                        e.message ?: "Unknown error"
                                    )
                            )
                        }
                    }
                }
        }
    }

    private fun markStudentAttendance(
        studentId: Int,
        date: LocalDate
    ) {
        viewModelScope.launch {
            attendanceUseCases.recordStudentAttendanceUseCase(
                StudentAttendance(
                    studentId = studentId,
                    date = date
                )
            ).onFailure { e ->
                _uiState.update {
                    it.copy(
                        error =
                            UiText.StringResource(
                                R.string.error_marking_attendance,
                                e.message ?: "Unknown error"
                            )
                    )
                }
            }
        }
    }

    private fun deleteStudentAttendance(
        studentId: Int,
        date: LocalDate
    ) {
        viewModelScope.launch {
            attendanceUseCases.deleteStudentAttendanceUseCase(studentId, date)
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            error =
                                UiText.StringResource(
                                    R.string.error_deleting_attendance,
                                    e.message ?: "Unknown error"
                                )
                        )
                    }
                }
        }
    }
}