# Research: Class Filter Refactor

## Unknown 1: Multi-select filter model

- **Decision**: Use `Set<Int>` (class IDs) in `CalendarScreenState`. No new sealed interface.
- **Rationale**: The calendar feature is the only place where multi-select is needed. ReportScreen and StudentsScreen continue using single-select `ClassFilter`. Adding a shared sealed interface would create dead code and complexity for no benefit.
- **Alternatives considered**:
  - New `sealed interface MultiClassFilter` — over-engineered for single-consumer feature.
  - `List<ClassFilter>` — less type-safe, no deduplication guarantee.

## Unknown 2: Checkbox dropdown component

- **Decision**: Build a new `ClassCheckboxFilter` composable inside `CalendarScreen.kt` or as a private component. Do not modify the shared `ClassFilterDropdown`.
- **Rationale**: The shared component uses `ExposedDropdownMenuBox` with single-select. A checkbox-based multi-select has fundamentally different interaction (toggles vs radio). Keeping them separate avoids breaking existing screens.
- **Alternatives considered**:
  - Parameterize `ClassFilterDropdown` with `multiSelect: Boolean` — adds complexity to a shared component, violates Single Responsibility.
  - Build a completely independent reusable component — not needed since only CalendarScreen uses it.

## Unknown 3: Deduplication strategy

- **Decision**: Use `distinctBy { it.id }` on the merged student list from selected classes. Wrap in `withContext(Dispatchers.Default)` if student list is large.
- **Rationale**: The AGENTS.md requires Main-safety for "collection processing." `distinctBy` on a `List<Student>` (typically < 1000 items) is fast, but offloading is trivial and follows existing patterns.
- **Performance estimate**: 1000 students × 10 classes → O(n) distinct by Int key → < 1ms. No caching needed.
- **Alternatives considered**:
  - Manual `HashSet<Int>` tracking — equivalent performance, less idiomatic Kotlin.

## Unknown 4: Present count overflow enforcement

- **Decision**: Enforce in `CalendarScreenState` via `derivedStateOf` or computed property at the ViewModel level. The `studentsForSelectedDate` is automatically limited because it only contains records for currently visible students. Additional protection: when filter changes, clip `studentsForSelectedDate` to only include attendance records for students in the filtered set.
- **Rationale**: The simplest approach is to re-derive `studentsForSelectedDate` (attendance records) whenever the filtered student list changes. Since attendance records are keyed by `studentId`, we filter them with `filter { it.studentId in visibleStudentIds }`. This naturally prevents the present count from exceeding the visible count.
- **Approach**: In `CalendarViewModel.applyFilter()`, after computing filtered students, also filter `studentsForSelectedDate` to only include records whose `studentId` exists in the filtered set.
- **Alternatives considered**:
  - UI-side clamping — fragile, violates MVI (state should be self-consistent).

## Dependency Patterns

- **StudentRepository.getAllStudents()**: Returns `Flow<Result<List<Student>>>` — collects as state, filters in ViewModel. No repository change needed.
- **StudentClassRepository.getAllClasses()**: Returns `Flow<Result<List<StudentClass>>>` — used to populate checkbox list. No change needed.
- **AttendanceRepository.getAttendanceForDate(date)**: Returns `Flow<Result<List<StudentAttendance>>>` — filtered after class selection. No change needed.

## Best Practices

- **Accessibility**: Checkbox dropdown items need `contentDescription` for each class checkbox. Dropdown label needs descriptive text. Touch targets already 48dp via Material3 `DropdownMenuItem`.
- **Testing**: New tests needed: multi-select filter combination, deduplication, present-count clamping, filter reset when classes unchecked.
- **String resources**: Add `filter_classes_selected` (pluralized: "%d classes selected"), `filter_one_class_selected` ("1 class selected"), `filter_no_class` ("No filter"). Arabic translations required.

## Performance

- Student filtering + deduplication: `withContext(Dispatchers.Default)` for safety.
- Attendance record filtering: same dispatcher.
- UI responsive: derived state updates instantly since operations are O(n) on small lists.
