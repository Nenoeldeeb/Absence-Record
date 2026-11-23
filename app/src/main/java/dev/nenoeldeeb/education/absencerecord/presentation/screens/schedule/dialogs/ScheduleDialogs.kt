package dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule

import androidx.compose.runtime.Composable
import dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule.dialogs.HourDialog
import dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule.dialogs.ReplanConfirmDialog
import dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule.dialogs.ScheduleConfirmDialog
import dev.nenoeldeeb.education.absencerecord.presentation.utils.TimeFormatter.formatTime

/**
 * Aggregator for every dialog the Schedule screen can show: the add/edit
 * hour dialog (with its live assigned-count derivation), the hour deletion
 * confirmation (with the assigned names mapping) and the replan review.
 */
@Composable
internal fun ScheduleDialogs(
    uiState: ScheduleScreenState,
    onEvent: (ScheduleScreenEvent) -> Unit
) {
    if (uiState.isHourDialogOpen) {
        HourDialog(
            startMinutes = uiState.hourStartMinutes,
            durationMinutes = uiState.hourDurationMinutes,
            validationError = uiState.hourValidationError,
            isEdit = uiState.editingHour != null,
            onStartSelected = {
                onEvent(ScheduleScreenEvent.SetHourStart(it))
            },
            onDurationChange = {
                onEvent(ScheduleScreenEvent.SetHourDuration(it))
            },
            onSave = { onEvent(ScheduleScreenEvent.SaveHour) },
            onDismiss = {
                onEvent(ScheduleScreenEvent.DismissHourDialog)
            }
        )
    }

    uiState.hourToDelete?.let { hour ->
        val assignedNames =
            uiState.hoursForWeekday
                .firstOrNull { it.hour.id == hour.id }
                ?.assignedStudentIds
                .orEmpty()
                .mapNotNull { id -> uiState.allStudents.firstOrNull { it.id == id }?.name }
                .sorted()
        ScheduleConfirmDialog(
            hourLabel = formatTime(hour.startMinutes),
            assignedStudentNames = assignedNames,
            onConfirm = {
                onEvent(ScheduleScreenEvent.ConfirmDeleteHour(hour))
            },
            onDismiss = {
                onEvent(ScheduleScreenEvent.DismissDeleteHourDialog)
            }
        )
    }

    // Nothing has been written yet — this is the review step (FR-011, FR-012).
    uiState.pendingReplan?.let { pending ->
        ReplanConfirmDialog(
            pending = pending,
            onConfirm = { onEvent(ScheduleScreenEvent.ConfirmReplan) },
            onDismiss = { onEvent(ScheduleScreenEvent.DismissReplan) }
        )
    }
}