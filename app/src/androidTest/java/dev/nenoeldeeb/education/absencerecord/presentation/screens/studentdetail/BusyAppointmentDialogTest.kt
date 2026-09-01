package dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentLessonEntry
import dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.dialogs.BUSY_DURATION_SLIDER_TAG
import dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.dialogs.BusyAppointmentDialog
import dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.dialogs.TIME_PICKER_CONFIRM_TAG
import dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.dialogs.TIME_PICKER_MODE_TOGGLE_TAG
import dev.nenoeldeeb.education.absencerecord.presentation.theme.AbsenceRecordTheme
import dev.nenoeldeeb.education.absencerecord.presentation.utils.TimeFormatter
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText
import kotlinx.datetime.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BusyAppointmentDialogTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun time(minutes: Int): String = TimeFormatter.format(minutes, context)

    private fun setDialog(
        startMinutes: Int = 540,
        durationMinutes: Int = 60,
        validationError: UiText? = null,
        isEdit: Boolean = false,
        conflictLessons: List<StudentLessonEntry> = emptyList(),
        onStartSelected: (Int) -> Unit = {},
        onDurationSelected: (Int) -> Unit = {},
        onSave: () -> Unit = {},
        onConfirmConflict: () -> Unit = {},
        onDismiss: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            AbsenceRecordTheme {
                BusyAppointmentDialog(
                    startMinutes = startMinutes,
                    durationMinutes = durationMinutes,
                    validationError = validationError,
                    isEdit = isEdit,
                    conflictLessons = conflictLessons,
                    onStartSelected = onStartSelected,
                    onDurationSelected = onDurationSelected,
                    onSave = onSave,
                    onConfirmConflict = onConfirmConflict,
                    onDismiss = onDismiss
                )
            }
        }
    }

    @Test
    fun busyDialog_addMode_usesAddTitle() {
        setDialog(isEdit = false)

        composeTestRule
            .onNodeWithText(context.getString(R.string.schedule_add_busy_title))
            .assertIsDisplayed()
    }

    @Test
    fun busyDialog_editMode_usesEditTitle() {
        setDialog(isEdit = true)

        composeTestRule
            .onNodeWithText(context.getString(R.string.schedule_edit_busy_title))
            .assertIsDisplayed()
    }

    @Test
    fun busyDialog_rendersStartDurationRangeAndEndsAt() {
        setDialog(startMinutes = 540, durationMinutes = 60)

        composeTestRule.onNodeWithText(time(540)).assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.schedule_busy_duration_hours, 1))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.schedule_busy_duration_range))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.schedule_busy_ends_at, time(600)))
            .assertIsDisplayed()
    }

    @Test
    fun busyDialog_invalidDuration_showsInlineErrorAndDisablesSave() {
        setDialog(startMinutes = 1420, durationMinutes = 60)

        composeTestRule
            .onNodeWithText(context.getString(R.string.error_busy_duration_invalid))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.action_save))
            .assertIsNotEnabled()
    }

    @Test
    fun busyDialog_sliderSelection_invokesDurationCallback() {
        val durations = mutableListOf<Int>()
        setDialog(durationMinutes = 30, onDurationSelected = { durations.add(it) })

        composeTestRule.onNodeWithTag(BUSY_DURATION_SLIDER_TAG).performTouchInput {
            swipe(
                start = center,
                end = Offset(width.toFloat() * 2f, center.y)
            )
        }
        composeTestRule.waitForIdle()

        assertEquals(listOf(360), durations)
        composeTestRule
            .onNodeWithText(context.getString(R.string.schedule_busy_duration_hours, 6))
            .assertIsDisplayed()
    }

    @Test
    fun busyDialog_timePicker_selectsExactMinuteStart() {
        val starts = mutableListOf<Int>()
        setDialog(startMinutes = 540, onStartSelected = { starts.add(it) })

        composeTestRule.onNodeWithText(time(540)).performClick()
        composeTestRule.onNodeWithTag(TIME_PICKER_MODE_TOGGLE_TAG).performClick()
        composeTestRule.onNodeWithText("00").performClick()
        composeTestRule.onNodeWithText("00").performTextReplacement("07")
        composeTestRule.onNodeWithTag(TIME_PICKER_CONFIRM_TAG).performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf(547), starts)
    }

    @Test
    fun busyDialog_validInput_saveInvokesCallback() {
        var saved = false
        setDialog(startMinutes = 540, durationMinutes = 60, onSave = { saved = true })

        composeTestRule
            .onNodeWithText(context.getString(R.string.action_save))
            .assertIsEnabled()
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals(true, saved)
    }

    @Test
    fun busyDialog_conflictPreview_validAppointment_confirmInvokesCallback() {
        val lesson = StudentLessonEntry(DayOfWeek.SATURDAY, 540, 600)
        var confirmed = false
        setDialog(conflictLessons = listOf(lesson), onConfirmConflict = { confirmed = true })

        composeTestRule
            .onNodeWithText(context.getString(R.string.student_detail_busy_conflict_title))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.student_detail_busy_conflict_message))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("${time(540)} – ${time(600)}").assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.student_detail_busy_conflict_confirm))
            .assertIsEnabled()
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals(true, confirmed)
    }

    @Test
    fun busyDialog_conflictPreview_midnightCrossing_disablesConfirm() {
        val lesson = StudentLessonEntry(DayOfWeek.SATURDAY, 1420, 1480)
        setDialog(startMinutes = 1420, durationMinutes = 60, conflictLessons = listOf(lesson))

        composeTestRule
            .onNodeWithText(context.getString(R.string.error_busy_duration_invalid))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.student_detail_busy_conflict_confirm))
            .assertIsNotEnabled()
    }

    @Test
    fun busyDialog_cancelInvokesDismiss() {
        var dismissed = false
        setDialog(onDismiss = { dismissed = true })

        composeTestRule.onNodeWithText(context.getString(R.string.action_cancel)).performClick()
        composeTestRule.waitForIdle()

        assertEquals(true, dismissed)
    }
}