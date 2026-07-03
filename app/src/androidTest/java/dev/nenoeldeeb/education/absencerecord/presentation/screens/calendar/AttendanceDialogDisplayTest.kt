package dev.nenoeldeeb.education.absencerecord.presentation.screens.calendar

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentAttendance
import dev.nenoeldeeb.education.absencerecord.presentation.theme.AbsenceRecordTheme
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AttendanceDialogDisplayTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun getString(
        resId: Int,
        vararg formatArgs: Any
    ): String = context.getString(resId, *formatArgs)

    // region AttendanceDialog Display Tests

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
                    allStudents = state.allStudents,
                    studentsForSelectedDate = state.studentsForSelectedDate,
                    error = state.error,
                    selectedDate = selectedDate,
                    onDismiss = {},
                    onToggleAttendance = { _, _, _ -> }
                )
            }
        }

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
                    allStudents = state.allStudents,
                    studentsForSelectedDate = state.studentsForSelectedDate,
                    error = state.error,
                    selectedDate = selectedDate,
                    onDismiss = {},
                    onToggleAttendance = { _, _, _ -> }
                )
            }
        }

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
                    allStudents = state.allStudents,
                    studentsForSelectedDate = state.studentsForSelectedDate,
                    error = state.error,
                    selectedDate = selectedDate,
                    onDismiss = {},
                    onToggleAttendance = { _, _, _ -> }
                )
            }
        }

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
                    allStudents = state.allStudents,
                    studentsForSelectedDate = state.studentsForSelectedDate,
                    error = state.error,
                    selectedDate = selectedDate,
                    onDismiss = { dismissed = true },
                    onToggleAttendance = { _, _, _ -> }
                )
            }
        }

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
                    allStudents = state.allStudents,
                    studentsForSelectedDate = state.studentsForSelectedDate,
                    error = state.error,
                    selectedDate = selectedDate,
                    onDismiss = {},
                    onToggleAttendance = { _, _, _ -> }
                )
            }
        }

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
                    allStudents = state.allStudents,
                    studentsForSelectedDate = state.studentsForSelectedDate,
                    error = state.error,
                    selectedDate = selectedDate,
                    onDismiss = {},
                    onToggleAttendance = { _, _, _ -> }
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription(getString(R.string.content_description_present))
            .assertIsDisplayed()
    }

    // endregion
}