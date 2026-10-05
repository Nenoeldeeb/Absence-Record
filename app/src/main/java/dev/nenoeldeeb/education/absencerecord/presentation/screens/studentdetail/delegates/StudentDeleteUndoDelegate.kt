package dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.delegates

import dev.nenoeldeeb.education.absencerecord.domain.models.BusyAppointment
import dev.nenoeldeeb.education.absencerecord.domain.models.DeletedStudentBackup
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentAttendance
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentLessonEntry
import dev.nenoeldeeb.education.absencerecord.domain.usecases.AttendanceUseCases
import dev.nenoeldeeb.education.absencerecord.domain.usecases.StudentManagementUseCases
import dev.nenoeldeeb.education.absencerecord.domain.usecases.schedule.ScheduleUseCases
import dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.StudentDetailScreenState
import dev.nenoeldeeb.education.absencerecord.presentation.utils.toUiText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class StudentDeleteUndoDelegate(
    private val studentManagementUseCases: StudentManagementUseCases,
    private val attendanceUseCases: AttendanceUseCases,
    private val scheduleUseCases: ScheduleUseCases,
    private val scope: CoroutineScope,
    private val updateState: ((StudentDetailScreenState) -> StudentDetailScreenState) -> Unit,
    private val emitNavigateBack: suspend () -> Unit
) {
    fun confirmDelete(state: StudentDetailScreenState) {
        val student = state.student ?: return
        val backup =
            DeletedStudentBackup(
                student = student,
                attendanceDates = state.allAttendanceDates
            )
        val lessons = state.studentSchedule?.lessons.orEmpty()
        val busy = state.studentSchedule?.busy.orEmpty()
        scope.launch {
            studentManagementUseCases.deleteStudentsUseCase(listOf(student))
                .onSuccess {
                    updateState {
                        it.copy(
                            isDeleteConfirmationDialogOpen = false,
                            deletedStudentBackup = backup,
                            deletedStudentLessons = lessons,
                            deletedStudentBusy = busy
                        )
                    }
                }
                .onFailure { e ->
                    updateState { current ->
                        current.copy(isDeleteConfirmationDialogOpen = false, error = e.toUiText())
                    }
                }
        }
    }

    fun undoDelete(state: StudentDetailScreenState) {
        val backup = state.deletedStudentBackup ?: return
        val lessons = state.deletedStudentLessons
        val busy = state.deletedStudentBusy
        updateState {
            it.copy(
                deletedStudentBackup = null,
                deletedStudentLessons = emptyList(),
                deletedStudentBusy = emptyList()
            )
        }
        scope.launch {
            val newId = restoreStudent(backup) ?: return@launch
            restoreAttendance(newId, backup.attendanceDates)
            restoreSchedule(newId, lessons, busy)
            emitNavigateBack()
        }
    }

    fun dismissDeleted() {
        updateState {
            it.copy(
                deletedStudentBackup = null,
                deletedStudentLessons = emptyList(),
                deletedStudentBusy = emptyList()
            )
        }
        scope.launch { emitNavigateBack() }
    }

    private suspend fun restoreStudent(backup: DeletedStudentBackup): Int? {
        val result =
            studentManagementUseCases.addStudentUseCase(
                Student(name = backup.student.name, classId = backup.student.classId)
            )
        return result.fold(
            onSuccess = { id -> id.toInt() },
            onFailure = { e ->
                updateState { it.copy(error = e.toUiText()) }
                null
            }
        )
    }

    private suspend fun restoreAttendance(
        newId: Int,
        dates: List<LocalDate>
    ) {
        for (date in dates) {
            attendanceUseCases.recordStudentAttendanceUseCase(
                StudentAttendance(studentId = newId, date = date)
            ).onFailure { e ->
                updateState { it.copy(error = e.toUiText()) }
                return
            }
        }
    }

    private suspend fun restoreSchedule(
        newId: Int,
        lessons: List<StudentLessonEntry>,
        busy: List<BusyAppointment>
    ) {
        for (lesson in lessons) {
            val hoursResult =
                scheduleUseCases.observeHoursForWeekdayUseCase(lesson.weekday).first()
            val hourId =
                hoursResult.getOrNull()
                    ?.firstOrNull { it.hour.startMinutes == lesson.startMinutes }
                    ?.hour?.id
            if (hourId == null) continue
            scheduleUseCases.assignStudentUseCase(hourId, newId, lesson.weekday)
                .onFailure { e ->
                    updateState { it.copy(error = e.toUiText()) }
                }
        }
        for (appointment in busy) {
            scheduleUseCases.insertBusyAppointmentUseCase(
                newId,
                appointment.weekday,
                appointment.startMinutes,
                appointment.durationMinutes
            ).onFailure { e ->
                updateState { it.copy(error = e.toUiText()) }
            }
        }
    }
}