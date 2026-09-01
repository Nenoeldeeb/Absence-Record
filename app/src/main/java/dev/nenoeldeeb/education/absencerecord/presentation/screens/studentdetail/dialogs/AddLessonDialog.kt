package dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.dialogs

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.BusyAppointment
import dev.nenoeldeeb.education.absencerecord.domain.models.HourWithOccupancy
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
    modifier: Modifier = Modifier
) {
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
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.student_detail_select_hour),
                        style = MaterialTheme.typography.labelLarge
                    )
                    hours.sortedBy { it.hour.startMinutes }.forEach { hour ->
                        val eligible = hour.isEligible(busyAppointments)
                        val reasonResId =
                            when {
                                hour.remainingSlots <= 0 -> R.string.assign_reason_hour_full
                                !eligible -> R.string.assign_reason_busy_conflict
                                else -> null
                            }
                        HourOptionRow(
                            hour = hour,
                            selected = selectedHourId == hour.hour.id,
                            enabled = eligible,
                            reasonResId = reasonResId,
                            onClick = { onHourSelected(hour.hour.id) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            val selectedIsEligible =
                hours.any { it.hour.id == selectedHourId && it.isEligible(busyAppointments) }
            TextButton(onClick = onConfirm, enabled = selectedIsEligible) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        modifier = modifier
    )
}

@Composable
private fun HourWithOccupancy.isEligible(busyAppointments: List<BusyAppointment>): Boolean =
    remainingSlots > 0 &&
        busyAppointments.none { busy ->
            ScheduleRules.overlaps(
                hour.startMinutes,
                endMinutes,
                busy.startMinutes,
                busy.startMinutes + busy.durationMinutes
            )
        }

@Composable
private fun HourOptionRow(
    hour: HourWithOccupancy,
    selected: Boolean,
    enabled: Boolean,
    @StringRes reasonResId: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeRange =
        stringResource(R.string.time_range, formatTime(hour.hour.startMinutes), formatTime(hour.endMinutes))
    val occupancy =
        stringResource(R.string.schedule_occupancy, hour.assignedCount, hour.maxStudents)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .selectable(
                    selected = selected,
                    onClick = onClick,
                    enabled = enabled,
                    role = Role.RadioButton
                )
                .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            enabled = enabled
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(timeRange, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = occupancy,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        reasonResId?.let { resId ->
            Text(
                text = stringResource(resId),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}