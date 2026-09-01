package dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.HourWithOccupancy
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.presentation.utils.TimeFormatter.formatTime

@Composable
fun HourRow(
    hour: HourWithOccupancy,
    students: List<Student>,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onStudentClick: (Int) -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val startLabel = formatTime(hour.hour.startMinutes)
    val endLabel = formatTime(hour.endMinutes)
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.time_range, startLabel, endLabel),
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text =
                            stringResource(
                                R.string.schedule_occupancy,
                                hour.assignedCount,
                                hour.maxStudents
                            ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (hour.isFull) {
                        Text(
                            text = stringResource(R.string.schedule_hour_full),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            IconButton(onClick = onToggleExpanded) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.outline_chevron_right_24),
                    contentDescription =
                        stringResource(
                            if (isExpanded) {
                                R.string.schedule_collapse_hour_description
                            } else {
                                R.string.schedule_expand_hour_description
                            },
                            startLabel
                        ),
                    modifier = Modifier.rotate(if (isExpanded) -90f else 90f)
                )
            }
            IconButton(onClick = onEditClick) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.outline_edit_24),
                    contentDescription =
                        stringResource(
                            R.string.schedule_edit_hour_description,
                            startLabel
                        )
                )
            }
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.outline_delete_24),
                    contentDescription =
                        stringResource(
                            R.string.schedule_delete_hour_description,
                            startLabel
                        )
                )
            }
        }
        if (isExpanded) {
            HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
            Column(modifier = Modifier.padding(top = 4.dp)) {
                val assignedStudents =
                    hour.assignedStudentIds
                        .mapNotNull { id -> students.firstOrNull { it.id == id } }
                        .sortedBy { it.name }
                if (assignedStudents.isEmpty()) {
                    Text(
                        text = stringResource(R.string.schedule_no_students_assigned),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                } else {
                    assignedStudents.forEach { student ->
                        Text(
                            text = student.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier =
                                Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .clickable { onStudentClick(student.id) }
                                    .padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}