# Implementation Plan: Unified Error Handling

**Branch**: `004-unified-error-handling` | **Date**: 2026-07-05 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/004-unified-error-handling/spec.md`

## Summary

This refactoring unifies error handling across the Entire Absence Record codebase using the `StudentError` sealed class hierarchy. It guarantees that all exceptions in the Data layer (database, file system, serialization) are mapped to `StudentError` at boundaries, preventing raw system Exceptions from leaking to Domain UseCases or Presentation ViewModels, and displays transient errors to users via Snackbars.

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

## Constitution Check

_GATE: Must pass before Phase 0 research. Re-check after Phase 1 design._

- [x] Clean Architecture Check: Are Domain, Data, and Presentation layers separated?
- [x] MVI / UDF Check: Is there a single immutable ScreenState, a ScreenEvent sealed interface, and lifecycle-aware collection?
- [x] Technology Check: Are we using `kotlinx-datetime`, `@Serializable`, Room, and `strings.xml` / `UiText`?
- [x] Test Check: Do we have test cases for both onSuccess and onFailure paths?
- [x] Main-Safety Check: Are intensive tasks offloaded to appropriate background dispatchers?
- [x] File Size Check: Does every source file (including tests) stay within 300 lines?
- [x] Accessibility Check: Are all interactive composables accessible (content descriptions, 48dp touch targets, contrast)?

## Project Structure

### Documentation (this feature)

```text
specs/004-unified-error-handling/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/
│   └── error-contracts.md # Interface contract specification
└── checklists/
    └── requirements.md    # Quality checklist
```

### Source Code (repository root)

```text
app/src/main/java/dev/nenoeldeeb/education/absencerecord/
├── app/                  # Application container & manual DI providers
├── domain/
│   ├── models/           # Domain models (StudentError.kt)
│   ├── repositories/     # Repository interfaces
│   ├── usecases/         # Single-action use cases
│   └── services/         # Service interfaces (SerializationService.kt)
├── data/
│   ├── datasources/local/# Room Entities, DAOs, Database
│   ├── repositories/     # Repository implementations
│   └── mappers/          # Entity <-> Domain mappers
└── presentation/
    ├── screens/
    │   ├── components/   # Shared UI components
    │   └── [feature]/    # Feature specific files
    ├── theme/            # Theme settings
    └── utils/            # UI wrappers/helpers (StudentErrorUiMapper.kt, UiText)
```

**Structure Decision**: Confirmed existing clean codebase layout.

## Complexity Tracking

> **No violations of the Constitution identified.**

---

## Detailed Phases

### Phase 0: Outline & Research
- Audited all 25 raw exception leak sites in repository implementations, serialization utilities, and use cases.
- Decided on non-blocking transient Snackbar display to preserve user flow and silent exception handling at repositories with zero network telemetry/local tracing.
- Consolidated findings in `research.md`.

### Phase 1: Design & Contracts
- Defined the final `StudentError` sealed class hierarchy with added `FileWrite` and `ReportGeneration` error classes.
- Specified contracts for `StudentError.toUiText()` mapper in both English and Arabic.
- Created unit-testable validation flows in `quickstart.md`.

### Phase 2: Implementation Steps
1. **Extend StudentError**: Add `FileWrite` and `ReportGeneration` objects to `StudentError.kt`.
2. **Add Resource Strings**: Add `error_writing_file` and `error_generating_report` to `strings.xml` and `values-ar/strings.xml`.
3. **Extend StudentErrorUiMapper**: Map the new error subclasses to their respective string resources.
4. **Refactor Repositories**: Update `AttendanceRepositoryImpl`, `StudentRepositoryImpl`, `StudentClassRepositoryImpl`, `StorageRepositoryImpl`, and `ReportRepositoryImpl` to catch and map all database/file exceptions to `StudentError` subclasses.
5. **Refactor Use Cases**: Refactor validation exceptions in UseCases to use `StudentError.Validation` and ensure no raw throwables escape to presentation layers.
6. **Refactor ViewModels & Delegates**: Clean up ViewModels and ActionDelegates to rely on the repository-mapped `StudentError` and update UI error state using the mapper.
7. **Verify & Format**: Run `./gradlew ktlintFormat` and `./gradlew check` to ensure all tests pass and code styling matches the rules.
