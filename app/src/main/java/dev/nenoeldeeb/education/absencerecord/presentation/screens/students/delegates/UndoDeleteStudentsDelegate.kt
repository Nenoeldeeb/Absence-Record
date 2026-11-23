package dev.nenoeldeeb.education.absencerecord.presentation.screens.students.delegates

import dev.nenoeldeeb.education.absencerecord.domain.models.DeletedStudentBackup
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentAttendance
import dev.nenoeldeeb.education.absencerecord.domain.usecases.AttendanceUseCases
import dev.nenoeldeeb.education.absencerecord.domain.usecases.StudentManagementUseCases
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenState
import dev.nenoeldeeb.education.absencerecord.presentation.utils.toUiText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Restores the students deleted by a bulk action, together with their
 * recorded attendance dates. Owns the undo backup lifecycle: building it
 * before deletion, restoring on undo, and discarding it when undo expires.
 */
class UndoDeleteStudentsDelegate(
    private val studentManagementUseCases: StudentManagementUseCases,
    private val attendanceUseCases: AttendanceUseCases,
    private val scope: CoroutineScope,
    private val updateState: ((StudentsScreenState) -> StudentsScreenState) -> Unit
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

    /**
     * Restores every student in [backup]. The backup snapshot is passed in by
     * the ViewModel, so this delegate never reads state directly.
     */
    fun handleUndo(backup: List<DeletedStudentBackup>) {
        if (backup.isEmpty()) return
        updateState { it.copy(lastDeletedBackup = emptyList()) }
        scope.launch {
            for (entry in backup) {
                val addResult =
                    studentManagementUseCases.addStudentUseCase(
                        Student(name = entry.student.name, classId = entry.student.classId)
                    )
                val newId =
                    addResult.fold(
                        onSuccess = { id -> id.toInt() },
                        onFailure = { e ->
                            updateState { state -> state.copy(error = e.toUiText()) }
                            return@launch
                        }
                    )
                for (date in entry.attendanceDates) {
                    attendanceUseCases.recordStudentAttendanceUseCase(
                        StudentAttendance(studentId = newId, date = date)
                    ).onFailure { e ->
                        updateState { state -> state.copy(error = e.toUiText()) }
                        return@launch
                    }
                }
            }
        }
    }

    fun consumeBackup() {
        updateState { it.copy(lastDeletedBackup = emptyList()) }
    }
}