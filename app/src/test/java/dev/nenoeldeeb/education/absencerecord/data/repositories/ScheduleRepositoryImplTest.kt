package dev.nenoeldeeb.education.absencerecord.data.repositories

import dev.nenoeldeeb.education.absencerecord.data.datasources.local.daos.ScheduleDao
import dev.nenoeldeeb.education.absencerecord.domain.models.AssignmentRemovalReport
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScheduleRepositoryImplTest {
    private val scheduleDao: ScheduleDao = mockk()
    private val repository = ScheduleRepositoryImpl(scheduleDao)

    @Test
    fun `insertHour delegates to dao and returns id`() =
        runTest {
            coEvery { scheduleDao.insertHour(6, 540, 5) } returns 3L

            val result = repository.insertHour(DayOfWeek.SATURDAY, 540, 5)

            assertTrue(result.isSuccess)
            assertEquals(3, result.getOrNull())
        }

    @Test
    fun `insertHour propagates HourOverlap failure`() =
        runTest {
            coEvery { scheduleDao.insertHour(6, 540, 5) } throws StudentError.HourOverlap

            val result = repository.insertHour(DayOfWeek.SATURDAY, 540, 5)

            assertTrue(result.isFailure)
            assertEquals(StudentError.HourOverlap, result.exceptionOrNull())
        }

    @Test
    fun `insertHour maps unexpected exception to Database error`() =
        runTest {
            coEvery { scheduleDao.insertHour(6, 540, 5) } throws RuntimeException("boom")

            val result = repository.insertHour(DayOfWeek.SATURDAY, 540, 5)

            assertTrue(result.isFailure)
            assertEquals(StudentError.Database, result.exceptionOrNull())
        }

    @Test
    fun `updateHour returns AssignmentRemovalReport on success`() =
        runTest {
            val report = AssignmentRemovalReport(listOf(7, 8))
            coEvery { scheduleDao.updateHour(1, 6, 600, 4) } returns report

            val result = repository.updateHour(1, DayOfWeek.SATURDAY, 600, 4)

            assertTrue(result.isSuccess)
            assertEquals(listOf(7, 8), result.getOrNull()?.removedStudentIds)
        }

    @Test
    fun `updateHour propagates MaxBelowAssigned failure`() =
        runTest {
            coEvery { scheduleDao.updateHour(1, 6, 600, 4) } throws StudentError.MaxBelowAssigned

            val result = repository.updateHour(1, DayOfWeek.SATURDAY, 600, 4)

            assertTrue(result.isFailure)
            assertEquals(StudentError.MaxBelowAssigned, result.exceptionOrNull())
        }

    @Test
    fun `deleteHour calls dao and succeeds`() =
        runTest {
            coEvery { scheduleDao.deleteHour(1) } returns Unit

            val result = repository.deleteHour(1)

            assertTrue(result.isSuccess)
            coVerify { scheduleDao.deleteHour(1) }
        }

    @Test
    fun `deleteHour maps exception to Database error`() =
        runTest {
            coEvery { scheduleDao.deleteHour(1) } throws RuntimeException("boom")

            val result = repository.deleteHour(1)

            assertTrue(result.isFailure)
            assertEquals(StudentError.Database, result.exceptionOrNull())
        }

    @Test
    fun `assignStudent delegates to dao on success`() =
        runTest {
            coEvery { scheduleDao.assignStudent(1, 7, 6) } returns Unit

            val result = repository.assignStudent(1, 7, DayOfWeek.SATURDAY)

            assertTrue(result.isSuccess)
            coVerify { scheduleDao.assignStudent(1, 7, 6) }
        }

    @Test
    fun `assignStudent propagates HourFull failure`() =
        runTest {
            coEvery { scheduleDao.assignStudent(1, 7, 6) } throws StudentError.HourFull

            val result = repository.assignStudent(1, 7, DayOfWeek.SATURDAY)

            assertTrue(result.isFailure)
            assertEquals(StudentError.HourFull, result.exceptionOrNull())
        }

    @Test
    fun `assignStudent propagates AssignmentExists failure`() =
        runTest {
            coEvery { scheduleDao.assignStudent(1, 7, 6) } throws StudentError.AssignmentExists

            val result = repository.assignStudent(1, 7, DayOfWeek.SATURDAY)

            assertTrue(result.isFailure)
            assertEquals(StudentError.AssignmentExists, result.exceptionOrNull())
        }

    @Test
    fun `assignStudent propagates BusyConflict failure`() =
        runTest {
            coEvery { scheduleDao.assignStudent(1, 7, 6) } throws StudentError.BusyConflict

            val result = repository.assignStudent(1, 7, DayOfWeek.SATURDAY)

            assertTrue(result.isFailure)
            assertEquals(StudentError.BusyConflict, result.exceptionOrNull())
        }

    @Test
    fun `unassignStudent delegates to dao on success`() =
        runTest {
            coEvery { scheduleDao.unassignStudent(1, 7) } returns Unit

            val result = repository.unassignStudent(1, 7)

            assertTrue(result.isSuccess)
            coVerify { scheduleDao.unassignStudent(1, 7) }
        }

    @Test
    fun `unassignStudent propagates Validation failure`() =
        runTest {
            coEvery { scheduleDao.unassignStudent(1, 7) } throws
                StudentError.Validation("Assignment not found")

            val result = repository.unassignStudent(1, 7)

            assertTrue(result.isFailure)
            assert(result.exceptionOrNull() is StudentError.Validation)
        }

    @Test
    fun `insertBusyAppointment returns removal report on success`() =
        runTest {
            val report = AssignmentRemovalReport(listOf(7))
            coEvery { scheduleDao.insertBusyAppointment(7, 6, 510, 60) } returns report

            val result = repository.insertBusyAppointment(7, DayOfWeek.SATURDAY, 510, 60)

            assertTrue(result.isSuccess)
            assertEquals(listOf(7), result.getOrNull()?.removedStudentIds)
        }

    @Test
    fun `insertBusyAppointment propagates Validation failure`() =
        runTest {
            coEvery { scheduleDao.insertBusyAppointment(7, 6, 1410, 60) } throws
                StudentError.Validation("Busy appointment cannot cross midnight")

            val result = repository.insertBusyAppointment(7, DayOfWeek.SATURDAY, 1410, 60)

            assertTrue(result.isFailure)
            assert(result.exceptionOrNull() is StudentError.Validation)
        }

    @Test
    fun `updateBusyAppointment returns removal report on success`() =
        runTest {
            val report = AssignmentRemovalReport(emptyList())
            coEvery { scheduleDao.updateBusyAppointment(1, 600, 30) } returns report

            val result = repository.updateBusyAppointment(1, 600, 30)

            assertTrue(result.isSuccess)
            assertEquals(emptyList(), result.getOrNull()?.removedStudentIds)
        }

    @Test
    fun `updateBusyAppointment propagates Validation failure`() =
        runTest {
            coEvery { scheduleDao.updateBusyAppointment(1, 600, 10) } throws
                StudentError.Validation("Busy appointment cannot cross midnight")

            val result = repository.updateBusyAppointment(1, 600, 10)

            assertTrue(result.isFailure)
            assert(result.exceptionOrNull() is StudentError.Validation)
        }

    @Test
    fun `deleteBusyAppointment calls dao and succeeds`() =
        runTest {
            coEvery { scheduleDao.deleteBusyAppointment(1) } returns Unit

            val result = repository.deleteBusyAppointment(1)

            assertTrue(result.isSuccess)
            coVerify { scheduleDao.deleteBusyAppointment(1) }
        }

    @Test
    fun `deleteBusyAppointment maps exception to Database error`() =
        runTest {
            coEvery { scheduleDao.deleteBusyAppointment(1) } throws RuntimeException("boom")

            val result = repository.deleteBusyAppointment(1)

            assertTrue(result.isFailure)
            assertEquals(StudentError.Database, result.exceptionOrNull())
        }
}