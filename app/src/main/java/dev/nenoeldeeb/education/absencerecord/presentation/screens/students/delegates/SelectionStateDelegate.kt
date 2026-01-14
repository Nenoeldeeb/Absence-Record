package dev.nenoeldeeb.education.absencerecord.presentation.screens.students.delegates

import dev.nenoeldeeb.education.absencerecord.domain.models.Student

/**
 * Delegate class responsible for managing multi-selection state.
 * Extracts selection logic from StudentsViewModel to comply with Single Responsibility Principle.
 */
class SelectionStateDelegate {
    /**
     * Toggles selection state for a single student.
     * If student is selected, it will be deselected and vice versa.
     */
    fun toggleSelection(
        currentSelection: Set<Int>,
        studentId: Int
    ): Set<Int> {
        return currentSelection.toMutableSet().apply {
            if (contains(studentId)) {
                remove(studentId)
            } else {
                add(studentId)
            }
        }
    }

    /**
     * Selects all students in the provided list.
     */
    fun selectAll(students: List<Student>): Set<Int> {
        return students.map { it.id }.toSet()
    }

    /**
     * Clears all selections.
     */
    fun clearSelection(): Set<Int> = emptySet()

    /**
     * Toggles selection mode on/off.
     * When turning off, also clears all selections.
     *
     * @return Pair of (new mode state, new selection set)
     */
    fun toggleMode(
        isCurrentlyInSelectionMode: Boolean,
        currentSelection: Set<Int>
    ): Pair<Boolean, Set<Int>> {
        val newMode = !isCurrentlyInSelectionMode
        val newSelection = if (!newMode) emptySet() else currentSelection
        return newMode to newSelection
    }
}