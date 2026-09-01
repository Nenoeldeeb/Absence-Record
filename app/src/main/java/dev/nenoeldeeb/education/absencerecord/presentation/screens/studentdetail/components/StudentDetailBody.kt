package dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.presentation.screens.components.EmptyStateMessage
import dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.StudentDetailScreenEvent
import dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.StudentDetailScreenState

@Composable
internal fun StudentDetailBody(
    uiState: StudentDetailScreenState,
    onEvent: (StudentDetailScreenEvent) -> Unit,
    modifier: Modifier = Modifier,
    pagerState: PagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            uiState.student == null -> {
                EmptyStateMessage(
                    message = R.string.no_students_found,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                val student = uiState.student
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            student.name,
                            style = MaterialTheme.typography.headlineMedium
                        )
                        uiState.assignedClassName?.let { className ->
                            Text(
                                className,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    StudentDetailPageDots(
                        currentPage = pagerState.currentPage,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    ) { page ->
                        when (page) {
                            0 ->
                                AttendanceReportSection(
                                    attendanceDates = uiState.allAttendanceDates,
                                    selectedMonth = uiState.selectedMonth,
                                    onMonthSelected = { month ->
                                        onEvent(StudentDetailScreenEvent.SelectMonth(month))
                                    },
                                    onShare = {
                                        onEvent(StudentDetailScreenEvent.ShareAttendanceReport)
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )

                            else ->
                                StudentScheduleSection(
                                    uiState = uiState,
                                    onEvent = onEvent,
                                    modifier =
                                        Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 16.dp)
                                )
                        }
                    }
                }
            }
        }
    }
}