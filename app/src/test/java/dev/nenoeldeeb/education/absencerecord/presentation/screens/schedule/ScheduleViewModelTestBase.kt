package dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule

import dev.nenoeldeeb.education.absencerecord.MainDispatcherRule
import dev.nenoeldeeb.education.absencerecord.domain.usecases.StudentManagementUseCases
import dev.nenoeldeeb.education.absencerecord.domain.usecases.schedule.AssignStudentUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.schedule.DeleteBusyAppointmentUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.schedule.DeleteHourUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.schedule.InsertBusyAppointmentUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.schedule.InsertHourUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.schedule.ObserveBusyAppointmentsForWeekdayUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.schedule.ObserveHoursForWeekdayUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.schedule.ScheduleUseCases
import dev.nenoeldeeb.education.absencerecord.domain.usecases.schedule.UnassignStudentUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.schedule.UpdateBusyAppointmentUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.schedule.UpdateHourUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
open class ScheduleViewModelTestBase {
    companion object {
        @JvmStatic
        @RegisterExtension
        val mainDispatcherRule = MainDispatcherRule()
    }

    protected lateinit var observeHoursForWeekdayUseCase: ObserveHoursForWeekdayUseCase
    protected lateinit var insertHourUseCase: InsertHourUseCase
    protected lateinit var updateHourUseCase: UpdateHourUseCase
    protected lateinit var deleteHourUseCase: DeleteHourUseCase
    protected lateinit var assignStudentUseCase: AssignStudentUseCase
    protected lateinit var unassignStudentUseCase: UnassignStudentUseCase
    protected lateinit var observeBusyAppointmentsForWeekdayUseCase: ObserveBusyAppointmentsForWeekdayUseCase
    protected lateinit var insertBusyAppointmentUseCase: InsertBusyAppointmentUseCase
    protected lateinit var updateBusyAppointmentUseCase: UpdateBusyAppointmentUseCase
    protected lateinit var deleteBusyAppointmentUseCase: DeleteBusyAppointmentUseCase
    protected lateinit var scheduleUseCases: ScheduleUseCases
    protected lateinit var studentManagementUseCases: StudentManagementUseCases
    protected lateinit var viewModel: ScheduleViewModel

    fun commonSetUp() {
        observeHoursForWeekdayUseCase = mockk(relaxed = true)
        insertHourUseCase = mockk(relaxed = true)
        updateHourUseCase = mockk(relaxed = true)
        deleteHourUseCase = mockk(relaxed = true)
        assignStudentUseCase = mockk(relaxed = true)
        unassignStudentUseCase = mockk(relaxed = true)
        observeBusyAppointmentsForWeekdayUseCase = mockk(relaxed = true)
        insertBusyAppointmentUseCase = mockk(relaxed = true)
        updateBusyAppointmentUseCase = mockk(relaxed = true)
        deleteBusyAppointmentUseCase = mockk(relaxed = true)

        scheduleUseCases = mockk(relaxed = true)
        every { scheduleUseCases.observeHoursForWeekdayUseCase } returns observeHoursForWeekdayUseCase
        every { scheduleUseCases.insertHourUseCase } returns insertHourUseCase
        every { scheduleUseCases.updateHourUseCase } returns updateHourUseCase
        every { scheduleUseCases.deleteHourUseCase } returns deleteHourUseCase
        every { scheduleUseCases.assignStudentUseCase } returns assignStudentUseCase
        every { scheduleUseCases.unassignStudentUseCase } returns unassignStudentUseCase
        every {
            scheduleUseCases.observeBusyAppointmentsForWeekdayUseCase
        } returns observeBusyAppointmentsForWeekdayUseCase
        every { scheduleUseCases.insertBusyAppointmentUseCase } returns insertBusyAppointmentUseCase
        every { scheduleUseCases.updateBusyAppointmentUseCase } returns updateBusyAppointmentUseCase
        every { scheduleUseCases.deleteBusyAppointmentUseCase } returns deleteBusyAppointmentUseCase

        studentManagementUseCases = mockk(relaxed = true)
    }

    protected fun createViewModel() {
        viewModel = ScheduleViewModel(scheduleUseCases, studentManagementUseCases)
    }
}