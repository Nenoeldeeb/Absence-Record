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
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ScheduleDao {
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

    @Query(
        """
        SELECT COUNT(*) FROM available_lesson_hours
        WHERE weekday = :weekday
          AND startMinutes < :endMinutes
          AND startMinutes + :lessonDuration > :startMinutes
          AND id != :excludeId
        """
    )
    abstract suspend fun countHoursOverlapping(
        weekday: Int,
        startMinutes: Int,
        endMinutes: Int,
        lessonDuration: Int,
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
        maxStudents: Int
    ): Long {
        validateHour(weekday, startMinutes, maxStudents)
        if (hasOverlappingHour(weekday, startMinutes, excludeId = 0)) {
            throw StudentError.HourOverlap
        }
        return insertAvailableHour(
            AvailableLessonHourEntity(
                weekday = weekday,
                startMinutes = startMinutes,
                maxStudents = maxStudents
            )
        )
    }

    @Transaction
    open suspend fun updateHour(
        id: Int,
        weekday: Int,
        startMinutes: Int,
        maxStudents: Int
    ): AssignmentRemovalReport {
        getAvailableHourById(id) ?: throw StudentError.Validation("Lesson hour not found")
        validateHour(weekday, startMinutes, maxStudents)
        if (hasOverlappingHour(weekday, startMinutes, excludeId = id)) {
            throw StudentError.HourOverlap
        }
        val assignedCount = countAssignmentsForHour(id)
        if (maxStudents < assignedCount) {
            throw StudentError.MaxBelowAssigned
        }
        updateAvailableHour(
            AvailableLessonHourEntity(
                id = id,
                weekday = weekday,
                startMinutes = startMinutes,
                maxStudents = maxStudents
            )
        )
        return removeBusyConflictingAssignments(weekday, id)
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
        if (countAssignmentsForHour(hourId) >= hour.maxStudents) {
            throw StudentError.HourFull
        }
        if (getAssignmentsForStudentAndWeekday(studentId, weekday).isNotEmpty()) {
            throw StudentError.AssignmentExists
        }
        if (hasBusyConflict(studentId, weekday, hour.startMinutes)) {
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