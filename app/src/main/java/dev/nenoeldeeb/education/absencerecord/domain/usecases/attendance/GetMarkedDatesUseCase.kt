package dev.nenoeldeeb.education.absencerecord.domain.usecases.attendance

import dev.nenoeldeeb.education.absencerecord.domain.repositories.AttendanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

class GetMarkedDatesUseCase(
    private val repository: AttendanceRepository
) {
    operator fun invoke(): Flow<Result<List<LocalDate>>> {
        return repository.getDistinctDatesWithAttendance()
    }
}