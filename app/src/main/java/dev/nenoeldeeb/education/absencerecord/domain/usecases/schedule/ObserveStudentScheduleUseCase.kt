package dev.nenoeldeeb.education.absencerecord.domain.usecases.schedule

import dev.nenoeldeeb.education.absencerecord.domain.models.StudentScheduleView
import dev.nenoeldeeb.education.absencerecord.domain.repositories.ScheduleRepository
import kotlinx.coroutines.flow.Flow

class ObserveStudentScheduleUseCase(
    private val repository: ScheduleRepository
) {
    operator fun invoke(studentId: Int): Flow<Result<StudentScheduleView>> {
        return repository.observeStudentSchedule(studentId)
    }
}