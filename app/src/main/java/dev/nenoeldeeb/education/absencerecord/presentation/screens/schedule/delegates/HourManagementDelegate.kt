package dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule.delegates

import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.AssignmentRemovalReport
import dev.nenoeldeeb.education.absencerecord.domain.models.AvailableLessonHour
import dev.nenoeldeeb.education.absencerecord.domain.services.ScheduleRules
import dev.nenoeldeeb.education.absencerecord.domain.usecases.schedule.ScheduleUseCases
import dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule.ScheduleScreenState
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText
import dev.nenoeldeeb.education.absencerecord.presentation.utils.toUiText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class HourManagementDelegate(
    private val scheduleUseCases: ScheduleUseCases,
    private val scope: CoroutineScope,
    private val updateState: ((ScheduleScreenState) -> ScheduleScreenState) -> Unit
) {
    fun openHourDialog(hour: AvailableLessonHour?) {
        updateState {
            it.copy(
                isHourDialogOpen = true,
                editingHour = hour,
                hourStartMinutes = hour?.startMinutes ?: ScheduleRules.DEFAULT_NEW_HOUR_START_MINUTES,
                hourMaxStudents =
                    hour?.maxStudents?.toString()
                        ?: ScheduleRules.DEFAULT_NEW_HOUR_MAX_STUDENTS.toString(),
                hourValidationError = null
            )
        }
    }

    fun dismissHourDialog() {
        updateState {
            it.copy(isHourDialogOpen = false, editingHour = null, hourValidationError = null)
        }
    }

    fun setHourStart(minutes: Int) {
        updateState { it.copy(hourStartMinutes = minutes) }
    }

    fun setHourMaxStudents(value: String) {
        updateState { it.copy(hourMaxStudents = value) }
    }

    fun saveHour(state: ScheduleScreenState) {
        val maxStudents = ScheduleRules.parseMaxStudents(state.hourMaxStudents)
        if (maxStudents == null || maxStudents < 1) {
            updateState {
                it.copy(hourValidationError = UiText.StringResource(R.string.error_capacity_invalid))
            }
            return
        }
        val weekday = state.selectedWeekday
        val startMinutes = state.hourStartMinutes
        val editingHour = state.editingHour
        scope.launch {
            if (editingHour == null) {
                scheduleUseCases.insertHourUseCase(weekday, startMinutes, maxStudents)
                    .onSuccess {
                        updateState {
                            it.copy(
                                isHourDialogOpen = false,
                                editingHour = null,
                                hourStartMinutes = ScheduleRules.DEFAULT_NEW_HOUR_START_MINUTES,
                                hourMaxStudents =
                                    ScheduleRules.DEFAULT_NEW_HOUR_MAX_STUDENTS.toString(),
                                hourValidationError = null
                            )
                        }
                    }
                    .onFailure { e ->
                        updateState {
                            it.copy(hourValidationError = e.toUiText(), error = null)
                        }
                    }
            } else {
                scheduleUseCases.updateHourUseCase(editingHour.id, weekday, startMinutes, maxStudents)
                    .onSuccess { report ->
                        updateState {
                            it.copy(
                                isHourDialogOpen = false,
                                editingHour = null,
                                hourValidationError = null
                            )
                        }
                        if (report.removedStudentIds.isNotEmpty()) {
                            updateState {
                                it.copy(toastMessage = removalToast(state, report))
                            }
                        }
                    }
                    .onFailure { e ->
                        updateState {
                            it.copy(hourValidationError = e.toUiText(), error = null)
                        }
                    }
            }
        }
    }

    fun confirmDeleteHour(
        hour: AvailableLessonHour,
        state: ScheduleScreenState
    ) {
        if (state.hourToDelete == null) {
            updateState { it.copy(hourToDelete = hour) }
            return
        }
        updateState { it.copy(hourToDelete = null) }
        val assignedIds =
            state.hoursForWeekday
                .firstOrNull { it.hour.id == hour.id }
                ?.assignedStudentIds
                .orEmpty()
        scope.launch {
            scheduleUseCases.deleteHourUseCase(hour.id)
                .onSuccess {
                    updateState {
                        it.copy(deletedHour = hour, deletedHourAssignedIds = assignedIds)
                    }
                }
                .onFailure { e ->
                    updateState { it.copy(error = e.toUiText()) }
                }
        }
    }

    fun undoDeleteHour(state: ScheduleScreenState) {
        val hour = state.deletedHour ?: return
        val assignedIds = state.deletedHourAssignedIds
        updateState { it.copy(deletedHour = null, deletedHourAssignedIds = emptyList()) }
        scope.launch {
            scheduleUseCases.insertHourUseCase(hour.weekday, hour.startMinutes, hour.maxStudents)
                .onSuccess { newHourId ->
                    var firstError: UiText? = null
                    assignedIds.forEach { studentId ->
                        scheduleUseCases.assignStudentUseCase(newHourId, studentId, hour.weekday)
                            .onFailure { e ->
                                if (firstError == null) firstError = e.toUiText()
                            }
                    }
                    firstError?.let { error ->
                        updateState { it.copy(error = error) }
                    }
                }
                .onFailure { e ->
                    updateState { it.copy(error = e.toUiText()) }
                }
        }
    }

    fun dismissDeleteHourDialog() {
        updateState { it.copy(hourToDelete = null) }
    }

    private fun removalToast(
        state: ScheduleScreenState,
        report: AssignmentRemovalReport
    ): UiText {
        val names =
            state.allStudents
                .filter { it.id in report.removedStudentIds }
                .map { it.name }
                .joinToString(", ")
        return UiText.PluralResource(
            R.plurals.removed_from_hour,
            report.removedStudentIds.size,
            UiText.FormattedTimeText(state.hourStartMinutes),
            names
        )
    }
}