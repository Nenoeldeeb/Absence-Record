package dev.nenoeldeeb.education.absencerecord.domain.services

import dev.nenoeldeeb.education.absencerecord.domain.models.DropReason
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
class LessonSlotPlannerOrderingTest {
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

    // region quickstart §2 rows 1-2: capacity derivation (SC-002)

    @Test
    fun `row 1 - a ten minute block accepts at most one student`() {
        val result = plan(duration = 10, students = listOf(student(1), student(2)))
        assertEquals(1, result.plan.maxStudents)
        assertEquals(1, result.plan.slots.size)
    }

    @Test
    fun `row 2 - a 120 minute block accepts at most twelve students`() {
        val result = plan(duration = 120, students = (1..13).map { student(it) })
        assertEquals(12, result.plan.maxStudents)
        assertEquals(12, result.plan.slots.size)
    }

    // endregion

    // region quickstart §2 row 3: a later commitment inside the block is fine (SC-003)

    @Test
    fun `row 3 - a commitment at 10 30 permits a slot starting at 10 00`() {
        val result = plan(start = tenOClock, duration = 120, students = listOf(student(1, busyStarts = listOf(tenOClock + 30))))
        assertNoConflict(result)
        assertEquals(tenOClock, result.plan.slots.single().startMinutes)
        assertEquals(tenOClock + 10, result.plan.slots.single().endMinutes)
    }

    @Test
    fun `a commitment later inside the block does not reject the student`() {
        // FR-027: only the student's own ten minutes matter, not the block extent.
        val result = plan(start = tenOClock, duration = 120, students = listOf(student(1, busyStarts = listOf(tenOClock + 90))))
        assertNoConflict(result)
    }

    // endregion

    // region quickstart §2 row 4: ordering by next relevant commitment

    @Test
    fun `row 4 - students are ordered by their next relevant busy appointment`() {
        val result =
            plan(
                start = tenOClock,
                duration = 60,
                students =
                    listOf(
                        student(1, "A", listOf(tenOClock + 30)),
                        student(2, "B", listOf(tenOClock + 40)),
                        student(3, "C", listOf(tenOClock + 60))
                    )
            )
        assertNoConflict(result)
        assertEquals(listOf(1, 2, 3), result.plan.slots.map { it.studentId })
        assertEquals(tenOClock, result.plan.slots[0].startMinutes)
        assertEquals(tenOClock + 10, result.plan.slots[1].startMinutes)
        assertEquals(tenOClock + 20, result.plan.slots[2].startMinutes)
        assertEquals(tenOClock + 30, result.plan.slots[2].endMinutes)
    }

    @Test
    fun `a commitment ending exactly at the block start is not relevant`() {
        // FR-019: only commitments at or after the block start count.
        val result = plan(start = tenOClock, duration = 60, students = listOf(student(1, busyStarts = listOf(tenOClock - 30))))
        assertNoConflict(result)
        assertEquals(listOf(1), result.plan.slots.map { it.studentId })
    }

    @Test
    fun `a commitment still running at the block start blocks the first slot`() {
        // FR-026: an appointment spanning blockStart collides with slot 1.
        val result = plan(start = tenOClock, duration = 60, students = listOf(student(1, busyStarts = listOf(tenOClock - 10))))
        assertEquals(1, result.conflicts.size)
        assertEquals(tenOClock, result.conflicts.single().slotStartMinutes)
    }

    // endregion

    // region quickstart §2 row 5: deterministic ordering (SC-004)

    @Test
    fun `row 5 - students tied on the same appointment time are ordered alphabetically`() {
        val result =
            plan(
                start = tenOClock,
                duration = 60,
                students =
                    listOf(
                        student(3, "Carol", listOf(tenOClock + 30)),
                        student(1, "Alice", listOf(tenOClock + 30)),
                        student(2, "Bob", listOf(tenOClock + 30))
                    )
            )
        assertEquals(listOf("Alice", "Bob", "Carol"), result.plan.slots.map { it.studentName })
    }

    @Test
    fun `alphabetical tie break ignores input order`() {
        val forward = listOf(student(1, "Alice"), student(2, "Bob"), student(3, "Carol"))
        val reversed = forward.reversed()
        val a = plan(start = tenOClock, duration = 60, students = forward)
        val b = plan(start = tenOClock, duration = 60, students = reversed)
        assertEquals(a.plan.slots.map { it.studentId }, b.plan.slots.map { it.studentId })
        assertEquals(listOf("Alice", "Bob", "Carol"), b.plan.slots.map { it.studentName })
    }

    @Test
    fun `students with no commitment sort after those who have one`() {
        val result =
            plan(
                start = tenOClock,
                duration = 60,
                students =
                    listOf(
                        student(1, "Zoe"),
                        student(2, "Adam", listOf(tenOClock + 40))
                    )
            )
        assertEquals(listOf("Adam", "Zoe"), result.plan.slots.map { it.studentName })
    }

    @Test
    fun `students with no commitment are ordered alphabetically among themselves`() {
        val result =
            plan(
                start = tenOClock,
                duration = 60,
                students = listOf(student(1, "Zoe"), student(2, "Adam"), student(3, "Mia"))
            )
        assertEquals(listOf("Adam", "Mia", "Zoe"), result.plan.slots.map { it.studentName })
    }

    @Test
    fun `identical names fall back to a stable id so the order is deterministic`() {
        // FR-024
        val result =
            plan(
                start = tenOClock,
                duration = 60,
                students = listOf(student(9, "Sam"), student(4, "Sam"), student(7, "Sam"))
            )
        assertEquals(listOf(4, 7, 9), result.plan.slots.map { it.studentId })
    }

    // endregion

    // region quickstart §2 rows 6-8: half-open slot boundaries (FR-028)

    @Test
    fun `row 6 - a commitment starting exactly at the slot end is accepted`() {
        val result = plan(start = tenOClock, duration = 60, students = listOf(student(1, busyStarts = listOf(tenOClock + 10))))
        assertNoConflict(result)
    }

    @Test
    fun `row 7 - a commitment starting exactly at the slot start is rejected`() {
        val result = plan(start = tenOClock, duration = 60, students = listOf(student(1, busyStarts = listOf(tenOClock))))
        assertEquals(1, result.conflicts.size)
    }

    @Test
    fun `a commitment inside the slot is rejected`() {
        val result = plan(start = tenOClock, duration = 60, students = listOf(student(1, busyStarts = listOf(tenOClock + 5))))
        assertEquals(1, result.conflicts.size)
    }

    // endregion

    // region quickstart §2 rows 9-10: capacity and full reporting (FR-034, FR-035)

    @Test
    fun `row 9 - a thirteenth student is refused on a 120 minute block`() {
        val students = (1..13).map { student(it, "P%02d".format(it), listOf(clear)) }
        val result = plan(start = tenOClock, duration = 120, students = students)
        assertEquals(12, result.plan.slots.size)
        assertEquals(1, result.dropped.size)
        // Ties break alphabetically (FR-022), so "P13" — the last by name — is the
        // one that falls past the derived capacity, not the highest id.
        assertEquals(13, result.dropped.single().studentId)
        assertEquals("P13", result.dropped.single().studentName)
        assertEquals(DropReason.ExceedsCapacity, result.dropped.single().reason)
    }

    @Test
    fun `row 10 - every conflict is reported not just the first`() {
        // Both students share a commitment that spans the whole block, so each is
        // flagged against their own slot — the report must carry both.
        val result =
            plan(
                start = tenOClock,
                duration = 60,
                students =
                    listOf(
                        student(1, "A", listOf(tenOClock)),
                        student(2, "B", listOf(tenOClock))
                    )
            )
        assertEquals(2, result.conflicts.size)
        assertEquals(listOf(1, 2), result.conflicts.map { it.studentId })
    }

    @Test
    fun `input order does not change which student is flagged`() {
        val a = student(1, "A", listOf(tenOClock))
        val b = student(2, "B", listOf(tenOClock))
        val forward = plan(start = tenOClock, duration = 60, students = listOf(a, b))
        val reversed = plan(start = tenOClock, duration = 60, students = listOf(b, a))
        assertEquals(forward.plan.slots.map { it.studentId }, reversed.plan.slots.map { it.studentId })
        assertEquals(forward.conflicts.map { it.studentId }, reversed.conflicts.map { it.studentId })
    }

    // endregion
}