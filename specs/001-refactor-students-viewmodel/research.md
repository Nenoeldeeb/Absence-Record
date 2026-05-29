# Research: Refactor Students ViewModel

**Branch**: `001-refactor-students-viewmodel` | **Date**: 2026-05-29 | **Spec**: [spec.md](file:///home/nenoeldeeb/Projects/AndroidStudioProjects/Absencerecord/specs/001-refactor-students-viewmodel/spec.md)

## Current State Analysis

### ViewModel (574 lines → target <300)
- 12 methods: 1 public `onEvent` (101 lines) + 11 private methods
- Handles 21 event variants via `when` block
- Already imports `SelectionStateDelegate` and `ImportExportDelegate` in constructor
- Has an unused `exportSelectionMap` field in state (dead code)

### Existing Delegates
- `SelectionStateDelegate` (53 lines): Pure stateless functions for multi-selection (toggle, selectAll, clear, toggleMode)
- `ImportExportDelegate` (84 lines): Import dialog state transformations (prepareImportDialog, toggleImportSelection, buildImportResultMessage)

### Missing Delegates (to be created)
- `StudentActionDelegate`: Student CRUD (add, update, delete selected, export, export-and-delete)
- `ClassActionDelegate`: Class CRUD (add, rename, delete, drop filter on deleted class)

### Domain Layer (already exists, out of scope)
| Use Case | Purpose | Methods |
|----------|---------|---------|
| `AddStudentUseCase` | Insert student | `suspend invoke(Student): Result<Long>` |
| `UpdateStudentUseCase` | Update student | `suspend invoke(Student): Result<Unit>` |
| `DeleteStudentsUseCase` | Bulk delete | `suspend invoke(List<Student>): Result<Unit>` |
| `GetAllStudentsUseCase` | Observe all | `Flow<Result<List<Student>>>` |
| `AddClassUseCase` | Insert class | `suspend invoke(StudentClass): Result<Long>` |
| `UpdateClassUseCase` | Rename class | `suspend invoke(StudentClass): Result<Unit>` |
| `DeleteClassUseCase` | Delete class | `suspend invoke(StudentClass): Result<Unit>` |
| `GetAllClassesUseCase` | Observe all | `Flow<Result<List<StudentClass>>>` |
| `ImportStudentsUseCase` | Import flow | `parseFile(uri): Result<List<ParsedStudentImportData>>`, `performImport(parsed, selection): Result<ImportResult>` |
| `ExportStudentsUseCase` | Export flow | `invoke(uri, selectedIds, allStudents): Result<Unit>` |

### Aggregates (DI entry points)
- `StudentManagementUseCases`: Holds add/update/delete/getAll/import/export use cases
- `ClassManagementUseCases`: Holds add/update/delete/getAll class use cases

### Data Layer (out of scope)
- Room DB with `StudentEntity`, `StudentClassEntity`, `StudentAttendanceEntity`
- DAOs: `StudentDao`, `StudentClassDao`, `AttendanceDao`
- Repositories: `StudentRepository`, `StudentClassRepository`, `StorageRepository`, `AttendanceRepository`

### Dead Code Identified
- `StudentsScreenState.exportSelectionMap: Map<Int, Boolean>` — never written to in ViewModel or delegates

## Design Decisions

### Decision 1: StudentActionDelegate Scope
- **Decision**: Student CRUD + bulk delete + export + export-and-delete
- **Rationale**: Co-locates all student-mutating operations in one delegate, matching the existing `StudentManagementUseCases` aggregate. The ViewModel calls one delegate method per event instead of 5 separate use case calls.
- **Alternatives considered**: Per-operation delegates (too fragmented), inline in ViewModel (current, too large)

### Decision 2: ClassActionDelegate Scope
- **Decision**: Class CRUD + class filter management
- **Rationale**: All class operations in one place. Filter fallback logic (reset to "All" when filtered class is deleted) lives here rather than in the ViewModel.
- **Alternatives considered**: Keep filter logic in ViewModel (violates extraction goal)

### Decision 3: Delegate Return Pattern
- **Decision**: Delegates return `Result<T, StudentError>` for operations; `Result<UiText?>` for operations that produce a toast message
- **Rationale**: Matches AGENTS.md rule of standardized `Result<T>` return types. The ViewModel handles onSuccess/onFailure for UI state updates.
- **Alternatives considered**: Callback pattern (more complex), throwing exceptions (violates architecture)

### Decision 4: Loading State Ownership
- **Decision**: ViewModel sets `_uiState.update { it.copy(isLoading...) }` before/after delegate calls
- **Rationale**: Keeps delegates stateless testable pure functions. ViewModel already owns the state mutation lifecycle.
- **Spec reference**: Clarification Session 2026-05-29, Q5

### Decision 5: Import Two-Phase Design
- **Decision**: `StudentActionDelegate` wraps `ImportStudentsUseCase.parseFile()` for phase 1 (parse → return preview data), and `ImportStudentsUseCase.performImport()` for phase 2 (persist selections)
- **Rationale**: The use case already implements this two-phase pattern. The delegate acts as a thin adapter — no need to reimplement.
- **Spec reference**: Clarification Session 2026-05-29, Q4

### Decision 6: StudentError Design
- **Decision**: Sealed interface `StudentError` with variants: `Database`, `DuplicateClass`, `FileRead`, `ImportParse`, `Cancelled`
- **Rationale**: Typed errors map directly to `UiText` resources in the ViewModel. Covers all failure modes from spec edge cases.
- **Spec reference**: Clarification Session 2026-05-29, Q3

### Decision 7: Dead Code Removal
- **Decision**: Remove `exportSelectionMap` from `StudentsScreenState` (unused)
- **Rationale**: Dead code eliminated as part of refactoring. No behavioral change (SC-003).
