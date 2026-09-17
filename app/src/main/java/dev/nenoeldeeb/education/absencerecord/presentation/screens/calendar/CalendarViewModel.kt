package dev.nenoeldeeb.education.absencerecord.presentation.screens.calendar

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentAttendance
import dev.nenoeldeeb.education.absencerecord.domain.repositories.ClassFilterRepository
import dev.nenoeldeeb.education.absencerecord.domain.usecases.AttendanceUseCases
import dev.nenoeldeeb.education.absencerecord.domain.usecases.ClassManagementUseCases
import dev.nenoeldeeb.education.absencerecord.domain.usecases.StudentManagementUseCases
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText
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
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

@Stable
class CalendarViewModel(
    private val attendanceUseCases: AttendanceUseCases,
    private val studentManagementUseCases: StudentManagementUseCases,
    private val classManagementUseCases: ClassManagementUseCases,
    private val classFilterRepository: ClassFilterRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CalendarScreenState())
    val uiState: StateFlow<CalendarScreenState> = _uiState.asStateFlow()

    private var rawStudents: List<Student> = emptyList()

    init {
        initializeStudents()
        initializeClasses()
        setupAttendanceListener()
        observeClassFilter()
        observeMarkedDates()
    }

    private fun initializeStudents() {
        viewModelScope.launch {
            studentManagementUseCases.getAllStudentsUseCase().collectLatest { result ->
                result
                    .onSuccess { students ->
                        rawStudents = students
                        _uiState.update { state ->
                            val filteredStudents =
                                students.applyMultiClassFilter(classFilterRepository.selectedClassIds.value)
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

            is CalendarScreenEvent.ConsumeAttendanceMessage ->
                _uiState.update { it.copy(attendanceMessage = null, lastAttendanceChange = null) }

            is CalendarScreenEvent.ConsumeSound ->
                _uiState.update { it.copy(pendingSoundIsAdd = null) }

            is CalendarScreenEvent.UndoLastAttendanceChange ->
                undoLastAttendanceChange()
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
        classFilterRepository.toggleClass(classId)
    }

    private fun observeClassFilter() {
        viewModelScope.launch {
            classFilterRepository.selectedClassIds.collectLatest { ids ->
                _uiState.update { state ->
                    val filteredStudents = rawStudents.applyMultiClassFilter(ids)
                    val visibleIds = filteredStudents.map { it.id }.toSet()
                    state.copy(
                        selectedClassIds = ids,
                        allStudents = filteredStudents,
                        studentsForSelectedDate =
                            state.studentsForSelectedDate.filter {
                                it.studentId in visibleIds
                            }
                    )
                }
            }
        }
    }

    private fun observeMarkedDates() {
        viewModelScope.launch {
            attendanceUseCases.getMarkedDatesUseCase().collectLatest { result ->
                result
                    .onSuccess { dates ->
                        _uiState.update { it.copy(markedDates = dates.toSet()) }
                    }
                    .onFailure { e ->
                        _uiState.update { it.copy(error = e.toUiText()) }
                    }
            }
        }
    }

    private fun today(): LocalDate {
        return Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    }

    private fun undoLastAttendanceChange() {
        val change = _uiState.value.lastAttendanceChange ?: return
        _uiState.update { it.copy(lastAttendanceChange = null, attendanceMessage = null) }
        if (change.markedPresent) {
            deleteStudentAttendance(change.studentId, change.date)
        } else {
            markStudentAttendance(change.studentId, change.date)
        }
    }

    private fun markStudentAttendance(
        studentId: Int,
        date: LocalDate
    ) {
        if (date > today()) {
            _uiState.update {
                it.copy(error = UiText.StringResource(R.string.error_future_date_not_allowed))
            }
            return
        }
        viewModelScope.launch {
            attendanceUseCases.recordStudentAttendanceUseCase(
                StudentAttendance(studentId = studentId, date = date)
            )
                .onSuccess {
                    val name = rawStudents.firstOrNull { it.id == studentId }?.name
                    _uiState.update {
                        it.copy(
                            attendanceMessage =
                                if (name != null) {
                                    UiText.StringResource(R.string.calendar_attendance_marked, name)
                                } else {
                                    null
                                },
                            lastAttendanceChange =
                                LastAttendanceChange(
                                    studentId = studentId,
                                    date = date,
                                    markedPresent = true
                                ),
                            pendingSoundIsAdd = true
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

    private fun deleteStudentAttendance(
        studentId: Int,
        date: LocalDate
    ) {
        if (date > today()) {
            _uiState.update {
                it.copy(error = UiText.StringResource(R.string.error_future_date_not_allowed))
            }
            return
        }
        viewModelScope.launch {
            attendanceUseCases.deleteStudentAttendanceUseCase(studentId, date)
                .onSuccess {
                    val name = rawStudents.firstOrNull { it.id == studentId }?.name
                    _uiState.update {
                        it.copy(
                            attendanceMessage =
                                if (name != null) {
                                    UiText.StringResource(R.string.calendar_attendance_removed, name)
                                } else {
                                    null
                                },
                            lastAttendanceChange =
                                LastAttendanceChange(
                                    studentId = studentId,
                                    date = date,
                                    markedPresent = false
                                ),
                            pendingSoundIsAdd = false
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