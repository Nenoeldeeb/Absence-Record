package dev.nenoeldeeb.education.absencerecord.presentation.screens.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.presentation.theme.AbsenceRecordTheme
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Clock

/**
 * Instrumentation tests for [ComposeCalendar] composable.
 *
 * These tests verify the UI behavior of calendar display, navigation, date selection, and visual
 * indicators.
 */
@RunWith(AndroidJUnit4::class)
class ComposeCalendarTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    // region Helper functions

    private fun getString(
        resId: Int, vararg formatArgs: Any
    ): String = context.getString(resId, *formatArgs)

    private fun getToday(): LocalDate {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        return LocalDate(now.year, now.month, now.day)
    }

    // endregion

    // region Display Tests

    @Test
    fun composeCalendar_displaysCurrentMonthHeader() {
        val today = getToday()

        composeTestRule.setContent {
            AbsenceRecordTheme { ComposeCalendar(onDateSelected = {}, initialMonth = today) }
        }

        // The header should contain the year
        composeTestRule.onNodeWithText(today.year.toString(), substring = true).assertIsDisplayed()
    }

    @Test
    fun composeCalendar_displaysDaysOfWeekHeader() {
        composeTestRule.setContent { AbsenceRecordTheme { ComposeCalendar(onDateSelected = {}) } }

        // Verify day abbreviations are displayed (calendar starts with Saturday)
        composeTestRule.onNodeWithText(getString(R.string.day_short_saturday)).assertIsDisplayed()
        composeTestRule.onNodeWithText(getString(R.string.day_short_sunday)).assertIsDisplayed()
        composeTestRule.onNodeWithText(getString(R.string.day_short_monday)).assertIsDisplayed()
        composeTestRule.onNodeWithText(getString(R.string.day_short_friday)).assertIsDisplayed()
    }

    @Test
    fun composeCalendar_displaysDaysInMonth() {
        val today = getToday()

        composeTestRule.setContent {
            AbsenceRecordTheme { ComposeCalendar(onDateSelected = {}, initialMonth = today) }
        }

        // Verify at least day 1 and day 15 are displayed (common to all months)
        composeTestRule.onNodeWithText("1").assertIsDisplayed()
        composeTestRule.onNodeWithText("15").assertIsDisplayed()
    }

    @Test
    fun composeCalendar_currentDayHasContentDescription() {
        val today = getToday()

        composeTestRule.setContent {
            AbsenceRecordTheme { ComposeCalendar(onDateSelected = {}, initialMonth = today) }
        }

        // Current day should have "Current day." content description
        composeTestRule.onNodeWithContentDescription(getString(R.string.content_description_today)).assertIsDisplayed()
    }

    @Test
    fun composeCalendar_markedDatesHavePresentContentDescription() {
        val today = getToday()
        val markedDate = LocalDate(today.year, today.month, 10)

        composeTestRule.setContent {
            AbsenceRecordTheme {
                ComposeCalendar(
                    onDateSelected = {}, initialMonth = today, markedDates = setOf(markedDate)
                )
            }
        }

        // Day 10 should have "Present" content description
        composeTestRule.onNodeWithContentDescription(getString(R.string.content_description_present)).assertIsDisplayed()
    }

    // endregion

    // region Navigation Tests

    @Test
    fun composeCalendar_previousMonthButtonNavigatesToPreviousMonth() {
        val today = getToday()
        val previousMonth = today.minus(1, DateTimeUnit.MONTH)

        composeTestRule.setContent {
            AbsenceRecordTheme { ComposeCalendar(onDateSelected = {}, initialMonth = today) }
        }

        // Click previous month button
        composeTestRule.onNodeWithContentDescription(getString(R.string.previous_month)).performClick()

        // Verify the previous month's year is displayed
        composeTestRule.onNodeWithText(previousMonth.year.toString(), substring = true).assertIsDisplayed()
    }

    @Test
    fun composeCalendar_nextMonthButtonNavigatesToNextMonth() {
        val today = getToday()
        val nextMonth = today.plus(1, DateTimeUnit.MONTH)

        composeTestRule.setContent {
            AbsenceRecordTheme { ComposeCalendar(onDateSelected = {}, initialMonth = today) }
        }

        // Click next month button
        composeTestRule.onNodeWithContentDescription(getString(R.string.next_month)).performClick()

        // Verify the next month's year is displayed
        composeTestRule.onNodeWithText(nextMonth.year.toString(), substring = true).assertIsDisplayed()
    }

    // endregion

    // region Interaction Tests

    @Test
    fun composeCalendar_clickingDateInvokesCallback() {
        var selectedDate: LocalDate? = null

        composeTestRule.setContent {
            AbsenceRecordTheme {
                ComposeCalendar(
                    onDateSelected = { date -> selectedDate = date }, initialMonth = getToday()
                )
            }
        }

        // Click on day 15
        composeTestRule.onNodeWithText("15").performClick()

        // Verify callback was invoked (date should not be null)
        assert(selectedDate != null) { "Expected date to be selected but was null" }
        assert(selectedDate?.day == 15) { "Expected day 15 but got ${selectedDate?.day}" }
    }

    // endregion
}