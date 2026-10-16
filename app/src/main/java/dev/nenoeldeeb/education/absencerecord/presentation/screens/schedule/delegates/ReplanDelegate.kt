package dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule.delegates

import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.HourReplanResult
import dev.nenoeldeeb.education.absencerecord.domain.usecases.schedule.ScheduleUseCases
import dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule.ScheduleScreenState
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Owns the confirm-first re-plan flow (FR-009…FR-015).
 *
 * The first save returns without writing and parks a proposal in the state; the
 * teacher either confirms — which commits — or declines, which leaves the block
 * exactly as it was. Kept out of the ViewModel so the ViewModel stays cohesive
 * (constitution: ViewModel delegate decomposition).
 */
class ReplanDelegate(
    private val scheduleUseCases: ScheduleUseCases,
    private val scope: CoroutineScope,
    private val updateState: ((ScheduleScreenState) -> ScheduleScreenState) -> Unit
) {
    /** Re-runs the last edit with `confirmed = true` to commit it. */
    fun confirm(state: ScheduleScreenState) {
        val hour = state.editingHour ?: return
        val pending = state.pendingReplan ?: return
        updateState { it.copy(pendingReplan = null, isHourDialogOpen = false, editingHour = null) }
        scope.launch {
            scheduleUseCases
                .updateHourUseCase(
                    id = hour.id,
                    weekday = hour.weekday,
                    startMinutes = state.hourStartMinutes,
                    durationMinutes = state.hourDurationMinutes,
                    confirmed = true
                )
                .onSuccess { outcome ->
                    if (outcome is HourReplanResult.Applied && outcome.removedStudentIds.isNotEmpty()) {
                        updateState { it.copy(toastMessage = removedToast(outcome.removedStudentIds.size)) }
                    }
                }
                .onFailure { error ->
                    updateState { it.copy(error = UiText.DynamicString(error.message ?: "")) }
                }
        }
    }

    /** Declines: nothing was written, so simply drop the proposal (FR-012). */
    fun dismiss(state: ScheduleScreenState) {
        updateState { it.copy(pendingReplan = null) }
    }

    private fun removedToast(count: Int) = UiText.PluralResource(R.plurals.removed_from_hour, count, "")
}