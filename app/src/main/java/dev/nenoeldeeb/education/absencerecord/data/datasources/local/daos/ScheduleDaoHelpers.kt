package dev.nenoeldeeb.education.absencerecord.data.datasources.local.daos

import dev.nenoeldeeb.education.absencerecord.domain.models.AssignmentRemovalReport
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError
import dev.nenoeldeeb.education.absencerecord.domain.services.ScheduleRules

internal fun validateHour(
    weekday: Int,
    startMinutes: Int,
    maxStudents: Int
) {
    if (!ScheduleRules.isHourStartValid(startMinutes)) {
        throw StudentError.Validation("Invalid start time")
    }
    if (maxStudents < 1) {
        throw StudentError.Validation("Max students must be at least 1")
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
    excludeId: Int
): Boolean =
    countHoursOverlapping(
        weekday = weekday,
        startMinutes = startMinutes,
        endMinutes = startMinutes + ScheduleRules.LESSON_DURATION_MINUTES,
        lessonDuration = ScheduleRules.LESSON_DURATION_MINUTES,
        excludeId = excludeId
    ) > 0

internal suspend fun ScheduleDao.hasBusyConflict(
    studentId: Int,
    weekday: Int,
    hourStart: Int
): Boolean {
    val busyAppointments = getBusyAppointmentsForStudentAndWeekday(studentId, weekday)
    val hourEnd = hourStart + ScheduleRules.LESSON_DURATION_MINUTES
    return busyAppointments.any { busy ->
        ScheduleRules.overlaps(
            hourStart,
            hourEnd,
            busy.startMinutes,
            busy.startMinutes + busy.durationMinutes
        )
    }
}

internal suspend fun ScheduleDao.removeBusyConflictingAssignments(
    weekday: Int,
    hourId: Int
): AssignmentRemovalReport {
    val hour = getAvailableHourById(hourId) ?: return AssignmentRemovalReport(emptyList())
    val assignments = getAssignmentsForHour(hourId)
    val hourEnd = hour.startMinutes + ScheduleRules.LESSON_DURATION_MINUTES
    val removed = mutableListOf<Int>()
    for (assignment in assignments) {
        val hasConflict =
            getBusyAppointmentsForStudentAndWeekday(assignment.studentId, weekday).any { busy ->
                ScheduleRules.overlaps(
                    hour.startMinutes,
                    hourEnd,
                    busy.startMinutes,
                    busy.startMinutes + busy.durationMinutes
                )
            }
        if (hasConflict) {
            deleteAssignment(hourId, assignment.studentId)
            removed += assignment.studentId
        }
    }
    return AssignmentRemovalReport(removed.distinct())
}

internal suspend fun ScheduleDao.removeConflictingAssignmentsForBusy(
    studentId: Int,
    weekday: Int,
    startMinutes: Int,
    durationMinutes: Int
): AssignmentRemovalReport {
    val busyEnd = startMinutes + durationMinutes
    val assignments = getAssignmentsForStudentAndWeekday(studentId, weekday)
    val removed = mutableListOf<Int>()
    for (assignment in assignments) {
        val hour = getAvailableHourById(assignment.availableHourId) ?: continue
        val hourEnd = hour.startMinutes + ScheduleRules.LESSON_DURATION_MINUTES
        if (ScheduleRules.overlaps(hour.startMinutes, hourEnd, startMinutes, busyEnd)) {
            deleteAssignment(assignment.availableHourId, studentId)
            removed += studentId
        }
    }
    return AssignmentRemovalReport(removed.distinct())
}