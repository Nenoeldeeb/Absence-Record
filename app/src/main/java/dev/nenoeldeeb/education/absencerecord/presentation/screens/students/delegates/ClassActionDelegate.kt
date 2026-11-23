package dev.nenoeldeeb.education.absencerecord.presentation.screens.students.delegates

import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentClass
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError
import dev.nenoeldeeb.education.absencerecord.domain.repositories.ClassFilterRepository
import dev.nenoeldeeb.education.absencerecord.domain.usecases.ClassManagementUseCases
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenState
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText
import dev.nenoeldeeb.education.absencerecord.presentation.utils.toUiText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Class CRUD orchestration for the ManageClasses dialog, plus the pure
 * CRUD/class-filter-fallback helpers below. Mutates state only through
 * `updateState`; launches only via `scope`. The class-filter repository is
 * read directly for the fallback, since it is the source of truth for the
 * active filter selection.
 */
class ClassActionDelegate(
    private val classManagementUseCases: ClassManagementUseCases,
    private val classFilterRepository: ClassFilterRepository,
    private val scope: CoroutineScope,
    private val updateState: ((StudentsScreenState) -> StudentsScreenState) -> Unit
) {
    fun handleAddClass(name: String) {
        scope.launch {
            addClass(name)
                .onSuccess { toast -> updateState { it.copy(toastMessage = toast) } }
                .onFailure { error -> updateState { it.copy(error = error.toUiText()) } }
        }
    }

    fun handleRenameClass(
        studentClass: StudentClass,
        newName: String
    ) {
        scope.launch {
            renameClass(studentClass, newName)
                .onSuccess { toast -> updateState { it.copy(toastMessage = toast) } }
                .onFailure { error -> updateState { it.copy(error = error.toUiText()) } }
        }
    }

    fun handleDeleteClass(studentClass: StudentClass) {
        scope.launch {
            deleteClass(studentClass)
                .onSuccess { toast ->
                    val fallback =
                        onDeletedClassFilterFallback(
                            classFilterRepository.selectedClassIds.value,
                            studentClass
                        )
                    classFilterRepository.setSelectedClassIds(fallback)
                    updateState { it.copy(toastMessage = toast) }
                }
                .onFailure { error -> updateState { it.copy(error = error.toUiText()) } }
        }
    }

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