package dev.nenoeldeeb.education.absencerecord.domain.models

import dev.nenoeldeeb.education.absencerecord.domain.services.LessonSlotPlanner
import dev.nenoeldeeb.education.absencerecord.domain.services.LessonSlotPlanner.BusyWindow
import dev.nenoeldeeb.education.absencerecord.domain.services.LessonSlotPlanner.StudentInput
import kotlinx.datetime.DayOfWeek

data class AvailableLessonHour(
    val id: Int,
    val weekday: DayOfWeek,
    val startMinutes: Int,
    /** Lesson length in minutes. The single source of truth for capacity (FR-001, FR-003). */
    val durationMinutes: Int
) {
    /** Derived, never stored and never independently editable (FR-003, FR-004). */
    val maxStudents: Int get() = durationMinutes / SLOT_MINUTES

    private companion object {
        /** Each student occupies one ten-minute slot (FR-016). */
        const val SLOT_MINUTES = 10
    }
}

data class LessonAssignment(
    val id: Int,
    val availableHourId: Int,
    val studentId: Int,
    val weekday: DayOfWeek
)

data class BusyAppointment(
    val id: Int,
    val studentId: Int,
    val weekday: DayOfWeek,
    val startMinutes: Int,
    val durationMinutes: Int
)

data class AssignmentRemovalReport(
    val removedStudentIds: List<Int>
)

data class HourWithOccupancy(
    val hour: AvailableLessonHour,
    val endMinutes: Int,
    /**
     * The block's students in the DERIVED running order (FR-020 … FR-025).
     *
     * This is recomputed on every read, never stored and never cached
     * (FR-025, FR-060), so reopening the app yields the same order.
     */
    val assignedStudentIds: List<Int>,
    val assignedCount: Int,
    val durationMinutes: Int = hour.durationMinutes,
    /** Each student's contiguous ten-minute slot, in running order (FR-016, FR-017). */
    val slots: List<PlannedSlot> = emptyList(),
    /**
     * Each assigned student's commitments on this day, keyed by student id.
     *
     * Derived on read, never stored. It is present so the newcomer's PROSPECTIVE
     * slot can be computed without guessing: where they land depends on where
     * everyone else lands, which depends on their commitments (FR-019, FR-031).
     */
    val studentBusy: Map<Int, List<BusyWindow>> = emptyMap(),
    /** Each assigned student's name, keyed by id, for the order's alphabetical tie-break. */
    val studentNames: Map<Int, String> = emptyMap()
) {
    /** All capacity figures derive from the block's length (FR-004). */
    val maxStudents: Int get() = hour.maxStudents

    val remainingSlots: Int get() = (maxStudents - assignedCount).coerceAtLeast(0)

    val isFull: Boolean get() = assignedCount >= maxStudents

    /** One student's slot, or null when they are not in this block. */
    fun slotFor(studentId: Int): PlannedSlot? = slots.firstOrNull { it.studentId == studentId }

    /**
     * Where [newcomer] would land if added to this block right now.
     *
     * Re-plans the whole roster with the newcomer included, exactly as the
     * repository does on read, so the caller never has to re-derive the order
     * by hand. Null when the block is already full or the incumbent commitments
     * are unknown — in which case the caller must not treat the block as
     * eligible, because it cannot prove otherwise.
     */
    fun prospectiveSlotFor(
        newcomerId: Int,
        newcomerName: String,
        newcomerBusy: List<BusyWindow>
    ): PlannedSlot? {
        if (remainingSlots <= 0) return null
        if (assignedStudentIds.isNotEmpty() && studentBusy.size < assignedStudentIds.size) return null
        val roster =
            assignedStudentIds.map { id ->
                StudentInput(
                    id = id,
                    name = studentNames[id] ?: "Student $id",
                    busy = studentBusy[id].orEmpty()
                )
            } + StudentInput(newcomerId, newcomerName, newcomerBusy)
        return LessonSlotPlanner.plan(hour.startMinutes, hour.durationMinutes, roster)
            .plan
            .slots
            .firstOrNull { it.studentId == newcomerId }
    }
}

data class StudentScheduleView(
    val studentId: Int,
    val lessons: List<StudentLessonEntry>,
    val busy: List<BusyAppointment>
)

data class StudentLessonEntry(
    val weekday: DayOfWeek,
    val startMinutes: Int,
    val endMinutes: Int,
    /** This student's own ten minutes inside the block, not the whole block (FR-018). */
    val slotStartMinutes: Int? = null,
    val slotEndMinutes: Int? = null
)