package dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import dev.nenoeldeeb.education.absencerecord.domain.models.Student

@Composable
fun HourAssignedStudents(
    students: List<Student>,
    onStudentClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val openProfileActionLabel = stringResource(R.string.schedule_open_profile_action)
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
                val openProfileDescription =
                    stringResource(
                        R.string.schedule_open_student_profile_description,
                        student.name
                    )
                Text(
                    text = student.name,
                    style =
                        MaterialTheme.typography.bodyLarge.copy(
                            textDirection = TextDirection.Content
                        ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag(
                                HOUR_ASSIGNED_STUDENT_TAG_PREFIX + student.id
                            )
                            .semantics {
                                contentDescription = openProfileDescription
                            }
                            .clickable(
                                role = Role.Button,
                                onClickLabel = openProfileActionLabel,
                                onClick = { onStudentClick(student.id) }
                            )
                            .padding(vertical = 8.dp)
                )
            }
        }
    }
}