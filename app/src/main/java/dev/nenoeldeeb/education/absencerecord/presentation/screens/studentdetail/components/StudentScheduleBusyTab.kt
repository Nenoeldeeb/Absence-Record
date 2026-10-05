package dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.BusyAppointment
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentScheduleView
import dev.nenoeldeeb.education.absencerecord.presentation.utils.TimeFormatter.formatTime
import kotlinx.datetime.DayOfWeek

@Composable
internal fun StudentScheduleBusyTab(
    schedule: StudentScheduleView?,
    selectedWeekday: DayOfWeek,
    onEdit: (BusyAppointment) -> Unit,
    onDelete: (BusyAppointment) -> Unit,
    onAddBusy: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Filter+sort once per schedule/weekday change instead of on every recomposition.
    val busyAppointments =
        remember(schedule, selectedWeekday) {
            schedule?.busy.orEmpty()
                .filter { it.weekday == selectedWeekday }
                .sortedBy { it.startMinutes }
        }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (busyAppointments.isEmpty()) {
            Text(
                text = stringResource(R.string.student_detail_no_busy_for_day),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        } else {
            busyAppointments.forEach { appointment ->
                key(appointment.id) {
                    BusyRow(
                        appointment = appointment,
                        onEdit = { onEdit(appointment) },
                        onDelete = { onDelete(appointment) }
                    )
                }
            }
        }
        Button(onClick = onAddBusy) {
            Text(stringResource(R.string.schedule_add_busy_appointment))
        }
    }
}

@Composable
private fun BusyRow(
    appointment: BusyAppointment,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val endMinutes = appointment.startMinutes + appointment.durationMinutes
    val timeRange =
        stringResource(
            R.string.time_range,
            formatTime(appointment.startMinutes),
            formatTime(endMinutes)
        )
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = timeRange,
            style =
                MaterialTheme.typography.bodyMedium.copy(
                    textDirection = TextDirection.Ltr
                ),
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onEdit) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.outline_edit_24),
                contentDescription =
                    stringResource(R.string.schedule_edit_busy_description, timeRange)
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.outline_delete_24),
                contentDescription =
                    stringResource(R.string.schedule_delete_busy_description, timeRange)
            )
        }
    }
}