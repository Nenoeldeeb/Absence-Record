# Data Model: Unified Class Filter System

## Entities & Data Structures

### 1. Class Filter State (`Set<Int>`)

Represents the active collection of selected class identifiers shared globally across the application.

- **Type**: `Set<Int>`
- **Initial State**: `emptySet()` (all checkboxes unchecked)
- **Lifecycle**: In-memory state maintained across active screen navigation during a session; resets to `emptySet()` on application cold start.

#### State Mapping & Filtering Rules

| Checkbox State              | `selectedClassIds` Value          | Student Filter Logic                                     | Target Student Group                     |
|-----------------------------|-----------------------------------|----------------------------------------------------------|------------------------------------------|
| **All Unchecked** (Default) | `emptySet()`                      | `student.classId == null`                                | Unassigned students only                 |
| **Subset Checked**          | `Set<Int>` (e.g. `{1, 3}`)        | `student.classId != null && student.classId in classIds` | Enrolled students in selected classes    |
| **All Classes Checked**     | `Set<Int>` (All active class IDs) | `student.classId != null && student.classId in classIds` | All enrolled students across all classes |

---

### 2. Class Filter Repository Interface (`ClassFilterRepository`)

Repository contract managing the global class filter state.

- **State Flow**: `val selectedClassIds: StateFlow<Set<Int>>`
- **Operations**:
  - `toggleClass(classId: Int)`: Toggles inclusion of `classId` in `selectedClassIds`.
  - `setSelectedClassIds(classIds: Set<Int>)`: Sets the filter selection to a specific set of IDs.
  - `clearFilter()`: Resets `selectedClassIds` to `emptySet()`.
