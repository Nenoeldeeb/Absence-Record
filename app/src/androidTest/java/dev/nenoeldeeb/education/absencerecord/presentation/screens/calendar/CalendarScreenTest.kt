package dev.nenoeldeeb.education.absencerecord.presentation.screens.calendar

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentAttendance
import dev.nenoeldeeb.education.absencerecord.presentation.theme.AbsenceRecordTheme
import dev.nenoeldeeb.education.absencerecord.presentation.util.UiText
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation tests for [CalendarScreen] AttendanceDialog composable.
 *
 * These tests verify the UI behavior of attendance dialog, student attendance toggling, error
 * states, and empty states.
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

    // region AttendanceDialog Tests

    @Test
    fun attendanceDialog_displaysFormattedDateInTitle() {
        val selectedDate = LocalDate(2026, 1, 15)
        val state =
            CalendarScreenState(
                selectedDateForDialog = selectedDate,
                allStudents = listOf(Student(id = 1, name = "Test Student")),
                studentsForSelectedDate = emptyList()
            )

        composeTestRule.setContent {
            AbsenceRecordTheme {
                AttendanceDialog(
                    uiState = state,
                    selectedDate = selectedDate,
                    onDismiss = {},
                    onToggleAttendance = { _, _, _ -> }
                )
            }
        }

        // Verify dialog title contains "Mark Attendance"
        composeTestRule.onNode(hasText("Mark Attendance", substring = true)).assertIsDisplayed()
    }

    @Test
    fun attendanceDialog_displaysAttendanceCount() {
        val selectedDate = LocalDate(2026, 1, 15)
        val state =
            CalendarScreenState(
                selectedDateForDialog = selectedDate,
                allStudents =
                    listOf(
                        Student(id = 1, name = "Student 1"),
                        Student(id = 2, name = "Student 2"),
                        Student(id = 3, name = "Student 3")
                    ),
                studentsForSelectedDate =
                    listOf(StudentAttendance(studentId = 1, date = selectedDate))
            )

        composeTestRule.setContent {
            AbsenceRecordTheme {
                AttendanceDialog(
                    uiState = state,
                    selectedDate = selectedDate,
                    onDismiss = {},
                    onToggleAttendance = { _, _, _ -> }
                )
            }
        }

        // Verify attendance count format "3 | 1" (totalStudents | presentStudents)
        composeTestRule
            .onNodeWithText(getString(R.string.attendance_count, 3, 1))
            .assertIsDisplayed()
    }

    @Test
    fun attendanceDialog_displaysStudentList() {
        val selectedDate = LocalDate(2026, 1, 15)
        val state =
            CalendarScreenState(
                selectedDateForDialog = selectedDate,
                allStudents =
                    listOf(
                        Student(id = 1, name = "Alice"),
                        Student(id = 2, name = "Bob")
                    ),
                studentsForSelectedDate = emptyList()
            )

        composeTestRule.setContent {
            AbsenceRecordTheme {
                AttendanceDialog(
                    uiState = state,
                    selectedDate = selectedDate,
                    onDismiss = {},
                    onToggleAttendance = { _, _, _ -> }
                )
            }
        }

        // Verify student names are displayed
        composeTestRule.onNodeWithText("Alice").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bob").assertIsDisplayed()
    }

    @Test
    fun attendanceDialog_closeButtonDismissesDialog() {
        var dismissed = false
        val selectedDate = LocalDate(2026, 1, 15)
        val state =
            CalendarScreenState(
                selectedDateForDialog = selectedDate,
                allStudents = listOf(Student(id = 1, name = "Test")),
                studentsForSelectedDate = emptyList()
            )

        composeTestRule.setContent {
            AbsenceRecordTheme {
                AttendanceDialog(
                    uiState = state,
                    selectedDate = selectedDate,
                    onDismiss = { dismissed = true },
                    onToggleAttendance = { _, _, _ -> }
                )
            }
        }

        // Click close button
        composeTestRule.onNodeWithText(getString(R.string.action_close)).performClick()

        assert(dismissed) { "Expected dialog to be dismissed" }
    }

    @Test
    fun attendanceDialog_absentStudentHasAbsentContentDescription() {
        val selectedDate = LocalDate(2026, 1, 15)
        val state =
            CalendarScreenState(
                selectedDateForDialog = selectedDate,
                allStudents = listOf(Student(id = 1, name = "Absent Student")),
                studentsForSelectedDate = emptyList()
            )

        composeTestRule.setContent {
            AbsenceRecordTheme {
                AttendanceDialog(
                    uiState = state,
                    selectedDate = selectedDate,
                    onDismiss = {},
                    onToggleAttendance = { _, _, _ -> }
                )
            }
        }

        // Absent student should have "Absent" content description
        composeTestRule
            .onNodeWithContentDescription(getString(R.string.content_description_absent))
            .assertIsDisplayed()
    }

    @Test
    fun attendanceDialog_presentStudentHasPresentContentDescription() {
        val selectedDate = LocalDate(2026, 1, 15)
        val state =
            CalendarScreenState(
                selectedDateForDialog = selectedDate,
                allStudents = listOf(Student(id = 1, name = "Present Student")),
                studentsForSelectedDate =
                    listOf(StudentAttendance(studentId = 1, date = selectedDate))
            )

        composeTestRule.setContent {
            AbsenceRecordTheme {
                AttendanceDialog(
                    uiState = state,
                    selectedDate = selectedDate,
                    onDismiss = {},
                    onToggleAttendance = { _, _, _ -> }
                )
            }
        }

        // Present student should have "Present" content description
        composeTestRule
            .onNodeWithContentDescription(getString(R.string.content_description_present))
            .assertIsDisplayed()
    }

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
                    uiState = state,
                    selectedDate = selectedDate,
                    onDismiss = {},
                    onToggleAttendance = { s, d, p ->
                        toggledStudent = s
                        toggledDate = d
                        wasPresent = p
                    }
                )
            }
        }

        // Click on the student
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
                    uiState = state,
                    selectedDate = selectedDate,
                    onDismiss = {},
                    onToggleAttendance = { _, _, p -> wasPresent = p }
                )
            }
        }

        // Click on the present student
        composeTestRule.onNodeWithText("Present Student").performClick()

        assert(wasPresent == true) { "Expected isPresent=true for present student" }
    }

    // endregion

    // region Error State Tests

    @Test
    fun attendanceDialog_displaysErrorMessage() {
        val selectedDate = LocalDate(2026, 1, 15)
        val errorMessage = "Test error occurred"
        val state =
            CalendarScreenState(
                selectedDateForDialog = selectedDate,
                allStudents = listOf(Student(id = 1, name = "Test")),
                studentsForSelectedDate = emptyList(),
                error = UiText.DynamicString(errorMessage)
            )

        composeTestRule.setContent {
            AbsenceRecordTheme {
                AttendanceDialog(
                    uiState = state,
                    selectedDate = selectedDate,
                    onDismiss = {},
                    onToggleAttendance = { _, _, _ -> }
                )
            }
        }

        // Verify error message is displayed in "Error: {message}" format
        composeTestRule
            .onNodeWithText(getString(R.string.error_display, errorMessage))
            .assertIsDisplayed()
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
                    uiState = state,
                    selectedDate = selectedDate,
                    onDismiss = {},
                    onToggleAttendance = { _, _, _ -> }
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
                    uiState = state,
                    selectedDate = selectedDate,
                    onDismiss = {},
                    onToggleAttendance = { _, _, _ -> }
                )
            }
        }

        // Verify both students are displayed
        composeTestRule.onNodeWithText("Present Alice").assertIsDisplayed()
        composeTestRule.onNodeWithText("Absent Bob").assertIsDisplayed()

        // Verify correct content descriptions exist
        composeTestRule
            .onAllNodesWithContentDescription(getString(R.string.content_description_present))
            .fetchSemanticsNodes()
            .isNotEmpty()
        composeTestRule
            .onAllNodesWithContentDescription(getString(R.string.content_description_absent))
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
                    uiState = state,
                    selectedDate = selectedDate,
                    onDismiss = {},
                    onToggleAttendance = { _, _, _ -> }
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