package dev.nenoeldeeb.education.absencerecord.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.nenoeldeeb.education.absencerecord.app.navigation.AppNavigation
import dev.nenoeldeeb.education.absencerecord.presentation.screens.calendar.CalendarScreen
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreen
import dev.nenoeldeeb.education.absencerecord.presentation.theme.AbsenceRecordTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AbsenceRecordTheme {
                Scaffold { padding ->
                    AppNavigation(contentPadding = padding)
                }
            }
        }
    }
}

@Composable
fun MainScreen(
    contentPadding: PaddingValues,
    pagerState: PagerState,
    studentsListState: LazyListState,
    onStudentClick: (Int) -> Unit
) {
    // 0: Students, 1: Calendar
    HorizontalPager(
        state = pagerState,
        contentPadding = contentPadding
    ) { page ->
        when (page) {
            0 ->
                StudentsScreen(
                    modifier = Modifier.fillMaxSize(),
                    listState = studentsListState,
                    onStudentClick = onStudentClick
                )
            1 -> CalendarScreen(modifier = Modifier.fillMaxSize())
        }
    }
}