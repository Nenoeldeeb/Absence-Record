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
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.presentation.theme.AbsenceRecordTheme
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReportScreenTest {
    @get:Rule val composeTestRule = createComposeRule()

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
        val uiState = ReportScreenState()

        composeTestRule.setContent {
            AbsenceRecordTheme { ReportControls(uiState = uiState, onEvent = {}) }
        }

        composeTestRule.onNodeWithText(getString(R.string.all_months)).assertIsDisplayed()
    }

    @Test
    fun reportControls_displaysSelectedMonth() {
        val selectedMonth = LocalDate(2026, 1, 1)
        val uiState = ReportScreenState(selectedMonth = selectedMonth)

        composeTestRule.setContent {
            AbsenceRecordTheme { ReportControls(uiState = uiState, onEvent = {}) }
        }

        // The month is formatted as "January 2026"
        composeTestRule.onNodeWithText("January 2026", substring = true).assertIsDisplayed()
    }

    @Test
    fun reportControls_togglingSortTypeInvokesEvent() {
        var eventSent: ReportScreenEvent? = null
        val uiState = ReportScreenState(sortType = SortType.ByName)

        composeTestRule.setContent {
            AbsenceRecordTheme { ReportControls(uiState = uiState, onEvent = { eventSent = it }) }
        }

        // When sorted by name, button shows "Sort by Rate" (R.string.sort_action_rate)
        composeTestRule.onNodeWithText(getString(R.string.sort_action_rate)).performClick()

        assert(eventSent is ReportScreenEvent.ToggleSortType)
    }

    @Test
    fun reportControls_clearingMonthFilterInvokesEvent() {
        var events = mutableListOf<ReportScreenEvent>()
        val selectedMonth = LocalDate(2026, 1, 1)
        val uiState = ReportScreenState(selectedMonth = selectedMonth)

        composeTestRule.setContent {
            AbsenceRecordTheme { ReportControls(uiState = uiState, onEvent = { events += it }) }
        }

        composeTestRule
            .onNodeWithContentDescription(getString(R.string.clear_month_filter))
            .performClick()

        assert(events.any { it is ReportScreenEvent.ClearMonthFilter })
    }

    @Test
    fun reportControls_openingDropdownDisplaysMonths() {
        val availableMonths = listOf(LocalDate(2026, 1, 1), LocalDate(2026, 2, 1))
        val uiState =
            ReportScreenState(availableMonths = availableMonths, monthDropdownExpanded = true)

        composeTestRule.setContent {
            AbsenceRecordTheme { ReportControls(uiState = uiState, onEvent = {}) }
        }

        composeTestRule.onNodeWithText("January 2026", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("February 2026", substring = true).assertIsDisplayed()
    }

    @Test
    fun reportControls_selectingMonthInvokesUpdateEvent() {
        var events = mutableListOf<ReportScreenEvent>()
        val month = LocalDate(2026, 1, 1)
        val uiState =
            ReportScreenState(availableMonths = listOf(month), monthDropdownExpanded = true)

        composeTestRule.setContent {
            AbsenceRecordTheme { ReportControls(uiState = uiState, onEvent = { events += it }) }
        }

        composeTestRule.onNodeWithText("January 2026", substring = true).performClick()

        assert(events.any { it is ReportScreenEvent.UpdateSelectedMonth })
        val eventMonth =
            (
                events.find { it is ReportScreenEvent.UpdateSelectedMonth }
                    as ReportScreenEvent.UpdateSelectedMonth
            ).month
        assert(eventMonth == month)
    }

    // endregion

    // region StudentList Tests

    @Test
    fun studentList_displaysNoStudentsMessage_whenListEmpty() {
        composeTestRule.setContent {
            AbsenceRecordTheme { StudentList(students = emptyList(), onStudentClick = {}) }
        }

        composeTestRule.onNodeWithText(getString(R.string.no_students_found)).assertIsDisplayed()
    }

    @Test
    fun studentList_displaysStudentNames() {
        val students = listOf(Student(id = 1, name = "Alice"), Student(id = 2, name = "Bob"))

        composeTestRule.setContent {
            AbsenceRecordTheme { StudentList(students = students, onStudentClick = {}) }
        }

        composeTestRule.onNodeWithText("Alice").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bob").assertIsDisplayed()
    }

    @Test
    fun studentList_clickingStudentInvokesCallback() {
        var clickedStudent: Student? = null
        val student = Student(id = 1, name = "Alice")

        composeTestRule.setContent {
            AbsenceRecordTheme {
                StudentList(students = listOf(student), onStudentClick = { clickedStudent = it })
            }
        }

        composeTestRule.onNodeWithText("Alice").performClick()

        assert(clickedStudent == student)
    }

    // endregion

    // region StudentHistoryDialog Tests

    @Test
    fun studentHistoryDialog_displaysTitleWithStudentName() {
        val student = Student(id = 1, name = "Alice")

        composeTestRule.setContent {
            AbsenceRecordTheme {
                StudentHistoryDialog(
                    student = student,
                    history = emptyList(),
                    onDismiss = {},
                    onShareMonth = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText(getString(R.string.student_history_title, "Alice"))
            .assertIsDisplayed()
    }

    @Test
    fun studentHistoryDialog_displaysNoRecordsMessage_whenHistoryEmpty() {
        val student = Student(id = 1, name = "Alice")

        composeTestRule.setContent {
            AbsenceRecordTheme {
                StudentHistoryDialog(
                    student = student,
                    history = emptyList(),
                    onDismiss = {},
                    onShareMonth = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText(getString(R.string.no_attendance_records))
            .assertIsDisplayed()
    }

    @Test
    fun studentHistoryDialog_displaysHistoryItems() {
        val student = Student(id = 1, name = "Alice")
        val history =
            listOf(
                LocalDate(2026, 1, 1) to
                    listOf(LocalDate(2026, 1, 5), LocalDate(2026, 1, 10))
            )

        composeTestRule.setContent {
            AbsenceRecordTheme {
                StudentHistoryDialog(
                    student = student,
                    history = history,
                    onDismiss = {},
                    onShareMonth = {}
                )
            }
        }

        composeTestRule.onNodeWithText("January 2026", substring = true).assertIsDisplayed()
        // Check for plural string "2 days"
        // Since it's a plural, we can check for substring "2" or "days"
        composeTestRule.onNodeWithText("2", substring = true).assertIsDisplayed()
    }

    @Test
    fun studentHistoryDialog_clickingShareInvokesCallback() {
        var sharedMonth: LocalDate? = null
        val student = Student(id = 1, name = "Alice")
        val month = LocalDate(2026, 1, 1)
        val history = listOf(month to listOf(LocalDate(2026, 1, 5)))

        composeTestRule.setContent {
            AbsenceRecordTheme {
                StudentHistoryDialog(
                    student = student,
                    history = history,
                    onDismiss = {},
                    onShareMonth = { sharedMonth = it }
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription(getString(R.string.share_calendar_image))
            .performClick()

        assert(sharedMonth == month)
    }

    // endregion

    // region CalendarPreviewDialog Tests

    @Test
    fun calendarPreviewDialog_displaysShareButton() {
        val student = Student(id = 1, name = "Alice")
        val month = LocalDate(2026, 1, 1)

        composeTestRule.setContent {
            AbsenceRecordTheme {
                CalendarPreviewDialog(
                    student = student,
                    monthToPreview = month,
                    datesForPreviewMonth = emptyList(),
                    onDismiss = {},
                    onShare = { _, _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithText(getString(R.string.action_share_image)).assertIsDisplayed()
    }

    @Test
    fun calendarPreviewDialog_clickingShareInvokesCallback() {
        var sharedMonth: LocalDate? = null
        var sharedId: Int? = null
        var sharedName: String? = null

        val student = Student(id = 1, name = "Alice")
        val month = LocalDate(2026, 1, 1)

        composeTestRule.setContent {
            AbsenceRecordTheme {
                CalendarPreviewDialog(
                    student = student,
                    monthToPreview = month,
                    datesForPreviewMonth = emptyList(),
                    onDismiss = {},
                    onShare = { m, id, name ->
                        sharedMonth = m
                        sharedId = id
                        sharedName = name
                    }
                )
            }
        }

        composeTestRule.onNodeWithText(getString(R.string.action_share_image)).performClick()

        assert(sharedMonth == month)
        assert(sharedId == student.id)
        assert(sharedName == student.name)
    }

    // endregion
}