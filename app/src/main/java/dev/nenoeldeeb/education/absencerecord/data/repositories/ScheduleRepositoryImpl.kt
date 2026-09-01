package dev.nenoeldeeb.education.absencerecord.data.repositories

import dev.nenoeldeeb.education.absencerecord.data.datasources.local.daos.ScheduleDao
import dev.nenoeldeeb.education.absencerecord.data.mappers.toAvailableLessonHour
import dev.nenoeldeeb.education.absencerecord.data.mappers.toBusyAppointment
import dev.nenoeldeeb.education.absencerecord.data.mappers.toLessonAssignment
import dev.nenoeldeeb.education.absencerecord.domain.models.AssignmentRemovalReport
import dev.nenoeldeeb.education.absencerecord.domain.models.AvailableLessonHour
import dev.nenoeldeeb.education.absencerecord.domain.models.BusyAppointment
import dev.nenoeldeeb.education.absencerecord.domain.models.HourWithOccupancy
import dev.nenoeldeeb.education.absencerecord.domain.models.LessonAssignment
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentLessonEntry
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentScheduleView
import dev.nenoeldeeb.education.absencerecord.domain.repositories.ScheduleRepository
import dev.nenoeldeeb.education.absencerecord.domain.services.ScheduleRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.isoDayNumber

class ScheduleRepositoryImpl(
    private val scheduleDao: ScheduleDao
) : ScheduleRepository {
    override fun observeHours(): Flow<Result<List<AvailableLessonHour>>> =
        scheduleDao.observeHours()
            .map { entities -> Result.success(entities.map { it.toAvailableLessonHour() }) }
            .catch { emit(Result.failure(StudentError.Database)) }

    override fun observeHoursForWeekday(weekday: DayOfWeek): Flow<Result<List<HourWithOccupancy>>> =
        combine(
            scheduleDao.observeHoursForWeekday(weekday.isoDayNumber),
            scheduleDao.observeAssignments()
        ) { hourEntities, assignmentEntities ->
            val hours = hourEntities.map { it.toAvailableLessonHour() }
            val assignments = assignmentEntities.map { it.toLessonAssignment() }
            val hoursWithOccupancy =
                hours.map { hour ->
                    val assignedCount = assignments.count { it.availableHourId == hour.id }
                    HourWithOccupancy(
                        hour = hour,
                        endMinutes = hour.startMinutes + ScheduleRules.LESSON_DURATION_MINUTES,
                        assignedStudentIds =
                            assignments.filter { it.availableHourId == hour.id }.map { it.studentId },
                        assignedCount = assignedCount,
                        maxStudents = hour.maxStudents,
                        remainingSlots = hour.maxStudents - assignedCount,
                        isFull = assignedCount >= hour.maxStudents
                    )
                }
            Result.success(hoursWithOccupancy)
        }.catch { emit(Result.failure(StudentError.Database)) }

    override fun observeAssignments(): Flow<Result<List<LessonAssignment>>> =
        scheduleDao.observeAssignments()
            .map { entities -> Result.success(entities.map { it.toLessonAssignment() }) }
            .catch { emit(Result.failure(StudentError.Database)) }

    override fun observeBusyAppointments(): Flow<Result<List<BusyAppointment>>> =
        scheduleDao.observeBusyAppointments()
            .map { entities -> Result.success(entities.map { it.toBusyAppointment() }) }
            .catch { emit(Result.failure(StudentError.Database)) }

    override fun observeStudentSchedule(studentId: Int): Flow<Result<StudentScheduleView>> =
        combine(
            scheduleDao.observeAssignments(),
            scheduleDao.observeBusyAppointmentsForStudent(studentId),
            scheduleDao.observeHours()
        ) { assignmentEntities, busyEntities, hourEntities ->
            val hours = hourEntities.associate { it.id to it.toAvailableLessonHour() }
            val lessons =
                assignmentEntities
                    .filter { it.studentId == studentId }
                    .mapNotNull { assignment ->
                        hours[assignment.availableHourId]?.let { hour ->
                            StudentLessonEntry(
                                weekday = hour.weekday,
                                startMinutes = hour.startMinutes,
                                endMinutes = hour.startMinutes + ScheduleRules.LESSON_DURATION_MINUTES
                            )
                        }
                    }
            Result.success(
                StudentScheduleView(
                    studentId = studentId,
                    lessons = lessons,
                    busy = busyEntities.map { it.toBusyAppointment() }
                )
            )
        }.catch { emit(Result.failure(StudentError.Database)) }

    override suspend fun insertHour(
        weekday: DayOfWeek,
        startMinutes: Int,
        maxStudents: Int
    ): Result<Int> =
        runSafely {
            scheduleDao.insertHour(weekday.isoDayNumber, startMinutes, maxStudents).toInt()
        }

    override suspend fun updateHour(
        id: Int,
        weekday: DayOfWeek,
        startMinutes: Int,
        maxStudents: Int
    ): Result<AssignmentRemovalReport> =
        runSafely {
            scheduleDao.updateHour(id, weekday.isoDayNumber, startMinutes, maxStudents)
        }

    override suspend fun deleteHour(id: Int): Result<Unit> =
        runSafely {
            scheduleDao.deleteHour(id)
        }

    override suspend fun assignStudent(
        hourId: Int,
        studentId: Int,
        weekday: DayOfWeek
    ): Result<Unit> =
        runSafely {
            scheduleDao.assignStudent(hourId, studentId, weekday.isoDayNumber)
        }

    override suspend fun unassignStudent(
        hourId: Int,
        studentId: Int
    ): Result<Unit> =
        runSafely {
            scheduleDao.unassignStudent(hourId, studentId)
        }

    override suspend fun insertBusyAppointment(
        studentId: Int,
        weekday: DayOfWeek,
        startMinutes: Int,
        durationMinutes: Int
    ): Result<AssignmentRemovalReport> =
        runSafely {
            scheduleDao.insertBusyAppointment(studentId, weekday.isoDayNumber, startMinutes, durationMinutes)
        }

    override suspend fun updateBusyAppointment(
        id: Int,
        startMinutes: Int,
        durationMinutes: Int
    ): Result<AssignmentRemovalReport> =
        runSafely {
            scheduleDao.updateBusyAppointment(id, startMinutes, durationMinutes)
        }

    override suspend fun deleteBusyAppointment(id: Int): Result<Unit> =
        runSafely {
            scheduleDao.deleteBusyAppointment(id)
        }

    private suspend fun <T> runSafely(block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: StudentError) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(StudentError.Database)
        }
}