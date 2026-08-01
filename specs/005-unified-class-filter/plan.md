# Implementation Plan: Unified Class Filter System

**Branch**: `005-unified-class-filter` | **Date**: 2026-07-31 | **Spec**: [spec.md](file:///home/nenoeldeeb/Projects/AndroidStudioProjects/Absencerecord/specs/005-unified-class-filter/spec.md)

**Input**: Feature specification from `specs/005-unified-class-filter/spec.md`

## Summary

Unify the class filter interaction across Attendance dialog (Calendar screen), Report screen, and Students screen by replacing the single-select `ClassFilterDropdown` on Report and Students screens with the multi-select `ClassCheckboxFilter`. Introduce a centralized `ClassFilterRepository` in the domain/data layers to manage a shared in-memory `StateFlow<Set<Int>>` filter state, ensuring real-time filter synchronization across all three views while defaulting to an empty set (unassigned students) on cold start.

## Technical Context

**Language/Version**: Kotlin 2.4.0+ / Kotlin JVM 25

**Primary Dependencies**: Jetpack Compose, Room, Kotlinx Datetime, Kotlinx Serialization

**Storage**: Room SQLite database (Student & Class tables), in-memory `StateFlow<Set<Int>>` for filter synchronization.

**Testing**: JUnit, MockK, Compose rules, runTest

**Target Platform**: Android (API 26+)

**Project Type**: Android App Module (Clean + MVI)

**Performance Goals**: Responsive UI at 60+ fps, instant filter synchronization across screens without Main-thread blocking.

**Constraints**: Main safe backgrounding via dispatchers, manual DI (`AppContainer` & `AppViewModelProvider`), max line count <= 300 lines per file, Arabic localization support.

**Scale/Scope**: Local offline-first architecture.

## Constitution Check

_GATE: Must pass before Phase 0 research. Re-check after Phase 1 design._

- [x] Clean Architecture Check: Domain (`ClassFilterRepository`), Data (`ClassFilterRepositoryImpl`), and Presentation (ViewModels, UI components) layers are strictly separated.
- [x] MVI / UDF Check: ViewModels maintain immutable ScreenState, handle ScreenEvent, and collect `selectedClassIds` with lifecycle awareness.
- [x] Technology Check: Using standard Compose components, `Set<Int>` state, and `strings.xml` string resources (`UiText`).
- [x] Test Check: Unit tests will cover `selectedClassIds` emission, filtering logic, and ViewModel state bindings for both empty and non-empty selections.
- [x] Main-Safety Check: State updates use Kotlin StateFlow, and background operations use coroutine dispatchers.
- [x] File Size Check: All created/modified source files (including components and test files) stay strictly under 300 lines.
- [x] Accessibility Check: `ClassCheckboxFilter` includes proper `contentDescription` for checkboxes and minimum 48dp touch targets.

## Project Structure

### Documentation (this feature)

```text
specs/005-unified-class-filter/
├── plan.md              # Implementation plan
├── research.md          # Technical decisions & research
├── data-model.md        # Entities & state mappings
├── quickstart.md        # Validation scenarios & test commands
└── contracts/           # Repository contract definitions
    └── class_filter_repository.md
```

### Source Code (repository root)

```text
app/src/main/java/dev/nenoeldeeb/education/absencerecord/
├── app/
│   ├── AppContainer.kt                   # Adds classFilterRepository
│   └── AppViewModelProvider.kt           # Passes repository to ViewModels
├── domain/
│   └── repositories/
│       └── ClassFilterRepository.kt       # New repository interface
├── data/
│   └── repositories/
│       └── ClassFilterRepositoryImpl.kt   # New repository implementation
└── presentation/
    ├── screens/
    │   ├── components/
    │   │   ├── ClassCheckboxFilter.kt    # Shared multi-select checkbox filter
    │   │   └── ClassFilterDropdown.kt    # Deprecated/removed
    │   ├── calendar/
    │   │   ├── CalendarViewModel.kt      # Observes ClassFilterRepository
    │   │   └── CalendarScreenState.kt
    │   ├── report/
    │   │   ├── ReportViewModel.kt        # Observes ClassFilterRepository
    │   │   ├── ReportScreenState.kt      # Uses selectedClassIds: Set<Int>
    │   │   └── ReportScreenEvent.kt
    │   └── students/
    │       ├── StudentsViewModel.kt      # Observes ClassFilterRepository
    │       ├── StudentsScreenState.kt    # Uses selectedClassIds: Set<Int>
    │       └── StudentsScreenEvent.kt
    └── utils/
        └── StudentFilters.kt             # Cleaned up multi-class filter extensions
```

**Structure Decision**: Clean Architecture with MVI structure, moving `ClassCheckboxFilter` to `presentation/screens/components/` for shared access, and placing `ClassFilterRepository` in `domain/repositories/`.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|--------------------------------------|
| None      | N/A        | N/A                                  |
