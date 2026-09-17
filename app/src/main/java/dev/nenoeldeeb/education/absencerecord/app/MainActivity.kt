package dev.nenoeldeeb.education.absencerecord.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.nenoeldeeb.education.absencerecord.app.navigation.AppNavigation
import dev.nenoeldeeb.education.absencerecord.presentation.screens.calendar.CalendarScreen
import dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule.ScheduleScreen
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreen
import dev.nenoeldeeb.education.absencerecord.presentation.theme.AbsenceRecordTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            AbsenceRecordTheme {
                // Single inset owner: each screen's Scaffold consumes system bars.
                // No outer Scaffold here so insets are not double-applied.
                AppNavigation(contentPadding = PaddingValues(0.dp))
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
    // 0: Students, 1: Calendar, 2: Schedule
    // NOTE: swipe pager is the committed navigation metaphor (DESIGN.md bans bottom bar),
    // so no TabRow/indicator is added here. Each page fills the viewport; inner Scaffolds
    // own system-bar insets.
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize().padding(contentPadding)
    ) { page ->
        when (page) {
            0 ->
                StudentsScreen(
                    modifier = Modifier.fillMaxSize(),
                    listState = studentsListState,
                    onStudentClick = onStudentClick
                )
            1 -> CalendarScreen(modifier = Modifier.fillMaxSize())
            2 -> {
                ScheduleScreen(
                    modifier = Modifier.fillMaxSize(),
                    onStudentClick = onStudentClick
                )
            }
        }
    }
}