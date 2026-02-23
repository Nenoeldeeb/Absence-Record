package dev.nenoeldeeb.education.absencerecord.presentation.screens.components

import android.annotation.SuppressLint
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenState
import dev.nenoeldeeb.education.absencerecord.presentation.theme.AbsenceRecordTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StudentsListTest {

    @get:Rule
    val composeTestRule = createComposeRule()


    @Test
    fun studentList_clickingStudentInvokesCallback() {
        var clickedStudent: Student? = null
        val student = Student(id = 1, name = "Alice")

        composeTestRule.setContent {
            AbsenceRecordTheme {
                StudentList(allStudents = listOf(student), onStudentClick = { clickedStudent = it }, onStudentLongClick = {})
            }
        }

        composeTestRule.onNodeWithText("Alice").performClick()

        assert(clickedStudent == student)
    }

    @Test
    fun studentList_displaysStudents() {
        val students = listOf(Student(id = 1, name = "Alice"), Student(id = 2, name = "Bob"))
        val uiState = StudentsScreenState(allStudents = students)

        composeTestRule.setContent {
            AbsenceRecordTheme {
                StudentList(
                    allStudents = uiState.allStudents,
                    selectedStudentIds = uiState.selectedStudentIds,
                    onStudentClick = {},
                    onStudentLongClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Alice").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bob").assertIsDisplayed()
    }


    @SuppressLint("CheckResult")
    @Test
    fun studentList_longClickingStudentInvokesCallback() {
        val student = Student(id = 1, name = "Alice")
        val students = listOf(student)
        val uiState = StudentsScreenState(allStudents = students)
        var longClickedId: Int? = null

        composeTestRule.setContent {
            AbsenceRecordTheme {
                StudentList(
                    allStudents = uiState.allStudents,
                    selectedStudentIds = uiState.selectedStudentIds,
                    onStudentClick = {},
                    onStudentLongClick = { longClickedId = it }
                )
            }
        }

        composeTestRule.onNodeWithText("Alice").performTouchInput { longClick(durationMillis = 400L) }

        assert(longClickedId == student.id)
    }
}