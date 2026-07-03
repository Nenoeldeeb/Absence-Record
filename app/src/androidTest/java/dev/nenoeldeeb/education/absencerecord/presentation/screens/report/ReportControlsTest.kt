package dev.nenoeldeeb.education.absencerecord.presentation.screens.report

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.SortType
import dev.nenoeldeeb.education.absencerecord.presentation.screens.report.components.ReportControls
import dev.nenoeldeeb.education.absencerecord.presentation.theme.AbsenceRecordTheme
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReportControlsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun getString(
        resId: Int,
        vararg formatArgs: Any
    ): String {
        return context.getString(resId, *formatArgs)
    }

    // region ReportControls Tests

    @Test
    fun reportControls_displaysAllMonthsByDefault() {
        composeTestRule.setContent {
            AbsenceRecordTheme {
                ReportControls(
                    selectedMonth = null,
                    availableMonths = emptyList(),
                    monthDropdownExpanded = false,
                    sortType = SortType.ByName,
                    onMonthSelected = {},
                    onMonthCleared = {},
                    onMonthDropdownToggled = {},
                    onSortTypeToggled = {}
                )
            }
        }

        composeTestRule.onNodeWithText(getString(R.string.all_months)).assertIsDisplayed()
    }

    @Test
    fun reportControls_displaysSelectedMonth() {
        val selectedMonth = LocalDate(2026, 1, 1)

        composeTestRule.setContent {
            AbsenceRecordTheme {
                ReportControls(
                    selectedMonth = selectedMonth,
                    availableMonths = emptyList(),
                    monthDropdownExpanded = false,
                    sortType = SortType.ByName,
                    onMonthSelected = {},
                    onMonthCleared = {},
                    onMonthDropdownToggled = {},
                    onSortTypeToggled = {}
                )
            }
        }

        composeTestRule.onNodeWithText("January 2026", substring = true).assertIsDisplayed()
    }

    @Test
    fun reportControls_togglingSortTypeInvokesEvent() {
        var eventTriggered = false

        composeTestRule.setContent {
            AbsenceRecordTheme {
                ReportControls(
                    selectedMonth = null,
                    availableMonths = emptyList(),
                    monthDropdownExpanded = false,
                    sortType = SortType.ByName,
                    onMonthSelected = {},
                    onMonthCleared = {},
                    onMonthDropdownToggled = {},
                    onSortTypeToggled = { eventTriggered = true }
                )
            }
        }

        composeTestRule.onNodeWithText(getString(R.string.sort_action_rate)).performClick()

        assert(eventTriggered)
    }

    @Test
    fun reportControls_clearingMonthFilterInvokesEvent() {
        var clearTriggered = false
        val selectedMonth = LocalDate(2026, 1, 1)

        composeTestRule.setContent {
            AbsenceRecordTheme {
                ReportControls(
                    selectedMonth = selectedMonth,
                    availableMonths = emptyList(),
                    monthDropdownExpanded = false,
                    sortType = SortType.ByName,
                    onMonthSelected = {},
                    onMonthCleared = { clearTriggered = true },
                    onMonthDropdownToggled = {},
                    onSortTypeToggled = {}
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription(getString(R.string.clear_month_filter))
            .performClick()

        assert(clearTriggered)
    }

    @Test
    fun reportControls_openingDropdownDisplaysMonths() {
        val availableMonths = listOf(LocalDate(2026, 1, 1), LocalDate(2026, 2, 1))

        composeTestRule.setContent {
            AbsenceRecordTheme {
                ReportControls(
                    selectedMonth = null,
                    availableMonths = availableMonths,
                    monthDropdownExpanded = true,
                    sortType = SortType.ByName,
                    onMonthSelected = {},
                    onMonthCleared = {},
                    onMonthDropdownToggled = {},
                    onSortTypeToggled = {}
                )
            }
        }

        composeTestRule.onNodeWithText("January 2026", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("February 2026", substring = true).assertIsDisplayed()
    }

    @Test
    fun reportControls_selectingMonthInvokesUpdateEvent() {
        var selectedMonth: LocalDate? = null
        val month = LocalDate(2026, 1, 1)

        composeTestRule.setContent {
            AbsenceRecordTheme {
                ReportControls(
                    selectedMonth = null,
                    availableMonths = listOf(month),
                    monthDropdownExpanded = true,
                    sortType = SortType.ByName,
                    onMonthSelected = { selectedMonth = it },
                    onMonthCleared = {},
                    onMonthDropdownToggled = {},
                    onSortTypeToggled = {}
                )
            }
        }

        composeTestRule.onNodeWithText("January 2026", substring = true).performClick()

        assert(selectedMonth == month)
    }

    // endregion
}
