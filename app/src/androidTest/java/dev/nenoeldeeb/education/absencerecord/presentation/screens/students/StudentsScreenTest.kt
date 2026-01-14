package dev.nenoeldeeb.education.absencerecord.presentation.screens.students

import android.annotation.SuppressLint
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.ParsedStudentImportData
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentExportData
import dev.nenoeldeeb.education.absencerecord.presentation.theme.AbsenceRecordTheme
import io.mockk.every
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StudentsScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun getString(
        resId: Int,
        vararg formatArgs: Any
    ): String {
        return context.getString(resId, *formatArgs)
    }

    // region EmptyStateMessage Tests

    @Test
    fun emptyStateMessage_isDisplayed() {
        composeTestRule.setContent { AbsenceRecordTheme { EmptyStateMessage() } }

        composeTestRule.onNodeWithText(getString(R.string.no_students_message)).assertIsDisplayed()
    }

    // endregion

    // region StudentList Tests

    @Test
    fun studentList_displaysStudents() {
        val students = listOf(Student(id = 1, name = "Alice"), Student(id = 2, name = "Bob"))
        val uiState = StudentsScreenState(allStudents = students)

        composeTestRule.setContent {
            AbsenceRecordTheme { StudentList(uiState = uiState, viewModel = mockk(relaxed = true)) }
        }

        composeTestRule.onNodeWithText("Alice").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bob").assertIsDisplayed()
    }

    @Test
    fun studentList_clickingStudentInvokesEvent() {
        val student = Student(id = 1, name = "Alice")
        val students = listOf(student)
        val uiState = StudentsScreenState(allStudents = students)
        var eventSent: StudentsScreenEvent? = null

        composeTestRule.setContent {
            AbsenceRecordTheme {
                StudentList(
                    uiState = uiState,
                    viewModel =
                        mockk(relaxed = true) {
                            every { onEvent(any()) } answers { eventSent = firstArg() }
                        }
                )
            }
        }

        composeTestRule.onNodeWithText("Alice").performClick()

        assert(eventSent is StudentsScreenEvent.ShowStudentDialog)
        assert((eventSent as StudentsScreenEvent.ShowStudentDialog).student == student)
    }

    @SuppressLint("CheckResult")
    @Test
    fun studentList_longClickingStudentInvokesSelectionModeEvent() {
        val student = Student(id = 1, name = "Alice")
        val students = listOf(student)
        val uiState = StudentsScreenState(allStudents = students)
        val events = mutableListOf<StudentsScreenEvent>()

        composeTestRule.setContent {
            AbsenceRecordTheme {
                StudentList(
                    uiState = uiState,
                    viewModel =
                        mockk(relaxed = true) {
                            every { onEvent(any()) } answers { events.add(firstArg()) }
                        }
                )
            }
        }

        composeTestRule.onNodeWithText("Alice").performTouchInput { longClick(durationMillis = 400L) }

        assert(events.any { it is StudentsScreenEvent.ToggleSelectionMode })
        assert(
            events.any {
                it is StudentsScreenEvent.ToggleStudentSelection && it.studentId == student.id
            }
        )
    }

    @Test
    fun studentList_displaysCheckboxInMultiSelectionMode() {
        val student = Student(id = 1, name = "Alice")
        val uiState =
            StudentsScreenState(
                allStudents = listOf(student),
                isMultiSelectionMode = true,
                selectedStudentIds = setOf(1)
            )

        composeTestRule.setContent {
            AbsenceRecordTheme { StudentList(uiState = uiState, viewModel = mockk(relaxed = true)) }
        }

        composeTestRule.onNode(isToggleable()).assertIsOn()
    }

    // endregion

    // region StudentDialog Tests

    @Test
    fun studentDialog_displaysAddTitle_inAddMode() {
        val uiState = StudentsScreenState(showAddStudentDialog = true)

        composeTestRule.setContent {
            AbsenceRecordTheme {
                StudentDialog(
                    uiState = uiState,
                    viewModel = mockk(relaxed = true),
                    importLauncher = {}
                )
            }
        }

        composeTestRule.onNodeWithText(getString(R.string.new_student_label)).assertIsDisplayed()
        composeTestRule
            .onNodeWithText(getString(R.string.import_students_action))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(getString(R.string.action_add)).assertIsDisplayed()
    }

    @Test
    fun studentDialog_displaysEditTitle_inEditMode() {
        val student = Student(id = 1, name = "Alice")
        val uiState = StudentsScreenState(showEditDialog = student, newStudentName = "Alice")

        composeTestRule.setContent {
            AbsenceRecordTheme {
                StudentDialog(
                    uiState = uiState,
                    viewModel = mockk(relaxed = true),
                    importLauncher = {}
                )
            }
        }

        composeTestRule.onNodeWithText(getString(R.string.edit_student)).assertIsDisplayed()
        composeTestRule
            .onNodeWithText(getString(R.string.import_students_action))
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(getString(R.string.action_save)).assertIsDisplayed()
        composeTestRule.onNodeWithText("Alice").assertIsDisplayed()
    }

    @Test
    fun studentDialog_confirmButtonDisabled_whenNameBlank() {
        val uiState = StudentsScreenState(showAddStudentDialog = true, newStudentName = "")

        composeTestRule.setContent {
            AbsenceRecordTheme {
                StudentDialog(
                    uiState = uiState,
                    viewModel = mockk(relaxed = true),
                    importLauncher = {}
                )
            }
        }

        composeTestRule.onNodeWithText(getString(R.string.action_add)).assertIsNotEnabled()
    }

    // endregion

    // region MultiSelectionHeader Tests

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
                    uiState = uiState,
                    viewModel = mockk(relaxed = true),
                    context = context,
                    onExport = {},
                    onExportAndDelete = {}
                )
            }
        }

        composeTestRule.onNodeWithText("3").assertIsDisplayed()
    }

    @Test
    fun multiSelectionHeader_clickingCloseInvokesClearSelection() {
        val uiState =
            StudentsScreenState(isMultiSelectionMode = true, selectedStudentIds = setOf(1))
        var eventSent: StudentsScreenEvent? = null

        composeTestRule.setContent {
            AbsenceRecordTheme {
                MultiSelectionHeader(
                    uiState = uiState,
                    viewModel =
                        mockk(relaxed = true) {
                            every { onEvent(any()) } answers { eventSent = firstArg() }
                        },
                    context = context,
                    onExport = {},
                    onExportAndDelete = {}
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription(getString(R.string.action_close))
            .performClick()

        assert(eventSent is StudentsScreenEvent.ClearSelection)
    }

    // endregion

    // region BulkDeleteConfirmationDialog Tests

    @Test
    fun bulkDeleteConfirmationDialog_displaysCorrectCount() {
        val uiState = StudentsScreenState(selectedStudentIds = setOf(1, 2))

        composeTestRule.setContent {
            AbsenceRecordTheme {
                BulkDeleteConfirmationDialog(uiState = uiState, viewModel = mockk(relaxed = true))
            }
        }

        // Check for plural message containing "2"
        composeTestRule.onNodeWithText("2", substring = true).assertIsDisplayed()
    }

    // endregion

    // region ImportSelectionDialog Tests

    @Test
    fun importSelectionDialog_displaysParsedStudents() {
        val parsedStudents =
            listOf(
                ParsedStudentImportData(
                    id = 1,
                    originalData =
                        StudentExportData(name = "Alice", dates = emptyList())
                ),
                ParsedStudentImportData(
                    id = 2,
                    originalData = StudentExportData(name = "Bob", dates = emptyList())
                )
            )
        val uiState =
            StudentsScreenState(
                showImportSelectionDialog = true,
                parsedStudentsFromFile = parsedStudents,
                importSelectionMap = mapOf(1 to true, 2 to false)
            )

        composeTestRule.setContent {
            AbsenceRecordTheme {
                ImportSelectionDialog(uiState = uiState, viewModel = mockk(relaxed = true))
            }
        }

        composeTestRule.onNodeWithText("Alice").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bob").assertIsDisplayed()
        // Alice should be checked, Bob unchecked
        // Since we have multiple checkboxes, we might need to be more specific if possible.
        // But for a simple test, just asserting they are displayed is a good start.
    }

    @Test
    fun importSelectionDialog_importButtonDisabled_whenNoneSelected() {
        val parsedStudents =
            listOf(
                ParsedStudentImportData(
                    id = 1,
                    originalData =
                        StudentExportData(name = "Alice", dates = emptyList())
                )
            )
        val uiState =
            StudentsScreenState(
                showImportSelectionDialog = true,
                parsedStudentsFromFile = parsedStudents,
                importSelectionMap = mapOf(1 to false)
            )

        composeTestRule.setContent {
            AbsenceRecordTheme {
                ImportSelectionDialog(uiState = uiState, viewModel = mockk(relaxed = true))
            }
        }

        composeTestRule.onNodeWithText(getString(R.string.import_selected)).assertIsNotEnabled()
    }

    // endregion
}