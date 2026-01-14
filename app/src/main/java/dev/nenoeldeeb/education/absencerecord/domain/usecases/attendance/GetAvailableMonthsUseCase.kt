package dev.nenoeldeeb.education.absencerecord.domain.usecases.attendance

import dev.nenoeldeeb.education.absencerecord.domain.repositories.AttendanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

class GetAvailableMonthsUseCase(
    private val attendanceRepository: AttendanceRepository
) {
    operator fun invoke(): Flow<Result<List<LocalDate>>> {
        return attendanceRepository.getDistinctDatesWithAttendance()
            .map { result ->
                result.map { localDates ->
                    localDates
                        .map { LocalDate(it.year, it.month, 1) }
                        .distinct()
                        .sortedDescending()
                }
            }
    }
}