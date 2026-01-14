package dev.nenoeldeeb.education.absencerecord.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.nenoeldeeb.education.absencerecord.presentation.screens.calendar.CalendarScreen
import dev.nenoeldeeb.education.absencerecord.presentation.screens.calendar.CalendarViewModel
import dev.nenoeldeeb.education.absencerecord.presentation.screens.report.ReportScreen
import dev.nenoeldeeb.education.absencerecord.presentation.screens.report.ReportViewModel
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreen
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsViewModel
import dev.nenoeldeeb.education.absencerecord.presentation.theme.AbsenceRecordTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val app = (application as Application)
            val studentsViewModel: StudentsViewModel =
                viewModel(
                    factory =
                        viewModelFactory {
                            initializer {
                                StudentsViewModel(
                                    studentManagementUseCases = app.appContainer.studentManagementUseCases
                                )
                            }
                        }
                )
            val calendarViewModel: CalendarViewModel =
                viewModel(
                    factory =
                        viewModelFactory {
                            initializer {
                                CalendarViewModel(
                                    attendanceUseCases = app.appContainer.attendanceUseCases,
                                    studentManagementUseCases = app.appContainer.studentManagementUseCases
                                )
                            }
                        }
                )
            val reportViewModel: ReportViewModel =
                viewModel(
                    factory =
                        viewModelFactory {
                            initializer {
                                ReportViewModel(
                                    studentManagementUseCases = app.appContainer.studentManagementUseCases,
                                    attendanceUseCases = app.appContainer.attendanceUseCases,
                                    reportUseCases = app.appContainer.reportUseCases
                                )
                            }
                        }
                )
            AbsenceRecordTheme {
                Surface {
                    MainScreen(
                        studentsViewModel = studentsViewModel,
                        calendarViewModel = calendarViewModel,
                        reportViewModel = reportViewModel
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    studentsViewModel: StudentsViewModel,
    calendarViewModel: CalendarViewModel,
    reportViewModel: ReportViewModel
) {
    // 0: Students, 1: Calendar, 2: Report
    val pagerState =
        rememberPagerState(
            initialPage = 1,
            pageCount = { 3 }
        )

    HorizontalPager(
        state = pagerState,
        modifier = Modifier
    ) { page ->
        when (page) {
            0 -> StudentsScreen(viewModel = studentsViewModel)
            1 -> CalendarScreen(viewModel = calendarViewModel)
            2 -> ReportScreen(viewModel = reportViewModel)
        }
    }
}