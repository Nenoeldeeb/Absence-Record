package dev.nenoeldeeb.education.absencerecord.domain.usecases.schedule

import dev.nenoeldeeb.education.absencerecord.domain.models.AssignmentRemovalReport
import dev.nenoeldeeb.education.absencerecord.domain.repositories.ScheduleRepository
import kotlinx.datetime.DayOfWeek

class InsertBusyAppointmentUseCase(
    private val repository: ScheduleRepository
) {
    suspend operator fun invoke(
        studentId: Int,
        weekday: DayOfWeek,
        startMinutes: Int,
        durationMinutes: Int
    ): Result<AssignmentRemovalReport> {
        return repository.insertBusyAppointment(studentId, weekday, startMinutes, durationMinutes)
    }
}