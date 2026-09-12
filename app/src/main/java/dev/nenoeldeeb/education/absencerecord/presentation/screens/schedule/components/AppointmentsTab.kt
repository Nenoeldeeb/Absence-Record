package dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.HourWithOccupancy
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.presentation.screens.components.EmptyStateMessage
import dev.nenoeldeeb.education.absencerecord.presentation.utils.DateFormatter.toUiText
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText
import kotlinx.datetime.DayOfWeek

const val HOURS_ERROR_RETRY_TAG = "hours_error_retry"
const val HOURS_EMPTY_ADD_TAG = "hours_empty_add"
const val HOURS_ADD_TAG = "hours_add"

@Composable
fun AppointmentsTab(
    hours: List<HourWithOccupancy>,
    students: List<Student>,
    selectedWeekday: DayOfWeek,
    expandedHourIds: Set<Int>,
    isHoursLoading: Boolean,
    hoursError: UiText?,
    onAddHour: () -> Unit,
    onToggleHourExpanded: (Int) -> Unit,
    onStudentClick: (Int) -> Unit,
    onEditClick: (HourWithOccupancy) -> Unit,
    onDeleteClick: (HourWithOccupancy) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    when {
        isHoursLoading -> {
            val loadingDescription = stringResource(R.string.schedule_hours_loading)
            Box(
                modifier =
                    modifier
                        .fillMaxSize()
                        .semantics { contentDescription = loadingDescription },
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        hoursError != null -> {
            Column(
                modifier =
                    modifier
                        .fillMaxSize()
                        .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.outline_warning_24),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = hoursError.asString(context),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Button(
                    onClick = onRetry,
                    modifier =
                        Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag(HOURS_ERROR_RETRY_TAG)
                ) {
                    Text(stringResource(R.string.action_retry))
                }
            }
        }

        hours.isEmpty() -> {
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
                    modifier =
                        Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag(HOURS_EMPTY_ADD_TAG)
                ) {
                    Text(stringResource(R.string.schedule_add_hour))
                }
            }
        }

        else -> {
            val weekdayName = selectedWeekday.toUiText(fullName = true).asString()
            val hoursCountText =
                pluralStringResource(
                    R.plurals.schedule_day_summary_hours,
                    hours.size,
                    hours.size
                )
            val occupancyText =
                stringResource(
                    R.string.schedule_occupancy,
                    hours.sumOf { it.assignedCount },
                    hours.sumOf { it.maxStudents }
                )
            Column(modifier = modifier.fillMaxSize()) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .semantics { heading() }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = weekdayName,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text =
                            stringResource(
                                R.string.schedule_day_counts,
                                hoursCountText,
                                occupancyText
                            ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                HorizontalDivider()
                LazyColumn(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                ) {
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
                            onDeleteClick = { onDeleteClick(hour) },
                            modifier = Modifier.animateItem()
                        )
                        HorizontalDivider()
                    }
                }
                HorizontalDivider()
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = onAddHour,
                        modifier =
                            Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag(HOURS_ADD_TAG)
                    ) {
                        Text(stringResource(R.string.schedule_add_hour))
                    }
                }
            }
        }
    }
}