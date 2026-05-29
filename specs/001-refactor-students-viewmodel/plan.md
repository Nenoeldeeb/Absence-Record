# Implementation Plan: Refactor Students ViewModel

**Branch**: `001-refactor-students-viewmodel` | **Date**: 2026-05-29 | **Spec**: [spec.md](file:///home/nenoeldeeb/Projects/AndroidStudioProjects/Absencerecord/specs/001-refactor-students-viewmodel/spec.md)

**Input**: Feature specification from `specs/001-refactor-students-viewmodel/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

## Summary

Refactor `StudentsViewModel.kt` (574 lines) to under 300 lines by extracting remaining business logic into two action delegates (`StudentActionDelegate`, `ClassActionDelegate`). The ViewModel currently has 11 private methods + a 101-line `onEvent` dispatcher handling 21 event variants. Two delegates already exist (`SelectionStateDelegate`, `ImportExportDelegate`) — the refactoring consolidates and complements them so the ViewModel becomes a thin orchestration layer.

## Technical Context

**Language/Version**: Kotlin 2.4.0+ / Kotlin JVM 25

**Primary Dependencies**: Jetpack Compose, Room, Kotlinx Datetime, Kotlinx Serialization

**Storage**: Room SQLite database

**Testing**: JUnit, MockK, Compose rules, runTest

**Target Platform**: Android (API 26+)

**Project Type**: Android App Module (Clean + MVI)

**Performance Goals**: Responsive UI at 60+ fps, no Main-thread blocking for CPU-intensive operations (parsing/db/serialization).

**Constraints**: Main safe backgrounding via dispatchers, manual DI, Arabic localization support.

**Scale/Scope**: Local offline-first architecture.

**Current ViewModel State**: 574 lines, 12 methods (11 private + 1 public onEvent), 21 event variants in StudentsScreenEvent, 19 fields in StudentsScreenState.

**Existing Delegates**: `SelectionStateDelegate` (53 lines, pure stateless selection logic), `ImportExportDelegate` (84 lines, import dialog state transformations).

**Clarified Design Decisions** (from Spec Session 2026-05-29):
- **Delegates**: `StudentActionDelegate` (student CRUD, bulk, export) + `ClassActionDelegate` (class CRUD, filtering).
- **Import/Export format**: JSON via `kotlinx.serialization @Serializable`.
- **Error handling**: Sealed `StudentError` interface (`Database`, `DuplicateClass`, `FileRead`, `ImportParse`, `Validation`, `Cancelled`).
- **Import flow**: Two-phase (parse → preview → confirm → persist).
- **Loading state**: ViewModel-owned; delegates return pure `Result<T>`.
- **Out of scope**: UI composables, data layer (repositories/DAOs/Room entities), domain layer.

## Constitution Check

_GATE: Must pass before Phase 0 research. Re-check after Phase 1 design._

- [x] Clean Architecture Check: Layers already separated. Delegates sit in presentation layer and call domain use cases via repository interfaces.
- [x] MVI / UDF Check: Single `@Stable` ScreenState, sealed ScreenEvent interface, `collectAsStateWithLifecycle()` in composable — all preserved.
- [x] Technology Check: `kotlinx-datetime`, `@Serializable`, Room, `strings.xml`/`UiText` — all present and used.
- [x] Test Check: Existing tests cover onSuccess/onFailure paths (FR-004). New delegate tests required (FR-005/SC-004).
- [x] Main-Safety Check: Intensive ops (JSON parse/serialize, DB writes) offloaded via `withContext(Dispatchers.IO)` in existing pattern.

No violations. All gates pass.

## Project Structure

### Documentation (this feature)

```text
specs/001-refactor-students-viewmodel/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/
│   ├── student-action-delegate.md
│   └── class-action-delegate.md
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Re-evaluation (Post-Phase 1)

_GATE: Phase 1 design re-check_

- [x] Clean Architecture Check: Delegates in presentation layer call domain use cases only. No layer leakage.
- [x] MVI / UDF Check: ViewModel retains single `onEvent` dispatch, single `_uiState.update` mutation, delegates return `Result<T>`.
- [x] Technology Check: JSON via `@Serializable` confirmed; import two-phase already uses `ImportStudentsUseCase`.
- [x] Test Check: Each delegate method independently testable with mocked use cases. Existing test structure preserved.
- [x] Main-Safety Check: Use cases handle `withContext(Dispatchers.IO)` for DB/IO ops. Delegates are suspend but don't manage dispatchers.

No violations. All design gates pass.

### Source Code (repository root)

```text
app/src/main/java/dev/nenoeldeeb/education/absencerecord/
├── app/                  # Application container & manual DI providers
├── domain/
│   ├── models/           # Domain models
│   │   └── StudentError.kt   # NEW
│   ├── repositories/     # Repository interfaces
│   ├── usecases/         # Single-action use cases
│   └── services/         # Service interfaces
├── data/
│   ├── datasources/local/# Room Entities, DAOs, Database
│   ├── repositories/     # Repository implementations
│   └── mappers/          # Entity <-> Domain mappers
└── presentation/
    ├── screens/
    │   ├── components/   # Shared UI components
    │   └── students/     # Feature-specific files
    │       ├── Screen.kt
    │       ├── ViewModel.kt
    │       ├── ScreenState.kt
    │       ├── ScreenEvent.kt
    │       ├── delegates/
    │       │   ├── StudentActionDelegate.kt    # NEW
    │       │   ├── ClassActionDelegate.kt      # NEW
    │       │   ├── SelectionStateDelegate.kt   # EXISTS (53 lines)
    │       │   └── ImportExportDelegate.kt     # EXISTS (84 lines)
    │       ├── components/
    │       │   └── MultiSelectionHeader.kt
    │       └── dialogs/
    │           ├── BulkDeleteConfirmationDialog.kt
    │           ├── ImportSelectionDialog.kt
    │           ├── ManageClassesDialog.kt
    │           └── StudentDialog.kt
    ├── theme/            # Theme settings
    └── utils/            # UI wrappers/helpers (e.g. UiText)
```

**Structure Decision**: New delegates placed in existing `delegates/` subdirectory alongside `SelectionStateDelegate` and `ImportExportDelegate`. No new package or module — keeps cohesion within the feature package.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

No violations. No complexity tracking needed.
