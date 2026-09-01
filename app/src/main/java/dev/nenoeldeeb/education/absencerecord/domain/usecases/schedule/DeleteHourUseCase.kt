package dev.nenoeldeeb.education.absencerecord.domain.usecases.schedule

import dev.nenoeldeeb.education.absencerecord.domain.repositories.ScheduleRepository

class DeleteHourUseCase(
    private val repository: ScheduleRepository
) {
    suspend operator fun invoke(id: Int): Result<Unit> {
        return repository.deleteHour(id)
    }
}