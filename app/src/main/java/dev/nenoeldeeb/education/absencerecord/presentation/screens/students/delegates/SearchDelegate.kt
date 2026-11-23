package dev.nenoeldeeb.education.absencerecord.presentation.screens.students.delegates

import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenState
import dev.nenoeldeeb.education.absencerecord.presentation.utils.applyStudentListFilters

/**
 * Stateless search delegates: transforms the passed-in roster against the
 * current filter inputs and hands the new state to `updateState`. No scope.
 */
class SearchDelegate(
    private val updateState: ((StudentsScreenState) -> StudentsScreenState) -> Unit
) {
    fun handleUpdateQuery(
        query: String,
        rawStudents: List<Student>
    ) {
        updateState { state ->
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
        rawStudents: List<Student>
    ) {
        updateState { state ->
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