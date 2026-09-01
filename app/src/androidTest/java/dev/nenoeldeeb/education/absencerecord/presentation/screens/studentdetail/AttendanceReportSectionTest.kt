package dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.components.AttendanceReportSection
import dev.nenoeldeeb.education.absencerecord.presentation.theme.AbsenceRecordTheme
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AttendanceReportSectionTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun getString(
        resId: Int,
        vararg formatArgs: Any
    ): String = context.getString(resId, *formatArgs)

    private fun getQuantityString(
        resId: Int,
        quantity: Int,
        vararg formatArgs: Any
    ): String = context.resources.getQuantityString(resId, quantity, *formatArgs)

    private fun monthYear(
        year: Int,
        monthResId: Int
    ): String = getString(R.string.date_format_month_year, getString(monthResId), year)

    private fun openMonthSelector() {
        composeTestRule
            .onNodeWithContentDescription(
                getString(R.string.student_detail_select_month),
                substring = true
            )
            .performClick()
    }

    @Test
    fun calendarIsShownEvenWhenNoMarkedDates() {
        composeTestRule.setContent {
            AbsenceRecordTheme {
                AttendanceReportSection(
                    attendanceDates = emptyList(),
                    selectedMonth = null,
                    onMonthSelected = {},
                    onShare = {}
                )
            }
        }

        composeTestRule.onNodeWithText(getString(R.string.day_short_saturday)).assertIsDisplayed()
        composeTestRule.onNodeWithText(getString(R.string.no_attendance_records)).assertIsDisplayed()
    }

    @Test
    fun monthDropdownShowsMonthYearAndPresentCount() {
        composeTestRule.setContent {
            AbsenceRecordTheme {
                AttendanceReportSection(
                    attendanceDates =
                        listOf(
                            LocalDate(2023, 1, 15),
                            LocalDate(2023, 1, 20),
                            LocalDate(2023, 2, 3)
                        ),
                    selectedMonth = null,
                    onMonthSelected = {},
                    onShare = {}
                )
            }
        }

        openMonthSelector()

        composeTestRule.onNodeWithText(monthYear(2023, R.string.month_february)).assertIsDisplayed()
        composeTestRule.onNodeWithText(monthYear(2023, R.string.month_january)).assertIsDisplayed()
        composeTestRule
            .onNodeWithText(getQuantityString(R.plurals.present_days_count, 1, 1))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(getQuantityString(R.plurals.present_days_count, 2, 2))
            .assertIsDisplayed()
    }

    @Test
    fun selectingMonthFromDropdownSelectsIt() {
        var selectedMonth: LocalDate? = null

        composeTestRule.setContent {
            AbsenceRecordTheme {
                AttendanceReportSection(
                    attendanceDates =
                        listOf(
                            LocalDate(2023, 1, 15),
                            LocalDate(2023, 1, 20),
                            LocalDate(2023, 2, 3)
                        ),
                    selectedMonth = null,
                    onMonthSelected = { month -> selectedMonth = month },
                    onShare = {}
                )
            }
        }

        openMonthSelector()

        val januaryLabel = monthYear(2023, R.string.month_january)
        val twoDaysLabel = getQuantityString(R.plurals.present_days_count, 2, 2)
        composeTestRule
            .onNodeWithContentDescription("$januaryLabel, $twoDaysLabel")
            .performClick()

        assertEquals(
            "Expected January 2023 to be selected but was $selectedMonth",
            LocalDate(2023, 1, 1),
            selectedMonth
        )
    }

    @Test
    fun shareButtonInvokesOnShare() {
        var shared = false

        composeTestRule.setContent {
            AbsenceRecordTheme {
                AttendanceReportSection(
                    attendanceDates = emptyList(),
                    selectedMonth = null,
                    onMonthSelected = {},
                    onShare = { shared = true }
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription(getString(R.string.student_detail_share))
            .performClick()

        assertTrue("Expected onShare to be invoked", shared)
    }
}