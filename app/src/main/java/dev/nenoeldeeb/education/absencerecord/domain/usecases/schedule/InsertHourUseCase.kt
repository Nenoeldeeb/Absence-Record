package dev.nenoeldeeb.education.absencerecord.domain.usecases.schedule

import dev.nenoeldeeb.education.absencerecord.domain.repositories.ScheduleRepository
import kotlinx.datetime.DayOfWeek

class InsertHourUseCase(
    private val repository: ScheduleRepository
) {
    suspend operator fun invoke(
        weekday: DayOfWeek,
        startMinutes: Int,
        maxStudents: Int
    ): Result<Int> {
        return repository.insertHour(weekday, startMinutes, maxStudents)
    }
}