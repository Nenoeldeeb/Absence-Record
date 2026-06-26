# Data Model: Class Filter Refactor

## Entity Map (no schema changes)

| Entity | Table | Key Fields | Changes |
|--------|-------|------------|---------|
| `StudentEntity` | `students` | `id: Int (PK)`, `name: String`, `classId: Int?` | None |
| `StudentClassEntity` | `classes` | `id: Int (PK)`, `name: String` | None |
| `StudentAttendanceEntity` | `student_attendance` | `id: Int (PK)`, `studentId: Int (FK)`, `date: LocalDate` | None |

## In-Memory State Model (MVI)

### CalendarScreenState (modified)

```
@Stable
data class CalendarScreenState(
    studentsForSelectedDate: List<StudentAttendance>,
    allStudents: List<Student>,              // filtered students
    availableClasses: List<StudentClass>,     // all classes for checkbox list
    selectedClassIds: Set<Int>,               // REPLACES selectedClassFilter
    filterDropdownExpanded: Boolean,          // REPLACES classDropdownExpanded
    // REMOVED: isClassFilterVisible
    selectedDateForDialog: LocalDate?,
    error: UiText?
)
```

**Removed fields**:
- `selectedClassFilter: ClassFilter` — single-select no longer used
- `classDropdownExpanded: Boolean` — renamed to `filterDropdownExpanded`
- `isClassFilterVisible: Boolean` — filter always lives inside dialog

**New fields**:
- `selectedClassIds: Set<Int>` — class IDs selected via checkboxes; empty set = show all students
- `filterDropdownExpanded: Boolean` — controls dropdown visibility inside dialog

### Validation Rules

1. **Student total count = `allStudents.size`**: Always equals the number of visible student rows. Derived from filtering `rawStudents` by `selectedClassIds`.
2. **Present count ≤ total count**: `studentsForSelectedDate` is filtered to only include records for students whose IDs are in the currently visible set:
   ```
   visibleStudentIds = allStudents.map { it.id }.toSet()
   filteredAttendance = studentsForSelectedDate.filter { it.studentId in visibleStudentIds }
   ```
3. **Empty selectedClassIds → show all**: When `selectedClassIds.isEmpty()`, `allStudents = rawStudents` (including unassigned).
4. **Deduplication**: `selectedClassIds` produces a merged list via `distinctBy { it.id }`. A student in multiple selected classes appears once.

### CalendarScreenEvent (modified)

```kotlin
sealed interface CalendarScreenEvent {
    data class MarkStudentAttendance(val studentId: Int, val date: LocalDate)
    data class DeleteStudentAttendance(val studentId: Int, val date: LocalDate)
    data class SelectDateForDialog(val date: LocalDate?)
    data class ToggleClassSelection(val classId: Int)      // NEW: replaces SelectClassFilter
    data object ToggleFilterDropdown                       // NEW: replaces ToggleClassDropdown(expanded)
    // REMOVED: ToggleClassFilterVisibility, ToggleClassDropdown, SelectClassFilter
}
```

**Event processing** (ViewModel):
- `ToggleClassSelection(classId)` → add/remove `classId` from `selectedClassIds` → re-filter `allStudents` → clamp `studentsForSelectedDate`
- `ToggleFilterDropdown` → toggle `filterDropdownExpanded`

### State Transitions

```
[Dialog opens]
  ↓
selectedClassIds = {}, allStudents = rawStudents (all)
  ↓
[User checks "Class A"]
  ↓
selectedClassIds = {classA.id}, allStudents = rawStudents filtered to classA
studentsForSelectedDate = filtered to only students in allStudents
  ↓
[User checks "Class B"]
  ↓
selectedClassIds = {classA.id, classB.id}, allStudents = rawStudents filtered to classA ∪ classB (dedup)
studentsForSelectedDate = filtered to only students in allStudents
  ↓
[User unchecks "Class A"]
  ↓
selectedClassIds = {classB.id}, allStudents = rawStudents filtered to classB
studentsForSelectedDate = filtered to only students in allStudents
  ↓
[User unchecks all]
  ↓
selectedClassIds = {}, allStudents = rawStudents (all)
studentsForSelectedDate = filtered to only students in allStudents
```

## UI Component Model

### ClassCheckboxFilter (new composable, inside CalendarScreen.kt or separate file)

```
ClassCheckboxFilter(
    availableClasses: List<StudentClass>,
    selectedClassIds: Set<Int>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onClassToggle: (classId: Int) -> Unit
)
```

- **Layout**: `DropdownMenu` with `DropdownMenuItem` per class, each having a `Checkbox` leading icon
- **Label**: When `selectedClassIds.isEmpty()` → "No filter"; when 1 selected → class name; when >1 selected → "N classes selected"
- **Behavior**: Tapping a class checkbox toggles it without closing the dropdown. Dropdown closes when tapping outside.
