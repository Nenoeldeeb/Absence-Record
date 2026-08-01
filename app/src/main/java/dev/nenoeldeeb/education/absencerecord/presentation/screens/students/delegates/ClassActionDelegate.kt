package dev.nenoeldeeb.education.absencerecord.presentation.screens.students.delegates

import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentClass
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError
import dev.nenoeldeeb.education.absencerecord.domain.usecases.ClassManagementUseCases
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText

class ClassActionDelegate(
    private val classManagementUseCases: ClassManagementUseCases
) {
    suspend fun addClass(name: String): Result<UiText?> {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            return Result.failure(StudentError.Validation("Class name must not be blank"))
        }
        return classManagementUseCases.addClassUseCase(StudentClass(name = trimmed))
            .fold(
                onSuccess = { Result.success(UiText.StringResource(R.string.class_added, trimmed) as UiText?) },
                onFailure = { e ->
                    when (e) {
                        is StudentError.DuplicateClass -> Result.failure(StudentError.DuplicateClass)
                        else -> Result.failure(e)
                    }
                }
            )
    }

    suspend fun renameClass(
        studentClass: StudentClass,
        newName: String
    ): Result<UiText?> {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) {
            return Result.failure(StudentError.Validation("Class name must not be blank"))
        }
        return classManagementUseCases.updateClassUseCase(studentClass.copy(name = trimmed))
            .fold(
                onSuccess = { Result.success(UiText.StringResource(R.string.class_updated, trimmed) as UiText?) },
                onFailure = { e ->
                    when (e) {
                        is StudentError.DuplicateClass -> Result.failure(StudentError.DuplicateClass)
                        else -> Result.failure(e)
                    }
                }
            )
    }

    suspend fun deleteClass(studentClass: StudentClass): Result<UiText?> {
        return classManagementUseCases.deleteClassUseCase(studentClass)
            .map { UiText.StringResource(R.string.class_deleted) as UiText? }
    }

    fun onDeletedClassFilterFallback(
        currentClassIds: Set<Int>,
        deletedClass: StudentClass
    ): Set<Int> = currentClassIds - deletedClass.id
}