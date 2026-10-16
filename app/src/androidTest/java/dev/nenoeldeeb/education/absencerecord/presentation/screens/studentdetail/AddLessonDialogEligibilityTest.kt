package dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.AvailableLessonHour
import dev.nenoeldeeb.education.absencerecord.domain.models.BusyAppointment
import dev.nenoeldeeb.education.absencerecord.domain.models.HourWithOccupancy
import dev.nenoeldeeb.education.absencerecord.domain.models.PlannedSlot
import dev.nenoeldeeb.education.absencerecord.domain.services.LessonSlotPlanner.BusyWindow
import dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.dialogs.AddLessonDialog
import dev.nenoeldeeb.education.absencerecord.presentation.theme.AbsenceRecordTheme
import dev.nenoeldeeb.education.absencerecord.presentation.utils.TimeFormatter
import kotlinx.datetime.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Eligibility is judged on the newcomer's own ten minutes, not the block's
 * whole extent (FR-027).
 *
 * The regression: a commitment that merely sits later inside a block used to
 * mark that block unavailable, hiding a lesson the student could in fact take.
 */
@RunWith(AndroidJUnit4::class)
class AddLessonDialogEligibilityTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun time(minutes: Int): String = TimeFormatter.format(minutes, context)

    private fun busy(
        start: Int,
        duration: Int = 30
    ) = BusyAppointment(1, 1, DayOfWeek.SATURDAY, start, duration)

    /** A 9:00–10:00 block with no incumbents, so the newcomer holds 9:00–9:10. */
    private fun emptyBlock(
        start: Int = 540,
        duration: Int = 60
    ) = HourWithOccupancy(
        hour = AvailableLessonHour(3, DayOfWeek.SATURDAY, start, duration),
        endMinutes = start + duration,
        assignedStudentIds = emptyList(),
        assignedCount = 0,
        slots = emptyList()
    )

    /**
     * The same block with one incumbent, so the newcomer holds the SECOND slot.
     *
     * The incumbent's 9:05 commitment is what keeps the newcomer behind them: a
     * newcomer only stays second while their own commitments are later.
     */
    private fun blockWithOneIncumbent(
        start: Int = 540,
        duration: Int = 60
    ) = HourWithOccupancy(
        hour = AvailableLessonHour(3, DayOfWeek.SATURDAY, start, duration),
        endMinutes = start + duration,
        assignedStudentIds = listOf(99),
        assignedCount = 1,
        slots =
            listOf(
                PlannedSlot(
                    99,
                    "Incumbent",
                    1,
                    start,
                    start + 10
                )
            ),
        studentBusy = mapOf(99 to listOf(BusyWindow(start + 5, 30))),
        studentNames = mapOf(99 to "Incumbent")
    )

    private fun setDialog(
        hours: List<HourWithOccupancy>,
        busyAppointments: List<BusyAppointment> = emptyList(),
        selected: MutableState<Int?> = mutableStateOf(null),
        onHourSelected: (Int) -> Unit = { selected.value = it }
    ) {
        composeTestRule.setContent {
            AbsenceRecordTheme {
                AddLessonDialog(
                    hours = hours,
                    busyAppointments = busyAppointments,
                    studentId = 1,
                    studentName = "Newcomer",
                    selectedHourId = selected.value,
                    onHourSelected = onHourSelected,
                    onConfirm = {},
                    onDismiss = {}
                )
            }
        }
    }

    @Test
    fun aCommitmentLaterInsideTheBlockDoesNotBlockIt() {
        setDialog(hours = listOf(emptyBlock()), busyAppointments = listOf(busy(570)))

        // 9:30 is inside the block but past the newcomer's 9:00–9:10 slot
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.assign_reason_busy_conflict))
            .assertDoesNotExist()
        composeTestRule.onNodeWithText("${time(540)} – ${time(600)}").performClick()
        composeTestRule
            .onNodeWithText(context.getString(R.string.action_save))
            .assertIsEnabled()
    }

    @Test
    fun aCommitmentOnTheNewcomersSlotDoesBlockIt() {
        setDialog(hours = listOf(emptyBlock()), busyAppointments = listOf(busy(540)))

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.assign_reason_busy_conflict))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("${time(540)} – ${time(600)}").performClick()
        composeTestRule
            .onNodeWithText(context.getString(R.string.action_save))
            .assertIsNotEnabled()
    }

    @Test
    fun eligibilityFollowsTheDerivedSlotNotTheBlockStart() {
        // With one incumbent, the newcomer runs second and holds 9:10–9:20.
        // A 9:15 commitment lands on THAT slot even though the block starts 9:00.
        setDialog(
            hours = listOf(blockWithOneIncumbent()),
            busyAppointments = listOf(busy(555), busy(660))
        )

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.assign_reason_busy_conflict))
            .assertIsDisplayed()
    }

    @Test
    fun aCommitmentBeforeTheDerivedSlotIsAccepted() {
        // Same shape, but the commitment ends as the newcomer's slot starts.
        setDialog(
            hours = listOf(blockWithOneIncumbent()),
            busyAppointments = listOf(busy(660), busy(690))
        )

        composeTestRule.onNodeWithText("${time(540)} – ${time(600)}").performClick()
        composeTestRule
            .onNodeWithText(context.getString(R.string.action_save))
            .assertIsEnabled()
    }

    @Test
    fun aFullBlockStaysUnavailable() {
        // a 10-minute block holds exactly one student, so one incumbent fills it
        setDialog(hours = listOf(blockWithOneIncumbent(duration = 10)))

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.assign_reason_hour_full))
            .assertIsDisplayed()
    }

    @Test
    fun selectingAnEligibleBlockReportsItsId() {
        val selected = mutableListOf<Int>()
        setDialog(
            hours = listOf(emptyBlock()),
            busyAppointments = listOf(busy(570)),
            onHourSelected = { selected.add(it) }
        )

        composeTestRule.onNodeWithText("${time(540)} – ${time(600)}").performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf(3), selected)
    }
}