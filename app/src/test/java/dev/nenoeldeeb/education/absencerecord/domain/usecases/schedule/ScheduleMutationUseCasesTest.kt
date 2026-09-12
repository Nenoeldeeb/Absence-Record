package dev.nenoeldeeb.education.absencerecord.domain.usecases.schedule

import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError
import dev.nenoeldeeb.education.absencerecord.domain.repositories.ScheduleRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScheduleMutationUseCasesTest {
    private val repository: ScheduleRepository = mockk()

    @Test
    fun `insertHour delegates on success`() =
        runTest {
            coEvery { repository.insertHour(DayOfWeek.SATURDAY, 540, 5) } returns Result.success(7)

            val result = InsertHourUseCase(repository)(DayOfWeek.SATURDAY, 540, 5)

            assertTrue(result.isSuccess)
            assertEquals(7, result.getOrNull())
            coVerify { repository.insertHour(DayOfWeek.SATURDAY, 540, 5) }
        }

    @Test
    fun `insertHour propagates HourOverlap failure`() =
        runTest {
            coEvery { repository.insertHour(DayOfWeek.SATURDAY, 540, 5) } returns
                Result.failure(StudentError.HourOverlap)

            val result = InsertHourUseCase(repository)(DayOfWeek.SATURDAY, 540, 5)

            assertTrue(result.isFailure)
            assertEquals(StudentError.HourOverlap, result.exceptionOrNull())
        }

    @Test
    fun `updateHour propagates Validation failure`() =
        runTest {
            coEvery { repository.updateHour(1, DayOfWeek.SATURDAY, 540, 1) } returns
                Result.failure(StudentError.Validation("capacity"))

            val result = UpdateHourUseCase(repository)(1, DayOfWeek.SATURDAY, 540, 1)

            assertTrue(result.isFailure)
            assert(result.exceptionOrNull() is StudentError.Validation)
        }

    @Test
    fun `deleteHour delegates on success`() =
        runTest {
            coEvery { repository.deleteHour(3) } returns Result.success(Unit)

            val result = DeleteHourUseCase(repository)(3)

            assertTrue(result.isSuccess)
            coVerify { repository.deleteHour(3) }
        }

    @Test
    fun `deleteHour propagates failure`() =
        runTest {
            coEvery { repository.deleteHour(3) } returns
                Result.failure(StudentError.Validation("Lesson hour not found"))

            val result = DeleteHourUseCase(repository)(3)

            assertTrue(result.isFailure)
            assert(result.exceptionOrNull() is StudentError.Validation)
        }

    @Test
    fun `assignStudent delegates on success`() =
        runTest {
            coEvery { repository.assignStudent(1, 7, DayOfWeek.SATURDAY) } returns Result.success(Unit)

            val result = AssignStudentUseCase(repository)(1, 7, DayOfWeek.SATURDAY)

            assertTrue(result.isSuccess)
            coVerify { repository.assignStudent(1, 7, DayOfWeek.SATURDAY) }
        }

    @Test
    fun `assignStudent propagates HourFull failure`() =
        runTest {
            coEvery { repository.assignStudent(1, 7, DayOfWeek.SATURDAY) } returns
                Result.failure(StudentError.HourFull)

            val result = AssignStudentUseCase(repository)(1, 7, DayOfWeek.SATURDAY)

            assertTrue(result.isFailure)
            assertEquals(StudentError.HourFull, result.exceptionOrNull())
        }

    @Test
    fun `assignStudent propagates AssignmentExists failure`() =
        runTest {
            coEvery { repository.assignStudent(1, 7, DayOfWeek.SATURDAY) } returns
                Result.failure(StudentError.AssignmentExists)

            val result = AssignStudentUseCase(repository)(1, 7, DayOfWeek.SATURDAY)

            assertTrue(result.isFailure)
            assertEquals(StudentError.AssignmentExists, result.exceptionOrNull())
        }

    @Test
    fun `assignStudent propagates BusyConflict failure`() =
        runTest {
            coEvery { repository.assignStudent(1, 7, DayOfWeek.SATURDAY) } returns
                Result.failure(StudentError.BusyConflict)

            val result = AssignStudentUseCase(repository)(1, 7, DayOfWeek.SATURDAY)

            assertTrue(result.isFailure)
            assertEquals(StudentError.BusyConflict, result.exceptionOrNull())
        }

    @Test
    fun `unassignStudent delegates on success`() =
        runTest {
            coEvery { repository.unassignStudent(1, 7) } returns Result.success(Unit)

            val result = UnassignStudentUseCase(repository)(1, 7)

            assertTrue(result.isSuccess)
            coVerify { repository.unassignStudent(1, 7) }
        }

    @Test
    fun `unassignStudent propagates failure`() =
        runTest {
            coEvery { repository.unassignStudent(1, 7) } returns
                Result.failure(StudentError.Validation("Assignment not found"))

            val result = UnassignStudentUseCase(repository)(1, 7)

            assertTrue(result.isFailure)
            assert(result.exceptionOrNull() is StudentError.Validation)
        }

    @Test
    fun `insertBusyAppointment propagates Validation failure`() =
        runTest {
            coEvery { repository.insertBusyAppointment(7, DayOfWeek.SATURDAY, 1410, 60) } returns
                Result.failure(StudentError.Validation("Busy appointment cannot cross midnight"))

            val result =
                InsertBusyAppointmentUseCase(repository)(7, DayOfWeek.SATURDAY, 1410, 60)

            assertTrue(result.isFailure)
            assert(result.exceptionOrNull() is StudentError.Validation)
        }

    @Test
    fun `updateBusyAppointment propagates Validation failure`() =
        runTest {
            coEvery { repository.updateBusyAppointment(1, 600, 10) } returns
                Result.failure(StudentError.Validation("Busy appointment cannot cross midnight"))

            val result = UpdateBusyAppointmentUseCase(repository)(1, 600, 10)

            assertTrue(result.isFailure)
            assert(result.exceptionOrNull() is StudentError.Validation)
        }

    @Test
    fun `deleteBusyAppointment delegates on success`() =
        runTest {
            coEvery { repository.deleteBusyAppointment(2) } returns Result.success(Unit)

            val result = DeleteBusyAppointmentUseCase(repository)(2)

            assertTrue(result.isSuccess)
            coVerify { repository.deleteBusyAppointment(2) }
        }

    @Test
    fun `deleteBusyAppointment propagates failure`() =
        runTest {
            coEvery { repository.deleteBusyAppointment(2) } returns
                Result.failure(StudentError.Validation("Busy appointment not found"))

            val result = DeleteBusyAppointmentUseCase(repository)(2)

            assertTrue(result.isFailure)
            assert(result.exceptionOrNull() is StudentError.Validation)
        }
}