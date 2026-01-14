package dev.nenoeldeeb.education.absencerecord.domain.usecases.student

import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.repositories.StudentRepository

class AddStudentUseCase(
    private val repository: StudentRepository
) {
    suspend operator fun invoke(student: Student): Result<Long> {
        return repository.insertStudent(student)
    }
}