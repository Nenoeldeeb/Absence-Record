# Implementation Plan: Class Filter Refactor

**Branch**: `002-class-filter-refactor` | **Date**: 2026-06-20 | **Spec**: `specs/002-class-filter-refactor/spec.md`

**Input**: Feature specification from `/specs/002-class-filter-refactor/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

## Summary

Move class filter from a separate UI element (top bar icon + dropdown above calendar) into the attendance dialog as a multi-select checkbox dropdown. Students from selected classes are merged (deduplicated by ID), total count updates dynamically, and marked-present count never exceeds visible count.

## Technical Context

**Language/Version**: Kotlin 2.4.0+ / Kotlin JVM 25

**Primary Dependencies**: Jetpack Compose, Room, Kotlinx Datetime, Kotlinx Serialization

**Storage**: Room SQLite database

**Testing**: JUnit 5, MockK, Compose Test Rules, runTest

**Target Platform**: Android (API 26+)

**Project Type**: Android App Module (Clean + MVI)

**Performance Goals**: Responsive UI at 60+ fps, no Main-thread blocking for CPU-intensive operations (parsing/db/serialization). Student list filtering and deduplication must run off main thread using `withContext(Dispatchers.Default)`.

**Constraints**: Main safe backgrounding via dispatchers, manual DI, Arabic localization support, no `java.time`, no hardcoded strings.

**Scale/Scope**: Local offline-first architecture. Changes scoped to CalendarScreen and CalendarViewModel only. No data model changes.

**Key Unknowns**:
- Multi-select filter model: `Set<Int>` (class IDs) vs new sealed interface
- Checkbox dropdown component: build custom vs extend existing `ClassFilterDropdown`
- Deduplication performance with large class sets
- Present count overflow enforcement location (ViewModel vs UI)

## Constitution Check

_GATE: Must pass before Phase 0 research. Re-check after Phase 1 design._

- [x] Clean Architecture Check: Are Domain, Data, and Presentation layers separated?
- [x] MVI / UDF Check: Is there a single immutable ScreenState, a ScreenEvent sealed interface, and lifecycle-aware collection?
- [x] Technology Check: Are we using `kotlinx-datetime`, `@Serializable`, Room, and `strings.xml` / `UiText`?
- [x] Test Check: Do we have test cases for both onSuccess and onFailure paths?
- [x] Main-Safety Check: Are intensive tasks offloaded to appropriate background dispatchers?
- [x] File Size Check: Does every source file (including tests) stay within 300 lines?
- [x] Accessibility Check: Are all interactive composables accessible (content descriptions, 48dp touch targets, contrast)?

**Gate Status: PASS** — No violations to justify.

**Post-Design Re-evaluation**: PASS. All seven checks verified against final design. Domain/data layers untouched. MVI state with `@Stable`, immutable copy, sealed events. No new technology introduced. Tests for both paths. Main-safety via `withContext(Dispatchers.Default)`. CalendarScreen.kt (272 lines) under 300; if `ClassCheckboxFilter` exceeds space, extract to separate file. Accessibility via Material3 checkbox + contentDescriptions.

## Project Structure

### Documentation (this feature)

```text
specs/002-class-filter-refactor/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
└── tasks.md             # Phase 2 output (NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
app/src/main/java/dev/nenoeldeeb/education/absencerecord/
├── app/                  # Application container & manual DI providers
├── domain/
│   ├── models/           # Domain models (Student, StudentClass, StudentAttendance)
│   ├── repositories/     # Repository interfaces
│   ├── usecases/         # Single-action use cases
│   └── services/         # Service interfaces
├── data/
│   ├── datasources/local/# Room Entities, DAOs, Database
│   ├── repositories/     # Repository implementations
│   └── mappers/          # Entity <-> Domain mappers
└── presentation/
    ├── screens/
    │   ├── components/   # Shared UI components (ClassFilter, ClassFilterDropdown)
    │   │                 # ⚠ ClassFilterDropdown stays unchanged (used by Report, Students)
    │   └── calendar/     # Calendar feature files
    │       ├── CalendarScreen.kt      # MODIFY: remove top filter, add dialog dropdown
    │       ├── CalendarViewModel.kt   # MODIFY: multi-select filter logic
    │       ├── CalendarScreenState.kt # MODIFY: new filter fields
    │       └── CalendarScreenEvent.kt # MODIFY: new filter events
    ├── theme/            # Theme settings
    └── utils/            # UI wrappers/helpers (UiText, StudentFilters)
        # StudentFilters.kt  # MODIFY: add multi-class filter extension
```

**Structure Decision**: Feature-scoped changes within `presentation/screens/calendar/`. Shared `ClassFilter` and `ClassFilterDropdown` in `components/` remain unchanged. New multi-select filter for calendar uses `Set<Int>` directly in state, not a shared sealed interface.

## Complexity Tracking

No violations — all gates pass without need for complexity justification.
