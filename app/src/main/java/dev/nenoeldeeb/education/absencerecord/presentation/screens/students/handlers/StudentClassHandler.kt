package dev.nenoeldeeb.education.absencerecord.presentation.screens.students.handlers

import dev.nenoeldeeb.education.absencerecord.domain.models.StudentClass
import dev.nenoeldeeb.education.absencerecord.domain.repositories.ClassFilterRepository
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenState
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.delegates.ClassActionDelegate
import dev.nenoeldeeb.education.absencerecord.presentation.utils.toUiText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StudentClassHandler(
    private val classActionDelegate: ClassActionDelegate,
    private val classFilterRepository: ClassFilterRepository
) {
    fun handleAddClass(
        name: String,
        uiState: MutableStateFlow<StudentsScreenState>,
        scope: CoroutineScope
    ) {
        scope.launch {
            classActionDelegate.addClass(name)
                .onSuccess { toast -> uiState.update { it.copy(toastMessage = toast) } }
                .onFailure { error -> uiState.update { it.copy(error = error.toUiText()) } }
        }
    }

    fun handleRenameClass(
        studentClass: StudentClass,
        newName: String,
        uiState: MutableStateFlow<StudentsScreenState>,
        scope: CoroutineScope
    ) {
        scope.launch {
            classActionDelegate.renameClass(studentClass, newName)
                .onSuccess { toast -> uiState.update { it.copy(toastMessage = toast) } }
                .onFailure { error -> uiState.update { it.copy(error = error.toUiText()) } }
        }
    }

    fun handleDeleteClass(
        studentClass: StudentClass,
        uiState: MutableStateFlow<StudentsScreenState>,
        scope: CoroutineScope
    ) {
        scope.launch {
            classActionDelegate.deleteClass(studentClass)
                .onSuccess { toast ->
                    val fallback =
                        classActionDelegate.onDeletedClassFilterFallback(
                            uiState.value.selectedClassIds,
                            studentClass
                        )
                    classFilterRepository.setSelectedClassIds(fallback)
                    uiState.update { it.copy(toastMessage = toast) }
                }
                .onFailure { error -> uiState.update { it.copy(error = error.toUiText()) } }
        }
    }
}