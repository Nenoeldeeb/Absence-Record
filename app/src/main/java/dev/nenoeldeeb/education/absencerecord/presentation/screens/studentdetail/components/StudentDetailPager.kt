package dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.StudentDetailScreenEvent
import dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.StudentDetailScreenState
import kotlinx.datetime.LocalDate

@Composable
internal fun DetailTabRow(
    pagerState: PagerState,
    onTabSelected: (Int) -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    PrimaryTabRow(
        selectedTabIndex = pagerState.currentPage,
        modifier =
            modifier.fillMaxWidth().padding(
                top = if (compact) 4.dp else 12.dp,
                bottom = if (compact) 4.dp else 12.dp
            )
    ) {
        Tab(
            selected = pagerState.currentPage == 0,
            onClick = { onTabSelected(0) },
            text = {
                Text(stringResource(R.string.student_detail_tab_attendance))
            }
        )
        Tab(
            selected = pagerState.currentPage == 1,
            onClick = { onTabSelected(1) },
            text = {
                Text(stringResource(R.string.student_detail_tab_schedule))
            }
        )
    }
}

@Composable
internal fun DetailPager(
    uiState: StudentDetailScreenState,
    onEvent: (StudentDetailScreenEvent) -> Unit,
    pagerState: PagerState,
    landscape: Boolean,
    modifier: Modifier = Modifier
) {
    // Remembered so month/share taps don't allocate new lambdas (and recompose
    // the calendar) on every unrelated state change.
    val onMonthSelected =
        remember(onEvent) {
            { month: LocalDate ->
                onEvent(StudentDetailScreenEvent.SelectMonth(month))
            }
        }
    val onShareReport =
        remember(onEvent) {
            {
                onEvent(StudentDetailScreenEvent.ShareAttendanceReport)
            }
        }
    HorizontalPager(
        state = pagerState,
        modifier = modifier
    ) { page ->
        when (page) {
            0 ->
                AttendanceReportSection(
                    attendanceDates = uiState.allAttendanceDates,
                    selectedMonth = uiState.selectedMonth,
                    onMonthSelected = onMonthSelected,
                    onShare = onShareReport,
                    modifier = Modifier.fillMaxSize(),
                    landscape = landscape
                )

            else -> {
                val scheduleModifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                if (landscape) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                    ) {
                        StudentScheduleSection(
                            uiState = uiState,
                            onEvent = onEvent,
                            modifier = scheduleModifier
                        )
                    }
                } else {
                    StudentScheduleSection(
                        uiState = uiState,
                        onEvent = onEvent,
                        modifier = scheduleModifier
                    )
                }
            }
        }
    }
}