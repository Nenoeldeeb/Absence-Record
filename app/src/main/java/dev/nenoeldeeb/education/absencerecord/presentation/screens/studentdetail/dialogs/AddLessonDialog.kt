package dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.dialogs

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.BusyAppointment
import dev.nenoeldeeb.education.absencerecord.domain.models.HourWithOccupancy
import dev.nenoeldeeb.education.absencerecord.domain.services.LessonSlotPlanner.BusyWindow
import dev.nenoeldeeb.education.absencerecord.domain.services.ScheduleRules
import dev.nenoeldeeb.education.absencerecord.presentation.utils.TimeFormatter.formatTime

@Composable
internal fun AddLessonDialog(
    hours: List<HourWithOccupancy>,
    busyAppointments: List<BusyAppointment>,
    selectedHourId: Int?,
    onHourSelected: (Int) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    studentId: Int = 0,
    studentName: String = ""
) {
    // Sort once per hours change; the dialog recomposes on every radio selection.
    val sortedHours = remember(hours) { hours.sortedBy { it.hour.startMinutes } }
    // Eligibility and conflict are both answered by the newcomer's OWN
    // prospective ten minutes, not by the block's whole extent: a commitment
    // later inside the block must not hide a block they could take (FR-027).
    // Derived once and memoised, so each radio tap is an O(1) lookup.
    val verdicts =
        remember(hours, busyAppointments, studentId, studentName) {
            val windows = busyAppointments.map { BusyWindow(it.startMinutes, it.durationMinutes) }
            hours.associate { hour ->
                val slot = hour.prospectiveSlotFor(studentId, studentName, windows)
                val collides =
                    slot != null &&
                        windows.any { busy ->
                            ScheduleRules.overlaps(
                                slot.startMinutes,
                                slot.endMinutes,
                                busy.startMinutes,
                                busy.endMinutes
                            )
                        }
                // A null slot means the block is full, or the roster's own
                // commitments are unknown. Neither can be called eligible.
                hour.hour.id to Verdict(eligible = slot != null && !collides, conflict = collides)
            }
        }
    val eligibility = remember(verdicts) { verdicts.mapValues { it.value.eligible } }
    // Busy overlap is tracked separately so conflict styling takes priority
    // over full styling when both apply.
    val busyOverlap = remember(verdicts) { verdicts.mapValues { it.value.conflict } }
    // Eligibility check is O(1) lookup; recompute only when its inputs change.
    val selectedIsEligible =
        remember(eligibility, selectedHourId) {
            eligibility[selectedHourId] == true
        }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.student_detail_add_lesson_title)) },
        text = {
            if (hours.isEmpty()) {
                Text(stringResource(R.string.student_detail_no_hours_for_day))
            } else {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.student_detail_select_hour),
                        style = MaterialTheme.typography.labelLarge
                    )
                    sortedHours.forEach { hour ->
                        val eligible = eligibility[hour.hour.id] == true
                        val hasOverlap = busyOverlap[hour.hour.id] == true
                        val statusDescriptionResId =
                            when {
                                hasOverlap -> R.string.assign_reason_busy_conflict
                                hour.remainingSlots <= 0 -> R.string.assign_reason_hour_full
                                else -> null
                            }
                        HourOptionRow(
                            hour = hour,
                            selected = selectedHourId == hour.hour.id,
                            enabled = eligible,
                            hasBusyOverlap = hasOverlap,
                            statusDescriptionResId = statusDescriptionResId,
                            onClick = { onHourSelected(hour.hour.id) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = selectedIsEligible,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        modifier = modifier
    )
}

@Composable
private fun HourOptionRow(
    hour: HourWithOccupancy,
    selected: Boolean,
    enabled: Boolean,
    hasBusyOverlap: Boolean,
    @StringRes statusDescriptionResId: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isConflict = hasBusyOverlap
    val isFull = hour.remainingSlots <= 0
    val colorScheme = MaterialTheme.colorScheme
    val titleColor =
        when {
            isConflict -> colorScheme.outline
            isFull -> colorScheme.error
            else -> colorScheme.onTertiaryContainer
        }
    val secondaryColor =
        when {
            isConflict -> colorScheme.outline
            isFull -> colorScheme.error
            else -> colorScheme.onTertiaryContainer
        }
    val iconTint =
        when {
            isConflict -> colorScheme.outline
            isFull -> colorScheme.error
            else -> colorScheme.onTertiaryContainer
        }
    val timeRange =
        stringResource(R.string.time_range, formatTime(hour.hour.startMinutes), formatTime(hour.endMinutes))
    val occupancy =
        stringResource(R.string.schedule_occupancy, hour.assignedCount, hour.maxStudents)
    val statusDescription = statusDescriptionResId?.let { stringResource(it) }
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .selectable(
                    selected = selected,
                    onClick = onClick,
                    enabled = enabled,
                    role = Role.RadioButton
                )
                .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            enabled = enabled
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = timeRange,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                style =
                    MaterialTheme.typography.titleMedium.copy(
                        textDirection = TextDirection.Ltr
                    ),
                color = titleColor
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.outline_person_24),
                    contentDescription = null,
                    tint = secondaryColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = occupancy,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    style =
                        MaterialTheme.typography.bodyMedium.copy(
                            textDirection = TextDirection.Ltr
                        ),
                    color = secondaryColor,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        }
        when {
            isConflict -> {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.outline_schedule_24),
                    contentDescription = statusDescription,
                    tint = iconTint
                )
            }
            isFull -> {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.outline_close_24),
                    contentDescription = statusDescription,
                    tint = iconTint
                )
            }
            else -> {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.outline_check_24),
                    contentDescription = null,
                    tint = iconTint
                )
            }
        }
    }
}

/** Whether a newcomer can take a block, and whether a slot collision blocks it. */
private data class Verdict(
    val eligible: Boolean,
    val conflict: Boolean
)