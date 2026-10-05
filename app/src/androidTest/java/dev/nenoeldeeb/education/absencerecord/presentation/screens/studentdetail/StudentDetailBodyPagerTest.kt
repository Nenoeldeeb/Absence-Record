package dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail

import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.components.StudentDetailBody
import dev.nenoeldeeb.education.absencerecord.presentation.theme.AbsenceRecordTheme
import kotlinx.datetime.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StudentDetailBodyPagerTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val studentName = "Pager Test Student"
    private val className = "Grade 1"

    private fun getString(resId: Int): String = context.getString(resId)

    private fun aState(): StudentDetailScreenState =
        StudentDetailScreenState().copy(
            student = Student(id = 1, name = studentName, classId = null),
            assignedClassName = className,
            selectedScheduleWeekday = DayOfWeek.SATURDAY,
            selectedScheduleTab = StudentScheduleTab.Lessons,
            isLoading = false
        )

    private fun setBody(onPagerState: (PagerState) -> Unit) {
        composeTestRule.setContent {
            AbsenceRecordTheme {
                val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
                onPagerState(pagerState)
                StudentDetailBody(
                    uiState = aState(),
                    onEvent = {},
                    pagerState = pagerState
                )
            }
        }
    }

    private fun swipeToSchedule(pagerState: PagerState) {
        composeTestRule.onRoot().performTouchInput { swipeLeft() }
        composeTestRule.waitUntil(timeoutMillis = 5_000) { pagerState.currentPage == 1 }
        composeTestRule.waitForIdle()
    }

    @Test
    fun pager_startsOnAttendance_swipesBetweenPages_headerPinned_tabsReflectPage() {
        lateinit var pagerState: PagerState
        setBody { pagerState = it }
        composeTestRule.waitForIdle()

        assertEquals(0, pagerState.currentPage)
        composeTestRule.onNodeWithText(className).assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription(getString(R.string.student_detail_share))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(getString(R.string.student_detail_tab_attendance))
            .assertIsDisplayed()
            .assertIsSelected()
        composeTestRule
            .onNodeWithText(getString(R.string.student_detail_tab_schedule))
            .assertIsDisplayed()
            .assertIsNotSelected()

        swipeToSchedule(pagerState)

        assertEquals(1, pagerState.currentPage)
        composeTestRule.onNodeWithText(className).assertIsDisplayed()
        composeTestRule
            .onNodeWithText(getString(R.string.student_schedule_title))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(getString(R.string.student_detail_tab_schedule))
            .assertIsSelected()
        composeTestRule
            .onNodeWithText(getString(R.string.student_detail_tab_attendance))
            .assertIsNotSelected()

        composeTestRule.onRoot().performTouchInput { swipeRight() }
        composeTestRule.waitUntil(timeoutMillis = 5_000) { pagerState.currentPage == 0 }
        composeTestRule.waitForIdle()

        assertEquals(0, pagerState.currentPage)
        composeTestRule.onNodeWithText(className).assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription(getString(R.string.student_detail_share))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(getString(R.string.student_detail_tab_attendance))
            .assertIsSelected()
    }

    @Test
    fun pager_tabsClick_navigatesBetweenPages() {
        lateinit var pagerState: PagerState
        setBody { pagerState = it }
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText(getString(R.string.student_detail_tab_schedule))
            .performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) { pagerState.currentPage == 1 }
        composeTestRule.waitForIdle()

        assertEquals(1, pagerState.currentPage)
        composeTestRule
            .onNodeWithText(getString(R.string.student_schedule_title))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(getString(R.string.student_detail_tab_attendance))
            .performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) { pagerState.currentPage == 0 }
        composeTestRule.waitForIdle()

        assertEquals(0, pagerState.currentPage)
        composeTestRule
            .onNodeWithContentDescription(getString(R.string.student_detail_share))
            .assertIsDisplayed()
    }

    @Test
    fun pager_reOpeningProfile_resetsToAttendancePage() {
        lateinit var pagerState: PagerState
        var showBody by mutableStateOf(true)
        composeTestRule.setContent {
            AbsenceRecordTheme {
                if (showBody) {
                    val state = rememberPagerState(initialPage = 0, pageCount = { 2 })
                    pagerState = state
                    StudentDetailBody(
                        uiState = aState(),
                        onEvent = {},
                        pagerState = state
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        swipeToSchedule(pagerState)
        assertEquals(1, pagerState.currentPage)

        showBody = false
        composeTestRule.waitForIdle()

        showBody = true
        composeTestRule.waitForIdle()

        assertEquals("Re-opening a profile must reset to the Attendance page", 0, pagerState.currentPage)
        composeTestRule
            .onNodeWithContentDescription(getString(R.string.student_detail_share))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(getString(R.string.student_detail_tab_attendance))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(getString(R.string.student_detail_tab_schedule))
            .assertIsDisplayed()
    }

    @Test
    fun body_showsClassLabel_whenAssigned_andUnassignedLabelOtherwise() {
        composeTestRule.setContent {
            AbsenceRecordTheme {
                StudentDetailBody(
                    uiState = aState().copy(assignedClassName = null),
                    onEvent = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText(getString(R.string.class_unassigned_label))
            .assertIsDisplayed()
    }

    @Test
    fun pager_dragOverWeekdayChips_scrollsChips_doesNotFlipPage() {
        lateinit var pagerState: PagerState
        setBody { pagerState = it }
        composeTestRule.waitForIdle()

        swipeToSchedule(pagerState)

        val chips = composeTestRule.onNodeWithTag("weekday_selector_chips")
        chips.performTouchInput { swipeLeft() }
        chips.performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()

        assertEquals(1, pagerState.currentPage)
        composeTestRule.onNodeWithText(getString(R.string.day_short_friday)).assertIsDisplayed()

        chips.performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()

        assertEquals(
            "A rightward drag over the weekday chips must scroll the chips, not flip to Attendance",
            1,
            pagerState.currentPage
        )
    }
}