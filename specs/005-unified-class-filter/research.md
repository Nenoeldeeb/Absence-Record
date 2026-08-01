# Research: Unified Class Filter System

## Research Tasks & Decisions

### Decision 1: Shared Global Filter State Management Architecture

- **Decision**: Introduce a `ClassFilterRepository` in `domain/repositories/` backed by an in-memory `ClassFilterRepositoryImpl` in `data/repositories/` holding a `MutableStateFlow<Set<Int>>`, provided as a singleton dependency via `AppContainer`.
- **Rationale**:
  - The feature specification requires real-time filter synchronization across the Attendance dialog (in Calendar screen), Report screen, and Students screen (`FR-002`, `FR-003`).
  - An in-memory `StateFlow<Set<Int>>` initialized with `emptySet()` guarantees that the filter selection state is shared seamlessly during a session while resetting to the default state (`emptySet()`, showing unassigned students) on every cold restart (`FR-004`, `FR-009`).
  - Keeping it in the domain repository layer preserves Clean Architecture and MVI state observation via ViewModels.
- **Alternatives Considered**:
  - *Persisting to Room / DataStore*: Rejected because `FR-009` explicitly requires resetting to the default unchecked state on app cold start.
  - *Passing filter state as navigation parameters*: Rejected because navigation back-and-forth does not maintain dynamic synchronization between active screens/dialogs without complex state passing.

---

### Decision 2: Shared UI Composable Location & Refactoring

- **Decision**: Refactor `ClassCheckboxFilter` from `presentation/screens/calendar/ClassCheckboxFilter.kt` into `presentation/screens/components/ClassCheckboxFilter.kt` as a public/internal shared composable. Remove single-select `ClassFilterDropdown.kt` and `ClassFilter.kt`.
- **Rationale**:
  - The Report screen and Students screen currently use single-select `ClassFilterDropdown.kt`.
  - Moving `ClassCheckboxFilter` to `presentation/screens/components/` allows all three screens to consume the exact same multi-select checkbox UI component (`FR-001`), ensuring 100% UI consistency (`SC-001`).
- **Alternatives Considered**:
  - *Duplicating `ClassCheckboxFilter` composable in each screen package*: Rejected because it violates DRY principles and creates maintenance overhead.

---

### Decision 3: ViewModel State & Filter Extension Standardisation

- **Decision**: Update `ReportScreenState` and `StudentsScreenState` to use `selectedClassIds: Set<Int> = emptySet()` instead of `selectedClassFilter: ClassFilter = ClassFilter.All`. Standardize student list filtering using `applyMultiClassFilter(selectedClassIds)`.
- **Rationale**:
  - `applyMultiClassFilter` in `presentation/utils/StudentFilters.kt` already handles filtering logic:
    - `classIds.isEmpty()` -> returns students where `it.classId == null` (unassigned students).
    - `classIds.isNotEmpty()` -> returns students where `it.classId != null && it.classId in classIds`.
  - This perfectly aligns with `FR-005` (0 checked = unassigned students) and `FR-006` / `FR-008` (checked classes = enrolled students in those classes).
- **Alternatives Considered**:
  - *Keeping `ClassFilter.All` / `ClassFilter.Unassigned` sealed interface*: Rejected because `Set<Int>` is simpler, type-safe, and directly handles multi-selection state across all ViewModels.
