package dev.nenoeldeeb.education.absencerecord.domain.usecases.schedule

import dev.nenoeldeeb.education.absencerecord.domain.models.AssignmentRemovalReport
import dev.nenoeldeeb.education.absencerecord.domain.repositories.ScheduleRepository

class UpdateBusyAppointmentUseCase(
    private val repository: ScheduleRepository
) {
    suspend operator fun invoke(
        id: Int,
        startMinutes: Int,
        durationMinutes: Int
    ): Result<AssignmentRemovalReport> {
        return repository.updateBusyAppointment(id, startMinutes, durationMinutes)
    }
}