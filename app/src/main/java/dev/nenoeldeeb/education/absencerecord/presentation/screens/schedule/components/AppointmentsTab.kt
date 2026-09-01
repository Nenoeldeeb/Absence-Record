package dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.HourWithOccupancy
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.presentation.screens.components.EmptyStateMessage

@Composable
fun AppointmentsTab(
    hours: List<HourWithOccupancy>,
    students: List<Student>,
    expandedHourIds: Set<Int>,
    onAddHour: () -> Unit,
    onToggleHourExpanded: (Int) -> Unit,
    onStudentClick: (Int) -> Unit,
    onEditClick: (HourWithOccupancy) -> Unit,
    onDeleteClick: (HourWithOccupancy) -> Unit,
    modifier: Modifier = Modifier
) {
    if (hours.isEmpty()) {
        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
        ) {
            EmptyStateMessage(R.string.schedule_no_hours_for_day)
            Button(
                onClick = onAddHour,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
            ) {
                Text(stringResource(R.string.schedule_add_hour))
            }
        }
    } else {
        LazyColumn(modifier = modifier.fillMaxSize()) {
            items(
                items = hours,
                key = { it.hour.id }
            ) { hour ->
                HourRow(
                    hour = hour,
                    students = students,
                    isExpanded = hour.hour.id in expandedHourIds,
                    onToggleExpanded = { onToggleHourExpanded(hour.hour.id) },
                    onStudentClick = onStudentClick,
                    onEditClick = { onEditClick(hour) },
                    onDeleteClick = { onDeleteClick(hour) }
                )
                HorizontalDivider()
            }
            item {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = onAddHour,
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    ) {
                        Text(stringResource(R.string.schedule_add_hour))
                    }
                }
            }
        }
    }
}