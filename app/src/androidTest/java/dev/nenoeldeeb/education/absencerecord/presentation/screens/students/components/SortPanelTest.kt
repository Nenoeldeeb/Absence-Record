package dev.nenoeldeeb.education.absencerecord.presentation.screens.students.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.SortType
import dev.nenoeldeeb.education.absencerecord.presentation.theme.AbsenceRecordTheme
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SortPanelTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val december = LocalDate(2024, Month.DECEMBER, 1)

    private fun getString(
        resId: Int,
        vararg formatArgs: Any
    ): String {
        return context.getString(resId, *formatArgs)
    }

    private fun monthLabel(date: LocalDate): String =
        getString(
            R.string.date_format_month_year,
            getString(R.string.month_december),
            date.year
        )

    @Test
    fun sortPanel_displaysAllMonthsAndCurrentSortMode_byDefault() {
        composeTestRule.setContent {
            AbsenceRecordTheme {
                SortPanel(
                    sortType = SortType.ByName,
                    selectedMonth = null,
                    availableMonths = listOf(december),
                    monthDropdownExpanded = false,
                    onMonthDropdownExpandedChange = {},
                    onMonthSelected = {},
                    onToggleSortType = {}
                )
            }
        }

        composeTestRule.onNodeWithText(getString(R.string.all_months)).assertIsDisplayed()
        composeTestRule.onNodeWithText(getString(R.string.sort_by_name)).assertIsDisplayed()
    }

    @Test
    fun sortPanel_displaysAttendanceMode_whenByAttendance() {
        composeTestRule.setContent {
            AbsenceRecordTheme {
                SortPanel(
                    sortType = SortType.ByAttendance,
                    selectedMonth = null,
                    availableMonths = emptyList(),
                    monthDropdownExpanded = false,
                    onMonthDropdownExpandedChange = {},
                    onMonthSelected = {},
                    onToggleSortType = {}
                )
            }
        }

        composeTestRule.onNodeWithText(getString(R.string.sort_by_attendance)).assertIsDisplayed()
    }

    @Test
    fun sortPanel_selectingMonth_firesCallback() {
        var selectedMonth: LocalDate? = null
        composeTestRule.setContent {
            AbsenceRecordTheme {
                SortPanel(
                    sortType = SortType.ByName,
                    selectedMonth = null,
                    availableMonths = listOf(december),
                    monthDropdownExpanded = true,
                    onMonthDropdownExpandedChange = {},
                    onMonthSelected = { selectedMonth = it },
                    onToggleSortType = {}
                )
            }
        }

        composeTestRule.onNodeWithText(monthLabel(december)).performClick()

        assertEquals(december, selectedMonth)
    }

    @Test
    fun sortPanel_selectingAllMonths_clearsSelection() {
        var selectedMonth: LocalDate? = december
        composeTestRule.setContent {
            AbsenceRecordTheme {
                SortPanel(
                    sortType = SortType.ByName,
                    selectedMonth = december,
                    availableMonths = listOf(december),
                    monthDropdownExpanded = true,
                    onMonthDropdownExpandedChange = {},
                    onMonthSelected = { selectedMonth = it },
                    onToggleSortType = {}
                )
            }
        }

        composeTestRule.onNodeWithText(getString(R.string.all_months)).performClick()

        assertEquals(null, selectedMonth)
    }

    @Test
    fun sortPanel_toggleButton_firesCallback() {
        var toggleCount = 0
        composeTestRule.setContent {
            AbsenceRecordTheme {
                SortPanel(
                    sortType = SortType.ByName,
                    selectedMonth = null,
                    availableMonths = emptyList(),
                    monthDropdownExpanded = false,
                    onMonthDropdownExpandedChange = {},
                    onMonthSelected = {},
                    onToggleSortType = { toggleCount++ }
                )
            }
        }

        composeTestRule.onNodeWithText(getString(R.string.sort_by_name)).performClick()

        assertEquals(1, toggleCount)
    }

    @Test
    fun sortPanel_sortButton_hasContentDescription() {
        composeTestRule.setContent {
            AbsenceRecordTheme {
                SortPanel(
                    sortType = SortType.ByName,
                    selectedMonth = null,
                    availableMonths = emptyList(),
                    monthDropdownExpanded = false,
                    onMonthDropdownExpandedChange = {},
                    onMonthSelected = {},
                    onToggleSortType = {}
                )
            }
        }

        val description =
            getString(R.string.sort_toggle_description, getString(R.string.sort_by_name))
        composeTestRule.onNodeWithContentDescription(description).assertIsDisplayed()
    }

    @Test
    fun sortPanel_monthButton_hasContentDescription() {
        composeTestRule.setContent {
            AbsenceRecordTheme {
                SortPanel(
                    sortType = SortType.ByName,
                    selectedMonth = december,
                    availableMonths = listOf(december),
                    monthDropdownExpanded = false,
                    onMonthDropdownExpandedChange = {},
                    onMonthSelected = {},
                    onToggleSortType = {}
                )
            }
        }

        val description =
            "${getString(R.string.sort_select_month)}: ${monthLabel(december)}"
        composeTestRule.onNodeWithContentDescription(description).assertIsDisplayed()
    }
}