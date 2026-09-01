package dev.nenoeldeeb.education.absencerecord.domain.usecases.schedule

import dev.nenoeldeeb.education.absencerecord.domain.models.BusyAppointment
import dev.nenoeldeeb.education.absencerecord.domain.repositories.ScheduleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DayOfWeek

class ObserveBusyAppointmentsForWeekdayUseCase(
    private val repository: ScheduleRepository
) {
    operator fun invoke(weekday: DayOfWeek): Flow<Result<List<BusyAppointment>>> {
        return repository.observeBusyAppointments().map { result ->
            result.map { list -> list.filter { it.weekday == weekday } }
        }
    }
}