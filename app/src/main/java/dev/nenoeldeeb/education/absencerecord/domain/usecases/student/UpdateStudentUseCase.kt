package dev.nenoeldeeb.education.absencerecord.domain.usecases.student

import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.repositories.StudentRepository

class UpdateStudentUseCase(
    private val repository: StudentRepository
) {
    suspend operator fun invoke(student: Student): Result<Unit> {
        return repository.updateStudent(student)
    }
}