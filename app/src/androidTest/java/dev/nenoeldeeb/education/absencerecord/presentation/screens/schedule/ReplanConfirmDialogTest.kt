package dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.DropReason
import dev.nenoeldeeb.education.absencerecord.domain.models.DroppedStudent
import dev.nenoeldeeb.education.absencerecord.domain.models.HourReplanResult
import dev.nenoeldeeb.education.absencerecord.domain.models.PlannedHour
import dev.nenoeldeeb.education.absencerecord.domain.models.PlannedSlot
import dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule.dialogs.REPLAN_CANCEL_BUTTON_TAG
import dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule.dialogs.REPLAN_CONFIRM_BUTTON_TAG
import dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule.dialogs.REPLAN_CONFIRM_DIALOG_TAG
import dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule.dialogs.ReplanConfirmDialog
import dev.nenoeldeeb.education.absencerecord.presentation.theme.AbsenceRecordTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The review step before a block edit is applied (FR-011, FR-012, FR-045).
 */
@RunWith(AndroidJUnit4::class)
class ReplanConfirmDialogTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun proposal(
        dropped: List<DroppedStudent>,
        kept: Int = 1
    ) = HourReplanResult.NeedsConfirmation(
        proposed =
            PlannedHour(
                blockStartMinutes = 540,
                blockDurationMinutes = 60,
                maxStudents = 6,
                slots =
                    (1..kept).map {
                        PlannedSlot(it, "Student $it", it, 540 + (it - 1) * 10, 550 + (it - 1) * 10)
                    }
            ),
        dropped = dropped
    )

    private fun show(
        pending: HourReplanResult.NeedsConfirmation,
        onConfirm: () -> Unit = {},
        onDismiss: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            AbsenceRecordTheme {
                ReplanConfirmDialog(pending = pending, onConfirm = onConfirm, onDismiss = onDismiss)
            }
        }
    }

    @Test
    fun showsHowManyStudentsWouldBeDropped() {
        show(
            proposal(
                listOf(
                    DroppedStudent(7, "Zoe", DropReason.ExceedsCapacity),
                    DroppedStudent(8, "Max", DropReason.ExceedsCapacity)
                )
            )
        )
        composeTestRule
            .onNodeWithText(context.getString(R.string.schedule_replan_leave, 2))
            .assertIsDisplayed()
    }

    @Test
    fun saysSoPlainlyWhenEveryStudentWouldBeRemoved() {
        val dropped = (1..3).map { DroppedStudent(it, "S$it", DropReason.ExceedsCapacity) }
        show(proposal(dropped, kept = 0))
        composeTestRule
            .onNodeWithText(context.getString(R.string.schedule_replan_drop_all, 3))
            .assertIsDisplayed()
    }

    @Test
    fun confirmIsReachableAndReportsTheIntent() {
        var confirmed = false
        show(
            proposal(listOf(DroppedStudent(7, "Zoe", DropReason.ExceedsCapacity))),
            onConfirm = { confirmed = true }
        )
        composeTestRule.onNodeWithTag(REPLAN_CONFIRM_BUTTON_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(REPLAN_CONFIRM_BUTTON_TAG).performClick()
        assertTrue(confirmed)
    }

    @Test
    fun cancelIsReachableAndNeverAlsoConfirms() {
        var dismissed = false
        var confirmed = false
        show(
            proposal(listOf(DroppedStudent(7, "Zoe", DropReason.ExceedsCapacity))),
            onConfirm = { confirmed = true },
            onDismiss = { dismissed = true }
        )
        composeTestRule.onNodeWithTag(REPLAN_CANCEL_BUTTON_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(REPLAN_CANCEL_BUTTON_TAG).performClick()
        assertTrue(dismissed)
        // declining must not commit anything (FR-012)
        assertFalse(confirmed)
    }

    @Test
    fun theDialogIsTaggedForInstrumentation() {
        show(proposal(emptyList()))
        composeTestRule.onNodeWithTag(REPLAN_CONFIRM_DIALOG_TAG).assertIsDisplayed()
    }
}