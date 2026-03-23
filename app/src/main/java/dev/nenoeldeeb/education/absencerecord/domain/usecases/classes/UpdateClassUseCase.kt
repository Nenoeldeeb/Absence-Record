package dev.nenoeldeeb.education.absencerecord.domain.usecases.classes

import dev.nenoeldeeb.education.absencerecord.domain.models.StudentClass
import dev.nenoeldeeb.education.absencerecord.domain.repositories.StudentClassRepository

class UpdateClassUseCase(private val repository: StudentClassRepository) {
    suspend operator fun invoke(studentClass: StudentClass): Result<Unit> = repository.updateClass(studentClass)
}