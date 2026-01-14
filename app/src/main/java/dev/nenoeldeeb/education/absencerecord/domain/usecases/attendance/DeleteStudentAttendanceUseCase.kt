package dev.nenoeldeeb.education.absencerecord.domain.usecases.attendance

import dev.nenoeldeeb.education.absencerecord.domain.repositories.AttendanceRepository
import kotlinx.datetime.LocalDate

class DeleteStudentAttendanceUseCase(
    private val repository: AttendanceRepository
) {
    suspend operator fun invoke(
        studentId: Int,
        date: LocalDate
    ): Result<Unit> {
        return repository.deleteAttendance(studentId, date)
    }
}