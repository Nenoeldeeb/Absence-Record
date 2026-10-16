package dev.nenoeldeeb.education.absencerecord.presentation.screens.students

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.dialogs.StudentDialog
import dev.nenoeldeeb.education.absencerecord.presentation.theme.AbsenceRecordTheme
import org.junit.Test
import org.junit.runner.RunWith

/** The add/edit student dialog (SC-018). */
@RunWith(AndroidJUnit4::class)
class StudentDialogTest : StudentsScreenTestBase() {
    @Test
    fun studentDialog_displaysAddTitle_inAddMode() {
        val uiState = StudentsScreenState(showAddStudentDialog = true)

        composeTestRule.setContent {
            AbsenceRecordTheme {
                StudentDialog(
                    newStudentName = uiState.newStudentName,
                    availableClasses = emptyList(),
                    onStudentNameChange = {},
                    onSave = { _, _ -> },
                    onImportClick = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithText(getString(R.string.new_student_label)).assertIsDisplayed()
        composeTestRule.onNodeWithText(getString(R.string.import_students_action)).assertIsDisplayed()
        composeTestRule.onNodeWithText(getString(R.string.action_add)).assertIsDisplayed()
    }

    @Test
    fun studentDialog_confirmButtonDisabled_whenNameBlank() {
        val uiState = StudentsScreenState(showAddStudentDialog = true, newStudentName = "")

        composeTestRule.setContent {
            AbsenceRecordTheme {
                StudentDialog(
                    newStudentName = uiState.newStudentName,
                    availableClasses = emptyList(),
                    onStudentNameChange = {},
                    onSave = { _, _ -> },
                    onImportClick = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithText(getString(R.string.action_add)).assertIsNotEnabled()
    }
}