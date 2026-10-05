package dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail

import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
    fun emptyState_hidesCalendar_disablesShare() {
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

        composeTestRule.onNodeWithText(getString(R.string.no_attendance_records)).assertIsDisplayed()
        composeTestRule.onNodeWithText(getString(R.string.day_short_saturday)).assertIsNotDisplayed()
        composeTestRule
            .onNodeWithContentDescription(getString(R.string.student_detail_share))
            .assertIsNotEnabled()
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
                    selectedMonth = LocalDate(2023, 2, 1),
                    onMonthSelected = {},
                    onShare = {}
                )
            }
        }

        openMonthSelector()

        composeTestRule.onNodeWithText(monthYear(2023, R.string.month_february)).performScrollTo()
        composeTestRule.onNodeWithText(monthYear(2023, R.string.month_february)).assertIsDisplayed()
        composeTestRule.onNodeWithText(monthYear(2023, R.string.month_january)).performScrollTo()
        composeTestRule.onNodeWithText(monthYear(2023, R.string.month_january)).assertIsDisplayed()
        composeTestRule
            .onNodeWithText(getQuantityString(R.plurals.present_days_count, 1, 1))
            .assertIsDisplayed()
    }

    @Test
    fun monthDropdown_hidesZeroCountMonthsInRange() {
        composeTestRule.setContent {
            AbsenceRecordTheme {
                AttendanceReportSection(
                    attendanceDates = listOf(LocalDate(2023, 1, 15)),
                    selectedMonth = LocalDate(2023, 1, 1),
                    onMonthSelected = {},
                    onShare = {}
                )
            }
        }

        openMonthSelector()

        composeTestRule.onNodeWithText(monthYear(2023, R.string.month_january)).performScrollTo()
        composeTestRule.onNodeWithText(monthYear(2023, R.string.month_january)).assertIsDisplayed()
        assertTrue(
            "Expected no zero-count month in the dropdown",
            composeTestRule
                .onAllNodesWithText(getQuantityString(R.plurals.present_days_count, 0, 0))
                .fetchSemanticsNodes()
                .isEmpty()
        )
    }

    @Test
    fun share_disabledWhenDisplayMonthHasNoRecords() {
        composeTestRule.setContent {
            AbsenceRecordTheme {
                AttendanceReportSection(
                    attendanceDates = listOf(LocalDate(2023, 1, 15)),
                    selectedMonth = LocalDate(2023, 2, 1),
                    onMonthSelected = {},
                    onShare = {}
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription(getString(R.string.student_detail_share))
            .assertIsNotEnabled()
    }

    @Test
    fun share_enabledWhenDisplayMonthHasRecords() {
        composeTestRule.setContent {
            AbsenceRecordTheme {
                AttendanceReportSection(
                    attendanceDates = listOf(LocalDate(2023, 1, 15)),
                    selectedMonth = LocalDate(2023, 1, 1),
                    onMonthSelected = {},
                    onShare = {}
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription(getString(R.string.student_detail_share))
            .assertIsEnabled()
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
                    selectedMonth = LocalDate(2023, 2, 1),
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
            .performScrollTo()
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
                    attendanceDates = listOf(LocalDate(2023, 1, 15)),
                    selectedMonth = LocalDate(2023, 1, 1),
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

    @Test
    fun deadCalendar_daysAreNotClickable() {
        composeTestRule.setContent {
            AbsenceRecordTheme {
                AttendanceReportSection(
                    attendanceDates = listOf(LocalDate(2023, 1, 15)),
                    selectedMonth = LocalDate(2023, 1, 1),
                    onMonthSelected = {},
                    onShare = {}
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription(
                getString(R.string.calendar_date_present, "15"),
                substring = false
            )
            .assertIsDisplayed()
            .assertHasNoClickAction()
    }
}