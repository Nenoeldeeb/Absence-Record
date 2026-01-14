package dev.nenoeldeeb.education.absencerecord.domain.usecases.attendance

import dev.nenoeldeeb.education.absencerecord.domain.models.StudentAttendance
import dev.nenoeldeeb.education.absencerecord.domain.repositories.AttendanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

class GetAttendanceForDateUseCase(
    private val repository: AttendanceRepository
) {
    operator fun invoke(date: LocalDate): Flow<Result<List<StudentAttendance>>> {
        return repository.getAttendanceForDate(date)
    }
}