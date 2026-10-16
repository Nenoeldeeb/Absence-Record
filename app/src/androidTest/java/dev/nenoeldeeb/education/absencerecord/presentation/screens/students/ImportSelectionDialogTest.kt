package dev.nenoeldeeb.education.absencerecord.presentation.screens.students

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.ParsedImportData
import dev.nenoeldeeb.education.absencerecord.domain.models.ParsedStudentImportData
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentExportData
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.dialogs.ImportSelectionDialog
import dev.nenoeldeeb.education.absencerecord.presentation.theme.AbsenceRecordTheme
import org.junit.Test
import org.junit.runner.RunWith

/** The per-student picker shown after an import file is parsed (SC-018). */
@RunWith(AndroidJUnit4::class)
class ImportSelectionDialogTest : StudentsScreenTestBase() {
    @Test
    fun importSelectionDialog_displaysParsedStudents() {
        val parsedStudents =
            listOf(
                ParsedStudentImportData(
                    id = 1,
                    originalData = StudentExportData(name = "Alice", dates = emptyList())
                ),
                ParsedStudentImportData(
                    id = 2,
                    originalData = StudentExportData(name = "Bob", dates = emptyList())
                )
            )
        val parsedImportData =
            ParsedImportData(
                students = parsedStudents,
                availableHours = emptyList()
            )
        val uiState =
            StudentsScreenState(
                showImportSelectionDialog = true,
                parsedImportData = parsedImportData,
                importSelectionMap = mapOf(1 to true, 2 to false)
            )

        composeTestRule.setContent {
            AbsenceRecordTheme {
                ImportSelectionDialog(
                    parsedStudentsFromFile = uiState.parsedImportData?.students,
                    importSelectionMap = uiState.importSelectionMap,
                    onToggleSelection = {},
                    onSelectAll = {},
                    onImport = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Alice").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bob").assertIsDisplayed()
    }

    @Test
    fun importSelectionDialog_importButtonDisabled_whenNoneSelected() {
        val parsedStudents =
            listOf(
                ParsedStudentImportData(
                    id = 1,
                    originalData = StudentExportData(name = "Alice", dates = emptyList())
                )
            )
        val parsedImportData =
            ParsedImportData(
                students = parsedStudents,
                availableHours = emptyList()
            )
        val uiState =
            StudentsScreenState(
                showImportSelectionDialog = true,
                parsedImportData = parsedImportData,
                importSelectionMap = mapOf(1 to false)
            )

        composeTestRule.setContent {
            AbsenceRecordTheme {
                ImportSelectionDialog(
                    parsedStudentsFromFile = uiState.parsedImportData?.students,
                    importSelectionMap = uiState.importSelectionMap,
                    onToggleSelection = {},
                    onSelectAll = {},
                    onImport = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithText(getString(R.string.import_selected)).assertIsNotEnabled()
    }
}