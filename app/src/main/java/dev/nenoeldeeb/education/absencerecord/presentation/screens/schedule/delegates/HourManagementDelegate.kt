package dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule.delegates

import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.AssignmentRemovalReport
import dev.nenoeldeeb.education.absencerecord.domain.models.AvailableLessonHour
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
                hourStartMinutes = hour?.startMinutes ?: 0,
                hourMaxStudents = hour?.maxStudents?.toString() ?: "",
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
        val maxStudents = state.hourMaxStudents.toIntOrNull()
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
                                hourStartMinutes = 0,
                                hourMaxStudents = "",
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
        scope.launch {
            scheduleUseCases.deleteHourUseCase(hour.id)
                .onSuccess { }
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