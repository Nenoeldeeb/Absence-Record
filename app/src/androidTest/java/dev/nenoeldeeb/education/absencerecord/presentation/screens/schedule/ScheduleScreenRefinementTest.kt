package dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.nenoeldeeb.education.absencerecord.R
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScheduleScreenRefinementTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val harness = ScheduleScreenTestHarness(composeTestRule)

    @Test
    fun weekdayChips_useShortLabels() {
        harness.setContent(MutableStateFlow(ScheduleScreenState(isHoursLoading = false)))

        composeTestRule.onNodeWithText("Sat").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sun").assertIsDisplayed()
        composeTestRule.onNodeWithText("Saturday").assertDoesNotExist()
        composeTestRule.onNodeWithText("Sunday").assertDoesNotExist()
    }

    @Test
    fun expandedHour_showsInlineEditDelete_andHidesOverflow() {
        harness.setContent(
            MutableStateFlow(
                ScheduleScreenState(
                    hoursForWeekday = listOf(harness.hourWith(id = 1, start = 540)),
                    isHoursLoading = false
                )
            )
        )

        composeTestRule
            .onNodeWithContentDescription(
                harness.getString(R.string.schedule_expand_hour_description, harness.time(540))
            )
            .performClick()

        composeTestRule
            .onNodeWithContentDescription(
                harness.getString(R.string.schedule_edit_hour_description, harness.time(540))
            )
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription(
                harness.getString(R.string.schedule_delete_hour_description, harness.time(540))
            )
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription(
                harness.getString(R.string.schedule_hour_options_description, harness.time(540))
            )
            .assertDoesNotExist()
    }

    @Test
    fun expandedHour_inlineDelete_opensConfirmDialog() {
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
                harness.getString(R.string.schedule_expand_hour_description, harness.time(540))
            )
            .performClick()
        composeTestRule
            .onNodeWithContentDescription(
                harness.getString(R.string.schedule_delete_hour_description, harness.time(540))
            )
            .performClick()

        composeTestRule
            .onNodeWithText(
                harness.getString(R.string.schedule_delete_hour_named, harness.time(540))
            )
            .assertIsDisplayed()
        assertTrue(stateFlow.value.hourToDelete != null)
    }

    @Test
    fun emptyExpandedHour_showsReadOnlyEmptyState_withoutAssignAction() {
        harness.setContent(
            MutableStateFlow(
                ScheduleScreenState(
                    hoursForWeekday = listOf(harness.hourWith(id = 1, start = 540)),
                    isHoursLoading = false
                )
            )
        )

        composeTestRule
            .onNodeWithContentDescription(
                harness.getString(R.string.schedule_expand_hour_description, harness.time(540))
            )
            .performClick()
        composeTestRule
            .onNodeWithText(harness.getString(R.string.schedule_no_students_assigned))
            .assertIsDisplayed()
    }

    @Test
    fun capacityStepper_incrementsDecrementAndBounds() {
        val stateFlow = MutableStateFlow(ScheduleScreenState(isHoursLoading = false))
        harness.setContent(stateFlow)

        composeTestRule
            .onNodeWithText(harness.getString(R.string.schedule_add_hour))
            .performClick()

        composeTestRule.onNodeWithTag("hour_capacity_increase").performClick()
        assertTrue(
            harness.containsEvent(ScheduleScreenEvent.SetHourMaxStudents("6"))
        )
        assertEquals("6", stateFlow.value.hourMaxStudents)

        composeTestRule.onNodeWithTag("hour_capacity_decrease").performClick()
        assertEquals("5", stateFlow.value.hourMaxStudents)

        composeTestRule.onNode(hasSetTextAction()).performTextClearance()
        composeTestRule.onNode(hasSetTextAction()).performTextInput("1")
        composeTestRule.onNodeWithTag("hour_capacity_decrease").assertIsNotEnabled()
    }
}