package dev.nenoeldeeb.education.absencerecord.presentation.screens.report

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.presentation.screens.report.dialogs.StudentHistoryDialog
import dev.nenoeldeeb.education.absencerecord.presentation.theme.AbsenceRecordTheme
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StudentHistoryDialogTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun getString(
        resId: Int,
        vararg formatArgs: Any
    ): String {
        return context.getString(resId, *formatArgs)
    }

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
}