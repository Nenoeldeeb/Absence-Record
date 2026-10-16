package dev.nenoeldeeb.education.absencerecord.data.datasources.local.daos

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.entities.AvailableLessonHourEntity
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.entities.BusyAppointmentEntity
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.entities.LessonAssignmentEntity
import dev.nenoeldeeb.education.absencerecord.domain.models.AssignmentRemovalReport
import dev.nenoeldeeb.education.absencerecord.domain.models.HourReplanResult
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError
import dev.nenoeldeeb.education.absencerecord.domain.services.LessonSlotPlanner
import dev.nenoeldeeb.education.absencerecord.domain.services.ScheduleRules
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ScheduleDao {
    /** Student names drive the alphabetical tie-break in the running order. */
    @Query("SELECT id, name FROM students WHERE id IN (:ids)")
    abstract suspend fun getStudentNamesByIds(ids: List<Int>): List<StudentNameRow>

    @Query("SELECT * FROM available_lesson_hours ORDER BY startMinutes ASC")
    abstract fun observeHours(): Flow<List<AvailableLessonHourEntity>>

    @Query("SELECT * FROM available_lesson_hours WHERE weekday = :weekday ORDER BY startMinutes ASC")
    abstract fun observeHoursForWeekday(weekday: Int): Flow<List<AvailableLessonHourEntity>>

    @Query("SELECT * FROM lesson_assignments")
    abstract fun observeAssignments(): Flow<List<LessonAssignmentEntity>>

    @Query("SELECT * FROM lesson_assignments WHERE availableHourId = :hourId")
    abstract fun observeAssignmentsForHour(hourId: Int): Flow<List<LessonAssignmentEntity>>

    @Query("SELECT * FROM busy_appointments")
    abstract fun observeBusyAppointments(): Flow<List<BusyAppointmentEntity>>

    @Query("SELECT * FROM busy_appointments WHERE studentId = :studentId")
    abstract fun observeBusyAppointmentsForStudent(studentId: Int): Flow<List<BusyAppointmentEntity>>

    /**
     * Commitments for one day across every student.
     *
     * The read path needs all of them at once to derive a block's running order
     * (FR-019): querying per student would mean one round trip per occupant on
     * every emission.
     */
    @Query("SELECT * FROM busy_appointments WHERE weekday = :weekday")
    abstract fun observeBusyAppointmentsForWeekday(weekday: Int): Flow<List<BusyAppointmentEntity>>

    /**
     * Student names for the derived order's alphabetical tie-break (FR-022, FR-023).
     *
     * Observable because a rename must re-derive the running order; a one-shot
     * read would leave the order stale until the block was next edited.
     */
    @Query("SELECT id, name FROM students")
    abstract fun observeStudentNames(): Flow<List<StudentNameRow>>

    @Query("SELECT * FROM available_lesson_hours WHERE id = :id")
    abstract suspend fun getAvailableHourById(id: Int): AvailableLessonHourEntity?

    @Query("SELECT * FROM busy_appointments WHERE id = :id")
    abstract suspend fun getBusyAppointmentById(id: Int): BusyAppointmentEntity?

    @Query(
        "SELECT * FROM busy_appointments WHERE studentId = :studentId AND weekday = :weekday"
    )
    abstract suspend fun getBusyAppointmentsForStudentAndWeekday(
        studentId: Int,
        weekday: Int
    ): List<BusyAppointmentEntity>

    @Query("SELECT * FROM lesson_assignments WHERE availableHourId = :hourId")
    abstract suspend fun getAssignmentsForHour(hourId: Int): List<LessonAssignmentEntity>

    @Query("SELECT * FROM lesson_assignments WHERE availableHourId = :hourId AND studentId = :studentId LIMIT 1")
    abstract suspend fun getAssignment(
        hourId: Int,
        studentId: Int
    ): LessonAssignmentEntity?

    @Query(
        "SELECT * FROM lesson_assignments WHERE studentId = :studentId AND weekday = :weekday"
    )
    abstract suspend fun getAssignmentsForStudentAndWeekday(
        studentId: Int,
        weekday: Int
    ): List<LessonAssignmentEntity>

    @Query("SELECT COUNT(*) FROM lesson_assignments WHERE availableHourId = :hourId")
    abstract suspend fun countAssignmentsForHour(hourId: Int): Int

    /**
     * Same-day blocks whose own extent overlaps the proposed one (FR-008).
     *
     * Both lengths matter and they are *different* lengths: `startMinutes` and
     * `durationMinutes` belong to the existing row, while `:startMinutes` and
     * `:endMinutes` describe the block being saved. Comparing the existing row
     * against the proposed block's duration silently misses any overlap where
     * the proposed block is shorter and later — see
     * `ScheduleDaoHourOverlapTest`.
     */
    @Query(
        """
        SELECT COUNT(*) FROM available_lesson_hours
        WHERE weekday = :weekday
          AND startMinutes < :endMinutes
          AND startMinutes + durationMinutes > :startMinutes
          AND id != :excludeId
        """
    )
    abstract suspend fun countHoursOverlapping(
        weekday: Int,
        startMinutes: Int,
        endMinutes: Int,
        excludeId: Int
    ): Int

    @Insert
    abstract suspend fun insertAvailableHour(hour: AvailableLessonHourEntity): Long

    @Insert
    abstract suspend fun insertAssignmentEntity(assignment: LessonAssignmentEntity): Long

    @Insert
    abstract suspend fun insertBusyAppointmentEntity(appointment: BusyAppointmentEntity): Long

    @Update
    abstract suspend fun updateAvailableHour(hour: AvailableLessonHourEntity)

    @Update
    abstract suspend fun updateBusyAppointmentEntity(appointment: BusyAppointmentEntity)

    @Query("DELETE FROM available_lesson_hours WHERE id = :id")
    abstract suspend fun deleteHourById(id: Int)

    @Query("DELETE FROM busy_appointments WHERE id = :id")
    abstract suspend fun deleteBusyAppointmentById(id: Int)

    @Query("DELETE FROM lesson_assignments WHERE availableHourId = :hourId AND studentId = :studentId")
    abstract suspend fun deleteAssignment(
        hourId: Int,
        studentId: Int
    )

    @Transaction
    open suspend fun insertHour(
        weekday: Int,
        startMinutes: Int,
        durationMinutes: Int
    ): Long {
        validateHour(weekday, startMinutes, durationMinutes)
        if (hasOverlappingHour(weekday, startMinutes, durationMinutes, excludeId = 0)) {
            throw StudentError.HourOverlap
        }
        return insertAvailableHour(
            AvailableLessonHourEntity(
                weekday = weekday,
                startMinutes = startMinutes,
                durationMinutes = durationMinutes
            )
        )
    }

    @Transaction
    open suspend fun updateHour(
        id: Int,
        weekday: Int,
        startMinutes: Int,
        durationMinutes: Int,
        confirmed: Boolean
    ): HourReplanResult {
        getAvailableHourById(id) ?: throw StudentError.Validation("Lesson hour not found")
        validateHour(weekday, startMinutes, durationMinutes)
        if (hasOverlappingHour(weekday, startMinutes, durationMinutes, excludeId = id)) {
            throw StudentError.HourOverlap
        }
        val plan = planHourWith(weekday, startMinutes, durationMinutes, id)
        val lost = closedDropSet(weekday, startMinutes, durationMinutes, id)

        // Nothing is written until the teacher confirms (FR-011, FR-013).
        if (lost.isNotEmpty() && !confirmed) {
            return HourReplanResult.NeedsConfirmation(plan.plan, lost)
        }
        updateAvailableHour(
            AvailableLessonHourEntity(
                id = id,
                weekday = weekday,
                startMinutes = startMinutes,
                durationMinutes = durationMinutes
            )
        )
        // Confirming must ACTUALLY unassign the students the teacher agreed to
        // lose, not merely report them (FR-013).
        lost.forEach { deleteAssignment(id, it.studentId) }
        val removed =
            lost.map { it.studentId } + removeBusyConflictingAssignments(weekday, id).removedStudentIds
        return HourReplanResult.Applied(removed.distinct())
    }

    @Transaction
    open suspend fun deleteHour(id: Int) {
        getAvailableHourById(id) ?: throw StudentError.Validation("Lesson hour not found")
        deleteHourById(id)
    }

    @Transaction
    open suspend fun assignStudent(
        hourId: Int,
        studentId: Int,
        weekday: Int
    ) {
        val hour =
            getAvailableHourById(hourId)
                ?: throw StudentError.Validation("Lesson hour not found")
        // The whole roster is re-planned, not just the newcomer: inserting a
        // student earlier in the order pushes the others later, and one of them
        // landing on their own commitment must refuse the whole addition
        // (FR-031, FR-033, FR-035).
        if (countAssignmentsForHour(hourId) >= ScheduleRules.capacityFor(hour.durationMinutes)) {
            throw StudentError.HourFull
        }
        if (getAssignmentsForStudentAndWeekday(studentId, weekday).isNotEmpty()) {
            throw StudentError.AssignmentExists
        }
        val prospective =
            studentInputsForHour(hourId, hour.weekday) + studentInputFor(studentId, hour.weekday)
        val result = LessonSlotPlanner.plan(hour.startMinutes, hour.durationMinutes, prospective)
        if (result.dropped.isNotEmpty() || result.conflicts.isNotEmpty()) {
            throw StudentError.BusyConflict
        }
        try {
            insertAssignmentEntity(
                LessonAssignmentEntity(
                    availableHourId = hourId,
                    studentId = studentId,
                    weekday = weekday
                )
            )
        } catch (e: SQLiteConstraintException) {
            throw StudentError.AssignmentExists
        }
    }

    @Transaction
    open suspend fun unassignStudent(
        hourId: Int,
        studentId: Int
    ) {
        if (getAssignment(hourId, studentId) == null) {
            throw StudentError.Validation("Assignment not found")
        }
        deleteAssignment(hourId, studentId)
    }

    @Transaction
    open suspend fun insertBusyAppointment(
        studentId: Int,
        weekday: Int,
        startMinutes: Int,
        durationMinutes: Int
    ): AssignmentRemovalReport {
        validateBusy(startMinutes, durationMinutes)
        insertBusyAppointmentEntity(
            BusyAppointmentEntity(
                studentId = studentId,
                weekday = weekday,
                startMinutes = startMinutes,
                durationMinutes = durationMinutes
            )
        )
        return removeConflictingAssignmentsForBusy(studentId, weekday, startMinutes, durationMinutes)
    }

    @Transaction
    open suspend fun updateBusyAppointment(
        id: Int,
        startMinutes: Int,
        durationMinutes: Int
    ): AssignmentRemovalReport {
        val existing =
            getBusyAppointmentById(id)
                ?: throw StudentError.Validation("Busy appointment not found")
        validateBusy(startMinutes, durationMinutes)
        updateBusyAppointmentEntity(existing.copy(startMinutes = startMinutes, durationMinutes = durationMinutes))
        return removeConflictingAssignmentsForBusy(existing.studentId, existing.weekday, startMinutes, durationMinutes)
    }

    @Transaction
    open suspend fun deleteBusyAppointment(id: Int) {
        getBusyAppointmentById(id) ?: throw StudentError.Validation("Busy appointment not found")
        deleteBusyAppointmentById(id)
    }
}