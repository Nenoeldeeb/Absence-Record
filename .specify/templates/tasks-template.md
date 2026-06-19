---
description: "Task list template for feature implementation"
---

# Tasks: [FEATURE NAME]

**Input**: Design documents from `/specs/[###-feature-name]/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: Tests are MANDATORY. Write tests for both `onSuccess` and `onFailure` paths.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Android App (Java/Kotlin)**: `app/src/main/java/dev/nenoeldeeb/education/absencerecord/`
- **Android App (Kotlin Only)**: `app/src/main/kotlin/dev/nenoeldeeb/education/absencerecord/`
- **Unit Tests (Java/Kotlin)**: `app/src/test/java/dev/nenoeldeeb/education/absencerecord/`
- **Unit Tests (Kotlin Only)**: `app/src/test/kotlin/dev/nenoeldeeb/education/absencerecord/`
- **UI Tests (Java/Kotlin)**: `app/src/androidTest/java/dev/nenoeldeeb/education/absencerecord/`
- **UI Tests (Kotlin Only)**: `app/src/androidTest/kotlin/dev/nenoeldeeb/education/absencerecord/`

<!--
  ============================================================================
  IMPORTANT: The tasks below are SAMPLE TASKS for illustration purposes only.

  The /speckit-tasks command MUST replace these with actual tasks based on:
  - User stories from spec.md (with their priorities P1, P2, P3...)
  - Feature requirements from plan.md
  - Entities from data-model.md
  - Endpoints from contracts/

  Tasks MUST be organized by user story so each story can be:
  - Implemented independently
  - Tested independently
  - Delivered as an MVP increment

  DO NOT keep these sample tasks in the generated tasks.md file.
  ============================================================================
-->

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Feature initialization and manual dependency injection config

- [ ] T001 Define [Feature] ScreenState and ScreenEvent in `presentation/screens/[feature]/`
- [ ] T002 Configure dependency injection in `app/AppContainer.kt` and `app/AppViewModelProvider.kt`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core data and domain layers that MUST be complete before UI can be implemented

**⚠️ CRITICAL**: No UI work can begin until this phase is complete

- [ ] T003 Setup Room entity and DAO in `data/datasources/local/`
- [ ] T004 Implement mapper in `data/mappers/`
- [ ] T005 Implement Repository interface in `domain/repositories/` and implementation in `data/repositories/`
- [ ] T006 Implement Use Case in `domain/usecases/` using `operator fun invoke` and returning `Result<T>`
- [ ] T007 Write unit tests for Repository and Use Case verifying BOTH success and failure paths

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - [Title] (Priority: P1) 🎯 MVP

**Goal**: [Brief description of what this story delivers]

**Independent Test**: [How to verify this story works on its own]

### Tests for User Story 1

- [ ] T008 [P] [US1] Write ViewModel unit tests for `onSuccess` and `onFailure` paths in `app/src/test/`
- [ ] T009 [P] [US1] Write UI tests using Compose Rules in `app/src/androidTest/`

### Implementation for User Story 1

- [ ] T010 [US1] Create ScreenState fields and ScreenEvent handling in `presentation/screens/[feature]/`
- [ ] T011 [US1] Implement Business Logic in ViewModel using `viewModelScope.launch` and offloading CPU-intensive tasks
- [ ] T012 [US1] Create UI Composables for Screen using `collectAsStateWithLifecycle()` in `presentation/screens/[feature]/Screen.kt`
- [ ] T013 [US1] Add string resources in `res/values/strings.xml` and `res/values-ar/strings.xml` using `UiText` wrapper

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently

---

## Phase N: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] TXXX [P] Format code using `./gradlew ktlintFormat`
- [ ] TXXX Verify all tests and lints pass using `./gradlew check`
- [ ] TXXX Perform local testing on emulator / physical device
- [ ] TXXX Ensure accessibility: add content descriptions, verify 48dp touch targets, test with TalkBack
- [ ] TXXX Verify no source file (including tests) exceeds 300 lines; refactor if needed

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all UI screens
- **User Stories (Phase 3+)**: All depend on Foundational phase completion
- **Polish (Final Phase)**: Depends on all desired user stories being complete

### Within Each User Story

- Room/Data Layer before Domain Layer (Use Cases)
- Use Cases before ViewModels
- ViewModels before Screens/Composables
- UI layout before localization strings

---

## Notes

- Verify tests fail before implementing (TDD cycle)
- Standardize all return types in Data and Domain layers to `Result<T>`
- Format code with `./gradlew ktlintFormat` before each commit
