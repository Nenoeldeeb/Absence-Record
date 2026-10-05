package dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyChild
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentClass
import dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.dialogs.ChangeClassDialog
import dev.nenoeldeeb.education.absencerecord.presentation.theme.AbsenceRecordTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChangeClassDialogTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val classes =
        listOf(
            StudentClass(id = 1, name = "Grade 1"),
            StudentClass(id = 2, name = "Grade 2")
        )

    private fun optionRow(label: String) =
        composeTestRule.onNode(
            hasClickAction().and(hasAnyChild(hasText(label, substring = false))),
            useUnmergedTree = true
        )

    private fun setDialog(
        currentClassId: Int? = null,
        onConfirm: (Int?) -> Unit = {},
        onDismiss: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            AbsenceRecordTheme {
                ChangeClassDialog(
                    availableClasses = classes,
                    currentClassId = currentClassId,
                    onConfirm = onConfirm,
                    onDismiss = onDismiss
                )
            }
        }
    }

    @Test
    fun dialog_optionsAreRadioButtons_reflectingCurrentSelection() {
        setDialog(currentClassId = 1)
        composeTestRule.waitForIdle()

        optionRow(context.getString(R.string.class_unassigned_label)).assertIsNotSelected()
        optionRow("Grade 1").assertIsSelected()
        optionRow("Grade 2").assertIsNotSelected()
    }

    @Test
    fun dialog_selectingOption_movesSelection() {
        setDialog(currentClassId = null)
        composeTestRule.waitForIdle()

        optionRow(context.getString(R.string.class_unassigned_label)).assertIsSelected()

        optionRow("Grade 2").performClick()
        composeTestRule.waitForIdle()

        optionRow("Grade 2").assertIsSelected()
        optionRow(context.getString(R.string.class_unassigned_label)).assertIsNotSelected()
    }

    @Test
    fun dialog_confirmDispatchesSelectedClassId() {
        var confirmed: Int? = -1
        setDialog(currentClassId = 1, onConfirm = { confirmed = it })
        composeTestRule.waitForIdle()

        optionRow("Grade 2").performClick()
        composeTestRule
            .onNodeWithText(context.getString(R.string.action_save))
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals(2, confirmed)
    }

    @Test
    fun dialog_confirmWithoutChangeDispatchesCurrentClassId() {
        var confirmed: Int? = -1
        setDialog(currentClassId = null, onConfirm = { confirmed = it })
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText(context.getString(R.string.action_save))
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals(null, confirmed)
    }

    @Test
    fun dialog_cancelInvokesDismiss() {
        var dismissed = false
        setDialog(onDismiss = { dismissed = true })
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText(context.getString(R.string.action_cancel))
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals(true, dismissed)
    }
}