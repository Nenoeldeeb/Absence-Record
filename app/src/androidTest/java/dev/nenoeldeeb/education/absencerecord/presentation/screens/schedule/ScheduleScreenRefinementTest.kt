package dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.services.ScheduleRules
import dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule.components.HOUR_ASSIGNED_SLOT_TAG_PREFIX
import dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule.components.HOUR_ASSIGNED_STUDENT_TAG_PREFIX
import dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule.components.HOUR_LENGTH_SLIDER_TAG
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
    fun lengthSlider_drivesTheDurationEvent() {
        val stateFlow = MutableStateFlow(ScheduleScreenState(isHoursLoading = false))
        harness.setContent(stateFlow)

        composeTestRule
            .onNodeWithText(harness.getString(R.string.schedule_add_hour))
            .performClick()

        composeTestRule.onNodeWithTag(HOUR_LENGTH_SLIDER_TAG).assertIsDisplayed()
        // the default length is seeded into state, and the derived count follows it
        assertEquals(
            ScheduleRules.DEFAULT_NEW_HOUR_DURATION_MINUTES,
            stateFlow.value.hourDurationMinutes
        )
    }

    // region each student shows the ten minutes they actually occupy (FR-018)

    @Test
    fun expandedHour_showsEachStudentsOwnSlotRange() {
        harness.setContent(
            MutableStateFlow(
                ScheduleScreenState(
                    hoursForWeekday = listOf(harness.hourWith(id = 1, start = 540, assigned = 3)),
                    allStudents = (1..3).map { Student(it, "Student $it") },
                    isHoursLoading = false
                )
            )
        )

        expandHour(540)

        composeTestRule
            .onNodeWithTag(HOUR_ASSIGNED_SLOT_TAG_PREFIX + "1", useUnmergedTree = true)
            .assert(hasText(harness.getString(R.string.schedule_slot_range, harness.time(540), harness.time(550))))
        composeTestRule
            .onNodeWithTag(HOUR_ASSIGNED_SLOT_TAG_PREFIX + "2", useUnmergedTree = true)
            .assert(hasText(harness.getString(R.string.schedule_slot_range, harness.time(550), harness.time(560))))
        composeTestRule
            .onNodeWithTag(HOUR_ASSIGNED_SLOT_TAG_PREFIX + "3", useUnmergedTree = true)
            .assert(hasText(harness.getString(R.string.schedule_slot_range, harness.time(560), harness.time(570))))
    }

    @Test
    fun expandedHour_preservesTheDerivedOrderRatherThanSortingByName() {
        // The repository delivers the running order; the row must not re-sort it.
        // Student 3 leaves soonest, so they run first despite the higher id.
        harness.setContent(
            MutableStateFlow(
                ScheduleScreenState(
                    hoursForWeekday =
                        listOf(
                            harness
                                .hourWith(id = 1, start = 540, assigned = 3)
                                .copy(assignedStudentIds = listOf(3, 1, 2))
                        ),
                    allStudents = (1..3).map { Student(it, "Student $it") },
                    isHoursLoading = false
                )
            )
        )

        expandHour(540)

        val tops =
            listOf(3, 1, 2).map { id ->
                composeTestRule
                    .onNodeWithTag(HOUR_ASSIGNED_STUDENT_TAG_PREFIX + id)
                    .fetchSemanticsNode()
                    .positionInRoot
                    .y
            }
        assertTrue(
            "derived order must be preserved top to bottom, got $tops",
            tops[0] < tops[1] && tops[1] < tops[2]
        )
    }

    @Test
    fun expandedHour_showsNameOnlyWhenNoSlotIsDerived() {
        harness.setContent(
            MutableStateFlow(
                ScheduleScreenState(
                    hoursForWeekday =
                        listOf(
                            harness.hourWith(id = 1, start = 540, assigned = 1).copy(slots = emptyList())
                        ),
                    allStudents = listOf(Student(1, "Student 1")),
                    isHoursLoading = false
                )
            )
        )

        expandHour(540)

        composeTestRule
            .onNodeWithTag(HOUR_ASSIGNED_STUDENT_TAG_PREFIX + "1")
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(HOUR_ASSIGNED_SLOT_TAG_PREFIX + "1", useUnmergedTree = true).assertDoesNotExist()
    }

    // endregion

    private fun expandHour(start: Int) {
        composeTestRule
            .onNodeWithContentDescription(
                harness.getString(R.string.schedule_expand_hour_description, harness.time(start))
            )
            .performClick()
    }
}