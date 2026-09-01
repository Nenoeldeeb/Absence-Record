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
            if (!isValidHourExport(hourData)) {
                counters.malformedSkipped++
                return@forEach
            }
            val weekday = DayOfWeek(hourData.weekday)
            val overlapsExisting =
                mutableHours.any { existing ->
                    existing.weekday == weekday &&
                        ScheduleRules.overlaps(
                            hourData.startMinutes,
                            hourData.startMinutes + ScheduleRules.LESSON_DURATION_MINUTES,
                            existing.startMinutes,
                            existing.startMinutes + ScheduleRules.LESSON_DURATION_MINUTES
                        )
                }
            if (overlapsExisting) {
                counters.hoursOverlapSkipped++
                return@forEach
            }
            scheduleRepository
                .insertHour(weekday, hourData.startMinutes, hourData.maxStudents)
                .onSuccess { newId ->
                    val hour =
                        AvailableLessonHour(
                            id = newId,
                            weekday = weekday,
                            startMinutes = hourData.startMinutes,
                            maxStudents = hourData.maxStudents
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
                if (hasBusyConflict(studentId, lessonData.weekday, lessonData.startMinutes, busy)) {
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
                scheduleRepository
                    .updateHour(hour.id, hour.weekday, hour.startMinutes, requiredCapacity)
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
        busy: List<BusyAppointment>
    ): Boolean {
        val lessonEnd = startMinutes + ScheduleRules.LESSON_DURATION_MINUTES
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

    private fun isValidHourExport(hour: AvailableHourExportData): Boolean =
        hour.weekday in 1..7 &&
            ScheduleRules.isHourStartValid(hour.startMinutes) &&
            hour.maxStudents >= 1

    private fun isValidBusyExport(busy: BusyAppointmentExportData): Boolean =
        busy.weekday in 1..7 &&
            ScheduleRules.isBusyStartValid(busy.startMinutes) &&
            busy.durationMinutes in ScheduleRules.MIN_BUSY_DURATION_MINUTES..ScheduleRules.MAX_BUSY_DURATION_MINUTES &&
            ScheduleRules.isBusyDurationValid(busy.startMinutes, busy.durationMinutes)

    private fun isValidLessonExport(lesson: LessonAssignmentExportData): Boolean =
        lesson.weekday in 1..7 && ScheduleRules.isHourStartValid(lesson.startMinutes)
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
}