package dev.nenoeldeeb.education.absencerecord.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import dev.nenoeldeeb.education.absencerecord.presentation.screens.calendar.CalendarScreen
import dev.nenoeldeeb.education.absencerecord.presentation.screens.report.ReportScreen
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreen
import dev.nenoeldeeb.education.absencerecord.presentation.theme.AbsenceRecordTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AbsenceRecordTheme {
                Scaffold { padding ->
                    MainScreen(contentPadding = padding)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(contentPadding: PaddingValues) {
    // 0: Students, 1: Calendar, 2: Report
    val pagerState = rememberPagerState(
        initialPage = 1, pageCount = { 3 })

    HorizontalPager(
        state = pagerState, contentPadding = contentPadding
    ) { page ->
        when (page) {
            0 -> StudentsScreen()
            1 -> CalendarScreen()
            2 -> ReportScreen()
        }
    }
}