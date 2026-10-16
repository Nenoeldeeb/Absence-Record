package dev.nenoeldeeb.education.absencerecord.data.repositories

import dev.nenoeldeeb.education.absencerecord.data.datasources.local.daos.StudentNameRow
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.entities.AvailableLessonHourEntity
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.entities.BusyAppointmentEntity
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.entities.LessonAssignmentEntity
import dev.nenoeldeeb.education.absencerecord.data.mappers.toAvailableLessonHour
import dev.nenoeldeeb.education.absencerecord.domain.models.HourWithOccupancy
import dev.nenoeldeeb.education.absencerecord.domain.services.LessonSlotPlanner

/**
 * Everything one day's running order is derived from, gathered up front.
 *
 * Bundling the inputs makes the derivation a pure function of this type, which
 * is what keeps it testable and off the database.
 */
internal data class DayPlanningInputs(
    val hours: List<AvailableLessonHourEntity>,
    val assignments: List<LessonAssignmentEntity>,
    val busy: List<BusyAppointmentEntity>,
    val names: List<StudentNameRow>
) {
    private val namesById: Map<Int, String> = names.associate { it.id to it.name }

    private val busyByStudentAndWeekday: Map<Pair<Int, Int>, List<LessonSlotPlanner.BusyWindow>> =
        busy.groupBy { it.studentId to it.weekday }
            .mapValues { (_, rows) ->
                rows.map { LessonSlotPlanner.BusyWindow(it.startMinutes, it.durationMinutes) }
            }

    private val assignmentsByHour: Map<Int, List<Int>> =
        assignments.groupBy { it.availableHourId }
            .mapValues { (_, rows) -> rows.map { it.studentId } }

    /**
     * One block, with its students re-sequenced and slotted.
     *
     * Membership comes from the stored assignments; their ORDER and each one's
     * ten minutes are derived here, on every read (FR-025, FR-060). A student
     * whose name is missing keeps a stable `Student <id>` stand-in so the
     * alphabetical tie-break stays deterministic rather than depending on the
     * order rows happened to come back in.
     */
    fun planHour(hour: AvailableLessonHourEntity): HourWithOccupancy {
        val studentIds = assignmentsByHour[hour.id].orEmpty()
        val result =
            LessonSlotPlanner.plan(
                hour.startMinutes,
                hour.durationMinutes,
                studentIds.map { id -> studentInput(id, hour.weekday) }
            )
        return HourWithOccupancy(
            hour = hour.toAvailableLessonHour(),
            endMinutes = hour.startMinutes + hour.durationMinutes,
            assignedStudentIds = result.plan.slots.map { it.studentId },
            assignedCount = studentIds.size,
            slots = result.plan.slots,
            // Carried so a newcomer's prospective slot can be derived without
            // re-querying: where they land depends on where everyone lands.
            studentBusy =
                studentIds.associateWith { id ->
                    busyByStudentAndWeekday[id to hour.weekday].orEmpty()
                },
            studentNames = studentIds.associateWith { id -> namesById[id] ?: "Student $id" }
        )
    }

    fun planAllHours(): List<HourWithOccupancy> = hours.map { planHour(it) }

    /** This student's slot inside [hour], derived from the block's whole roster. */
    fun slotForStudent(
        hour: AvailableLessonHourEntity,
        studentId: Int
    ): Pair<Int, Int>? =
        planHour(hour).slots
            .firstOrNull { it.studentId == studentId }
            ?.let { it.startMinutes to it.endMinutes }

    private fun studentInput(
        studentId: Int,
        weekday: Int
    ) = LessonSlotPlanner.StudentInput(
        id = studentId,
        name = namesById[studentId] ?: "Student $studentId",
        busy = busyByStudentAndWeekday[studentId to weekday].orEmpty()
    )
}