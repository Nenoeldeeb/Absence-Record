package dev.nenoeldeeb.education.absencerecord.data.repositories

import dev.nenoeldeeb.education.absencerecord.data.datasources.local.daos.ScheduleDao
import dev.nenoeldeeb.education.absencerecord.data.mappers.toAvailableLessonHour
import dev.nenoeldeeb.education.absencerecord.data.mappers.toBusyAppointment
import dev.nenoeldeeb.education.absencerecord.data.mappers.toLessonAssignment
import dev.nenoeldeeb.education.absencerecord.domain.models.AssignmentRemovalReport
import dev.nenoeldeeb.education.absencerecord.domain.models.AvailableLessonHour
import dev.nenoeldeeb.education.absencerecord.domain.models.BusyAppointment
import dev.nenoeldeeb.education.absencerecord.domain.models.HourReplanResult
import dev.nenoeldeeb.education.absencerecord.domain.models.HourWithOccupancy
import dev.nenoeldeeb.education.absencerecord.domain.models.LessonAssignment
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentLessonEntry
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentScheduleView
import dev.nenoeldeeb.education.absencerecord.domain.repositories.ScheduleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
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
            scheduleDao.observeAssignments(),
            scheduleDao.observeBusyAppointmentsForWeekday(weekday.isoDayNumber),
            scheduleDao.observeStudentNames()
        ) { hourEntities, assignmentEntities, busyEntities, nameRows ->
            // The running order and every ten-minute slot are DERIVED here on
            // each emission, off the main thread — never read from a stored
            // position and never cached (FR-025, FR-060, FR-061).
            Result.success(
                DayPlanningInputs(
                    hours = hourEntities,
                    assignments = assignmentEntities,
                    busy = busyEntities,
                    names = nameRows
                ).planAllHours()
            )
        }.flowOn(Dispatchers.Default).catch { emit(Result.failure(StudentError.Database)) }

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
            scheduleDao.observeHours(),
            scheduleDao.observeBusyAppointments(),
            scheduleDao.observeStudentNames()
        ) { assignmentEntities, busyEntities, hourEntities, allBusyEntities, nameRows ->
            val hours = hourEntities.associateBy { it.id }
            // The student's own ten minutes depend on where the WHOLE block
            // lands them, so the plan needs every occupant's commitments and
            // name, not just this student's (FR-018, FR-019).
            val inputs =
                DayPlanningInputs(
                    hours = hourEntities,
                    assignments = assignmentEntities,
                    busy = allBusyEntities,
                    names = nameRows
                )
            val lessons =
                assignmentEntities
                    .filter { it.studentId == studentId }
                    .mapNotNull { assignment ->
                        hours[assignment.availableHourId]?.let { hour ->
                            val slot = inputs.slotForStudent(hour, studentId)
                            StudentLessonEntry(
                                weekday = hour.toAvailableLessonHour().weekday,
                                startMinutes = hour.startMinutes,
                                endMinutes = hour.startMinutes + hour.durationMinutes,
                                slotStartMinutes = slot?.first,
                                slotEndMinutes = slot?.second
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
        }.flowOn(Dispatchers.Default).catch { emit(Result.failure(StudentError.Database)) }

    override suspend fun insertHour(
        weekday: DayOfWeek,
        startMinutes: Int,
        durationMinutes: Int
    ): Result<Int> =
        runSafely {
            scheduleDao.insertHour(weekday.isoDayNumber, startMinutes, durationMinutes).toInt()
        }

    override suspend fun updateHour(
        id: Int,
        weekday: DayOfWeek,
        startMinutes: Int,
        durationMinutes: Int,
        confirmed: Boolean
    ): Result<HourReplanResult> =
        runSafely {
            scheduleDao.updateHour(id, weekday.isoDayNumber, startMinutes, durationMinutes, confirmed)
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