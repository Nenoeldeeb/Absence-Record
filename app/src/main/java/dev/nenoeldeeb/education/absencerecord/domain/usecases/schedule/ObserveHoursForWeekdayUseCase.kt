package dev.nenoeldeeb.education.absencerecord.domain.usecases.schedule

import dev.nenoeldeeb.education.absencerecord.domain.models.HourWithOccupancy
import dev.nenoeldeeb.education.absencerecord.domain.repositories.ScheduleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.DayOfWeek

class ObserveHoursForWeekdayUseCase(
    private val repository: ScheduleRepository
) {
    operator fun invoke(weekday: DayOfWeek): Flow<Result<List<HourWithOccupancy>>> {
        return repository.observeHoursForWeekday(weekday)
    }
}