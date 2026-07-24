package dev.nenoeldeeb.education.absencerecord.domain.usecases.classes

import dev.nenoeldeeb.education.absencerecord.domain.models.StudentClass
import dev.nenoeldeeb.education.absencerecord.domain.repositories.StudentClassRepository
import dev.nenoeldeeb.education.absencerecord.domain.repositories.StudentRepository

class DeleteClassUseCase(
    private val studentClassRepository: StudentClassRepository,
    private val studentRepository: StudentRepository
) {
    suspend operator fun invoke(studentClass: StudentClass): Result<Unit> {
        return studentRepository.unassignStudentsFromClass(studentClass.id).fold(
            onSuccess = { studentClassRepository.deleteClass(studentClass) },
            onFailure = { Result.failure(it) }
        )
    }
}