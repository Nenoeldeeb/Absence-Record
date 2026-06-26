# Quickstart: Class Filter Refactor

## What to change (7 files)

### 1. `CalendarScreenState.kt`
- Remove `selectedClassFilter`, `classDropdownExpanded`, `isClassFilterVisible`
- Add `selectedClassIds: Set<Int> = emptySet()`, `filterDropdownExpanded: Boolean = false`
- Remove `ClassFilter` import

### 2. `CalendarScreenEvent.kt`
- Remove: `SelectClassFilter`, `ToggleClassFilterVisibility`, `ToggleClassDropdown`
- Add: `data class ToggleClassSelection(val classId: Int)`, `data object ToggleFilterDropdown`
- Remove `ClassFilter` import

### 3. `CalendarViewModel.kt`
- Replace filter handlers in `onEvent()`:
  - `SelectClassFilter` → `ToggleClassSelection(classId)`: add/remove from set, re-filter `rawStudents` with new multi-class filter, clamp `studentsForSelectedDate`
  - `ToggleClassFilterVisibility` → REMOVE
  - `ToggleClassDropdown` → `ToggleFilterDropdown`: toggle state
- Add `applyMultiClassFilter(students: List<Student>, classIds: Set<Int>): List<Student>` using `distinctBy` + `filter` off main thread
- Remove `applyClassFilter` import
- Remove `ClassFilter` import

### 4. `CalendarScreen.kt`
- Remove `AnimatedVisibility` + `ClassFilterDropdown` block and filter `IconButton` from `TopAppBar`
- Remove `import ClassFilterDropdown`
- Add `ClassCheckboxFilter` composable inside dialog (or inline in `DialogContent`)
- Pass `availableClasses`, `selectedClassIds`, `filterDropdownExpanded`, toggle events to dialog

### 5. `StudentFilters.kt`
- Add `fun Iterable<Student>.applyMultiClassFilter(classIds: Set<Int>): List<Student>` that filters students whose `classId` is in the set, or returns all if set is empty, with `distinctBy { it.id }`

### 6. `strings.xml` (both languages)
- Add:
  - `<string name="class_filter_none">No filter</string>`
  - `<string name="class_filter_n">%d classes selected</string>`
  - `<string name="class_filter_one">1 class selected</string>`

### 7. `CalendarViewModelTest.kt`
- Add tests for:
  - Toggling single class filter
  - Toggling multiple classes → merged + deduplicated list
  - Unchecking all classes → all students shown
  - Present count clamped when filter removes students
  - Unassigned students hidden when class filter active

## Order of implementation (per tasks.md phases)

1. **Foundational (Phase 2)** — Order: `StudentFilters.kt` > `CalendarScreenState.kt` > `CalendarScreenEvent.kt` > `strings.xml` (parallel-safe)
2. **US1+US2 (Phase 3)** — Order: `CalendarViewModel.kt` > `CalendarScreen.kt` (add filter UI) > `CalendarScreen.kt` (remove old UI) > `CalendarViewModelTest.kt` (filter tests)
3. **US3 (Phase 4)** — Order: `CalendarViewModel.kt` (clamping hardening) > `CalendarViewModelTest.kt` (clamping tests)
4. **Polish (Phase 5)** — Format, test verification, file size check, full validation

## Files NOT to change
- `ClassFilter.kt` (used by Report, Students)
- `ClassFilterDropdown.kt` (used by Report, Students)
- Any domain/data layer files (no model/service/repo changes needed)
- ReportScreen, StudentsScreen, their ViewModels or states

## Verification

```shell
./gradlew ktlintFormat
./gradlew testDebugUnitTest --tests "CalendarViewModelTest"
./gradlew check
```
