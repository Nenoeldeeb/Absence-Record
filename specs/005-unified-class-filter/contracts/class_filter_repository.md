# Contract: ClassFilterRepository

## Overview

Defines the contract for reading and updating the synchronized multi-select class filter state across the application.

## Package & Location

- **Interface**: `dev.nenoeldeeb.education.absencerecord.domain.repositories.ClassFilterRepository`
- **Implementation**: `dev.nenoeldeeb.education.absencerecord.data.repositories.ClassFilterRepositoryImpl`

## Interface Specification

```kotlin
package dev.nenoeldeeb.education.absencerecord.domain.repositories

import kotlinx.coroutines.flow.StateFlow

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
```

## Behavior Specifications

- **Thread Safety**: All operations MUST be thread-safe and non-blocking on the Main thread.
- **Initial Value**: `selectedClassIds.value` MUST be initialized to `emptySet()`.
- **State Emitted**: Any call to `toggleClass`, `setSelectedClassIds`, or `clearFilter` MUST immediately emit the updated `Set<Int>` to all active observers of `selectedClassIds`.
