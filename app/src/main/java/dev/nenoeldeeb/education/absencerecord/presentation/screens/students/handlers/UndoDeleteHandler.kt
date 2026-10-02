package dev.nenoeldeeb.education.absencerecord.presentation.screens.students.handlers

import dev.nenoeldeeb.education.absencerecord.domain.models.DeletedStudentBackup
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentAttendance
import dev.nenoeldeeb.education.absencerecord.domain.usecases.AttendanceUseCases
import dev.nenoeldeeb.education.absencerecord.domain.usecases.StudentManagementUseCases
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenState
import dev.nenoeldeeb.education.absencerecord.presentation.utils.toUiText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UndoDeleteHandler(
    private val studentManagementUseCases: StudentManagementUseCases,
    private val attendanceUseCases: AttendanceUseCases
) {
    suspend fun buildBackup(students: List<Student>): List<DeletedStudentBackup> {
        return students.map { student ->
            val dates =
                try {
                    val result =
                        attendanceUseCases.getStudentAttendanceDatesUseCase(student.id).first()
                    result.getOrElse { emptyList() }
                } catch (_: Exception) {
                    emptyList()
                }
            DeletedStudentBackup(student = student, attendanceDates = dates)
        }
    }

    fun handleUndo(
        _uiState: MutableStateFlow<StudentsScreenState>,
        viewModelScope: CoroutineScope
    ) {
        val backup = _uiState.value.lastDeletedBackup
        if (backup.isEmpty()) return
        _uiState.update { it.copy(lastDeletedBackup = emptyList()) }
        viewModelScope.launch {
            for (entry in backup) {
                val addResult =
                    studentManagementUseCases.addStudentUseCase(
                        Student(name = entry.student.name, classId = entry.student.classId)
                    )
                val newId =
                    addResult.fold(
                        onSuccess = { id -> id.toInt() },
                        onFailure = { e ->
                            _uiState.update { state -> state.copy(error = e.toUiText()) }
                            return@launch
                        }
                    )
                for (date in entry.attendanceDates) {
                    attendanceUseCases.recordStudentAttendanceUseCase(
                        StudentAttendance(studentId = newId, date = date)
                    ).onFailure { e ->
                        _uiState.update { state -> state.copy(error = e.toUiText()) }
                        return@launch
                    }
                }
            }
        }
    }

    fun consumeBackup(_uiState: MutableStateFlow<StudentsScreenState>) {
        _uiState.update { it.copy(lastDeletedBackup = emptyList()) }
    }
}