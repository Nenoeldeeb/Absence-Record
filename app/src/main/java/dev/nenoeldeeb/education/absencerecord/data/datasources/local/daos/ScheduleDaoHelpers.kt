package dev.nenoeldeeb.education.absencerecord.data.datasources.local.daos

import dev.nenoeldeeb.education.absencerecord.data.datasources.local.entities.AvailableLessonHourEntity
import dev.nenoeldeeb.education.absencerecord.domain.models.AssignmentRemovalReport
import dev.nenoeldeeb.education.absencerecord.domain.models.DroppedStudent
import dev.nenoeldeeb.education.absencerecord.domain.models.PlanResult
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError
import dev.nenoeldeeb.education.absencerecord.domain.services.LessonSlotPlanner
import dev.nenoeldeeb.education.absencerecord.domain.services.ScheduleRules

/** A student's id and name, as stored. */
data class StudentNameRow(
    val id: Int,
    val name: String
)

/** Builds the planner's view of one student from their commitments on that day. */
internal suspend fun ScheduleDao.studentInputFor(
    studentId: Int,
    weekday: Int
): LessonSlotPlanner.StudentInput {
    val name =
        getStudentNamesByIds(listOf(studentId)).firstOrNull()?.name ?: "Student $studentId"
    val busy =
        getBusyAppointmentsForStudentAndWeekday(studentId, weekday).map {
            LessonSlotPlanner.BusyWindow(it.startMinutes, it.durationMinutes)
        }
    return LessonSlotPlanner.StudentInput(id = studentId, name = name, busy = busy)
}

/** The planner's view of every student currently assigned to [hourId]. */
internal suspend fun ScheduleDao.studentInputsForHour(
    hourId: Int,
    weekday: Int
): List<LessonSlotPlanner.StudentInput> {
    val assignments = getAssignmentsForHour(hourId)
    if (assignments.isEmpty()) return emptyList()
    val namesById = getStudentNamesByIds(assignments.map { it.studentId }).associate { it.id to it.name }
    return assignments.map { assignment ->
        val busy =
            getBusyAppointmentsForStudentAndWeekday(assignment.studentId, weekday).map {
                LessonSlotPlanner.BusyWindow(it.startMinutes, it.durationMinutes)
            }
        LessonSlotPlanner.StudentInput(
            id = assignment.studentId,
            name = namesById[assignment.studentId] ?: "Student ${assignment.studentId}",
            busy = busy
        )
    }
}

/**
 * Plans a block's running order from its current roster.
 *
 * This is what makes the order derived rather than stored (FR-025): it is
 * recomputed from the roster and their commitments on every call.
 */
internal suspend fun ScheduleDao.planHourWith(
    weekday: Int,
    startMinutes: Int,
    durationMinutes: Int,
    hourId: Int
): PlanResult = LessonSlotPlanner.plan(startMinutes, durationMinutes, studentInputsForHour(hourId, weekday))

internal fun validateHour(
    weekday: Int,
    startMinutes: Int,
    durationMinutes: Int
) {
    if (!ScheduleRules.isHourDurationValid(durationMinutes)) {
        throw StudentError.Validation("Invalid lesson length")
    }
    if (!ScheduleRules.isHourStartValid(startMinutes, durationMinutes)) {
        throw StudentError.Validation("Invalid start time")
    }
}

internal fun validateBusy(
    startMinutes: Int,
    durationMinutes: Int
) {
    if (!ScheduleRules.isBusyStartValid(startMinutes)) {
        throw StudentError.Validation("Invalid start time")
    }
    if (!ScheduleRules.isBusyDurationValid(startMinutes, durationMinutes)) {
        throw StudentError.Validation("Busy appointment cannot cross midnight")
    }
}

internal suspend fun ScheduleDao.hasOverlappingHour(
    weekday: Int,
    startMinutes: Int,
    durationMinutes: Int,
    excludeId: Int
): Boolean =
    countHoursOverlapping(
        weekday = weekday,
        startMinutes = startMinutes,
        endMinutes = startMinutes + durationMinutes,
        excludeId = excludeId
    ) > 0

/**
 * Everyone who cannot survive an edit to this block, closed under deletion.
 *
 * Read through the roster rather than from a one-off plan, because the running
 * order is derived: removing a student re-plans everyone else's slots, so a
 * single pass misses students who only collide once someone ahead of them goes.
 * The teacher must be shown every name before consenting to lose any of them
 * (FR-011, FR-013, SC-015).
 */
internal suspend fun ScheduleDao.closedDropSet(
    weekday: Int,
    startMinutes: Int,
    durationMinutes: Int,
    hourId: Int
): List<DroppedStudent> =
    LessonSlotPlanner.closedLosses(
        startMinutes,
        durationMinutes,
        studentInputsForHour(hourId, weekday)
    )

/**
 * Evicts assignments whose occupant's own ten minutes no longer survive a change
 * to the block.
 *
 * The check is per-slot, never per-block (FR-038): a student whose slot is
 * clear of every commitment stays, even if a commitment lands elsewhere inside
 * the block. Slots come from the derived plan, so they already reflect where
 * each student actually runs.
 */
internal suspend fun ScheduleDao.removeBusyConflictingAssignments(
    weekday: Int,
    hourId: Int
): AssignmentRemovalReport {
    val hour = getAvailableHourById(hourId) ?: return AssignmentRemovalReport(emptyList())
    val conflicted = conflictedStudentIds(hour)
    for (studentId in conflicted) {
        deleteAssignment(hourId, studentId)
    }
    return AssignmentRemovalReport(conflicted)
}

/**
 * Which of a block's assigned students currently sit on a slot that collides
 * with one of their own commitments.
 *
 * Read from the derived plan rather than recomputed per student, so the answer
 * always matches the running order the teacher is shown (FR-038, FR-039).
 */
private suspend fun ScheduleDao.conflictedStudentIds(hour: AvailableLessonHourEntity): List<Int> {
    if (getAssignmentsForHour(hour.id).isEmpty()) return emptyList()
    return planHourWith(hour.weekday, hour.startMinutes, hour.durationMinutes, hour.id)
        .conflicts
        .map { it.studentId }
        .distinct()
}

/**
 * Evicts this student's assignments whose slot a new or moved commitment
 * lands on.
 *
 * Per-slot, not per-block (FR-038): adding a 9:30 commitment must not evict a
 * student whose 9:00–9:10 slot it never touches. The plan is re-derived because
 * the commitment itself changes who runs where.
 */
internal suspend fun ScheduleDao.removeConflictingAssignmentsForBusy(
    studentId: Int,
    weekday: Int,
    startMinutes: Int,
    durationMinutes: Int
): AssignmentRemovalReport {
    val removed = mutableListOf<Int>()
    for (assignment in getAssignmentsForStudentAndWeekday(studentId, weekday)) {
        val hour = getAvailableHourById(assignment.availableHourId) ?: continue
        if (conflictedStudentIds(hour).contains(studentId)) {
            deleteAssignment(assignment.availableHourId, studentId)
            removed += studentId
        }
    }
    return AssignmentRemovalReport(removed.distinct())
}