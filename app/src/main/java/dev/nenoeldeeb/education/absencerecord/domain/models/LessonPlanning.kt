package dev.nenoeldeeb.education.absencerecord.domain.models

/**
 * One assigned student's contiguous 10-minute slot within a block.
 *
 * This file is the vocabulary of duration-based lesson planning (FR-016…FR-036).
 * The types here are computed, never persisted: a student's place in the running
 * order — and therefore their 10-minute slot — is derived on every read from the
 * block's students, those students' commitments and the block's length.
 */
data class PlannedSlot(
    val studentId: Int,
    val studentName: String,
    val position: Int,
    val startMinutes: Int,
    val endMinutes: Int
)

/** A block's length, its derived capacity, and its students' slots in running order. */
data class PlannedHour(
    val blockStartMinutes: Int,
    val blockDurationMinutes: Int,
    val maxStudents: Int,
    val slots: List<PlannedSlot>
)

/** A slot that overlaps one of that student's own busy appointments. */
data class SlotConflict(
    val studentId: Int,
    val studentName: String,
    val slotStartMinutes: Int,
    val slotEndMinutes: Int,
    val busyStartMinutes: Int,
    val busyEndMinutes: Int
)

/** Why a student cannot be kept in the block after a re-plan. */
enum class DropReason {
    /** The block holds its derived maximum already and this student is past it. */
    ExceedsCapacity,

    /** The slot this student would occupy collides with one of their commitments. */
    SlotConflictsWithBusy
}

/** A student the re-plan would remove, named so the teacher can decide. */
data class DroppedStudent(
    val studentId: Int,
    val studentName: String,
    val reason: DropReason
)

/**
 * The full outcome of planning a block.
 *
 * [conflicts] holds every clash found, not just the first, so the teacher can
 * see the whole picture in one pass. [dropped] holds students beyond the derived
 * capacity, in the order they would have run.
 */
data class PlanResult(
    val plan: PlannedHour,
    val conflicts: List<SlotConflict>,
    val dropped: List<DroppedStudent>
)

/**
 * Everyone this single plan would cost the teacher.
 *
 * A student is lost two ways, and **both** are the teacher's decision: the block
 * got smaller and they are past its derived maximum (FR-010), or their own slot
 * sits on one of their commitments (FR-015, FR-038).
 *
 * This is one pass. Because the running order is derived, it is not the whole
 * answer either — see `LessonSlotPlanner.closedLosses`, which closes the set
 * under deletion.
 */
fun PlanResult.studentsLost(): List<DroppedStudent> {
    val conflicted =
        conflicts.map {
            DroppedStudent(it.studentId, it.studentName, DropReason.SlotConflictsWithBusy)
        }
    return (dropped + conflicted).distinctBy { it.studentId }
}

/**
 * The two-phase confirm contract for editing a block (FR-011, FR-013).
 *
 * The first call returns [NeedsConfirmation] **without writing anything**. The
 * teacher sees the resulting order and the named drop list, then a second call
 * with `confirmed = true` commits and returns [Applied].
 */
sealed interface HourReplanResult {
    /** The edit was committed; these students were unassigned as a result. */
    data class Applied(val removedStudentIds: List<Int>) : HourReplanResult

    /** The edit would drop students; nothing has been written yet. */
    data class NeedsConfirmation(
        val proposed: PlannedHour,
        val dropped: List<DroppedStudent>
    ) : HourReplanResult
}