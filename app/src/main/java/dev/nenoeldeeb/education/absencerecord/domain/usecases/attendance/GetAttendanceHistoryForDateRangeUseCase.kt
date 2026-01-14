package dev.nenoeldeeb.education.absencerecord.domain.usecases.attendance

import dev.nenoeldeeb.education.absencerecord.domain.models.AttendanceHistoryItem
import dev.nenoeldeeb.education.absencerecord.domain.repositories.AttendanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

class GetAttendanceHistoryForDateRangeUseCase(
    private val repository: AttendanceRepository
) {
    operator fun invoke(
        studentId: Int,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<Result<List<AttendanceHistoryItem>>> {
        return repository.getAttendanceHistoryForDateRange(studentId, startDate, endDate)
    }
}