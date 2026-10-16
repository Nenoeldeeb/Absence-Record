package dev.nenoeldeeb.education.absencerecord.presentation.screens.students

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.dialogs.BulkDeleteConfirmationDialog
import dev.nenoeldeeb.education.absencerecord.presentation.theme.AbsenceRecordTheme
import org.junit.Test
import org.junit.runner.RunWith

/** The bulk-delete confirmation dialog (SC-018). */
@RunWith(AndroidJUnit4::class)
class BulkDeleteConfirmationDialogTest : StudentsScreenTestBase() {
    @Test
    fun bulkDeleteConfirmationDialog_displaysCorrectCount() {
        val uiState = StudentsScreenState(selectedStudentIds = setOf(1, 2))

        composeTestRule.setContent {
            AbsenceRecordTheme {
                BulkDeleteConfirmationDialog(
                    selectedStudentIds = uiState.selectedStudentIds,
                    onConfirmDelete = {},
                    onDismiss = {}
                )
            }
        }

        // Check for plural message containing "2"
        composeTestRule.onNodeWithText("2", substring = true).assertIsDisplayed()
    }
}