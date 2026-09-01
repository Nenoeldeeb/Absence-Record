package dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScheduleScreenHourRowsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val harness = ScheduleScreenTestHarness(composeTestRule)

    @Test
    fun weekdayChips_renderSaturdayFirst_withDefaultSelection() {
        harness.setContent(MutableStateFlow(ScheduleScreenState()))

        composeTestRule.onNodeWithText("Saturday").assertIsDisplayed()
        composeTestRule.onNodeWithText("Saturday").assertIsSelected()

        val saturdayX = composeTestRule.onNodeWithText("Saturday").getBoundsInRoot().left
        val sundayX = composeTestRule.onNodeWithText("Sunday").getBoundsInRoot().left
        assertTrue("Saturday should be the first weekday chip", saturdayX < sundayX)
    }

    @Test
    fun weekdayChip_clickSelectsWeekday() {
        val stateFlow = MutableStateFlow(ScheduleScreenState())
        harness.setContent(stateFlow)

        composeTestRule.onNodeWithText("Sunday").performClick()

        composeTestRule.onNodeWithText("Sunday").assertIsSelected()
        assertTrue(harness.containsEvent(ScheduleScreenEvent.SelectWeekday(DayOfWeek.SUNDAY)))
        assertEquals(DayOfWeek.SUNDAY, stateFlow.value.selectedWeekday)
    }

    @Test
    fun hoursList_showsTimeRangeOccupancyAndEditDeleteActions() {
        harness.setContent(
            MutableStateFlow(
                ScheduleScreenState(
                    hoursForWeekday = listOf(harness.hourWith(id = 1, start = 540))
                )
            )
        )

        val timeRange = "${harness.time(540)} – ${harness.time(600)}"
        composeTestRule.onNodeWithText(timeRange).assertIsDisplayed()
        composeTestRule
            .onNodeWithText(harness.getString(R.string.schedule_occupancy, 0, 5))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription(
                harness.getString(R.string.schedule_expand_hour_description, harness.time(540))
            )
            .assertIsDisplayed()
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
                harness.getString(R.string.schedule_assign_students_description, harness.time(540))
            )
            .assertDoesNotExist()
    }

    @Test
    fun expandingHour_showsAssignedStudentNamesReadOnly() {
        val students = listOf(Student(1, "Ali"), Student(2, "Sara"))
        val stateFlow =
            MutableStateFlow(
                ScheduleScreenState(
                    allStudents = students,
                    hoursForWeekday =
                        listOf(
                            harness.hourWith(id = 1, start = 540, max = 5, assigned = 2)
                        )
                )
            )
        harness.setContent(stateFlow)

        composeTestRule
            .onNodeWithContentDescription(
                harness.getString(R.string.schedule_expand_hour_description, harness.time(540))
            )
            .performClick()

        composeTestRule.onNodeWithText("Ali").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sara").assertIsDisplayed()

        composeTestRule
            .onNodeWithContentDescription(
                harness.getString(R.string.schedule_collapse_hour_description, harness.time(540))
            )
            .performClick()

        composeTestRule.onNodeWithText("Ali").assertDoesNotExist()
        assertTrue(harness.containsEvent(ScheduleScreenEvent.ToggleHourExpanded(1)))
    }

    @Test
    fun expandingHour_withNoAssignments_showsEmptyNamesState() {
        harness.setContent(
            MutableStateFlow(
                ScheduleScreenState(
                    hoursForWeekday = listOf(harness.hourWith(id = 1, start = 540))
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
    fun tappingAssignedStudentName_invokesOnStudentClick() {
        val students = listOf(Student(1, "Omar"))
        harness.setContent(
            MutableStateFlow(
                ScheduleScreenState(
                    allStudents = students,
                    hoursForWeekday =
                        listOf(
                            harness.hourWith(id = 1, start = 540, max = 5, assigned = 1)
                        )
                )
            )
        )

        composeTestRule
            .onNodeWithContentDescription(
                harness.getString(R.string.schedule_expand_hour_description, harness.time(540))
            )
            .performClick()
        composeTestRule.onNodeWithText("Omar").performClick()

        assertEquals(listOf(1), harness.recordedStudentClicks)
    }

    @Test
    fun hoursList_emptyStateShowsMessageAndAddButton() {
        harness.setContent(MutableStateFlow(ScheduleScreenState()))

        composeTestRule
            .onNodeWithText(harness.getString(R.string.schedule_no_hours_for_day))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(harness.getString(R.string.schedule_add_hour))
            .assertIsDisplayed()
    }

    @Test
    fun allIcons_haveNonEmptyContentDescriptions() {
        harness.setContent(
            MutableStateFlow(
                ScheduleScreenState(
                    hoursForWeekday = listOf(harness.hourWith(id = 1, start = 540))
                )
            )
        )

        val iconNodes =
            composeTestRule
                .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription))
                .fetchSemanticsNodes()
        assertTrue("Expected at least one icon with a content description", iconNodes.isNotEmpty())
        iconNodes.forEach { node ->
            val descriptions =
                node.config.getOrNull(SemanticsProperties.ContentDescription)
            assertTrue("Icon missing content description", !descriptions.isNullOrEmpty())
            assertTrue(
                "Icon has an empty content description: $descriptions",
                descriptions.orEmpty().all { it.isNotBlank() }
            )
        }
    }
}