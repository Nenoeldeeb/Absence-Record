package dev.nenoeldeeb.education.absencerecord.domain.usecases.attendance

import dev.nenoeldeeb.education.absencerecord.domain.repositories.AttendanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

class GetStudentAttendanceDatesUseCase(
    private val repository: AttendanceRepository
) {
    operator fun invoke(studentId: Int): Flow<Result<List<LocalDate>>> {
        return repository.getStudentAttendanceDates(studentId)
    }
}