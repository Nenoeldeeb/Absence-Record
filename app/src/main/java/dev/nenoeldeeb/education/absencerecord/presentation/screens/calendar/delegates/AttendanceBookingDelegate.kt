package dev.nenoeldeeb.education.absencerecord.presentation.screens.calendar.delegates

import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentAttendance
import dev.nenoeldeeb.education.absencerecord.domain.usecases.AttendanceUseCases
import dev.nenoeldeeb.education.absencerecord.presentation.screens.calendar.CalendarScreenState
import dev.nenoeldeeb.education.absencerecord.presentation.screens.calendar.LastAttendanceChange
import dev.nenoeldeeb.education.absencerecord.presentation.utils.DateFormatter
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText
import dev.nenoeldeeb.education.absencerecord.presentation.utils.toUiText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/**
 * Attendance booking presenter for the calendar: mark, delete and the one-shot
 * undo of the last change. Reads nothing from state — the undo change is
 * passed in and the roster comes from [studentsProvider] — so per AGENTS.md it
 * mutates state only through `updateState` and launches only via `scope`.
 */
class AttendanceBookingDelegate(
    private val attendanceUseCases: AttendanceUseCases,
    private val scope: CoroutineScope,
    private val studentsProvider: () -> List<Student>,
    private val updateState: ((CalendarScreenState) -> CalendarScreenState) -> Unit
) {
    private val inFlightStudentDates = mutableSetOf<Pair<Int, LocalDate>>()

    fun markStudentAttendance(
        studentId: Int,
        date: LocalDate
    ) {
        if (date > today()) {
            updateState {
                it.copy(error = UiText.StringResource(R.string.error_future_date_not_allowed))
            }
            return
        }
        val key = studentId to date
        if (!inFlightStudentDates.add(key)) return

        scope.launch {
            try {
                attendanceUseCases.recordStudentAttendanceUseCase(
                    StudentAttendance(studentId = studentId, date = date)
                )
                    .onSuccess {
                        val name = studentsProvider().firstOrNull { it.id == studentId }?.name
                        updateState {
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
                        updateState {
                            it.copy(error = e.toUiText())
                        }
                    }
            } finally {
                inFlightStudentDates.remove(key)
            }
        }
    }

    fun deleteStudentAttendance(
        studentId: Int,
        date: LocalDate
    ) {
        if (date > today()) {
            updateState {
                it.copy(error = UiText.StringResource(R.string.error_future_date_not_allowed))
            }
            return
        }
        val key = studentId to date
        if (!inFlightStudentDates.add(key)) return

        scope.launch {
            try {
                attendanceUseCases.deleteStudentAttendanceUseCase(studentId, date)
                    .onSuccess {
                        val name = studentsProvider().firstOrNull { it.id == studentId }?.name
                        updateState {
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
                        updateState {
                            it.copy(error = e.toUiText())
                        }
                    }
            } finally {
                inFlightStudentDates.remove(key)
            }
        }
    }

    fun undoLastAttendanceChange(change: LastAttendanceChange?) {
        if (change == null) return
        updateState { it.copy(lastAttendanceChange = null, attendanceMessage = null) }
        if (change.markedPresent) {
            deleteStudentAttendance(change.studentId, change.date)
        } else {
            markStudentAttendance(change.studentId, change.date)
        }
    }

    private fun today(): LocalDate = DateFormatter.today()
}