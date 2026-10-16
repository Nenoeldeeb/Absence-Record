package dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.AvailableLessonHour
import dev.nenoeldeeb.education.absencerecord.domain.models.HourWithOccupancy
import dev.nenoeldeeb.education.absencerecord.domain.services.ScheduleRules
import dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule.components.HOUR_LENGTH_SLIDER_TAG
import dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule.components.HOUR_ROW_TAG_PREFIX
import dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule.components.LengthSlider
import dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule.components.SCHEDULE_HOURS_LIST_TAG
import dev.nenoeldeeb.education.absencerecord.presentation.theme.AbsenceRecordTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Accessibility of the length control and the day list (FR-051, FR-052, FR-054, SC-017).
 *
 * These assert the properties a screen-reader or switch-access user actually
 * depends on — a spoken label carrying both numbers, an operable control, and
 * targets big enough to hit — rather than re-reading the composable and agreeing
 * with it.
 */
@RunWith(AndroidJUnit4::class)
class LengthSliderAccessibilityTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    /** A controlled host, so a change can be driven the way a switch would. */
    @Composable
    private fun SliderHost(
        initial: Int,
        onChange: (Int) -> Unit
    ) {
        var duration by remember { mutableStateOf(initial) }
        LengthSlider(
            durationMinutes = duration,
            onDurationChange = { duration = it }
        )
    }

    private fun spokenLabel(): String =
        composeTestRule
            .onNodeWithTag(HOUR_LENGTH_SLIDER_TAG)
            .fetchSemanticsNode()
            .config[SemanticsProperties.ContentDescription]
            ?.joinToString("") { it }
            ?: ""

    /**
     * Sends an absolute target, the way assistive tech drives a slider.
     *
     * There is no separate increase action: TalkBack derives increment and
     * decrement from `SetProgress` plus the `ProgressBarRangeInfo` the control
     * publishes, computes the next value itself, and sends it as an absolute
     * target. So a test does the same.
     */
    private fun sendProgress(target: Float) {
        composeTestRule
            .onNodeWithTag(HOUR_LENGTH_SLIDER_TAG)
            .performSemanticsAction(SemanticsActions.SetProgress) { it(target) }
    }

    private fun rangeInfo() =
        composeTestRule
            .onNodeWithTag(HOUR_LENGTH_SLIDER_TAG)
            .fetchSemanticsNode()
            .config[SemanticsProperties.ProgressBarRangeInfo]

    // region the spoken label

    @Test
    fun theSpokenLabelCarriesBothTheMinutesAndTheStudentCount() {
        composeTestRule.setContent {
            AbsenceRecordTheme { SliderHost(initial = 50) {} }
        }

        val label = spokenLabel()
        assertTrue("label was '$label'", label.contains("50"))
        assertTrue(
            "the derived student count must be spoken too, was '$label'",
            label.contains(ScheduleRules.capacityFor(50).toString())
        )
    }

    @Test
    fun theSpokenLabelFollowsTheSelectedLength() {
        composeTestRule.setContent {
            AbsenceRecordTheme { SliderHost(initial = 20) {} }
        }
        val before = spokenLabel()

        sendProgress(30f) // the value one 10-minute step up would produce

        val after = spokenLabel()
        assertTrue("label did not change from '$before'", before != after)
        assertTrue("expected 30 minutes in '$after'", after.contains("30"))
        assertTrue("expected 3 students in '$after'", after.contains("3"))
    }

    @Test
    fun theSpokenLabelIsNotEmptyAtTheShortestLength() {
        composeTestRule.setContent {
            AbsenceRecordTheme {
                SliderHost(initial = ScheduleRules.MIN_HOUR_DURATION_MINUTES) {}
            }
        }

        val label = spokenLabel()
        assertTrue("label empty at the shortest length", label.isNotBlank())
        assertTrue(
            "capacity missing at the shortest length",
            label.contains(ScheduleRules.capacityFor(ScheduleRules.MIN_HOUR_DURATION_MINUTES).toString())
        )
    }

    @Test
    fun theSpokenLabelIsNotEmptyAtTheLongestLength() {
        composeTestRule.setContent {
            AbsenceRecordTheme {
                SliderHost(initial = ScheduleRules.MAX_HOUR_DURATION_MINUTES) {}
            }
        }

        val label = spokenLabel()
        assertTrue("label empty at the longest length", label.isNotBlank())
        assertTrue(
            "capacity missing at the longest length",
            label.contains(ScheduleRules.capacityFor(ScheduleRules.MAX_HOUR_DURATION_MINUTES).toString())
        )
    }

    // endregion

    // region operability without a pointer

    @Test
    fun theControlExposesAnAdjustActionForSwitchAccess() {
        composeTestRule.setContent {
            AbsenceRecordTheme { SliderHost(initial = 50) {} }
        }

        assertNotNull(
            "a screen-reader user could not operate the slider",
            composeTestRule
                .onNodeWithTag(HOUR_LENGTH_SLIDER_TAG)
                .fetchSemanticsNode()
                .config[SemanticsActions.SetProgress]
        )
    }

    @Test
    fun theControlPublishesTheRangeAssistiveTechNeedsToStepIt() {
        composeTestRule.setContent {
            AbsenceRecordTheme { SliderHost(initial = 50) {} }
        }

        // Without this, "increase" has nothing to compute from.
        val info = rangeInfo()
        assertNotNull("no range published, so increments cannot be computed", info)
        assertEquals(50f, info!!.current, 0.01f)
        assertEquals(ScheduleRules.MIN_HOUR_DURATION_MINUTES.toFloat(), info.range.start, 0.01f)
        assertEquals(ScheduleRules.MAX_HOUR_DURATION_MINUTES.toFloat(), info.range.endInclusive, 0.01f)
        // `steps` counts INTERIOR stops, so ten of them give twelve positions —
        // exactly the twelve selectable lengths, not a free-running dial.
        assertEquals(12, info.steps + 2)
    }

    @Test
    fun adjustingNeverLandsOnANonSelectableLength() {
        val changes = mutableListOf<Int>()
        composeTestRule.setContent {
            AbsenceRecordTheme {
                var duration by remember { mutableStateOf(50) }
                LengthSlider(
                    durationMinutes = duration,
                    onDurationChange = {
                        duration = it
                        changes += it
                    }
                )
            }
        }

        // Targets that are not legal lengths must still land on one.
        listOf(33f, 7f, 119f, 250f).forEach { sendProgress(it) }

        assertTrue("slider never reported a change", changes.isNotEmpty())
        changes.forEach {
            assertTrue("$it is not a selectable length", ScheduleRules.isHourDurationValid(it))
        }
        changes.forEach {
            assertTrue("$it is not a selectable length", ScheduleRules.isHourDurationValid(it))
        }
    }

    @Test
    fun theControlIsEnabled() {
        composeTestRule.setContent {
            AbsenceRecordTheme { SliderHost(initial = 50) {} }
        }

        composeTestRule.onNodeWithTag(HOUR_LENGTH_SLIDER_TAG).assertIsEnabled()
    }

    // endregion

    // region touch targets and reachable content

    @Composable
    private fun DayScreen(hours: List<HourWithOccupancy>) {
        ScheduleScreenContent(
            uiState = ScheduleScreenState(hoursForWeekday = hours, isHoursLoading = false),
            onEvent = {},
            onStudentClick = {}
        )
    }

    private fun oneBlock() =
        HourWithOccupancy(
            hour = AvailableLessonHour(1, kotlinx.datetime.DayOfWeek.SATURDAY, 540, 50),
            endMinutes = 590,
            assignedStudentIds = emptyList(),
            assignedCount = 0
        )

    @Test
    fun aBlockRowMeetsTheMinimumTouchTarget() {
        composeTestRule.setContent { AbsenceRecordTheme { DayScreen(listOf(oneBlock())) } }

        // 48dp, converted to pixels at this display's density.
        composeTestRule
            .onNodeWithTag(HOUR_ROW_TAG_PREFIX + "1", useUnmergedTree = true)
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun theDayListIsAddressableSoOffScreenBlocksCanBeReached() {
        composeTestRule.setContent { AbsenceRecordTheme { DayScreen(listOf(oneBlock())) } }

        composeTestRule.onNodeWithTag(SCHEDULE_HOURS_LIST_TAG).assertIsDisplayed()
    }

    @Test
    fun theAddActionIsReachableByLabel() {
        composeTestRule.setContent {
            AbsenceRecordTheme { DayScreen(emptyList()) }
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.schedule_add_hour))
            .performClick()
    }

    // endregion
}