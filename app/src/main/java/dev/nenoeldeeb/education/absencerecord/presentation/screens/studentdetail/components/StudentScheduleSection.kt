package dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
            SecondaryTabRow(
                selectedTabIndex = uiState.selectedScheduleTab.ordinal
            ) {
                Tab(
                    selected = uiState.selectedScheduleTab == StudentScheduleTab.Lessons,
                    onClick = {
                        onEvent(
                            StudentDetailScreenEvent.SelectScheduleTab(StudentScheduleTab.Lessons)
                        )
                    },
                    text = { Text(stringResource(R.string.student_detail_tab_lessons)) }
                )
                Tab(
                    selected = uiState.selectedScheduleTab == StudentScheduleTab.Busy,
                    onClick = {
                        onEvent(
                            StudentDetailScreenEvent.SelectScheduleTab(StudentScheduleTab.Busy)
                        )
                    },
                    text = { Text(stringResource(R.string.student_detail_tab_busy)) }
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