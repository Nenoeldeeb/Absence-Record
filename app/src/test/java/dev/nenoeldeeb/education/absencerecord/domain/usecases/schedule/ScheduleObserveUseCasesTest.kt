package dev.nenoeldeeb.education.absencerecord.domain.usecases.schedule

import dev.nenoeldeeb.education.absencerecord.domain.models.AssignmentRemovalReport
import dev.nenoeldeeb.education.absencerecord.domain.models.BusyAppointment
import dev.nenoeldeeb.education.absencerecord.domain.models.HourWithOccupancy
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentScheduleView
import dev.nenoeldeeb.education.absencerecord.domain.repositories.ScheduleRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScheduleObserveUseCasesTest {
    private val repository: ScheduleRepository = mockk()

    @Test
    fun `observeHoursForWeekday delegates on success`() =
        runTest {
            val hours = listOf<HourWithOccupancy>()
            coEvery { repository.observeHoursForWeekday(DayOfWeek.SATURDAY) } returns
                flowOf(Result.success(hours))

            val result =
                ObserveHoursForWeekdayUseCase(repository)(DayOfWeek.SATURDAY).first()

            assertTrue(result.isSuccess)
            assertEquals(hours, result.getOrNull())
            coVerify { repository.observeHoursForWeekday(DayOfWeek.SATURDAY) }
        }

    @Test
    fun `observeHoursForWeekday propagates failure`() =
        runTest {
            coEvery { repository.observeHoursForWeekday(DayOfWeek.SATURDAY) } returns
                flowOf(Result.failure(StudentError.Database))

            val result =
                ObserveHoursForWeekdayUseCase(repository)(DayOfWeek.SATURDAY).first()

            assertTrue(result.isFailure)
            assertEquals(StudentError.Database, result.exceptionOrNull())
        }

    @Test
    fun `observeBusyAppointmentsForWeekday filters by weekday on success`() =
        runTest {
            val busy =
                listOf(
                    BusyAppointment(
                        id = 1,
                        studentId = 7,
                        weekday = DayOfWeek.FRIDAY,
                        startMinutes = 510,
                        durationMinutes = 60
                    ),
                    BusyAppointment(
                        id = 2,
                        studentId = 7,
                        weekday = DayOfWeek.SATURDAY,
                        startMinutes = 540,
                        durationMinutes = 30
                    )
                )
            coEvery { repository.observeBusyAppointments() } returns flowOf(Result.success(busy))

            val result =
                ObserveBusyAppointmentsForWeekdayUseCase(repository)(DayOfWeek.FRIDAY).first()

            assertTrue(result.isSuccess)
            assertEquals(listOf(1), result.getOrNull()?.map { it.id })
            coVerify { repository.observeBusyAppointments() }
        }

    @Test
    fun `observeBusyAppointmentsForWeekday propagates failure`() =
        runTest {
            coEvery { repository.observeBusyAppointments() } returns
                flowOf(Result.failure(StudentError.Database))

            val result =
                ObserveBusyAppointmentsForWeekdayUseCase(repository)(DayOfWeek.FRIDAY).first()

            assertTrue(result.isFailure)
            assertEquals(StudentError.Database, result.exceptionOrNull())
        }

    @Test
    fun `observeStudentSchedule delegates on success`() =
        runTest {
            val view = StudentScheduleView(studentId = 7, lessons = emptyList(), busy = emptyList())
            coEvery { repository.observeStudentSchedule(7) } returns flowOf(Result.success(view))

            val result = ObserveStudentScheduleUseCase(repository)(7).first()

            assertTrue(result.isSuccess)
            assertEquals(view, result.getOrNull())
            coVerify { repository.observeStudentSchedule(7) }
        }

    @Test
    fun `observeStudentSchedule propagates failure`() =
        runTest {
            coEvery { repository.observeStudentSchedule(7) } returns
                flowOf(Result.failure(StudentError.Database))

            val result = ObserveStudentScheduleUseCase(repository)(7).first()

            assertTrue(result.isFailure)
            assertEquals(StudentError.Database, result.exceptionOrNull())
        }

    @Test
    fun `updateHour propagates AssignmentRemovalReport payload`() =
        runTest {
            val report = AssignmentRemovalReport(listOf(7, 8))
            coEvery { repository.updateHour(1, DayOfWeek.SATURDAY, 600, 4) } returns Result.success(report)

            val result = UpdateHourUseCase(repository)(1, DayOfWeek.SATURDAY, 600, 4)

            assertTrue(result.isSuccess)
            assertEquals(listOf(7, 8), result.getOrNull()?.removedStudentIds)
        }

    @Test
    fun `insertBusyAppointment propagates AssignmentRemovalReport payload`() =
        runTest {
            val report = AssignmentRemovalReport(listOf(7))
            coEvery { repository.insertBusyAppointment(7, DayOfWeek.SATURDAY, 510, 60) } returns
                Result.success(report)

            val result = InsertBusyAppointmentUseCase(repository)(7, DayOfWeek.SATURDAY, 510, 60)

            assertTrue(result.isSuccess)
            assertEquals(listOf(7), result.getOrNull()?.removedStudentIds)
        }

    @Test
    fun `updateBusyAppointment propagates AssignmentRemovalReport payload`() =
        runTest {
            val report = AssignmentRemovalReport(emptyList())
            coEvery { repository.updateBusyAppointment(1, 600, 30) } returns Result.success(report)

            val result = UpdateBusyAppointmentUseCase(repository)(1, 600, 30)

            assertTrue(result.isSuccess)
            assertEquals(emptyList(), result.getOrNull()?.removedStudentIds)
        }
}