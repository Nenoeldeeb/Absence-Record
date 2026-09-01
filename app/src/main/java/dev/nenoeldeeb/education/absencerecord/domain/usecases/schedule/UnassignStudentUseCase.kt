package dev.nenoeldeeb.education.absencerecord.domain.usecases.schedule

import dev.nenoeldeeb.education.absencerecord.domain.repositories.ScheduleRepository

class UnassignStudentUseCase(
    private val repository: ScheduleRepository
) {
    suspend operator fun invoke(
        hourId: Int,
        studentId: Int
    ): Result<Unit> {
        return repository.unassignStudent(hourId, studentId)
    }
}