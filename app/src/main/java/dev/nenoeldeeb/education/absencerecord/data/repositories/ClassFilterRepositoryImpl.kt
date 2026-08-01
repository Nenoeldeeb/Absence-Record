package dev.nenoeldeeb.education.absencerecord.data.repositories

import dev.nenoeldeeb.education.absencerecord.domain.repositories.ClassFilterRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-memory implementation of [ClassFilterRepository] backed by a
 * [MutableStateFlow] holding the set of selected class IDs.
 *
 * All operations are thread-safe and non-blocking as `StateFlow` updates are atomic.
 */
class ClassFilterRepositoryImpl : ClassFilterRepository {
    private val _selectedClassIds = MutableStateFlow<Set<Int>>(emptySet())

    override val selectedClassIds: StateFlow<Set<Int>> = _selectedClassIds.asStateFlow()

    override fun toggleClass(classId: Int) {
        _selectedClassIds.update { current ->
            if (classId in current) {
                current - classId
            } else {
                current + classId
            }
        }
    }

    override fun setSelectedClassIds(classIds: Set<Int>) {
        _selectedClassIds.update { classIds }
    }

    override fun clearFilter() {
        _selectedClassIds.update { emptySet() }
    }
}