package dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.delegates

import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.BusyAppointment
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentLessonEntry
import dev.nenoeldeeb.education.absencerecord.domain.services.ScheduleRules
import dev.nenoeldeeb.education.absencerecord.domain.usecases.schedule.ScheduleUseCases
import dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.StudentDetailScreenState
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText
import dev.nenoeldeeb.education.absencerecord.presentation.utils.toUiText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val MIN_BUSY_DURATION_MINUTES = 30
private const val MAX_BUSY_DURATION_MINUTES = 360

class BusyManagementDelegate(
    private val scheduleUseCases: ScheduleUseCases,
    private val scope: CoroutineScope,
    private val updateState: ((StudentDetailScreenState) -> StudentDetailScreenState) -> Unit
) {
    fun openBusyDialog(busy: BusyAppointment?) {
        val snappedDuration =
            busy?.durationMinutes
                ?.let { ((it + 15) / 30) * 30 }
                ?.coerceIn(MIN_BUSY_DURATION_MINUTES, MAX_BUSY_DURATION_MINUTES)
                ?: 30
        updateState {
            it.copy(
                isBusyDialogOpen = true,
                editingBusyAppointment = busy,
                busyStartMinutes = busy?.startMinutes ?: 0,
                busyDurationMinutes = snappedDuration,
                busyValidationError = null,
                busyConflictLessons = emptyList()
            )
        }
    }

    fun dismissBusyDialog() {
        updateState {
            it.copy(
                isBusyDialogOpen = false,
                editingBusyAppointment = null,
                busyValidationError = null,
                busyConflictLessons = emptyList()
            )
        }
    }

    fun setBusyStart(minutes: Int) {
        updateState { it.copy(busyStartMinutes = minutes, busyConflictLessons = emptyList()) }
    }

    fun setBusyDuration(duration: Int) {
        updateState { it.copy(busyDurationMinutes = duration, busyConflictLessons = emptyList()) }
    }

    fun saveBusyAppointment(state: StudentDetailScreenState) {
        val startMinutes = state.busyStartMinutes
        val durationMinutes = state.busyDurationMinutes
        if (!ScheduleRules.isBusyDurationValid(startMinutes, durationMinutes)) {
            updateState {
                it.copy(busyValidationError = UiText.StringResource(R.string.error_busy_duration_invalid))
            }
            return
        }
        val conflicts = overlappingLessons(state, startMinutes, durationMinutes)
        if (conflicts.isNotEmpty()) {
            updateState { it.copy(busyConflictLessons = conflicts) }
            return
        }
        performBusySave(state)
    }

    fun confirmBusyConflict(state: StudentDetailScreenState) {
        updateState { it.copy(busyConflictLessons = emptyList()) }
        performBusySave(state)
    }

    fun deleteBusyAppointment(id: Int) {
        scope.launch {
            scheduleUseCases.deleteBusyAppointmentUseCase(id)
                .onFailure { e ->
                    updateState { it.copy(error = e.toUiText(), toastMessage = null) }
                }
        }
    }

    private fun performBusySave(state: StudentDetailScreenState) {
        val studentId = state.student?.id ?: return
        val startMinutes = state.busyStartMinutes
        val durationMinutes = state.busyDurationMinutes
        val editing = state.editingBusyAppointment
        scope.launch {
            if (editing == null) {
                scheduleUseCases.insertBusyAppointmentUseCase(
                    studentId,
                    state.selectedScheduleWeekday,
                    startMinutes,
                    durationMinutes
                )
                    .onSuccess { closeBusyDialog() }
                    .onFailure { e ->
                        updateState { it.copy(busyValidationError = e.toUiText(), error = null) }
                    }
            } else {
                scheduleUseCases.updateBusyAppointmentUseCase(
                    editing.id,
                    startMinutes,
                    durationMinutes
                )
                    .onSuccess { closeBusyDialog() }
                    .onFailure { e ->
                        updateState { it.copy(busyValidationError = e.toUiText(), error = null) }
                    }
            }
        }
    }

    private fun closeBusyDialog() {
        updateState {
            it.copy(
                isBusyDialogOpen = false,
                editingBusyAppointment = null,
                busyValidationError = null,
                busyConflictLessons = emptyList()
            )
        }
    }

    private fun overlappingLessons(
        state: StudentDetailScreenState,
        startMinutes: Int,
        durationMinutes: Int
    ): List<StudentLessonEntry> {
        val weekday = state.selectedScheduleWeekday
        val busyEnd = startMinutes + durationMinutes
        return state.studentSchedule?.lessons.orEmpty()
            .filter { it.weekday == weekday }
            .filter {
                ScheduleRules.overlaps(it.startMinutes, it.endMinutes, startMinutes, busyEnd)
            }
    }
}