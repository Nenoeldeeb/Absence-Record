package dev.nenoeldeeb.education.absencerecord.domain.usecases.student

import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.repositories.StudentRepository

class DeleteStudentsUseCase(
    private val repository: StudentRepository
) {
    suspend operator fun invoke(students: List<Student>): Result<Unit> {
        return repository.deleteStudents(students)
    }
}