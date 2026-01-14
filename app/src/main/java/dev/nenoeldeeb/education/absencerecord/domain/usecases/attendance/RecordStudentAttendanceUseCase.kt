package dev.nenoeldeeb.education.absencerecord.domain.usecases.attendance

import dev.nenoeldeeb.education.absencerecord.domain.models.StudentAttendance
import dev.nenoeldeeb.education.absencerecord.domain.repositories.AttendanceRepository

class RecordStudentAttendanceUseCase(
    private val repository: AttendanceRepository
) {
    suspend operator fun invoke(attendance: StudentAttendance): Result<Long> {
        return repository.insertAttendance(attendance)
    }
}