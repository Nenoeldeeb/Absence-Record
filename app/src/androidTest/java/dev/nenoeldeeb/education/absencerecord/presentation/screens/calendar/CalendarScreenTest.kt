package dev.nenoeldeeb.education.absencerecord.presentation.screens.calendar

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentAttendance
import dev.nenoeldeeb.education.absencerecord.presentation.theme.AbsenceRecordTheme
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation tests for [CalendarScreen] AttendanceDialog composable.
 *
 * These tests verify the UI behavior of attendance dialog toggle callbacks, empty
 * states, attendance count updates, and screen-level snackbar errors.
 */
@RunWith(AndroidJUnit4::class)
class CalendarScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    // region Helper functions

    private fun getString(
        resId: Int,
        vararg formatArgs: Any
    ): String = context.getString(resId, *formatArgs)

    // endregion

    // region AttendanceDialog Toggle Callback Tests

    @Test
    fun attendanceDialog_clickingStudentInvokesToggleCallback() {
        var toggledStudent: Student? = null
        var toggledDate: LocalDate? = null
        var wasPresent: Boolean? = null

        val selectedDate = LocalDate(2026, 1, 15)
        val student = Student(id = 1, name = "Clickable Student")
        val state =
            CalendarScreenState(
                selectedDateForDialog = selectedDate,
                allStudents = listOf(student),
                studentsForSelectedDate = emptyList()
            )

        composeTestRule.setContent {
            AbsenceRecordTheme {
                AttendanceDialog(
                    allStudents = state.allStudents,
                    studentsForSelectedDate = state.studentsForSelectedDate,
                    selectedDate = selectedDate,
                    availableClasses = state.availableClasses,
                    selectedClassIds = state.selectedClassIds,
                    filterDropdownExpanded = state.filterDropdownExpanded,
                    onDismiss = {},
                    onToggleAttendance = { s, d, p ->
                        toggledStudent = s
                        toggledDate = d
                        wasPresent = p
                    },
                    onToggleClassSelection = { },
                    onToggleFilterDropdown = { }
                )
            }
        }

        composeTestRule.onNodeWithText("Clickable Student").performClick()

        assert(toggledStudent == student) { "Expected student callback" }
        assert(toggledDate == selectedDate) { "Expected date callback" }
        assert(wasPresent == false) { "Expected isPresent=false for absent student" }
    }

    @Test
    fun attendanceDialog_clickingPresentStudentPassesIsPresentTrue() {
        var wasPresent: Boolean? = null

        val selectedDate = LocalDate(2026, 1, 15)
        val student = Student(id = 1, name = "Present Student")
        val state =
            CalendarScreenState(
                selectedDateForDialog = selectedDate,
                allStudents = listOf(student),
                studentsForSelectedDate =
                    listOf(StudentAttendance(studentId = 1, date = selectedDate))
            )

        composeTestRule.setContent {
            AbsenceRecordTheme {
                AttendanceDialog(
                    allStudents = state.allStudents,
                    studentsForSelectedDate = state.studentsForSelectedDate,
                    selectedDate = selectedDate,
                    availableClasses = state.availableClasses,
                    selectedClassIds = state.selectedClassIds,
                    filterDropdownExpanded = state.filterDropdownExpanded,
                    onDismiss = {},
                    onToggleAttendance = { _, _, p -> wasPresent = p },
                    onToggleClassSelection = { },
                    onToggleFilterDropdown = { }
                )
            }
        }

        composeTestRule.onNodeWithText("Present Student").performClick()

        assert(wasPresent == true) { "Expected isPresent=true for present student" }
    }

    // endregion

    // region CalendarScreen Error Snackbar Tests

    @Test
    fun calendarScreen_displaysErrorInSnackbar() {
        val errorMessage = "Test error occurred"
        val mockViewModel = mockk<CalendarViewModel>(relaxed = true)
        val stateFlow = MutableStateFlow(CalendarScreenState(error = UiText.DynamicString(errorMessage)))
        every { mockViewModel.uiState } returns stateFlow

        composeTestRule.setContent {
            AbsenceRecordTheme {
                CalendarScreen(viewModel = mockViewModel)
            }
        }

        composeTestRule.onNodeWithText(errorMessage).assertIsDisplayed()
    }

    // endregion

    // region Empty State Tests

    @Test
    fun attendanceDialog_displaysNoStudentsMessage_whenStudentListEmpty() {
        val selectedDate = LocalDate(2026, 1, 15)
        val state =
            CalendarScreenState(
                selectedDateForDialog = selectedDate,
                allStudents = emptyList(),
                studentsForSelectedDate = emptyList()
            )

        composeTestRule.setContent {
            AbsenceRecordTheme {
                AttendanceDialog(
                    allStudents = state.allStudents,
                    studentsForSelectedDate = state.studentsForSelectedDate,
                    selectedDate = selectedDate,
                    availableClasses = state.availableClasses,
                    selectedClassIds = state.selectedClassIds,
                    filterDropdownExpanded = state.filterDropdownExpanded,
                    onDismiss = {},
                    onToggleAttendance = { _, _, _ -> },
                    onToggleClassSelection = { },
                    onToggleFilterDropdown = { }
                )
            }
        }

        // Verify "No students available to mark attendance." message
        composeTestRule
            .onNodeWithText(getString(R.string.no_students_for_attendance))
            .assertIsDisplayed()
    }

    // endregion

    // region Multiple Students Tests

    @Test
    fun attendanceDialog_displaysMixedAttendanceStates() {
        val selectedDate = LocalDate(2026, 1, 15)
        val state =
            CalendarScreenState(
                selectedDateForDialog = selectedDate,
                allStudents =
                    listOf(
                        Student(id = 1, name = "Present Alice"),
                        Student(id = 2, name = "Absent Bob")
                    ),
                studentsForSelectedDate =
                    listOf(StudentAttendance(studentId = 1, date = selectedDate))
            )

        composeTestRule.setContent {
            AbsenceRecordTheme {
                AttendanceDialog(
                    allStudents = state.allStudents,
                    studentsForSelectedDate = state.studentsForSelectedDate,
                    selectedDate = selectedDate,
                    availableClasses = state.availableClasses,
                    selectedClassIds = state.selectedClassIds,
                    filterDropdownExpanded = state.filterDropdownExpanded,
                    onDismiss = {},
                    onToggleAttendance = { _, _, _ -> },
                    onToggleClassSelection = { },
                    onToggleFilterDropdown = { }
                )
            }
        }

        // Verify both students are displayed
        composeTestRule.onNodeWithText("Present Alice").assertIsDisplayed()
        composeTestRule.onNodeWithText("Absent Bob").assertIsDisplayed()

        // Verify correct content descriptions exist
        composeTestRule
            .onAllNodes(
                hasContentDescription(
                    getString(
                        R.string.student_attendance_status,
                        "Present Alice",
                        getString(R.string.content_description_present)
                    )
                )
            )
            .fetchSemanticsNodes()
            .isNotEmpty()
        composeTestRule
            .onAllNodes(
                hasContentDescription(
                    getString(
                        R.string.student_attendance_status,
                        "Absent Bob",
                        getString(R.string.content_description_absent)
                    )
                )
            )
            .fetchSemanticsNodes()
            .isNotEmpty()
    }

    @Test
    fun attendanceDialog_attendanceCountUpdatesWithMultiplePresent() {
        val selectedDate = LocalDate(2026, 1, 15)
        val state =
            CalendarScreenState(
                selectedDateForDialog = selectedDate,
                allStudents =
                    listOf(
                        Student(id = 1, name = "Student 1"),
                        Student(id = 2, name = "Student 2"),
                        Student(id = 3, name = "Student 3"),
                        Student(id = 4, name = "Student 4")
                    ),
                studentsForSelectedDate =
                    listOf(
                        StudentAttendance(studentId = 1, date = selectedDate),
                        StudentAttendance(studentId = 3, date = selectedDate)
                    )
            )

        composeTestRule.setContent {
            AbsenceRecordTheme {
                AttendanceDialog(
                    allStudents = state.allStudents,
                    studentsForSelectedDate = state.studentsForSelectedDate,
                    selectedDate = selectedDate,
                    availableClasses = state.availableClasses,
                    selectedClassIds = state.selectedClassIds,
                    filterDropdownExpanded = state.filterDropdownExpanded,
                    onDismiss = {},
                    onToggleAttendance = { _, _, _ -> },
                    onToggleClassSelection = { },
                    onToggleFilterDropdown = { }
                )
            }
        }

        // Verify attendance count "4 | 2"
        composeTestRule
            .onNodeWithText(getString(R.string.attendance_count, 4, 2))
            .assertIsDisplayed()
    }

    // endregion
}