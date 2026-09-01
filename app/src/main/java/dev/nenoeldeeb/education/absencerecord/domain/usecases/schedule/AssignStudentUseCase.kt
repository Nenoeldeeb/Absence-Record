package dev.nenoeldeeb.education.absencerecord.domain.usecases.schedule

import dev.nenoeldeeb.education.absencerecord.domain.repositories.ScheduleRepository
import kotlinx.datetime.DayOfWeek

class AssignStudentUseCase(
    private val repository: ScheduleRepository
) {
    suspend operator fun invoke(
        hourId: Int,
        studentId: Int,
        weekday: DayOfWeek
    ): Result<Unit> {
        return repository.assignStudent(hourId, studentId, weekday)
    }
}