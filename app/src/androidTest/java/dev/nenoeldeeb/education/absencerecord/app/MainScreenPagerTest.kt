package dev.nenoeldeeb.education.absencerecord.app

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.presentation.theme.AbsenceRecordTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation test for the 3-page [MainScreen] pager.
 *
 * Uses the real [ScheduleScreen] (and real ViewModels via
 * [AppViewModelProvider.factory]) so page 2 exercises the real composable.
 */
@RunWith(AndroidJUnit4::class)
class MainScreenPagerTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun getString(resId: Int): String = context.getString(resId)

    @Test
    fun pager_hasThreePages_startsWithCalendar() {
        lateinit var pagerState: PagerState

        composeTestRule.setContent {
            AbsenceRecordTheme {
                pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
                MainScreen(
                    contentPadding = PaddingValues(0.dp),
                    pagerState = pagerState,
                    studentsListState = rememberLazyListState(),
                    onStudentClick = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        assertEquals("Pager must have exactly 3 pages", 3, pagerState.pageCount)
        assertEquals("Initial page must be Calendar (1)", 1, pagerState.currentPage)
        composeTestRule
            .onNodeWithContentDescription(getString(R.string.previous_month))
            .assertIsDisplayed()
    }

    @Test
    fun pager_swipeLeft_reachesSchedulePage() {
        lateinit var pagerState: PagerState

        composeTestRule.setContent {
            AbsenceRecordTheme {
                pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
                MainScreen(
                    contentPadding = PaddingValues(0.dp),
                    pagerState = pagerState,
                    studentsListState = rememberLazyListState(),
                    onStudentClick = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onRoot().performTouchInput { swipeLeft() }
        composeTestRule.waitUntil(timeoutMillis = 5_000) { pagerState.currentPage == 2 }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(getString(R.string.day_short_saturday)).assertIsDisplayed()
    }

    @Test
    fun pager_swipeRight_returnsToCalendarThenStudents() {
        lateinit var pagerState: PagerState

        composeTestRule.setContent {
            AbsenceRecordTheme {
                pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
                MainScreen(
                    contentPadding = PaddingValues(0.dp),
                    pagerState = pagerState,
                    studentsListState = rememberLazyListState(),
                    onStudentClick = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onRoot().performTouchInput { swipeRight() }
        composeTestRule.waitUntil(timeoutMillis = 5_000) { pagerState.currentPage == 0 }
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText(context.getString(R.string.students_title, 0))
            .assertIsDisplayed()

        composeTestRule.onRoot().performTouchInput { swipeLeft() }
        composeTestRule.waitUntil(timeoutMillis = 5_000) { pagerState.currentPage == 1 }
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithContentDescription(getString(R.string.previous_month))
            .assertIsDisplayed()
    }
}