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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentLessonEntry
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentScheduleView
import dev.nenoeldeeb.education.absencerecord.presentation.utils.TimeFormatter.formatTime
import kotlinx.datetime.DayOfWeek

@Composable
internal fun StudentScheduleLessonsTab(
    schedule: StudentScheduleView?,
    selectedWeekday: DayOfWeek,
    onUnassign: (StudentLessonEntry) -> Unit,
    onAddLesson: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lessons = schedule?.lessons.orEmpty().filter { it.weekday == selectedWeekday }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (lessons.isEmpty()) {
            Text(
                text = stringResource(R.string.student_detail_lessons_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
            Button(onClick = onAddLesson) {
                Text(stringResource(R.string.student_detail_add_lesson))
            }
        } else {
            lessons.sortedBy { it.startMinutes }.forEach { lesson ->
                LessonRow(lesson = lesson, onUnassign = { onUnassign(lesson) })
            }
            Text(
                text = stringResource(R.string.student_detail_lesson_already_booked),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LessonRow(
    lesson: StudentLessonEntry,
    onUnassign: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeRange =
        stringResource(R.string.time_range, formatTime(lesson.startMinutes), formatTime(lesson.endMinutes))
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = timeRange,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onUnassign) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.outline_person_remove_24),
                contentDescription =
                    stringResource(R.string.student_detail_lesson_unassign_description, timeRange)
            )
        }
    }
}