package dev.nenoeldeeb.education.absencerecord.domain.usecases.schedule

import dev.nenoeldeeb.education.absencerecord.domain.repositories.ScheduleRepository
import kotlinx.datetime.DayOfWeek

/** Creates a lesson block with a length in minutes (FR-001, FR-002). */
class InsertHourUseCase(
    private val repository: ScheduleRepository
) {
    suspend operator fun invoke(
        weekday: DayOfWeek,
        startMinutes: Int,
        durationMinutes: Int
    ): Result<Int> {
        return repository.insertHour(weekday, startMinutes, durationMinutes)
    }
}