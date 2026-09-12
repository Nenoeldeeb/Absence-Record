package dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.HourWithOccupancy
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.presentation.utils.TimeFormatter.formatTime

const val HOUR_ROW_TAG_PREFIX = "hour_row_"
const val HOUR_EXPAND_TAG_PREFIX = "hour_expand_"
const val HOUR_OPTIONS_TAG_PREFIX = "hour_options_"
const val HOUR_EDIT_TAG_PREFIX = "hour_edit_"
const val HOUR_DELETE_TAG_PREFIX = "hour_delete_"
const val HOUR_ASSIGNED_STUDENT_TAG_PREFIX = "assigned_student_"

@OptIn(ExperimentalLayoutApi::class)
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
    val expandLabel =
        stringResource(
            if (isExpanded) {
                R.string.schedule_collapse_hour_description
            } else {
                R.string.schedule_expand_hour_description
            },
            startLabel
        )
    val expandActionLabel =
        stringResource(
            if (isExpanded) {
                R.string.schedule_collapse_action
            } else {
                R.string.schedule_expand_action
            }
        )
    val occupancyDescription =
        stringResource(
            R.string.schedule_occupancy,
            hour.assignedCount,
            hour.maxStudents
        )
    val occupancyState =
        if (hour.isFull) {
            "$occupancyDescription, ${stringResource(R.string.schedule_hour_full)}"
        } else {
            occupancyDescription
        }
    val editDescription = stringResource(R.string.schedule_edit_hour_description, startLabel)
    val deleteDescription = stringResource(R.string.schedule_delete_hour_description, startLabel)
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "hour_expand_rotation"
    )
    val assignedStudents =
        remember(hour.assignedStudentIds, students) {
            hour.assignedStudentIds
                .mapNotNull { id -> students.firstOrNull { it.id == id } }
                .sortedBy { it.name }
        }
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .testTag(HOUR_ROW_TAG_PREFIX + hour.hour.id)
                .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier =
                    Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag(HOUR_EXPAND_TAG_PREFIX + hour.hour.id)
                        .semantics {
                            contentDescription = expandLabel
                            stateDescription = occupancyState
                        }
                        .clickable(
                            role = Role.Button,
                            onClickLabel = expandActionLabel,
                            onClick = onToggleExpanded
                        )
                        .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.time_range, startLabel, endLabel),
                        style =
                            MaterialTheme.typography.titleMedium.copy(
                                textDirection = TextDirection.Ltr
                            ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector =
                                    ImageVector.vectorResource(R.drawable.outline_person_24),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = occupancyDescription,
                                style =
                                    MaterialTheme.typography.bodyMedium.copy(
                                        textDirection = TextDirection.Ltr
                                    ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (hour.isFull) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector =
                                            ImageVector.vectorResource(R.drawable.outline_check_24),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.schedule_hour_full),
                                        style =
                                            MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Medium
                                            ),
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
                Icon(
                    imageVector =
                        ImageVector.vectorResource(R.drawable.outline_expand_more_24),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier =
                        Modifier
                            .size(24.dp)
                            .rotate(chevronRotation)
                )
            }
            Crossfade(
                targetState = isExpanded,
                animationSpec = tween(durationMillis = 150),
                label = "hour_actions_crossfade"
            ) { expanded ->
                if (expanded) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        IconButton(
                            onClick = onEditClick,
                            modifier =
                                Modifier
                                    .defaultMinSize(48.dp, 48.dp)
                                    .testTag(HOUR_EDIT_TAG_PREFIX + hour.hour.id)
                        ) {
                            Icon(
                                imageVector =
                                    ImageVector.vectorResource(R.drawable.outline_edit_24),
                                contentDescription = editDescription
                            )
                        }
                        IconButton(
                            onClick = onDeleteClick,
                            modifier =
                                Modifier
                                    .defaultMinSize(48.dp, 48.dp)
                                    .testTag(HOUR_DELETE_TAG_PREFIX + hour.hour.id)
                        ) {
                            Icon(
                                imageVector =
                                    ImageVector.vectorResource(R.drawable.outline_delete_24),
                                contentDescription = deleteDescription,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                } else {
                    HourOverflowMenu(
                        hourId = hour.hour.id,
                        startLabel = startLabel,
                        onEditClick = onEditClick,
                        onDeleteClick = onDeleteClick
                    )
                }
            }
        }
        AnimatedVisibility(
            visible = isExpanded,
            enter =
                expandVertically(animationSpec = tween(durationMillis = 250)) +
                    fadeIn(animationSpec = tween(durationMillis = 200)),
            exit =
                shrinkVertically(animationSpec = tween(durationMillis = 200)) +
                    fadeOut(animationSpec = tween(durationMillis = 150)),
            label = "hour_expansion"
        ) {
            Column {
                HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
                HourAssignedStudents(
                    students = assignedStudents,
                    onStudentClick = onStudentClick
                )
            }
        }
    }
}