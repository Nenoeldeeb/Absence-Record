package dev.nenoeldeeb.education.absencerecord.data.repositories

import dev.nenoeldeeb.education.absencerecord.data.datasources.local.daos.ScheduleDao
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.entities.AvailableLessonHourEntity
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.entities.BusyAppointmentEntity
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.entities.LessonAssignmentEntity
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScheduleRepositoryImplObserveTest {
    private val scheduleDao: ScheduleDao = mockk()
    private val repository = ScheduleRepositoryImpl(scheduleDao)

    @Test
    fun `observeHours maps entities on success`() =
        runTest {
            val entity =
                AvailableLessonHourEntity(id = 1, weekday = 6, startMinutes = 540, maxStudents = 5)
            every { scheduleDao.observeHours() } returns flowOf(listOf(entity))

            val result = repository.observeHours().first()

            assertTrue(result.isSuccess)
            val hour = result.getOrNull()!!.first()
            assertEquals(1, hour.id)
            assertEquals(DayOfWeek.SATURDAY, hour.weekday)
            assertEquals(540, hour.startMinutes)
            assertEquals(5, hour.maxStudents)
        }

    @Test
    fun `observeHours maps dao failure to Database error`() =
        runTest {
            val errorFlow = flow<List<AvailableLessonHourEntity>> { throw RuntimeException("boom") }
            every { scheduleDao.observeHours() } returns errorFlow

            val result = repository.observeHours().first()

            assertTrue(result.isFailure)
            assertEquals(StudentError.Database, result.exceptionOrNull())
        }

    @Test
    fun `observeHoursForWeekday combines occupancy`() =
        runTest {
            val hourEntity =
                AvailableLessonHourEntity(id = 1, weekday = 6, startMinutes = 540, maxStudents = 2)
            val assignment =
                LessonAssignmentEntity(id = 1, availableHourId = 1, studentId = 7, weekday = 6)
            every { scheduleDao.observeHoursForWeekday(6) } returns flowOf(listOf(hourEntity))
            every { scheduleDao.observeAssignments() } returns flowOf(listOf(assignment))

            val result = repository.observeHoursForWeekday(DayOfWeek.SATURDAY).first()

            assertTrue(result.isSuccess)
            val hour = result.getOrNull()!!.single()
            assertEquals(600, hour.endMinutes)
            assertEquals(listOf(7), hour.assignedStudentIds)
            assertEquals(1, hour.assignedCount)
            assertEquals(1, hour.remainingSlots)
            assertEquals(false, hour.isFull)
        }

    @Test
    fun `observeHoursForWeekday maps failure to Database error`() =
        runTest {
            every { scheduleDao.observeHoursForWeekday(6) } returns
                flow { throw RuntimeException("boom") }
            every { scheduleDao.observeAssignments() } returns flowOf(emptyList())

            val result = repository.observeHoursForWeekday(DayOfWeek.SATURDAY).first()

            assertTrue(result.isFailure)
            assertEquals(StudentError.Database, result.exceptionOrNull())
        }

    @Test
    fun `observeAssignments maps entities on success`() =
        runTest {
            val entity =
                LessonAssignmentEntity(id = 1, availableHourId = 1, studentId = 7, weekday = 6)
            every { scheduleDao.observeAssignments() } returns flowOf(listOf(entity))

            val result = repository.observeAssignments().first()

            assertTrue(result.isSuccess)
            val assignment = result.getOrNull()!!.single()
            assertEquals(1, assignment.availableHourId)
            assertEquals(7, assignment.studentId)
            assertEquals(DayOfWeek.SATURDAY, assignment.weekday)
        }

    @Test
    fun `observeAssignments maps failure to Database error`() =
        runTest {
            every { scheduleDao.observeAssignments() } returns
                flow { throw RuntimeException("boom") }

            val result = repository.observeAssignments().first()

            assertTrue(result.isFailure)
            assertEquals(StudentError.Database, result.exceptionOrNull())
        }

    @Test
    fun `observeBusyAppointments maps entities on success`() =
        runTest {
            val entity =
                BusyAppointmentEntity(
                    id = 1,
                    studentId = 7,
                    weekday = 6,
                    startMinutes = 510,
                    durationMinutes = 60
                )
            every { scheduleDao.observeBusyAppointments() } returns flowOf(listOf(entity))

            val result = repository.observeBusyAppointments().first()

            assertTrue(result.isSuccess)
            val busy = result.getOrNull()!!.single()
            assertEquals(7, busy.studentId)
            assertEquals(510, busy.startMinutes)
            assertEquals(60, busy.durationMinutes)
        }

    @Test
    fun `observeBusyAppointments maps failure to Database error`() =
        runTest {
            every { scheduleDao.observeBusyAppointments() } returns
                flow { throw RuntimeException("boom") }

            val result = repository.observeBusyAppointments().first()

            assertTrue(result.isFailure)
            assertEquals(StudentError.Database, result.exceptionOrNull())
        }

    @Test
    fun `observeStudentSchedule aggregates lessons and busy`() =
        runTest {
            val hourEntity =
                AvailableLessonHourEntity(id = 1, weekday = 6, startMinutes = 540, maxStudents = 5)
            val assignment =
                LessonAssignmentEntity(id = 1, availableHourId = 1, studentId = 7, weekday = 6)
            val busyEntity =
                BusyAppointmentEntity(
                    id = 1,
                    studentId = 7,
                    weekday = 5,
                    startMinutes = 600,
                    durationMinutes = 30
                )
            every { scheduleDao.observeAssignments() } returns flowOf(listOf(assignment))
            every { scheduleDao.observeBusyAppointmentsForStudent(7) } returns flowOf(listOf(busyEntity))
            every { scheduleDao.observeHours() } returns flowOf(listOf(hourEntity))

            val result = repository.observeStudentSchedule(7).first()

            assertTrue(result.isSuccess)
            val view = result.getOrNull()!!
            assertEquals(1, view.lessons.size)
            assertEquals(540, view.lessons.single().startMinutes)
            assertEquals(600, view.lessons.single().endMinutes)
            assertEquals(1, view.busy.size)
        }

    @Test
    fun `observeStudentSchedule maps failure to Database error`() =
        runTest {
            every { scheduleDao.observeAssignments() } returns flow { throw RuntimeException("boom") }
            every { scheduleDao.observeBusyAppointmentsForStudent(7) } returns flowOf(emptyList())
            every { scheduleDao.observeHours() } returns flowOf(emptyList())

            val result = repository.observeStudentSchedule(7).first()

            assertTrue(result.isFailure)
            assertEquals(StudentError.Database, result.exceptionOrNull())
        }
}