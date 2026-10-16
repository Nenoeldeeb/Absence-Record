package dev.nenoeldeeb.education.absencerecord.domain.services

import dev.nenoeldeeb.education.absencerecord.domain.models.AvailableHourExportData
import dev.nenoeldeeb.education.absencerecord.domain.models.AvailableLessonHour
import dev.nenoeldeeb.education.absencerecord.domain.models.BusyAppointment
import dev.nenoeldeeb.education.absencerecord.domain.models.BusyAppointmentExportData
import dev.nenoeldeeb.education.absencerecord.domain.models.LessonAssignment
import dev.nenoeldeeb.education.absencerecord.domain.models.LessonAssignmentExportData
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentExportData
import dev.nenoeldeeb.education.absencerecord.domain.repositories.ScheduleRepository
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.isoDayNumber

internal class ImportMergeEngine(
    private val scheduleRepository: ScheduleRepository
) {
    suspend fun mergeAvailableHours(
        availableHours: List<AvailableHourExportData>,
        hourIdByKey: MutableMap<Pair<Int, Int>, Int>,
        mutableHours: MutableList<AvailableLessonHour>,
        counters: ImportCounters
    ) {
        availableHours.forEach { hourData ->
            if (!counters.admitHours()) return@forEach
            // A block carrying both length signals, or neither, is ambiguous and
            // is counted rather than guessed at (FR-046, SC-009). A legacy
            // capacity is converted here, at the boundary (FR-040, FR-041).
            val durationMinutes = hourData.resolvedDurationMinutes()
            if (durationMinutes == null || !isValidHourExport(hourData.weekday, hourData.startMinutes, durationMinutes)) {
                counters.malformedSkipped++
                return@forEach
            }
            val weekday = DayOfWeek(hourData.weekday)
            val overlapsExisting =
                mutableHours.any { existing ->
                    existing.weekday == weekday &&
                        ScheduleRules.overlaps(
                            hourData.startMinutes,
                            hourData.startMinutes + durationMinutes,
                            existing.startMinutes,
                            existing.startMinutes + existing.durationMinutes
                        )
                }
            if (overlapsExisting) {
                counters.hoursOverlapSkipped++
                return@forEach
            }
            scheduleRepository
                .insertHour(weekday, hourData.startMinutes, durationMinutes)
                .onSuccess { newId ->
                    val hour =
                        AvailableLessonHour(
                            id = newId,
                            weekday = weekday,
                            startMinutes = hourData.startMinutes,
                            durationMinutes = durationMinutes
                        )
                    mutableHours += hour
                    hourIdByKey[weekday.isoDayNumber to hourData.startMinutes] = newId
                    counters.hoursAdded++
                }
                .onFailure {
                    counters.hoursOverlapSkipped++
                }
        }
    }

    suspend fun mergeBusyAppointments(
        studentId: Int,
        busyAppointments: List<BusyAppointmentExportData>,
        existingBusy: List<BusyAppointment>,
        counters: ImportCounters
    ) {
        val existingKeys =
            existingBusy
                .filter { it.studentId == studentId }
                .map {
                    Triple(it.weekday.isoDayNumber, it.startMinutes, it.durationMinutes)
                }
                .toMutableSet()
        busyAppointments.forEach { busyData ->
            if (!isValidBusyExport(busyData)) {
                counters.malformedSkipped++
                return@forEach
            }
            val key = Triple(busyData.weekday, busyData.startMinutes, busyData.durationMinutes)
            if (!existingKeys.add(key)) {
                return@forEach
            }
            scheduleRepository
                .insertBusyAppointment(
                    studentId = studentId,
                    weekday = DayOfWeek(busyData.weekday),
                    startMinutes = busyData.startMinutes,
                    durationMinutes = busyData.durationMinutes
                )
                .onSuccess { report ->
                    counters.lessonsRemovedBusyWins += report.removedStudentIds.size
                }
                .onFailure { }
        }
    }

    fun collectPlannedLessons(
        studentsToImport: List<StudentExportData>,
        studentIdByName: Map<String, Int>,
        hourIdByKey: Map<Pair<Int, Int>, Int>,
        durationByHourId: Map<Int, Int>,
        assignments: List<LessonAssignment>,
        busy: List<BusyAppointment>,
        counters: ImportCounters
    ): MutableMap<Int, MutableSet<PlannedLesson>> {
        val plannedByHour = mutableMapOf<Int, MutableSet<PlannedLesson>>()
        val alreadyPlanned = mutableSetOf<Pair<Int, Int>>()
        val existingLessonWeekdays =
            assignments.map { it.studentId to it.weekday.isoDayNumber }.toSet()

        studentsToImport.forEach { studentData ->
            val studentId = studentIdByName[studentData.name] ?: return@forEach
            studentData.lessonAssignments.forEach { lessonData ->
                if (!isValidLessonExport(lessonData)) {
                    counters.malformedSkipped++
                    return@forEach
                }
                val hourId = hourIdByKey[lessonData.weekday to lessonData.startMinutes]
                if (hourId == null) {
                    counters.lessonsSkipped++
                    return@forEach
                }
                if (
                    alreadyPlanned.contains(studentId to lessonData.weekday) ||
                    existingLessonWeekdays.contains(studentId to lessonData.weekday)
                ) {
                    counters.lessonsSkipped++
                    return@forEach
                }
                if (
                    hasBusyConflict(
                        studentId = studentId,
                        weekday = lessonData.weekday,
                        startMinutes = lessonData.startMinutes,
                        durationMinutes =
                            durationByHourId[hourId]
                                ?: ScheduleRules.DEFAULT_NEW_HOUR_DURATION_MINUTES,
                        busy = busy
                    )
                ) {
                    counters.lessonsSkipped++
                    return@forEach
                }
                plannedByHour.getOrPut(hourId) { mutableSetOf() }
                    .add(PlannedLesson(studentId, lessonData.weekday))
                alreadyPlanned.add(studentId to lessonData.weekday)
            }
        }
        return plannedByHour
    }

    suspend fun raiseExistingHourCapacities(
        existingHours: List<AvailableLessonHour>,
        assignments: List<LessonAssignment>,
        plannedByHour: Map<Int, Set<PlannedLesson>>
    ) {
        existingHours.forEach { hour ->
            val planned = plannedByHour[hour.id] ?: return@forEach
            val currentAssigned = assignments.count { it.availableHourId == hour.id }
            val requiredCapacity = currentAssigned + planned.size
            if (requiredCapacity > hour.maxStudents) {
                // Grow the block's length to the smallest valid step that fits.
                val grownDuration =
                    (requiredCapacity * ScheduleRules.SLOT_DURATION_MINUTES)
                        .coerceAtMost(ScheduleRules.MAX_HOUR_DURATION_MINUTES)
                scheduleRepository
                    // Import is an automatic merge, not a teacher edit, so there
                    // is nobody to confirm with — commit directly.
                    .updateHour(
                        hour.id,
                        hour.weekday,
                        hour.startMinutes,
                        grownDuration,
                        confirmed = true
                    )
                    .onFailure { }
            }
        }
    }

    suspend fun executePlannedLessons(
        plannedByHour: Map<Int, Set<PlannedLesson>>,
        counters: ImportCounters
    ) {
        plannedByHour.forEach { (hourId, plannedLessons) ->
            plannedLessons.forEach { planned ->
                if (!counters.admitLessons()) return@forEach
                scheduleRepository
                    .assignStudent(hourId, planned.studentId, DayOfWeek(planned.weekday))
                    .onSuccess { counters.lessonsAdded++ }
                    .onFailure { counters.lessonsSkipped++ }
            }
        }
    }

    private fun hasBusyConflict(
        studentId: Int,
        weekday: Int,
        startMinutes: Int,
        durationMinutes: Int,
        busy: List<BusyAppointment>
    ): Boolean {
        val lessonEnd = startMinutes + durationMinutes
        return busy.any { appointment ->
            appointment.studentId == studentId &&
                appointment.weekday.isoDayNumber == weekday &&
                ScheduleRules.overlaps(
                    startMinutes,
                    lessonEnd,
                    appointment.startMinutes,
                    appointment.startMinutes + appointment.durationMinutes
                )
        }
    }

    /** A block must sit on a real weekday and must not run past midnight (SC-012). */
    private fun isValidHourExport(
        weekday: Int,
        startMinutes: Int,
        durationMinutes: Int
    ): Boolean = weekday in 1..7 && ScheduleRules.isHourStartValid(startMinutes, durationMinutes)

    private fun isValidBusyExport(busy: BusyAppointmentExportData): Boolean =
        busy.weekday in 1..7 &&
            ScheduleRules.isBusyStartValid(busy.startMinutes) &&
            busy.durationMinutes in ScheduleRules.MIN_BUSY_DURATION_MINUTES..ScheduleRules.MAX_BUSY_DURATION_MINUTES &&
            ScheduleRules.isBusyDurationValid(busy.startMinutes, busy.durationMinutes)

    private fun isValidLessonExport(lesson: LessonAssignmentExportData): Boolean =
        lesson.weekday in 1..7 &&
            // A lesson must start where even the shortest block still fits.
            ScheduleRules.isHourStartValid(
                lesson.startMinutes,
                ScheduleRules.MIN_HOUR_DURATION_MINUTES
            )
}

internal data class PlannedLesson(
    val studentId: Int,
    val weekday: Int
)

internal class ImportCounters {
    var newCount: Int = 0
    var mergedCount: Int = 0
    var datesProcessed: Int = 0
    var datesSkipped: Int = 0
    var hoursAdded: Int = 0
    var hoursOverlapSkipped: Int = 0
    var lessonsAdded: Int = 0
    var lessonsRemovedBusyWins: Int = 0
    var lessonsSkipped: Int = 0
    var malformedSkipped: Int = 0

    /** Entries dropped because a record cap was reached (FR-056 … FR-059). */
    var studentsCapped: Int = 0
    var hoursCapped: Int = 0
    var lessonsCapped: Int = 0
    var datesCapped: Int = 0

    val totalSkipped: Int
        get() =
            datesSkipped + hoursOverlapSkipped + lessonsSkipped + malformedSkipped +
                studentsCapped + hoursCapped + lessonsCapped + datesCapped
}