package dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.nenoeldeeb.education.absencerecord.domain.models.AvailableLessonHour
import dev.nenoeldeeb.education.absencerecord.domain.models.HourWithOccupancy
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.services.LessonSlotPlanner
import dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule.components.HOUR_ROW_TAG_PREFIX
import dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule.components.SCHEDULE_HOURS_LIST_TAG
import dev.nenoeldeeb.education.absencerecord.presentation.theme.AbsenceRecordTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The device half of SC-019: rendering a full day of the schedule.
 *
 * A day holds at most 144 non-overlapping ten-minute blocks, so this composes
 * the real screen content against that worst case and reports the time to idle.
 *
 * Two things are measured deliberately and reported separately:
 *
 *  - **cold** — the very first composition in a fresh process. On an emulator
 *    this is dominated by Compose runtime start-up, font loading and JIT, none
 *    of which the app controls; it is reported for honesty, not as the budget.
 *  - **warm** — the first render of a full day once the runtime is up. This is
 *    the number SC-019 is actually about, and it is the one asserted.
 *
 * Caveats stated plainly rather than buried: this runs on an emulator against a
 * debug build, because AGP 9 creates no release instrumented-test variant for
 * this project — which is exactly why the cold number is meaningless and the
 * warm one is not. The derivation half of the budget, the part this feature
 * added, is measured exactly in `DayPlanningPerformanceTest`.
 */
@RunWith(AndroidJUnit4::class)
class ScheduleDayRenderMeasurementTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    /** The most blocks a single day can hold: 1,440 minutes / 10. */
    private val blocksPerDay = 1440 / 10

    private fun stateFor(hours: List<HourWithOccupancy>) =
        ScheduleScreenState(
            hoursForWeekday = hours,
            allStudents =
                (1..hours.sumOf { it.slots.size }).map { Student(it, "Student $it") },
            isHoursLoading = false
        )

    /** Every possible block for one day, derived exactly as the repository would. */
    private fun fullDay(
        blockMinutes: Int = 10,
        count: Int = blocksPerDay
    ): List<HourWithOccupancy> {
        val perBlock = blockMinutes / 10
        var nextStudentId = 1
        return (0 until count).map { slot ->
            val start = slot * blockMinutes
            val roster =
                (0 until perBlock).map { seat ->
                    val id = nextStudentId++
                    LessonSlotPlanner.StudentInput(
                        id = id,
                        name = "Student $id",
                        // A commitment, so the comparator has real work to do.
                        busy =
                            listOf(
                                LessonSlotPlanner.BusyWindow(
                                    startMinutes = (id * 7 + seat) % 1440,
                                    durationMinutes = 30
                                )
                            )
                    )
                }
            val plan = LessonSlotPlanner.plan(start, blockMinutes, roster)
            HourWithOccupancy(
                hour = AvailableLessonHour(slot + 1, DayOfWeek.SATURDAY, start, blockMinutes),
                endMinutes = start + blockMinutes,
                assignedStudentIds = plan.plan.slots.map { it.studentId },
                assignedCount = plan.plan.slots.size,
                slots = plan.plan.slots
            )
        }
    }

    /**
     * One composition, driven by state, so cold and warm can be distinguished —
     * `setContent` may only be called once per rule.
     */
    private fun renderAndTime(
        counts: List<Int>,
        blockMinutes: Int = 10
    ): Map<Int, Long> {
        val stateFlow = MutableStateFlow(stateFor(fullDay(count = counts.first(), blockMinutes = blockMinutes)))
        composeTestRule.setContent {
            AbsenceRecordTheme {
                val state = stateFlow.collectAsStateWithLifecycle().value
                ScheduleScreenContent(uiState = state, onEvent = {}, onStudentClick = {})
            }
        }
        return counts.associateWith { count ->
            val started = System.nanoTime()
            stateFlow.value = stateFor(fullDay(count = count, blockMinutes = blockMinutes))
            composeTestRule.waitForIdle()
            (System.nanoTime() - started) / 1_000_000
        }
    }

    @Test
    fun warmRenderOfAFullDayStaysInsideTheBudget() {
        // The first entry absorbs Compose's cold start; the rest are the number
        // SC-019 is about: a full day rendered on a live runtime.
        val timings = renderAndTime(listOf(12, blocksPerDay, blocksPerDay))
        val warmFullDay = timings.getValue(blocksPerDay)

        timings.forEach { (count, ms) -> println("SC-019 render: $count blocks -> $ms ms") }
        println("SC-019 warm full-day render: $blocksPerDay blocks -> $warmFullDay ms")

        assertTrue(
            "a full day rendered in ${warmFullDay}ms, over the 1s SC-019 budget",
            warmFullDay < 1_000
        )
    }

    @Test
    fun theDayIsCompleteAndNotTruncatedForBeingSlow() {
        val hours = fullDay()
        val stateFlow = MutableStateFlow(stateFor(hours))
        composeTestRule.setContent {
            AbsenceRecordTheme {
                val state = stateFlow.collectAsStateWithLifecycle().value
                ScheduleScreenContent(uiState = state, onEvent = {}, onStudentClick = {})
            }
        }
        composeTestRule.waitForIdle()

        assertEquals(blocksPerDay, hours.size)
        assertEquals(blocksPerDay, hours.count { it.assignedCount > 0 })

        // The list is a LazyColumn, so it composes what is visible rather than
        // all 144 rows at once. Reaching the LAST block is what proves the day
        // was not quietly truncated for being slow — a row count taken at the
        // top of the list could not show that.
        composeTestRule.onNodeWithTag(HOUR_ROW_TAG_PREFIX + "1", useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(SCHEDULE_HOURS_LIST_TAG)
            .performScrollToNode(hasTestTag(HOUR_ROW_TAG_PREFIX + hours.size))
        composeTestRule
            .onNodeWithTag(HOUR_ROW_TAG_PREFIX + hours.size, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun longFullyOccupiedBlocksStayInsideTheBudget() {
        // The other extreme: fewer blocks, but each holding twelve students.
        val twelveLong = renderAndTime(listOf(1, 12, 12), blockMinutes = 120).getValue(12)
        println("SC-019 render: 12 blocks x 12 students -> $twelveLong ms")
        assertTrue(
            "twelve full blocks rendered in ${twelveLong}ms",
            twelveLong < 1_000
        )
    }
}