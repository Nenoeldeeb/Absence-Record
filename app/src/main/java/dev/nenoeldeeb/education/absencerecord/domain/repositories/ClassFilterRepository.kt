package dev.nenoeldeeb.education.absencerecord.domain.repositories

import kotlinx.coroutines.flow.StateFlow

/**
 * Repository for the synchronized multi-select class filter state shared across
 * the Calendar (Attendance dialog), Report, and Students screens.
 */
interface ClassFilterRepository {
    /**
     * Observable state of currently selected class IDs.
     * An empty set indicates that all class checkboxes are unchecked (filtering for unassigned students).
     */
    val selectedClassIds: StateFlow<Set<Int>>

    /**
     * Toggles a class ID in the selected set. If present, removes it; if absent, adds it.
     */
    fun toggleClass(classId: Int)

    /**
     * Replaces the current selected class IDs with the provided set.
     */
    fun setSelectedClassIds(classIds: Set<Int>)

    /**
     * Resets the selected class IDs to an empty set (default state).
     */
    fun clearFilter()
}