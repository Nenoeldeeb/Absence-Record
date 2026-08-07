package dev.nenoeldeeb.education.absencerecord.domain.usecases.student

import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.repositories.StudentRepository
import kotlinx.coroutines.flow.Flow

class GetStudentByIdUseCase(
    private val repository: StudentRepository
) {
    operator fun invoke(studentId: Int): Flow<Result<Student?>> {
        return repository.getStudentById(studentId)
    }
}