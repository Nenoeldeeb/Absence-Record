package dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.services.ScheduleRules
import dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule.components.HOUR_LENGTH_SLIDER_TAG
import dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule.dialogs.HOUR_TIME_PICKER_MODE_TOGGLE_TAG
import dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule.dialogs.HourDialog
import dev.nenoeldeeb.education.absencerecord.presentation.theme.AbsenceRecordTheme
import dev.nenoeldeeb.education.absencerecord.presentation.utils.TimeFormatter
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HourDialogTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun time(minutes: Int): String = TimeFormatter.format(minutes, context)

    private fun setDialog(
        startMinutes: Int = 540,
        durationMinutes: Int = 50,
        validationError: UiText? = null,
        isEdit: Boolean = false,
        onStartSelected: (Int) -> Unit = {},
        onDurationChange: (Int) -> Unit = {},
        onSave: () -> Unit = {},
        onDismiss: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            AbsenceRecordTheme {
                var startMinutesState by remember { mutableStateOf(startMinutes) }
                HourDialog(
                    startMinutes = startMinutesState,
                    durationMinutes = durationMinutes,
                    validationError = validationError,
                    isEdit = isEdit,
                    onStartSelected = { minutes ->
                        startMinutesState = minutes
                        onStartSelected(minutes)
                    },
                    onDurationChange = onDurationChange,
                    onSave = onSave,
                    onDismiss = onDismiss
                )
            }
        }
    }

    @Test
    fun editHour_dialogPrefillsValues() {
        setDialog(isEdit = true)

        composeTestRule
            .onNodeWithText(context.getString(R.string.schedule_edit_hour_title))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(time(540)).assertIsDisplayed()
        composeTestRule.onNodeWithTag(HOUR_LENGTH_SLIDER_TAG).assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.schedule_derived_capacity, 5))
            .assertIsDisplayed()
    }

    @Test
    fun addHour_dialogUsesAddTitle() {
        setDialog(isEdit = false)

        composeTestRule
            .onNodeWithText(context.getString(R.string.schedule_add_hour_title))
            .assertIsDisplayed()
    }

    @Test
    fun hourDialog_tappingStartField_opensTimePicker() {
        setDialog()

        composeTestRule.onNodeWithText(time(540)).performClick()
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag(HOUR_TIME_PICKER_MODE_TOGGLE_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun hourDialog_pickingExactMinute_canSave() {
        var selectedStart: Int? = null
        var saved = false
        setDialog(
            onStartSelected = { selectedStart = it },
            onSave = { saved = true }
        )

        composeTestRule.onNodeWithText(time(540)).performClick()
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithTag(HOUR_TIME_PICKER_MODE_TOGGLE_TAG)
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithContentDescription(
                context.getString(androidx.compose.material3.R.string.m3c_time_picker_hour_text_field)
            )
            .performTextReplacement("9")
        composeTestRule.waitForIdle()
        val minuteSelector =
            context.getString(androidx.compose.material3.R.string.m3c_time_picker_minute_selection)
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
                context.getString(androidx.compose.material3.R.string.m3c_time_picker_minute_text_field)
            )
            .performTextReplacement("07")
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText(context.getString(R.string.action_set))
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals(547, requireNotNull(selectedStart))
        composeTestRule.onNodeWithText(time(547)).assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.schedule_hour_ends_at, time(597)))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.action_save)).assertIsEnabled()
        composeTestRule.onNodeWithText(context.getString(R.string.action_save)).performClick()
        assertEquals(true, saved)
    }

    @Test
    fun hourDialog_showsEndsAtPreview() {
        setDialog(startMinutes = 540, durationMinutes = 50)

        composeTestRule
            .onNodeWithText(context.getString(R.string.schedule_hour_ends_at, time(590)))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.action_save)).assertIsEnabled()
    }

    @Test
    fun hourDialog_exactMidnightEnd_isAllowed() {
        setDialog(startMinutes = 1380, durationMinutes = 60)

        composeTestRule
            .onNodeWithText(context.getString(R.string.schedule_hour_ends_at, time(1440)))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.action_save)).assertIsEnabled()
    }

    @Test
    fun hourDialog_passesMidnight_showsErrorAndDisablesSave() {
        setDialog(startMinutes = 1410, durationMinutes = 50)

        composeTestRule
            .onNodeWithText(context.getString(R.string.error_hour_passes_midnight))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.schedule_hour_ends_at, time(1460)))
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText(context.getString(R.string.action_save))
            .assertIsNotEnabled()
    }

    @Test
    fun hourDialog_showsInlineValidationError() {
        val errorText = context.getString(R.string.error_hour_overlap)
        setDialog(validationError = UiText.StringResource(R.string.error_hour_overlap))

        composeTestRule.onNodeWithText(errorText).assertIsDisplayed()
    }

    @Test
    fun hourDialog_showsTheLengthSlider() {
        setDialog()
        composeTestRule.onNodeWithTag(HOUR_LENGTH_SLIDER_TAG).assertIsDisplayed()
    }

    @Test
    fun hourDialog_spokenLabelCarriesMinutesAndDerivedCount() {
        setDialog(durationMinutes = 30)
        val expected = context.getString(R.string.schedule_slider_description, 30, 3)
        composeTestRule
            .onNodeWithTag(HOUR_LENGTH_SLIDER_TAG)
            .assert(hasContentDescription(expected))
    }

    @Test
    fun hourDialog_draggingTheSlider_emitsASteppedLength() {
        var emitted: Int? = null
        setDialog(onDurationChange = { emitted = it })

        composeTestRule.onNodeWithTag(HOUR_LENGTH_SLIDER_TAG).performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()

        val value = emitted
        assertNotNull("expected the slider to emit a length", value)
        // never an off-step value (FR-049, FR-052)
        assertEquals(0, value!! % ScheduleRules.HOUR_DURATION_STEP_MINUTES)
        assertTrue(value in ScheduleRules.MIN_HOUR_DURATION_MINUTES..ScheduleRules.MAX_HOUR_DURATION_MINUTES)
    }

    @Test
    fun hourDialog_derivedCapacityFollowsTheLength() {
        setDialog(durationMinutes = 120)
        composeTestRule
            .onNodeWithText(context.getString(R.string.schedule_derived_capacity, 12))
            .assertIsDisplayed()
    }
}