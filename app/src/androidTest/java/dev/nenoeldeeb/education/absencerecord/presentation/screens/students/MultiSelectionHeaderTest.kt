package dev.nenoeldeeb.education.absencerecord.presentation.screens.students

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.components.MultiSelectionHeader
import dev.nenoeldeeb.education.absencerecord.presentation.theme.AbsenceRecordTheme
import org.junit.Test
import org.junit.runner.RunWith

/** The multi-selection bar shown while the teacher is picking students (SC-018). */
@RunWith(AndroidJUnit4::class)
class MultiSelectionHeaderTest : StudentsScreenTestBase() {
    @Test
    fun multiSelectionHeader_displaysSelectedCount() {
        val uiState =
            StudentsScreenState(
                isMultiSelectionMode = true,
                selectedStudentIds = setOf(1, 2, 3)
            )

        composeTestRule.setContent {
            AbsenceRecordTheme {
                MultiSelectionHeader(
                    selectedStudentIds = uiState.selectedStudentIds,
                    totalCount = 5,
                    onCloseSelectionMode = {},
                    onToggleSelection = {},
                    onExport = {},
                    onExportAndDelete = {},
                    onShowDeleteDialog = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText(getString(R.string.selection_count_selected_with_total, 3, 5))
            .assertIsDisplayed()
    }

    @Test
    fun multiSelectionHeader_displaysSelectedCountWithoutTotal() {
        val uiState =
            StudentsScreenState(
                isMultiSelectionMode = true,
                selectedStudentIds = setOf(1, 2, 3)
            )

        composeTestRule.setContent {
            AbsenceRecordTheme {
                MultiSelectionHeader(
                    selectedStudentIds = uiState.selectedStudentIds,
                    onCloseSelectionMode = {},
                    onToggleSelection = {},
                    onExport = {},
                    onExportAndDelete = {},
                    onShowDeleteDialog = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText(getString(R.string.selection_count_selected, 3))
            .assertIsDisplayed()
    }

    @Test
    fun multiSelectionHeader_clickingCloseInvokesCallback() {
        val uiState = StudentsScreenState(isMultiSelectionMode = true, selectedStudentIds = setOf(1))
        var closeClicked = false

        composeTestRule.setContent {
            AbsenceRecordTheme {
                MultiSelectionHeader(
                    selectedStudentIds = uiState.selectedStudentIds,
                    onCloseSelectionMode = { closeClicked = true },
                    onToggleSelection = {},
                    onExport = {},
                    onExportAndDelete = {},
                    onShowDeleteDialog = {}
                )
            }
        }

        composeTestRule.onNodeWithContentDescription(getString(R.string.action_close)).performClick()

        assert(closeClicked)
    }

    @Test
    fun multiSelectionHeader_showsOverflowMenuWithSelection() {
        val uiState = StudentsScreenState(isMultiSelectionMode = true, selectedStudentIds = setOf(1))

        composeTestRule.setContent {
            AbsenceRecordTheme {
                MultiSelectionHeader(
                    selectedStudentIds = uiState.selectedStudentIds,
                    totalCount = 5,
                    onCloseSelectionMode = {},
                    onToggleSelection = {},
                    onExport = {},
                    onExportAndDelete = {},
                    onShowDeleteDialog = {}
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription(getString(R.string.more_options_description))
            .assertIsDisplayed()
    }
}