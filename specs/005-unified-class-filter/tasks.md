# Tasks: Unified Class Filter System

**Input**: Design documents from `/specs/005-unified-class-filter/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: Unit tests for repository, ViewModels, and filter functions are MANDATORY. Verify both empty and active filter state paths.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Includes exact file paths in descriptions

## Path Conventions

- **Android App**: `app/src/main/java/dev/nenoeldeeb/education/absencerecord/`
- **Unit Tests**: `app/src/test/java/dev/nenoeldeeb/education/absencerecord/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create repository interface, implementation, and dependency injection setup for class filter state synchronization.

- [X] T001 Create `ClassFilterRepository` interface with `selectedClassIds: StateFlow<Set<Int>>`, `toggleClass(classId: Int)`, `setSelectedClassIds(classIds: Set<Int>)`, and `clearFilter()` in `app/src/main/java/dev/nenoeldeeb/education/absencerecord/domain/repositories/ClassFilterRepository.kt`
- [X] T002 Implement `ClassFilterRepositoryImpl` holding an in-memory `MutableStateFlow<Set<Int>>` initialized to `emptySet()` in `app/src/main/java/dev/nenoeldeeb/education/absencerecord/data/repositories/ClassFilterRepositoryImpl.kt`
- [X] T003 Write unit tests for `ClassFilterRepositoryImpl` testing initial `emptySet()`, `toggleClass`, `setSelectedClassIds`, and `clearFilter` in `app/src/test/java/dev/nenoeldeeb/education/absencerecord/data/repositories/ClassFilterRepositoryImplTest.kt`
- [X] T004 Register `classFilterRepository` in `app/src/main/java/dev/nenoeldeeb/education/absencerecord/app/AppContainer.kt` and inject into ViewModels in `app/src/main/java/dev/nenoeldeeb/education/absencerecord/app/AppViewModelProvider.kt`

---

## Phase 2: Foundational Components Refactoring

**Purpose**: Shared UI components and utility function cleanup required before UI integration.

**⚠️ CRITICAL**: Must be completed before updating individual screen ViewModels.

- [X] T005 Move `ClassCheckboxFilter.kt` from `calendar/ClassCheckboxFilter.kt` to shared UI package in `app/src/main/java/dev/nenoeldeeb/education/absencerecord/presentation/screens/components/ClassCheckboxFilter.kt` and update import references in `CalendarScreen.kt`
- [X] T006 Clean up `app/src/main/java/dev/nenoeldeeb/education/absencerecord/presentation/utils/StudentFilters.kt` to standardize on `applyMultiClassFilter(classIds: Set<Int>)` and deprecate old `applyClassFilter`
- [X] T007 Safely delete unused `ClassFilter.kt` and `ClassFilterDropdown.kt` from `app/src/main/java/dev/nenoeldeeb/education/absencerecord/presentation/screens/components/` and clean up any unused references in `ClassActionDelegate.kt`

**Checkpoint**: Foundation ready - UI story integration can now proceed.

---

## Phase 3: User Story 1 - Multi-Select Class Filtering on Report and Students Screens (Priority: P1) 🎯 MVP

**Goal**: Replace single-select dropdown on Report and Students screens with multi-select `ClassCheckboxFilter` component.

**Independent Test**: Open Report screen or Students screen, click filter button, and verify multi-select checkbox dropdown renders and filters student list by `selectedClassIds: Set<Int>`.

### Implementation for User Story 1

- [X] T008 [P] [US1] Update `ReportScreenState` to replace `selectedClassFilter: ClassFilter` with `selectedClassIds: Set<Int> = emptySet()` in `app/src/main/java/dev/nenoeldeeb/education/absencerecord/presentation/screens/report/ReportScreenState.kt`
- [X] T009 [P] [US1] Update `ReportScreenEvent` to replace `SelectClassFilter` with `ToggleClassFilter(val classId: Int)` in `app/src/main/java/dev/nenoeldeeb/education/absencerecord/presentation/screens/report/ReportScreenEvent.kt`
- [X] T010 [P] [US1] Update `StudentsScreenState` to replace `selectedClassFilter: ClassFilter` with `selectedClassIds: Set<Int> = emptySet()` in `app/src/main/java/dev/nenoeldeeb/education/absencerecord/presentation/screens/students/StudentsScreenState.kt`
- [X] T011 [P] [US1] Update `StudentsScreenEvent` to replace `SelectClassFilter` with `ToggleClassFilter(val classId: Int)` in `app/src/main/java/dev/nenoeldeeb/education/absencerecord/presentation/screens/students/StudentsScreenEvent.kt`
- [X] T012 [US1] Replace `ClassFilterDropdown` usage with `ClassCheckboxFilter` component in `app/src/main/java/dev/nenoeldeeb/education/absencerecord/presentation/screens/report/ReportScreen.kt`
- [X] T013 [US1] Replace `ClassFilterDropdown` usage with `ClassCheckboxFilter` component in `app/src/main/java/dev/nenoeldeeb/education/absencerecord/presentation/screens/students/StudentsScreen.kt`

**Checkpoint**: User Story 1 complete - Report screen and Students screen use multi-select checkbox class filter.

---

## Phase 4: User Story 2 - Synchronized Class Filter Across All Views (Priority: P1)

**Goal**: Synchronize class filter selection across Attendance dialog (Calendar screen), Report screen, and Students screen via `ClassFilterRepository`.

**Independent Test**: Toggle a class filter option on Students screen, then navigate to Report screen and Attendance dialog to verify the exact same class checkboxes are checked and active.

### Implementation for User Story 2

- [X] T014 [US2] Connect `ReportViewModel` to observe `classFilterRepository.selectedClassIds` and delegate `ToggleClassFilter` events to repository in `app/src/main/java/dev/nenoeldeeb/education/absencerecord/presentation/screens/report/ReportViewModel.kt`
- [X] T015 [US2] Connect `StudentsViewModel` to observe `classFilterRepository.selectedClassIds` and delegate `ToggleClassFilter` events to repository in `app/src/main/java/dev/nenoeldeeb/education/absencerecord/presentation/screens/students/StudentsViewModel.kt`
- [X] T016 [US2] Connect `CalendarViewModel` to observe `classFilterRepository.selectedClassIds` and delegate class toggles to repository in `app/src/main/java/dev/nenoeldeeb/education/absencerecord/presentation/screens/calendar/CalendarViewModel.kt`
- [X] T017 [P] [US2] Update unit tests in `ReportViewModelTest.kt` to verify state flow synchronization with `ClassFilterRepository` in `app/src/test/java/dev/nenoeldeeb/education/absencerecord/presentation/screens/report/ReportViewModelTest.kt`
- [X] T018 [P] [US2] Update unit tests in `StudentsViewModelTest.kt` to verify state flow synchronization with `ClassFilterRepository` in `app/src/test/java/dev/nenoeldeeb/education/absencerecord/presentation/screens/students/StudentsViewModelTest.kt`
- [X] T019 [P] [US2] Update unit tests in `CalendarViewModelTest.kt` to verify state flow synchronization with `ClassFilterRepository` in `app/src/test/java/dev/nenoeldeeb/education/absencerecord/presentation/screens/calendar/CalendarViewModelTest.kt`

**Checkpoint**: User Story 2 complete - Class filter state is real-time synchronized across all three views.

---

## Phase 5: User Story 3 - Default Unchecked State and Unassigned Student View (Priority: P2)

**Goal**: Verify that cold start / default state has 0 checkboxes checked (showing unassigned students) and checking all classes shows enrolled students across all classes without any "Select All" dropdown entry.

### Implementation for User Story 3

- [X] T020 [US3] Write unit tests in `StudentFiltersTest.kt` verifying `applyMultiClassFilter` filtering logic: `emptySet()` returns unassigned students (`classId == null`), and non-empty `Set<Int>` returns enrolled students in those classes in `app/src/test/java/dev/nenoeldeeb/education/absencerecord/presentation/utils/StudentFiltersTest.kt`
- [X] T021 [US3] Verify `ClassCheckboxFilter.kt` contains no "Select All" option and correctly displays label string resources for 0 checked (`class_filter_none`), 1 checked (`class_filter_one`), and N checked (`class_filter_n`) in `app/src/main/java/dev/nenoeldeeb/education/absencerecord/presentation/screens/components/ClassCheckboxFilter.kt`

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Format code, run full check validation, audit file line limits, and update project documentation per Constitution rules.

- [X] T022 [P] Format code using `./gradlew ktlintFormat` across the codebase
- [X] T023 Verify all tests and lints pass using `./gradlew check`
- [X] T024 Audit all modified source files to verify no file exceeds the 300-line limit
- [X] T025 Update `AGENTS.md` and `README.md` to document the unified `ClassFilterRepository` and `ClassCheckboxFilter` architecture per Constitution Governance rules

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately.
- **Foundational (Phase 2)**: Depends on Phase 1 - BLOCKS UI integration.
- **User Story 1 (Phase 3)**: Depends on Phase 2 completion.
- **User Story 2 (Phase 4)**: Depends on Phase 3 completion.
- **User Story 3 (Phase 5)**: Depends on Phase 4 completion.
- **Polish (Phase 6)**: Depends on all User Stories completion.

---

## Notes

- Verify unit tests pass after each task implementation.
- Maintain Main-thread safety for all StateFlow updates.
- Format code with `./gradlew ktlintFormat` before final validation.
