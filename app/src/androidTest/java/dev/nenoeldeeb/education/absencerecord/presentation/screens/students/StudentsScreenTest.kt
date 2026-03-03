package dev.nenoeldeeb.education.absencerecord.presentation.screens.students

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.ParsedStudentImportData
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentExportData
import dev.nenoeldeeb.education.absencerecord.presentation.screens.components.EmptyStateMessage
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.components.MultiSelectionHeader
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.dialogs.BulkDeleteConfirmationDialog
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.dialogs.ImportSelectionDialog
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.dialogs.StudentDialog
import dev.nenoeldeeb.education.absencerecord.presentation.theme.AbsenceRecordTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StudentsScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun getString(
        resId: Int, vararg formatArgs: Any
    ): String {
        return context.getString(resId, *formatArgs)
    }

    // region EmptyStateMessage Tests

    @Test
    fun emptyStateMessage_isDisplayed() {
        composeTestRule.setContent { AbsenceRecordTheme { EmptyStateMessage(R.string.no_students_message) } }

        composeTestRule.onNodeWithText(getString(R.string.no_students_message)).assertIsDisplayed()
    }

    // endregion

    // region StudentDialog Tests

    @Test
    fun studentDialog_displaysAddTitle_inAddMode() {
        val uiState = StudentsScreenState(showAddStudentDialog = true)

        composeTestRule.setContent {
            AbsenceRecordTheme {
                StudentDialog(
                    showEditDialog = uiState.showEditDialog,
                    newStudentName = uiState.newStudentName,
                    availableClasses = emptyList(),
                    onStudentNameChange = {},
                    onSave = { _, _, _ -> },
                    onImportClick = {},
                    onDismiss = {})
            }
        }

        composeTestRule.onNodeWithText(getString(R.string.new_student_label)).assertIsDisplayed()
        composeTestRule.onNodeWithText(getString(R.string.import_students_action)).assertIsDisplayed()
        composeTestRule.onNodeWithText(getString(R.string.action_add)).assertIsDisplayed()
    }

    @Test
    fun studentDialog_displaysEditTitle_inEditMode() {
        val student = Student(id = 1, name = "Alice")
        val uiState = StudentsScreenState(showEditDialog = student, newStudentName = "Alice")

        composeTestRule.setContent {
            AbsenceRecordTheme {
                StudentDialog(
                    showEditDialog = uiState.showEditDialog,
                    newStudentName = uiState.newStudentName,
                    availableClasses = emptyList(),
                    onStudentNameChange = {},
                    onSave = { _, _, _ -> },
                    onImportClick = {},
                    onDismiss = {})
            }
        }

        composeTestRule.onNodeWithText(getString(R.string.edit_student)).assertIsDisplayed()
        composeTestRule.onNodeWithText(getString(R.string.import_students_action)).assertDoesNotExist()
        composeTestRule.onNodeWithText(getString(R.string.action_save)).assertIsDisplayed()
        composeTestRule.onNodeWithText("Alice").assertIsDisplayed()
    }

    @Test
    fun studentDialog_confirmButtonDisabled_whenNameBlank() {
        val uiState = StudentsScreenState(showAddStudentDialog = true, newStudentName = "")

        composeTestRule.setContent {
            AbsenceRecordTheme {
                StudentDialog(
                    showEditDialog = uiState.showEditDialog,
                    newStudentName = uiState.newStudentName,
                    availableClasses = emptyList(),
                    onStudentNameChange = {},
                    onSave = { _, _, _ -> },
                    onImportClick = {},
                    onDismiss = {})
            }
        }

        composeTestRule.onNodeWithText(getString(R.string.action_add)).assertIsNotEnabled()
    }

    // endregion

    // region MultiSelectionHeader Tests

    @Test
    fun multiSelectionHeader_displaysSelectedCount() {
        val uiState = StudentsScreenState(
            isMultiSelectionMode = true, selectedStudentIds = setOf(1, 2, 3)
        )

        composeTestRule.setContent {
            AbsenceRecordTheme {
                MultiSelectionHeader(
                    selectedStudentIds = uiState.selectedStudentIds,
                    onCloseSelectionMode = {},
                    onToggleSelection = {},
                    onExport = {},
                    onExportAndDelete = {},
                    onShowDeleteDialog = {})
            }
        }

        composeTestRule.onNodeWithText("3").assertIsDisplayed()
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
                    onShowDeleteDialog = {})
            }
        }

        composeTestRule.onNodeWithContentDescription(getString(R.string.action_close)).performClick()

        assert(closeClicked)
    }

    // endregion

    // region BulkDeleteConfirmationDialog Tests

    @Test
    fun bulkDeleteConfirmationDialog_displaysCorrectCount() {
        val uiState = StudentsScreenState(selectedStudentIds = setOf(1, 2))

        composeTestRule.setContent {
            AbsenceRecordTheme {
                BulkDeleteConfirmationDialog(
                    selectedStudentIds = uiState.selectedStudentIds,
                    onConfirmDelete = {},
                    onDismiss = {})
            }
        }

        // Check for plural message containing "2"
        composeTestRule.onNodeWithText("2", substring = true).assertIsDisplayed()
    }

    // endregion

    // region ImportSelectionDialog Tests

    @Test
    fun importSelectionDialog_displaysParsedStudents() {
        val parsedStudents = listOf(
            ParsedStudentImportData(
                id = 1, originalData = StudentExportData(name = "Alice", dates = emptyList())
            ), ParsedStudentImportData(
                id = 2, originalData = StudentExportData(name = "Bob", dates = emptyList())
            )
        )
        val uiState = StudentsScreenState(
            showImportSelectionDialog = true,
            parsedStudentsFromFile = parsedStudents,
            importSelectionMap = mapOf(1 to true, 2 to false)
        )

        composeTestRule.setContent {
            AbsenceRecordTheme {
                ImportSelectionDialog(
                    parsedStudentsFromFile = uiState.parsedStudentsFromFile,
                    importSelectionMap = uiState.importSelectionMap,
                    onToggleSelection = {},
                    onSelectAll = {},
                    onImport = {},
                    onDismiss = {})
            }
        }

        composeTestRule.onNodeWithText("Alice").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bob").assertIsDisplayed()
    }

    @Test
    fun importSelectionDialog_importButtonDisabled_whenNoneSelected() {
        val parsedStudents = listOf(
            ParsedStudentImportData(
                id = 1, originalData = StudentExportData(name = "Alice", dates = emptyList())
            )
        )
        val uiState = StudentsScreenState(
            showImportSelectionDialog = true, parsedStudentsFromFile = parsedStudents, importSelectionMap = mapOf(1 to false)
        )

        composeTestRule.setContent {
            AbsenceRecordTheme {
                ImportSelectionDialog(
                    parsedStudentsFromFile = uiState.parsedStudentsFromFile,
                    importSelectionMap = uiState.importSelectionMap,
                    onToggleSelection = {},
                    onSelectAll = {},
                    onImport = {},
                    onDismiss = {})
            }
        }

        composeTestRule.onNodeWithText(getString(R.string.import_selected)).assertIsNotEnabled()
    }

    // endregion
}