package dev.nenoeldeeb.education.absencerecord.domain.usecases.classes

import dev.nenoeldeeb.education.absencerecord.domain.models.StudentClass
import dev.nenoeldeeb.education.absencerecord.domain.repositories.StudentClassRepository
import kotlinx.coroutines.flow.Flow

class GetAllClassesUseCase(private val repository: StudentClassRepository) {
    operator fun invoke(): Flow<Result<List<StudentClass>>> = repository.getAllClasses()
}
