package dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule.components.WeekdaySelector
import dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.StudentDetailScreenEvent
import dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.StudentDetailScreenState
import dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.StudentScheduleTab

@Composable
internal fun StudentScheduleSection(
    uiState: StudentDetailScreenState,
    onEvent: (StudentDetailScreenEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    // Single pass over both lists; previously two full scans ran on every recomposition.
    val weekdayCounts =
        remember(uiState.studentSchedule, uiState.selectedScheduleWeekday) {
            var lessons = 0
            var busy = 0
            uiState.studentSchedule?.lessons?.forEach {
                if (it.weekday == uiState.selectedScheduleWeekday) lessons++
            }
            uiState.studentSchedule?.busy?.forEach {
                if (it.weekday == uiState.selectedScheduleWeekday) busy++
            }
            lessons to busy
        }
    val (lessonsForWeekday, busyForWeekday) = weekdayCounts
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.student_schedule_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() }
            )
            WeekdaySelector(
                selectedWeekday = uiState.selectedScheduleWeekday,
                onWeekdaySelected = { weekday ->
                    onEvent(StudentDetailScreenEvent.SelectScheduleWeekday(weekday))
                },
                modifier = Modifier.fillMaxWidth()
            )
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                SegmentedButton(
                    selected = uiState.selectedScheduleTab == StudentScheduleTab.Lessons,
                    onClick = {
                        onEvent(
                            StudentDetailScreenEvent.SelectScheduleTab(StudentScheduleTab.Lessons)
                        )
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    label = {
                        CountedTabLabel(
                            label = stringResource(R.string.student_detail_tab_lessons),
                            count = lessonsForWeekday
                        )
                    }
                )
                SegmentedButton(
                    selected = uiState.selectedScheduleTab == StudentScheduleTab.Busy,
                    onClick = {
                        onEvent(
                            StudentDetailScreenEvent.SelectScheduleTab(StudentScheduleTab.Busy)
                        )
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    label = {
                        CountedTabLabel(
                            label = stringResource(R.string.student_detail_tab_busy),
                            count = busyForWeekday
                        )
                    }
                )
            }
            when (uiState.selectedScheduleTab) {
                StudentScheduleTab.Lessons ->
                    StudentScheduleLessonsTab(
                        schedule = uiState.studentSchedule,
                        selectedWeekday = uiState.selectedScheduleWeekday,
                        onUnassign = { lesson ->
                            onEvent(StudentDetailScreenEvent.UnassignLesson(lesson))
                        },
                        onAddLesson = {
                            onEvent(StudentDetailScreenEvent.OpenAddLessonDialog)
                        }
                    )

                StudentScheduleTab.Busy ->
                    StudentScheduleBusyTab(
                        schedule = uiState.studentSchedule,
                        selectedWeekday = uiState.selectedScheduleWeekday,
                        onEdit = { busy ->
                            onEvent(StudentDetailScreenEvent.OpenBusyDialog(busy))
                        },
                        onDelete = { busy ->
                            onEvent(StudentDetailScreenEvent.DeleteBusyAppointment(busy.id))
                        },
                        onAddBusy = {
                            onEvent(StudentDetailScreenEvent.OpenBusyDialog(null))
                        }
                    )
            }
        }
    }
}

@Composable
private fun CountedTabLabel(
    label: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = label)
        Text(
            text = stringResource(R.string.student_detail_tab_count_format, count),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}