package dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.PlannedSlot
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.presentation.utils.TimeFormatter.formatTime

/**
 * The block's students, each with the ten minutes they actually occupy.
 *
 * [students] arrives in the derived running order and is not re-sorted here.
 * [slots] carries each student's own contiguous window (FR-016 … FR-018); a
 * student with no slot simply shows their name, which is the honest rendering
 * of an inconsistent block rather than an invented time.
 */
@Composable
fun HourAssignedStudents(
    students: List<Student>,
    onStudentClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    slots: List<PlannedSlot> = emptyList()
) {
    val openProfileActionLabel = stringResource(R.string.schedule_open_profile_action)
    val slotByStudentId = remember(slots) { slots.associateBy { it.studentId } }
    Column(
        modifier = modifier.padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (students.isEmpty()) {
            Text(
                text = stringResource(R.string.schedule_no_students_assigned),
                style =
                    MaterialTheme.typography.bodyMedium.copy(
                        textDirection = TextDirection.Content
                    ),
                color = MaterialTheme.colorScheme.outline,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        } else {
            students.forEach { student ->
                AssignedStudentRow(
                    student = student,
                    slot = slotByStudentId[student.id],
                    openProfileActionLabel = openProfileActionLabel,
                    onStudentClick = onStudentClick
                )
            }
        }
    }
}

@Composable
private fun AssignedStudentRow(
    student: Student,
    slot: PlannedSlot?,
    openProfileActionLabel: String,
    onStudentClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val slotLabel =
        slot?.let {
            stringResource(
                R.string.schedule_slot_range,
                formatTime(it.startMinutes),
                formatTime(it.endMinutes)
            )
        }
    val openProfileDescription =
        if (slotLabel != null) {
            stringResource(R.string.schedule_open_student_slot_description, student.name, slotLabel)
        } else {
            stringResource(R.string.schedule_open_student_profile_description, student.name)
        }
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .testTag(HOUR_ASSIGNED_STUDENT_TAG_PREFIX + student.id)
                .semantics { contentDescription = openProfileDescription }
                .clickable(
                    role = Role.Button,
                    onClickLabel = openProfileActionLabel,
                    onClick = { onStudentClick(student.id) }
                )
                .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = student.name,
            style =
                MaterialTheme.typography.bodyLarge.copy(
                    textDirection = TextDirection.Content
                ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (slotLabel != null) {
            // A clock time is not a phrase: it reads left-to-right in every
            // locale, so it takes LTR even in an RTL layout (T047).
            Text(
                text = slotLabel,
                style =
                    MaterialTheme.typography.bodyMedium.copy(
                        textDirection = TextDirection.Ltr
                    ),
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(HOUR_ASSIGNED_SLOT_TAG_PREFIX + student.id)
            )
        }
    }
}