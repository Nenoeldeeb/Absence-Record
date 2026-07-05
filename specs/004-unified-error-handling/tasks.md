# Tasks: Unified Error Handling

**Input**: Design documents from `/specs/004-unified-error-handling/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/error-contracts.md

**Tests**: Tests are MANDATORY. Write tests for both `onSuccess` and `onFailure` paths.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Android App (Kotlin)**: `app/src/main/java/dev/nenoeldeeb/education/absencerecord/`
- **Unit Tests**: `app/src/test/java/dev/nenoeldeeb/education/absencerecord/`
- **UI Tests**: `app/src/androidTest/java/dev/nenoeldeeb/education/absencerecord/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Setup the expanded StudentError hierarchy and localized UI strings.

- [X] T001 [P] Extend `StudentError` sealed class with `FileWrite` and `ReportGeneration` objects in `domain/models/StudentError.kt`
- [X] T002 [P] Add string resources `error_writing_file` ("Error writing file.") and `error_generating_report` ("Failed to generate report.") in `res/values/strings.xml` and Arabic translations in `res/values-ar/strings.xml`
- [X] T003 Update `StudentErrorUiMapper.kt` to map `StudentError.FileWrite` and `StudentError.ReportGeneration` to their respective string resources in `presentation/utils/StudentErrorUiMapper.kt`

---

## Phase 2: Foundational (Blocking Repository Refactoring)

**Purpose**: Update repositories to catch and wrap data-source exceptions in typed `StudentError` subclasses.

**⚠️ CRITICAL**: No UI work can begin until this phase is complete.

- [X] T004 Update repository mapping for Room SQLite exceptions in `data/repositories/AttendanceRepositoryImpl.kt` to return `StudentError.Database`
- [X] T005 Update repository mapping for Room SQLite exceptions in `data/repositories/StudentRepositoryImpl.kt` to return `StudentError.Database`
- [X] T006 Update class database mappings and rename uniqueness checks in `data/repositories/StudentClassRepositoryImpl.kt` to return `StudentError.DuplicateClass` or `StudentError.Database`
- [X] T007 Update read/write exceptions in `data/repositories/StorageRepositoryImpl.kt` to return `StudentError.FileRead` or `StudentError.FileWrite`
- [X] T008 Update image compression and saving failures in `data/repositories/ReportRepositoryImpl.kt` to return `StudentError.ReportGeneration`
- [X] T009 Refactor unit tests for all repositories under `app/src/test/java/dev/nenoeldeeb/education/absencerecord/data/repositories/` to assert that failures map to exact `StudentError` subclasses

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel.

---

## Phase 3: User Story 1 - Graceful Database Failure Handling (Priority: P1) 🎯 MVP

**Goal**: Catch database exceptions and present them as transient SnackBar notifications to the user without breaking screen state or causing crashes.

**Independent Test**: Force repository failure on loading students or marking attendance and verify a Snackbar with localized "Database operation failed." is displayed.

### Tests for User Story 1
- [X] T010 [P] [US1] Update `CalendarViewModelTestBase.kt` and associated tests to assert `StudentError.Database` is handled correctly
- [X] T011 [P] [US1] Update `StudentsViewModelCrudTest.kt` to mock and assert repository failures return `StudentError.Database`
- [X] T012 [P] [US1] Update `ReportViewModelTest.kt` to assert `StudentError.Database` or `StudentError.ReportGeneration` is mapped correctly

### Implementation for User Story 1
- [X] T013 [US1] Propagate repository error mappings through attendance and class use cases under `domain/usecases/attendance/` and `domain/usecases/classes/`
- [X] T014 [US1] Propagate repository error mappings through student use cases under `domain/usecases/student/`
- [X] T015 [US1] Refactor `CalendarViewModel.kt` to receive `StudentError` failures and map them to UI Snackbars using `toUiText()`
- [X] T016 [US1] Refactor `ReportViewModel.kt` to handle report database/generation errors and map them to UI Snackbars
- [X] T017 [US1] Refactor `StudentsViewModel.kt` to handle database errors and display them via Snackbars

**Checkpoint**: User Story 1 is fully functional and testable independently.

---

## Phase 4: User Story 2 - Duplicate Class Creation Prevention (Priority: P1)

**Goal**: Reject duplicate class names at the business logic and UI level with clear user feedback.

**Independent Test**: Attempt to rename/add a class with an existing class name, verify it returns `StudentError.DuplicateClass`, and shows the localized warning.

### Tests for User Story 2
- [X] T018 [P] [US2] Update `ClassActionDelegateTest.kt` to mock and verify that duplicate class name additions/updates fail with `StudentError.DuplicateClass`

### Implementation for User Story 2
- [X] T019 [US2] Ensure `AddClassUseCase.kt` and `UpdateClassUseCase.kt` propagate `StudentError.DuplicateClass` failures from the repository
- [X] T020 [US2] Refactor `ClassActionDelegate.kt` to handle repository duplicate failures, return `StudentError.DuplicateClass`, and display the warning in the UI

**Checkpoint**: User Story 2 is fully functional and testable independently.

---

## Phase 5: User Story 3 - Robust File Import/Export Error Handling (Priority: P2)

**Goal**: Map file IO/parsing errors to `StudentError` subclasses and notify the user using localized Snackbars.

**Independent Test**: Import a corrupted file or cancel file selection, and verify appropriate localized error Snackbar is displayed.

### Tests for User Story 3
- [X] T021 [P] [US3] Update `JsonSerializationServiceTest.kt` to verify serialization errors map to `StudentError` subclasses
- [X] T022 [P] [US3] Update `StudentActionDelegateTest.kt` to mock and verify file IO / JSON parsing failures map to `StudentError` subclasses

### Implementation for User Story 3
- [X] T023 [US3] Update `JsonSerializationService.kt` to catch parsing exceptions and wrap them in `StudentError.ImportParse` or `StudentError.Serialization`
- [X] T024 [US3] Refactor `ParseImportFileUseCase.kt`, `ExportStudentsUseCase.kt`, and `PerformImportUseCase.kt` to validate inputs and map exceptions to `StudentError.Validation`, `StudentError.FileRead`, `StudentError.FileWrite`, `StudentError.ImportParse`
- [X] T025 [US3] Refactor `StudentActionDelegate.kt` and `ImportExportHandler.kt` to catch validation/IO exceptions and trigger localized Snackbars on failure

**Checkpoint**: User Story 3 is fully functional and testable independently.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Format, check, and optimize all modified code files.

- [X] T026 [P] Format code using `./gradlew ktlintFormat`
- [X] T027 Verify all tests and lints pass using `./gradlew check`
- [X] T028 Verify no updated source file (including tests) exceeds 300 lines (refactor if needed)
- [X] T029 Perform local testing on emulator to confirm Snackbar UI transitions look correct (manual - skipped in CLI)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately.
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all UI screens.
- **User Stories (Phase 3+)**: All depend on Foundational phase completion.
- **Polish (Final Phase)**: Depends on all desired user stories being complete.

---

## Notes

- Verify tests fail before implementing (TDD cycle).
- Standardize all return types in Data and Domain layers to `Result<T>`.
- Format code with `./gradlew ktlintFormat` before each commit.

---

## Phase 7: Convergence

**Purpose**: Close gaps identified during convergence assessment between spec requirements and current implementation.

- [X] T030 Implement Snackbar-based transient error display in CalendarScreen, ReportScreen, and StudentsScreen — replace CalendarScreen's inline error text with Snackbar, add SnackbarHost/SnackbarHostState/error consume pattern in ReportScreen and StudentsScreen per FR-008 (<gap-type: contradicts/missing>)
- [X] T031 Add missing onFailure handler in ReportViewModel.initializeClasses() to map class loading errors to error state per FR-005 (<gap-type: missing>)
- [X] T032 Fix Arabic string typo in values-ar/strings.xml: `error_class_name_already_exists` — change `الاسمز` to `الاسم` per FR-006 (<gap-type: partial>)
