package dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule.dialogs.HOUR_TIME_PICKER_MODE_TOGGLE_TAG
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScheduleScreenAppointmentsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val harness = ScheduleScreenTestHarness(composeTestRule)

    @Test
    fun addHour_viaTimePickerAndMaxField_saves() {
        val stateFlow = MutableStateFlow(ScheduleScreenState(isHoursLoading = false))
        harness.setContent(stateFlow)

        composeTestRule
            .onNodeWithText(harness.getString(R.string.schedule_add_hour))
            .performClick()
        composeTestRule
            .onNodeWithText(harness.getString(R.string.schedule_add_hour_title))
            .assertIsDisplayed()

        composeTestRule
            .onNode(hasSetTextAction() and !hasText(harness.time(480)))
            .performTextClearance()
        composeTestRule
            .onNode(hasSetTextAction() and !hasText(harness.time(480)))
            .performTextInput("5")
        composeTestRule.onNodeWithText("5").assertIsDisplayed()

        composeTestRule.onNodeWithText(harness.time(480)).performClick()
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithTag(HOUR_TIME_PICKER_MODE_TOGGLE_TAG)
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithContentDescription(
                harness.getString(androidx.compose.material3.R.string.m3c_time_picker_hour_text_field)
            )
            .performTextReplacement("9")
        composeTestRule.waitForIdle()
        val minuteSelector =
            harness.getString(androidx.compose.material3.R.string.m3c_time_picker_minute_selection)
        if (
            composeTestRule
                .onAllNodesWithContentDescription(minuteSelector)
                .fetchSemanticsNodes()
                .isNotEmpty()
        ) {
            composeTestRule.onNodeWithContentDescription(minuteSelector).performClick()
            composeTestRule.waitForIdle()
        }
        composeTestRule
            .onNodeWithContentDescription(
                harness.getString(androidx.compose.material3.R.string.m3c_time_picker_minute_text_field)
            )
            .performTextReplacement("07")
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText(harness.getString(R.string.action_set))
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(harness.time(547)).assertIsDisplayed()

        composeTestRule.onNodeWithText(harness.getString(R.string.action_save)).performClick()

        assertTrue(harness.containsEvent(ScheduleScreenEvent.SaveHour))
    }

    @Test
    fun addHour_passesMidnight_blocksSave() {
        val stateFlow =
            MutableStateFlow(
                ScheduleScreenState(
                    isHourDialogOpen = true,
                    hourStartMinutes = 1410
                )
            )
        harness.setContent(stateFlow)

        composeTestRule
            .onNodeWithText(harness.getString(R.string.error_hour_passes_midnight))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(harness.getString(R.string.action_save))
            .assertIsNotEnabled()
    }

    @Test
    fun overlappingSave_showsInlineOverlapError() {
        harness.setContent(MutableStateFlow(ScheduleScreenState(isHoursLoading = false)))

        composeTestRule
            .onNodeWithText(harness.getString(R.string.schedule_add_hour))
            .performClick()
        composeTestRule
            .onNode(hasSetTextAction() and !hasText(harness.time(480)))
            .performTextClearance()
        composeTestRule
            .onNode(hasSetTextAction() and !hasText(harness.time(480)))
            .performTextInput("5")
        composeTestRule.onNodeWithText(harness.getString(R.string.action_save)).performClick()

        composeTestRule
            .onNodeWithText(harness.getString(R.string.error_hour_overlap))
            .assertIsDisplayed()
    }

    @Test
    fun deleteHour_opensConfirmDialog_andConfirms() {
        val stateFlow =
            MutableStateFlow(
                ScheduleScreenState(
                    hoursForWeekday = listOf(harness.hourWith(id = 1, start = 540)),
                    isHoursLoading = false
                )
            )
        harness.setContent(stateFlow)

        composeTestRule
            .onNodeWithContentDescription(
                harness.getString(R.string.schedule_hour_options_description, harness.time(540))
            )
            .performClick()
        composeTestRule
            .onNodeWithContentDescription(
                harness.getString(R.string.schedule_delete_hour_description, harness.time(540))
            )
            .performClick()

        composeTestRule
            .onNodeWithText(harness.getString(R.string.schedule_delete_hour_named, harness.time(540)))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(harness.getString(R.string.schedule_delete_hour_message_empty))
            .assertIsDisplayed()

        val expectedHour = stateFlow.value.hourToDelete
        assertNotNull("Expected hourToDelete to be set after opening the confirm dialog", expectedHour)
        composeTestRule.onNodeWithText(harness.getString(R.string.action_delete)).performClick()

        assertTrue(harness.containsEvent(ScheduleScreenEvent.ConfirmDeleteHour(expectedHour!!)))
    }

    @Test
    fun deleteHour_withAssignedStudents_enumeratesBlastRadius() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val stateFlow =
            MutableStateFlow(
                ScheduleScreenState(
                    allStudents = listOf(Student(1, "Ali"), Student(2, "Sara")),
                    hoursForWeekday =
                        listOf(harness.hourWith(id = 1, start = 540, max = 5, assigned = 2)),
                    isHoursLoading = false
                )
            )
        harness.setContent(stateFlow)

        composeTestRule
            .onNodeWithContentDescription(
                harness.getString(R.string.schedule_hour_options_description, harness.time(540))
            )
            .performClick()
        composeTestRule
            .onNodeWithContentDescription(
                harness.getString(R.string.schedule_delete_hour_description, harness.time(540))
            )
            .performClick()

        val expectedMessage =
            context.resources.getQuantityString(
                R.plurals.schedule_delete_hour_message_with_students,
                2,
                2,
                "Ali, Sara"
            )
        composeTestRule.onNodeWithText(expectedMessage).assertIsDisplayed()
    }
}