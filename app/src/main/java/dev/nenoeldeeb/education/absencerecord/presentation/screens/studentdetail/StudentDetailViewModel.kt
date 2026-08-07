package dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentClass
import dev.nenoeldeeb.education.absencerecord.domain.usecases.AttendanceUseCases
import dev.nenoeldeeb.education.absencerecord.domain.usecases.ClassManagementUseCases
import dev.nenoeldeeb.education.absencerecord.domain.usecases.ReportUseCases
import dev.nenoeldeeb.education.absencerecord.domain.usecases.StudentManagementUseCases
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText
import dev.nenoeldeeb.education.absencerecord.presentation.utils.toUiText
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class StudentDetailViewModel(
    private val studentId: Int,
    private val studentManagementUseCases: StudentManagementUseCases,
    private val attendanceUseCases: AttendanceUseCases,
    private val reportUseCases: ReportUseCases,
    private val classManagementUseCases: ClassManagementUseCases
) : ViewModel() {
    private val _uiState = MutableStateFlow(StudentDetailScreenState())
    val uiState: StateFlow<StudentDetailScreenState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<StudentDetailUiEffect>()
    val uiEffect: SharedFlow<StudentDetailUiEffect> = _uiEffect.asSharedFlow()

    init {
        initializeData()
    }

    private fun initializeData() {
        viewModelScope.launch {
            combine(
                studentManagementUseCases.getStudentByIdUseCase(studentId),
                classManagementUseCases.getAllClassesUseCase(),
                attendanceUseCases.getStudentAttendanceDatesUseCase(studentId)
            ) { studentResult, classesResult, datesResult ->
                mergeLoadResults(studentResult, classesResult, datesResult)
            }.collectLatest { }
        }
    }

    private fun mergeLoadResults(
        studentResult: Result<Student?>,
        classesResult: Result<List<StudentClass>>,
        datesResult: Result<List<LocalDate>>
    ) {
        _uiState.update { state ->
            var current = state

            studentResult.onSuccess { student ->
                current =
                    current.copy(
                        student = student,
                        assignedClassName = resolveAssignedClassName(student, current.availableClasses),
                        isLoading = false
                    )
            }.onFailure { e ->
                current = current.copy(error = e.toUiText(), isLoading = false)
            }

            classesResult.onSuccess { classes ->
                current =
                    current.copy(
                        availableClasses = classes,
                        assignedClassName = resolveAssignedClassName(current.student, classes)
                    )
            }.onFailure { e ->
                current = current.copy(error = e.toUiText())
            }

            datesResult.onSuccess { dates ->
                current = current.copy(allAttendanceDates = dates)
            }.onFailure { e ->
                current = current.copy(error = e.toUiText())
            }

            current
        }
    }

    private fun resolveAssignedClassName(
        student: Student?,
        classes: List<StudentClass>
    ): String? {
        return student?.classId?.let { studentClassId ->
            classes.firstOrNull { it.id == studentClassId }?.name
        }
    }

    fun onEvent(event: StudentDetailScreenEvent) {
        when (event) {
            is StudentDetailScreenEvent.ToggleEditNameDialog ->
                _uiState.update { it.copy(isEditNameDialogOpen = !it.isEditNameDialogOpen) }

            is StudentDetailScreenEvent.UpdateStudentName -> updateStudentName(event.newName)

            is StudentDetailScreenEvent.ToggleChangeClassDialog ->
                _uiState.update { it.copy(isChangeClassDialogOpen = !it.isChangeClassDialogOpen) }

            is StudentDetailScreenEvent.UpdateStudentClass -> updateStudentClass(event.classId)

            is StudentDetailScreenEvent.ToggleDeleteConfirmationDialog ->
                _uiState.update { it.copy(isDeleteConfirmationDialogOpen = !it.isDeleteConfirmationDialogOpen) }

            is StudentDetailScreenEvent.ConfirmDeleteStudent -> confirmDeleteStudent()

            is StudentDetailScreenEvent.SelectMonth ->
                _uiState.update { it.copy(selectedMonth = event.month) }

            is StudentDetailScreenEvent.ShareAttendanceReport -> shareAttendanceReport()

            is StudentDetailScreenEvent.ShareFileResult ->
                handleShareFileResult(event.uri, event.error)

            is StudentDetailScreenEvent.ConsumeError ->
                _uiState.update { it.copy(error = null) }

            is StudentDetailScreenEvent.ConsumeToastMessage ->
                _uiState.update { it.copy(toastMessage = null) }
        }
    }

    private fun updateStudentName(newName: String) {
        val currentStudent = _uiState.value.student ?: return
        val trimmedName = newName.trim()
        if (trimmedName.isEmpty()) {
            return
        }
        viewModelScope.launch {
            studentManagementUseCases.updateStudentUseCase(
                currentStudent.copy(name = trimmedName)
            ).onSuccess {
                _uiState.update { state ->
                    state.copy(
                        student = state.student?.copy(name = trimmedName),
                        isEditNameDialogOpen = false
                    )
                }
            }.onFailure { e ->
                _uiState.update { state ->
                    state.copy(isEditNameDialogOpen = false, error = e.toUiText())
                }
            }
        }
    }

    private fun updateStudentClass(classId: Int?) {
        val currentStudent = _uiState.value.student ?: return
        viewModelScope.launch {
            studentManagementUseCases.updateStudentUseCase(
                currentStudent.copy(classId = classId)
            ).onSuccess {
                _uiState.update { state ->
                    val updatedStudent = state.student?.copy(classId = classId)
                    state.copy(
                        student = updatedStudent,
                        assignedClassName = resolveAssignedClassName(updatedStudent, state.availableClasses),
                        isChangeClassDialogOpen = false
                    )
                }
            }.onFailure { e ->
                _uiState.update { state ->
                    state.copy(isChangeClassDialogOpen = false, error = e.toUiText())
                }
            }
        }
    }

    private fun confirmDeleteStudent() {
        val currentStudent = _uiState.value.student ?: return
        viewModelScope.launch {
            studentManagementUseCases.deleteStudentsUseCase(listOf(currentStudent))
                .onSuccess {
                    _uiState.update { it.copy(isDeleteConfirmationDialogOpen = false) }
                    _uiEffect.emit(StudentDetailUiEffect.NavigateBack)
                }
                .onFailure { e ->
                    _uiState.update { state ->
                        state.copy(isDeleteConfirmationDialogOpen = false, error = e.toUiText())
                    }
                }
        }
    }

    private fun shareAttendanceReport() {
        val state = _uiState.value
        val student = state.student ?: return
        val month = state.selectedMonth ?: currentMonth()
        val startOfMonth = LocalDate(month.year, month.month, 1)
        val endOfMonth = startOfMonth.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)

        viewModelScope.launch {
            attendanceUseCases.getAttendanceHistoryForDateRangeUseCase(
                studentId,
                startOfMonth,
                endOfMonth
            ).collectLatest { result ->
                result.onSuccess { items ->
                    reportUseCases.shareReportUseCase(
                        student.name,
                        month,
                        items.map { it.date }
                    ).onSuccess { filePath ->
                        _uiState.update { it.copy(shareFileUri = filePath.toUri()) }
                    }.onFailure { e ->
                        _uiState.update { it.copy(toastMessage = e.toUiText()) }
                    }
                }.onFailure { e ->
                    _uiState.update { it.copy(toastMessage = e.toUiText()) }
                }
            }
        }
    }

    private fun currentMonth(): LocalDate {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        return LocalDate(now.year, now.month, 1)
    }

    private fun handleShareFileResult(
        uri: Uri,
        error: UiText?
    ) {
        if (error != null) {
            _uiState.update { it.copy(toastMessage = error) }
        }
        _uiState.update { it.copy(shareFileUri = null) }
    }
}