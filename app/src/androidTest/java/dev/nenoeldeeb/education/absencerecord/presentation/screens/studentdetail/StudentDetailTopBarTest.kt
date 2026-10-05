package dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.presentation.theme.AbsenceRecordTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StudentDetailTopBarTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun getString(resId: Int): String = context.getString(resId)

    private fun setTopBar(
        studentFirstName: String? = "Ahmed",
        hasStudent: Boolean = true,
        onEditName: () -> Unit = {},
        onChangeClass: () -> Unit = {},
        onDelete: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            AbsenceRecordTheme {
                StudentDetailTopBar(
                    studentFirstName = studentFirstName,
                    hasStudent = hasStudent,
                    onBack = {},
                    onEditName = onEditName,
                    onChangeClass = onChangeClass,
                    onDelete = onDelete
                )
            }
        }
    }

    @Test
    fun topBar_showsFirstName_andThreeDirectActions() {
        var editCalls = 0
        setTopBar(studentFirstName = "Ahmed", onEditName = { editCalls++ })
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Ahmed").assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription(getString(R.string.student_detail_back_description))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription(getString(R.string.student_detail_edit_name_title))
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()
        assert(editCalls == 1)

        composeTestRule
            .onNodeWithContentDescription(
                getString(R.string.student_detail_change_class_icon_description)
            )
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription(
                getString(R.string.student_detail_delete_icon_description)
            )
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription(getString(R.string.more_options_description))
            .assertDoesNotExist()
    }

    @Test
    fun topBar_directActions_invokeChangeAndDelete() {
        var changeCalls = 0
        var deleteCalls = 0
        setTopBar(
            onChangeClass = { changeCalls++ },
            onDelete = { deleteCalls++ }
        )
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithContentDescription(
                getString(R.string.student_detail_change_class_icon_description)
            )
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()
        assert(changeCalls == 1)

        composeTestRule
            .onNodeWithContentDescription(
                getString(R.string.student_detail_delete_icon_description)
            )
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()
        assert(deleteCalls == 1)
    }

    @Test
    fun topBar_withoutStudent_hidesActions_showsPlaceholder() {
        setTopBar(studentFirstName = null, hasStudent = false)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(getString(R.string.student_detail_title_placeholder)).assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription(getString(R.string.student_detail_edit_name_title))
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithContentDescription(
                getString(R.string.student_detail_change_class_icon_description)
            )
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithContentDescription(
                getString(R.string.student_detail_delete_icon_description)
            )
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithContentDescription(getString(R.string.more_options_description))
            .assertDoesNotExist()
    }
}