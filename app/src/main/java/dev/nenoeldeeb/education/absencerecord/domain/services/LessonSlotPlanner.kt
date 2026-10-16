package dev.nenoeldeeb.education.absencerecord.domain.services

import dev.nenoeldeeb.education.absencerecord.domain.models.DropReason
import dev.nenoeldeeb.education.absencerecord.domain.models.DroppedStudent
import dev.nenoeldeeb.education.absencerecord.domain.models.PlanResult
import dev.nenoeldeeb.education.absencerecord.domain.models.PlannedHour
import dev.nenoeldeeb.education.absencerecord.domain.models.PlannedSlot
import dev.nenoeldeeb.education.absencerecord.domain.models.SlotConflict
import dev.nenoeldeeb.education.absencerecord.domain.models.studentsLost
import java.util.Locale

/**
 * The ordering and slot-allocation rules for a lesson block (FR-016…FR-030).
 *
 * Pure and deterministic: the same inputs always produce the same plan, and the
 * plan never depends on the order the students arrive in. No Android, Room or
 * Compose dependency, by design — this is business logic, not storage.
 */
object LessonSlotPlanner {
    /** A student's busy commitment on the block's day. */
    data class BusyWindow(
        val startMinutes: Int,
        val durationMinutes: Int
    ) {
        val endMinutes: Int get() = startMinutes + durationMinutes
    }

    /** One student as the planner sees them: who they are and when they are busy. */
    data class StudentInput(
        val id: Int,
        val name: String,
        val busy: List<BusyWindow>
    )

    /**
     * Plans a block's running order and slots.
     *
     * Steps, in order:
     *  1. each student's *next relevant commitment* — the earliest commitment at
     *     or after the block's start (FR-019);
     *  2. sort by that time ascending, students with none last (FR-020, FR-021);
     *     ties broken alphabetically by name (FR-022, FR-023), then by id so
     *     duplicate names cannot make the order ambiguous (FR-024);
     *  3. position *n* occupies `[start + (n-1)*10, start + n*10)` (FR-017);
     *  4. check every slot against every one of that student's commitments,
     *     collecting *all* clashes rather than stopping at the first (FR-026,
     *     FR-029, FR-030);
     *  5. drop the students past the derived capacity, in running order (FR-034).
     */
    fun plan(
        blockStartMinutes: Int,
        blockDurationMinutes: Int,
        students: List<StudentInput>
    ): PlanResult {
        val maxStudents = ScheduleRules.capacityFor(blockDurationMinutes)

        // The running order depends on WHERE THE BLOCK STARTS: a commitment that
        // is over before the block begins is not relevant, so this comparator has
        // to be built per call rather than cached (FR-019).
        val nextCommitmentById =
            students.associate { it.id to nextRelevantCommitment(it, blockStartMinutes) }
        val ordered = students.sortedWith(runningOrderFor(nextCommitmentById))

        val kept = ordered.take(maxStudents)
        val dropped =
            ordered.drop(maxStudents).map {
                DroppedStudent(it.id, it.name, DropReason.ExceedsCapacity)
            }

        val slots =
            kept.mapIndexed { index, student ->
                val slotStart = blockStartMinutes + index * ScheduleRules.SLOT_DURATION_MINUTES
                PlannedSlot(
                    studentId = student.id,
                    studentName = student.name,
                    position = index + 1,
                    startMinutes = slotStart,
                    endMinutes = slotStart + ScheduleRules.SLOT_DURATION_MINUTES
                )
            }

        val conflicts = kept.flatMap { student -> conflictsFor(student, slots) }

        return PlanResult(
            plan =
                PlannedHour(
                    blockStartMinutes = blockStartMinutes,
                    blockDurationMinutes = blockDurationMinutes,
                    maxStudents = maxStudents,
                    slots = slots
                ),
            conflicts = conflicts,
            dropped = dropped
        )
    }

    /**
     * Everyone who cannot survive an edit to this block, **closed under deletion**.
     *
     * One pass of [plan] is not enough, because the running order is derived
     * rather than stored: removing a student re-plans everyone else's slots. A
     * survivor can therefore slide backwards into a slot that lands on their own
     * commitment — and a drop list built from a single pass would unassign them
     * without ever having shown the teacher their name (FR-013, SC-015).
     *
     * Concretely, for a block holding two students where one is committed from
     * before the block starts and therefore sorts last: moving the block a few
     * minutes later makes the *first* student collide, and only once she is gone
     * does the second slide into the colliding slot.
     *
     * So the set is computed by repeatedly planning the remaining roster and
     * absorbing whoever the new plan displaces. Every pass that continues removes
     * at least one student, so this terminates in at most [MAX_STUDENTS_PER_BLOCK]
     * passes; capacity drops need no iteration, since shortening the roster can
     * only move them further out of contention.
     */
    fun closedLosses(
        blockStartMinutes: Int,
        blockDurationMinutes: Int,
        students: List<StudentInput>
    ): List<DroppedStudent> {
        val lost = mutableListOf<DroppedStudent>()
        var remaining = students
        while (remaining.isNotEmpty()) {
            val next = plan(blockStartMinutes, blockDurationMinutes, remaining).studentsLost()
            if (next.isEmpty()) return lost
            lost += next
            val gone = next.map { it.studentId }.toSet()
            remaining = remaining.filter { it.id !in gone }
        }
        return lost
    }

    /** The most students a block can hold: 120 minutes of ten-minute slots (FR-064). */
    const val MAX_STUDENTS_PER_BLOCK = 12

    /** Every clash between one student's slots and that student's own commitments. */
    private fun conflictsFor(
        student: StudentInput,
        slots: List<PlannedSlot>
    ): List<SlotConflict> =
        slots
            .filter { slot -> slot.studentId == student.id }
            .flatMap { slot ->
                student.busy
                    .filter { busy ->
                        ScheduleRules.overlaps(
                            slot.startMinutes,
                            slot.endMinutes,
                            busy.startMinutes,
                            busy.endMinutes
                        )
                    }
                    .map { busy ->
                        SlotConflict(
                            studentId = student.id,
                            studentName = student.name,
                            slotStartMinutes = slot.startMinutes,
                            slotEndMinutes = slot.endMinutes,
                            busyStartMinutes = busy.startMinutes,
                            busyEndMinutes = busy.endMinutes
                        )
                    }
            }

    /**
     * Earliest commitment at or after the block start, or [NO_RELEVANT_COMMITMENT]
     * so that students without one sort last (FR-021).
     */
    fun nextRelevantCommitment(
        student: StudentInput,
        blockStartMinutes: Int
    ): Int =
        student.busy
            .map { it.startMinutes }
            .filter { it >= blockStartMinutes }
            .minOrNull() ?: NO_RELEVANT_COMMITMENT

    const val NO_RELEVANT_COMMITMENT = Int.MAX_VALUE

    /**
     * Earliest next commitment first, then alphabetical, then by id.
     *
     * Names compare case-insensitively under [Locale.ROOT] so the same data never
     * yields different orders in different locales (FR-022, FR-023). The final
     * tie-break on id means two students sharing a name can never produce an
     * ambiguous order (FR-024).
     */
    private fun runningOrderFor(nextCommitmentById: Map<Int, Int>): Comparator<StudentInput> =
        Comparator { a, b ->
            val byCommitment = nextCommitmentById.getValue(a.id).compareTo(nextCommitmentById.getValue(b.id))
            if (byCommitment != 0) return@Comparator byCommitment
            val byName = a.name.lowercase(Locale.ROOT).compareTo(b.name.lowercase(Locale.ROOT))
            if (byName != 0) byName else a.id.compareTo(b.id)
        }
}