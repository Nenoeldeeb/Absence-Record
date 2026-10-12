---

description: "Task list template for feature implementation"
---

# Tasks: [FEATURE NAME]

**Input**: Design documents from `/specs/[###-feature-name]/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: The examples below include test tasks. Tests are OPTIONAL - only include them if explicitly requested in the feature specification.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

Base package: `app/src/main/java/dev/nenoeldeeb/education/absencerecord/`

- **Domain**: `domain/models/`, `domain/repositories/`, `domain/services/`, `domain/usecases/[group]/`
- **Data**: `data/datasources/local/{entities,daos}/`, `data/repositories/`, `data/mappers/`, `data/services/`, `data/utils/`
- **Presentation**: `presentation/screens/[feature]/`, `presentation/screens/components/`, `presentation/theme/`, `presentation/utils/`
- **Navigation / app wiring**: `app/`, `app/navigation/`
- **Unit tests**: `app/src/test/java/dev/nenoeldeeb/education/absencerecord/` (mirror the package path)
- **Instrumented tests**: `app/src/androidTest/java/dev/nenoeldeeb/education/absencerecord/`
- **Strings**: `app/src/main/res/values/strings.xml` and `app/src/main/res/values-ar/strings.xml`

Dependency direction: `presentation/` → `domain/` ← `data/`. Tasks MUST be
ordered so domain and data land before presentation.

### Build & Test Commands

```bash
# Format (also runs automatically as a preBuild dependency)
./gradlew ktlintFormat

# Full validation: ktlint + lint + unit tests
./gradlew check

# Targeted unit test (JVM)
./gradlew testDebugUnitTest --tests "PackageOrClass"

# Instrumented tests — AGP 9 REJECTS --tests here; use the runner argument.
# Filter with -Pandroid.testInstrumentationRunnerArguments.class="PackageOrClass"
./gradlew connectedDebugAndroidTest
```

<!--
  ============================================================================
  IMPORTANT: The tasks below are SAMPLE TASKS for illustration purposes only.

  The /speckit.tasks command MUST replace these with actual tasks based on:
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

**Purpose**: Project initialization and basic structure

- [ ] T001 Create project structure per implementation plan
- [ ] T002 Confirm dependencies in `gradle/libs.versions.toml` (no new libs unless the plan justifies them)
- [ ] T003 Add any new English strings to `app/src/main/res/values/strings.xml`
- [ ] T004 [P] Add matching Arabic translations to `app/src/main/res/values-ar/strings.xml`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

Order the foundation domain-first, then data, then wiring:

- [ ] T005 Create domain models in `domain/models/` (no Android imports)
- [ ] T006 Add repository interface(s) in `domain/repositories/`
- [ ] T007 Add pure business rules in `domain/services/` if the feature has thresholds or overlaps
- [ ] T008 Add single-action use cases in `domain/usecases/[group]/` returning `Result<T>`
- [ ] T009 [P] Add Room entity + DAO in `data/datasources/local/` (suspend or `Flow`, never blocking)
- [ ] T010 [P] Add mapper(s) in `data/mappers/`
- [ ] T011 Implement the repository in `data/repositories/` — catch errors here, return `Result`
- [ ] T012 [P] Add the Room `Migration` + version bump, and commit the regenerated `app/schemas/` JSON (REQUIRED if the schema changes; destructive migration is NOT acceptable)
- [ ] T013 [P] Add migration test in `androidTest/` if the schema changed
- [ ] T014 Register the repository in `AppContainer` and expose the ViewModel factory in `AppViewModelProvider`
- [ ] T015 [P] Write shared-state wiring if this feature participates in cross-screen state (e.g. the class filter `StateFlow`)

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - [Title] (Priority: P1) 🎯 MVP

**Goal**: [Brief description of what this story delivers]

**Independent Test**: [How to verify this story works on its own]

### Tests for User Story 1 (OPTIONAL - only if tests requested) ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**
>
> Convention (Constitution IV): cover BOTH the `onSuccess` and the `onFailure`
> path for every use case and ViewModel touched by this story, and assert that
> the error state binds to the UI.

- [ ] T016 [P] [US1] Unit test (JUnit 6 + MockK) for the use case in `app/src/test/.../domain/usecases/[group]/[Name]UseCaseTest.kt` — success and failure paths
- [ ] T017 [P] [US1] Unit test for the ViewModel in `app/src/test/.../presentation/screens/[feature]/[Feature]ViewModelTest.kt`
- [ ] T018 [US1] Compose UI test in `app/src/androidTest/.../` using a stateless content composable + harness (mockk-android cannot mock final ViewModels on-device)

### Implementation for User Story 1

- [ ] T019 [P] [US1] Extend domain model in `domain/models/[Entity].kt`
- [ ] T020 [P] [US1] Add use case in `domain/usecases/[group]/[Action]UseCase.kt` (single-action `operator fun invoke`, returns `Result<T>`)
- [ ] T021 [US1] Implement the data operation in `data/repositories/[Name]RepositoryImpl.kt` — catch here, return `Result`
- [ ] T022 [US1] Add/extend `[Feature]ScreenState.kt` and `[Feature]ScreenEvent.kt`
- [ ] T023 [US1] Handle the event in `[Feature]ViewModel.kt` via `_uiState.update { it.copy(...) }`
- [ ] T024 [P] [US1] Build the UI in `[Feature]Screen.kt` + `components/` (48dp targets, `contentDescription`, no color-only state)
- [ ] T025 [US1] Add English + Arabic strings
- [ ] T026 [US1] Verify: `./gradlew ktlintFormat check` and confirm every touched file is under 300 lines (extract a `delegates/` class if not)

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently

---

## Phase 4: User Story 2 - [Title] (Priority: P2)

**Goal**: [Brief description of what this story delivers]

**Independent Test**: [How to verify this story works on its own]

### Tests for User Story 2 (OPTIONAL - only if tests requested) ⚠️

- [ ] T027 [P] [US2] Unit test for the use case / ViewModel in `app/src/test/.../` — success and failure paths
- [ ] T028 [P] [US2] Compose UI test in `app/src/androidTest/.../` if the story has user-facing UI

### Implementation for User Story 2

- [ ] T029 [P] [US2] Add domain use case in `domain/usecases/[group]/`
- [ ] T030 [US2] Integrate with User Story 1 components (if needed)

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently

---

## Phase 5: User Story 3 - [Title] (Priority: P3)

**Goal**: [Brief description of what this story delivers]

**Independent Test**: [How to verify this story works on its own]

### Tests for User Story 3 (OPTIONAL - only if tests requested) ⚠️

- [ ] T031 [P] [US3] Unit test for the use case / ViewModel in `app/src/test/.../` — success and failure paths
- [ ] T032 [P] [US3] Compose UI test in `app/src/androidTest/.../` if the story has user-facing UI

### Implementation for User Story 3

- [ ] T033 [P] [US3] Add domain use case in `domain/usecases/[group]/`
- [ ] T034 [US3] Implement the UI in `presentation/screens/[feature]/`

**Checkpoint**: All user stories should now be independently functional

---

[Add more user story phases as needed, following the same pattern]

---

## Phase N: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] TXXX Run `./gradlew ktlintFormat` and `./gradlew check` — both MUST pass
- [ ] TXXX Verify every new/modified file is under 300 lines (Constitution V)
- [ ] TXXX Confirm all new strings exist in BOTH `values/strings.xml` and `values-ar/strings.xml`
- [ ] TXXX Accessibility pass: `contentDescription`, 48dp targets, color not the sole state indicator, TalkBack check
- [ ] TXXX Confirm CPU-heavy work (serialization, date parsing, bulk processing) is offloaded via `withContext`
- [ ] TXXX Confirm no `try-catch` escapes the data layer and no `java.time` import was introduced
- [ ] TXXX Update `AGENTS.md` and `README.md` — the constitution requires both to reflect new features/technologies
- [ ] TXXX Run quickstart.md validation

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3+)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3)
- **Polish (Final Phase)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - May integrate with US1 but should be independently testable
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) - May integrate with US1/US2 but should be independently testable

### Within Each User Story

- Tests (if included) MUST be written and FAIL before implementation
- Domain models before use cases
- Domain before data, data before presentation (dependency direction)
- Core implementation before integration
- Story complete before moving to next priority

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel
- All Foundational tasks marked [P] can run in parallel (within Phase 2)
- Once Foundational phase completes, all user stories can start in parallel (if team capacity allows)
- All tests for a user story marked [P] can run in parallel
- Models within a story marked [P] can run in parallel
- Different user stories can be worked on in parallel by different team members

---

## Parallel Example: User Story 1

```text
# Launch all tests for User Story 1 together (if tests requested):
Task: "[US1] Unit test for the use case in app/src/test/.../domain/usecases/[group]/[Name]UseCaseTest.kt"
Task: "[US1] Unit test for the ViewModel in app/src/test/.../presentation/screens/[feature]/[Feature]ViewModelTest.kt"
Task: "[US1] Compose UI test in app/src/androidTest/.../"

# Launch all domain models for User Story 1 together:
Task: "[US1] Extend domain model in domain/models/[Entity].kt"
Task: "[US1] Add domain model in domain/models/[Other].kt"
```

### Verifying this story

```bash
# Targeted unit tests for the packages touched by this story:
./gradlew testDebugUnitTest --tests "dev.nenoeldeeb.education.absencerecord.domain.*"
./gradlew testDebugUnitTest --tests "dev.nenoeldeeb.education.absencerecord.presentation.*"

# Full gate:
./gradlew ktlintFormat check

# Instrumented (AGP 9: no --tests; use the runner argument):
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class="dev.nenoeldeeb.education.absencerecord.presentation.*"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Test User Story 1 independently
5. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (MVP!)
3. Add User Story 2 → Test independently → Deploy/Demo
4. Add User Story 3 → Test independently → Deploy/Demo
5. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1
   - Developer B: User Story 2
   - Developer C: User Story 3
3. Stories complete and integrate independently

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence
