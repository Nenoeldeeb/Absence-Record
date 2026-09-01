package dev.nenoeldeeb.education.absencerecord.domain.usecases.schedule

import dev.nenoeldeeb.education.absencerecord.domain.repositories.ScheduleRepository

data class ScheduleUseCases(
    private val scheduleRepository: ScheduleRepository
) {
    val observeHoursForWeekdayUseCase: ObserveHoursForWeekdayUseCase =
        ObserveHoursForWeekdayUseCase(scheduleRepository)
    val observeBusyAppointmentsForWeekdayUseCase: ObserveBusyAppointmentsForWeekdayUseCase =
        ObserveBusyAppointmentsForWeekdayUseCase(scheduleRepository)
    val observeStudentScheduleUseCase: ObserveStudentScheduleUseCase =
        ObserveStudentScheduleUseCase(scheduleRepository)
    val insertHourUseCase: InsertHourUseCase = InsertHourUseCase(scheduleRepository)
    val updateHourUseCase: UpdateHourUseCase = UpdateHourUseCase(scheduleRepository)
    val deleteHourUseCase: DeleteHourUseCase = DeleteHourUseCase(scheduleRepository)
    val assignStudentUseCase: AssignStudentUseCase = AssignStudentUseCase(scheduleRepository)
    val unassignStudentUseCase: UnassignStudentUseCase = UnassignStudentUseCase(scheduleRepository)
    val insertBusyAppointmentUseCase: InsertBusyAppointmentUseCase =
        InsertBusyAppointmentUseCase(scheduleRepository)
    val updateBusyAppointmentUseCase: UpdateBusyAppointmentUseCase =
        UpdateBusyAppointmentUseCase(scheduleRepository)
    val deleteBusyAppointmentUseCase: DeleteBusyAppointmentUseCase =
        DeleteBusyAppointmentUseCase(scheduleRepository)
}