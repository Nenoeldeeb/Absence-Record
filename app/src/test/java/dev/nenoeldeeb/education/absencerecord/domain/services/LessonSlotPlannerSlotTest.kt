package dev.nenoeldeeb.education.absencerecord.domain.services

import dev.nenoeldeeb.education.absencerecord.domain.models.PlanResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The ordering and slot-allocation rules (FR-016…FR-030, SC-002…SC-005).
 *
 * Rows 1–10 mirror quickstart.md §2 so the acceptance scenarios are traceable
 * one-to-one.
 */
class LessonSlotPlannerSlotTest {
    private val tenOClock = 10 * 60

    /** An hour well past every commitment used below, so none of them collide. */
    private val clear = tenOClock + 600

    private fun student(
        id: Int,
        name: String = "Student $id",
        busyStarts: List<Int> = emptyList()
    ): LessonSlotPlanner.StudentInput =
        LessonSlotPlanner.StudentInput(
            id = id,
            name = name,
            busy =
                busyStarts.map {
                    LessonSlotPlanner.BusyWindow(startMinutes = it, durationMinutes = 30)
                }
        )

    private fun plan(
        start: Int = tenOClock,
        duration: Int = 60,
        students: List<LessonSlotPlanner.StudentInput> = emptyList()
    ): PlanResult = LessonSlotPlanner.plan(start, duration, students)

    private fun assertNoConflict(result: PlanResult) {
        assertTrue(result.conflicts.isEmpty(), "expected no conflicts but got ${result.conflicts}")
    }

    // region slot arithmetic (FR-016, FR-017, SC-011)

    @Test
    fun `slot n starts at block start plus n minus one times ten`() {
        val students = (1..6).map { student(it, busyStarts = listOf(clear)) }
        val result = plan(start = tenOClock, duration = 60, students = students)
        result.plan.slots.forEachIndexed { index, slot ->
            assertEquals(index + 1, slot.position)
            assertEquals(tenOClock + index * 10, slot.startMinutes)
            assertEquals(tenOClock + index * 10 + 10, slot.endMinutes)
        }
    }

    @Test
    fun `the final slot ends exactly at the block end`() {
        val students = (1..3).map { student(it, busyStarts = listOf(clear)) }
        val result = plan(start = tenOClock, duration = 30, students = students)
        assertEquals(tenOClock + 30, result.plan.slots.last().endMinutes)
    }

    @Test
    fun `an empty block plans no slots`() {
        val result = plan(start = tenOClock, duration = 60)
        assertTrue(result.plan.slots.isEmpty())
        assertNoConflict(result)
        assertTrue(result.dropped.isEmpty())
    }

    // endregion

    // region determinism and multi-commitment handling (SC-004, FR-029, FR-030)

    @Test
    fun `planning the same inputs twice yields the same result`() {
        val students =
            listOf(
                student(1, "Alice", listOf(tenOClock + 40)),
                student(2, "Bob", listOf(tenOClock + 20)),
                student(3, "Cara", listOf(tenOClock + 20)),
                student(4, "Zed")
            )
        val first = plan(start = tenOClock, duration = 60, students = students)
        val second = plan(start = tenOClock, duration = 60, students = students)
        assertEquals(first.plan, second.plan)
        assertEquals(first.conflicts, second.conflicts)
        assertEquals(first.dropped, second.dropped)
    }

    @Test
    fun `the earliest commitment drives ordering even when a later one exists`() {
        // FR-030
        val result =
            plan(
                start = tenOClock,
                duration = 60,
                students =
                    listOf(
                        student(1, "A", listOf(tenOClock + 50, tenOClock + 20)),
                        student(2, "B", listOf(tenOClock + 35))
                    )
            )
        assertEquals(listOf("A", "B"), result.plan.slots.map { it.studentName })
    }

    @Test
    fun `every one of a student's commitments is considered during validation`() {
        // FR-029: the earliest drives position, but a later one can still collide.
        val result =
            plan(start = tenOClock, duration = 60, students = listOf(student(1, "A", listOf(clear, tenOClock))))
        assertEquals(1, result.conflicts.size)
        assertEquals(tenOClock, result.conflicts.single().slotStartMinutes)
        assertEquals(tenOClock, result.conflicts.single().busyStartMinutes)
    }

    @Test
    fun `a commitment before the block start cannot create a conflict with a later slot`() {
        // Regression: ordering is relative to the block, not to midnight.
        val result =
            plan(
                start = tenOClock,
                duration = 60,
                students =
                    listOf(
                        student(1, "Early", listOf(tenOClock - 60)),
                        student(2, "Late", listOf(clear))
                    )
            )
        assertNoConflict(result)
        assertEquals(listOf("Late", "Early"), result.plan.slots.map { it.studentName })
    }

    // endregion
}