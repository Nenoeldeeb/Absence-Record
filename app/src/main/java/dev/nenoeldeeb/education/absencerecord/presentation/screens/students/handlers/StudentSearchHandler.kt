package dev.nenoeldeeb.education.absencerecord.presentation.screens.students.handlers

import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenState
import dev.nenoeldeeb.education.absencerecord.presentation.utils.applyStudentListFilters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class StudentSearchHandler {
    fun handleUpdateQuery(
        query: String,
        rawStudents: List<Student>,
        uiState: MutableStateFlow<StudentsScreenState>
    ) {
        uiState.update { state ->
            state.copy(
                searchQuery = query,
                allStudents =
                    rawStudents.applyStudentListFilters(
                        state.selectedClassIds,
                        state.isMultiSelectionMode,
                        query
                    )
            )
        }
    }

    fun handleToggleSearch(
        active: Boolean,
        rawStudents: List<Student>,
        uiState: MutableStateFlow<StudentsScreenState>
    ) {
        uiState.update { state ->
            val query = if (active) state.searchQuery else ""
            state.copy(
                isSearchActive = active,
                searchQuery = query,
                allStudents =
                    rawStudents.applyStudentListFilters(
                        state.selectedClassIds,
                        state.isMultiSelectionMode,
                        query
                    )
            )
        }
    }
}