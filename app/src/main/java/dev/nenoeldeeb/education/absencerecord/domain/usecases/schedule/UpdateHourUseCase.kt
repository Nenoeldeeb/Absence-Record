package dev.nenoeldeeb.education.absencerecord.domain.usecases.schedule

import dev.nenoeldeeb.education.absencerecord.domain.models.AssignmentRemovalReport
import dev.nenoeldeeb.education.absencerecord.domain.repositories.ScheduleRepository
import kotlinx.datetime.DayOfWeek

class UpdateHourUseCase(
    private val repository: ScheduleRepository
) {
    suspend operator fun invoke(
        id: Int,
        weekday: DayOfWeek,
        startMinutes: Int,
        maxStudents: Int
    ): Result<AssignmentRemovalReport> {
        return repository.updateHour(id, weekday, startMinutes, maxStudents)
    }
}