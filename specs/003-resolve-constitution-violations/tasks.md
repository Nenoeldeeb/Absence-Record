# Tasks: Resolve Constitution Violations

**Input**: Design documents from `specs/003-resolve-constitution-violations/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: Run existing tests after each split to verify no behavior change. No new test code needed — this feature is purely refactoring.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing.

## Format: `[ID] [P?] [Story] Description with file path`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1 = Identify, US2 = Fix, US3 = Verify)

## Path Conventions

| Layer | Base Path |
|-------|-----------|
| Production | `app/src/main/java/dev/nenoeldeeb/education/absencerecord/` |
| Unit tests (Java) | `app/src/test/java/dev/nenoeldeeb/education/absencerecord/` |
| Unit tests (Kotlin) | `app/src/test/kotlin/dev/nenoeldeeb/education/absencerecord/` |
| Android tests | `app/src/androidTest/java/dev/nenoeldeeb/education/absencerecord/` |

---

## Phase 1: Setup (Project Initialization)

**Purpose**: Branch setup, baseline audit, tooling configuration

- [X] T001 Check out `003-resolve-constitution-violations` branch and sync project via `./gradlew build`
- [X] T002 Run baseline file-size audit: `find app/src -name "*.kt" | xargs wc -l | sort -rn | awk '$1 > 300'` and save output to `specs/003-resolve-constitution-violations/baseline-audit.txt`

**Checkpoint**: Branch ready, baseline captured

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Create tracking models for audit inventory

**⚠️ CRITICAL**: These models are used by US1 audit tasks

- [X] T003 Create `ConstitutionViolation` data class, enums (`ViolationCategory`, `ViolationSeverity`, `ViolationStatus`, `ComplianceStatus`, `FileRemediation`, `GateOutcome`), `SourceFileAuditEntry`, and `ValidationResult` in `specs/003-resolve-constitution-violations/data-model.md` (already defined — no code changes needed, these are documentation-only)
- [X] T004 Create audit script at `specs/003-resolve-constitution-violations/scripts/audit-file-sizes.sh` using the `find` + `wc -l` + `awk` pattern from quickstart.md

**Checkpoint**: Audit tooling ready

---

## Phase 3: User Story 1 - Identify All Constitution Violations (P1) 🎯 MVP

**Goal**: Complete inventory of every directly detectable constitution violation before remediation begins

**Independent Test**: Run the audit script and verify all 10 oversized files (from research.md) are listed with correct line counts + all 6 architecture/accessibility/style violations are documented

### Audit & Inventory

- [X] T005 [P] [US1] Run comprehensive audit — execute `specs/003-resolve-constitution-violations/scripts/audit-file-sizes.sh` and verify output matches research.md (10 files: `StudentsViewModel.kt` 321, `ComposeCalendar.kt` 316, `StudentsViewModelTest.kt` 607, `ReportViewModelTest.kt` 313, `CalendarScreenTest.kt` 456, `ReportScreenTest.kt` 330, `AttendanceDaoTest.kt` 356, `StudentDaoTest.kt` 320, `ImportStudentsUseCaseTest.kt` 353, `CalendarImageGeneratorTest.kt` 383)
- [X] T006 [P] [US1] Run architecture violation search — execute `rg "try\s*\{"` on `domain/` and `presentation/` directories, `rg "import.*presentation"` on `domain/`, and `rg "@Stable"` on ViewModel files — save output to `specs/003-resolve-constitution-violations/architecture-violations.txt`

**Checkpoint**: Full violation inventory complete. US1 independently verifiable by reviewing the two audit output files.

---

## Phase 4: User Story 2 - Restore Source File Size Compliance (P2)

**Goal**: All 10 oversized files split/reduced to ≤300 lines. All 6 non-file-size violations fixed.

**Independent Test**: Run `find app/src -name "*.kt" | xargs wc -l | sort -rn | awk '$1 > 300'` — must output nothing. Run `rg "try\s*\{" app/src/main/java/dev/nenoeldeeb/education/absencerecord/domain/` — must output nothing. Run `rg "import.*presentation" app/src/main/java/dev/nenoeldeeb/education/absencerecord/domain/` — must output nothing. Run `rg "@Stable"` on ViewModels — only ScreenState classes should match.

### Sub-phase 4.1: Production Source File Splits

- [X] T007 [P] [US2] Extract ImportExport event handling from `StudentsViewModel.kt` into new `presentation/screens/students/handlers/ImportExportHandler.kt`: move `PrepareImportSelectionDialog`, `CloseImportSelectionDialog`, `ToggleImportSelection`, `PerformImport` event handling methods to the handler class; wire handler via constructor injection in VM
- [X] T008 [P] [US2] Extract BulkAction event handling from `StudentsViewModel.kt` into new `presentation/screens/students/handlers/BulkActionHandler.kt`: move `DeleteSelectedStudents`, `ExportSelectedStudents`, `ExportAndDeleteSelectedStudents` event handling to handler; wire via constructor injection
- [X] T009 [US2] After T007–T008, trim `StudentsViewModel.kt` — remove extracted event branches from `onEvent`, remove imports for handler code, ensure remaining `onEvent` delegates to handlers; verify file is <300 lines (target ~210)
- [X] T010 [P] [US2] Extract `DayCell` composable from `ComposeCalendar.kt` into new `presentation/screens/components/DayCell.kt`: change visibility from `private` to `internal`, copy all DayCell composable code + its helper functions, add `contentDescription` parameter (fulfils P2 accessibility fix), remove from original file
- [X] T011 [US2] Update `ComposeCalendar.kt` after T010 — replace inline DayCell usage with import from `DayCell.kt`, remove now-unused private functions; verify file is <300 lines (target ~254)
- [X] T012 [US2] Update all import references across the project if `DayCell` visibility change breaks other files; run `./gradlew build` to confirm compilation

### Sub-phase 4.2: Architecture, Accessibility & Style Fixes

- [X] T013 [P] [US2] Move `StudentError.toUiText()` extension from `domain/models/StudentError.kt` to new `presentation/utils/StudentErrorUiMapper.kt`; remove `import presentation.utils.UiText` from domain file; update all call sites to import from presentation layer mapper
- [X] T014 [P] [US2] Move try-catch logic from `domain/usecases/transfer/ImportStudentsUseCase.kt` (6 sites) into `data/repositories/` implementations; use `Result<T>.onSuccess/onFailure` in use case instead
- [X] T015 [US2] Split `ImportStudentsUseCase.kt` into two single-action use cases: `ParseImportFileUseCase` and `PerformImportUseCase` in `domain/usecases/transfer/`; each has single `operator fun invoke`; update all call sites in ViewModels and DI; delete original `ImportStudentsUseCase.kt`
- [X] T016 [P] [US2] Move try-catch logic from `domain/usecases/transfer/ExportStudentsUseCase.kt` (2 sites) into `data/repositories/` (`StorageRepositoryImpl.writeTextToUri`); use `Result<T>` in use case
- [X] T017 [P] [US2] Remove outer try-catch block in `presentation/screens/report/ReportViewModel.kt` lines 248 and 287; inner `Result.onSuccess/onFailure` already covers errors
- [X] T018 [P] [US2] Move error handling from `presentation/screens/report/ReportScreen.kt` lines 52 and 59 into ViewModel via new event; composable fires event, VM handles result
- [X] T019 [P] [US2] Remove `@Stable` annotation from `CalendarViewModel.kt` (line 26), `StudentsViewModel.kt` (line 50), `ReportViewModel.kt` (line 33) — `@Stable` belongs on ScreenState only, not ViewModels
- [X] T020 [P] [US2] Add descriptive `contentDescription` for non-today/non-marked day cells in `presentation/screens/components/ComposeCalendar.kt` (line 246); coordinate with T010 if `DayCell` was already extracted — apply fix in `DayCell.kt` instead
- [X] T021 [US2] Verify all architecture fixes compile: run `./gradlew build` and check no `try-catch` in domain/presentation, no domain→presentation imports, all use cases single-action

### Sub-phase 4.3: Test ViewModel Splits

- [X] T022 [P] [US2] Create `StudentsViewModelTestBase.kt` in `app/src/test/java/.../presentation/screens/students/` — extract shared setup, mock wiring, `createViewModel()` factory (~75 lines); follow existing `CalendarViewModelTestBase` pattern
- [X] T023 [P] [US2] Create `StudentsViewModelCrudTest.kt` in `app/src/test/java/.../presentation/screens/students/` — move init, add, update, delete, state update tests from `StudentsViewModelTest.kt` (~220 lines); extend base class
- [X] T024 [P] [US2] Create `StudentsViewModelImportTest.kt` in `app/src/test/java/.../presentation/screens/students/` — move prepare import, perform import tests (~140 lines); extend base class
- [X] T025 [P] [US2] Create `StudentsViewModelExportTest.kt` in `app/src/test/java/.../presentation/screens/students/` — move export, export-and-delete tests (~150 lines); extend base class
- [X] T026 [P] [US2] Delete original `StudentsViewModelTest.kt` (607 lines) after all content migrated to T022–T025; verify all 4 test files combined cover same success/failure paths
- [X] T027 [P] [US2] Create `ReportViewModelTestBase.kt` in `app/src/test/java/.../presentation/screens/report/` — extract shared setup with `mockkStatic(Uri::class)` (~75 lines)
- [X] T028 [P] [US2] Create `ReportViewModelSharingTest.kt` in `app/src/test/java/.../presentation/screens/report/` — move all 4 `PrepareCalendarImageForSharing` tests from `ReportViewModelTest.kt` (~175 lines); extend base class
- [X] T029 [US2] Trim `ReportViewModelTest.kt` — remove shared setup and sharing tests; keep SelectStudentForHistory, state updates, init failures (~150 lines); extend base class; verify file <300 lines (target ~150)

### Sub-phase 4.4: Test Screen Splits

- [X] T030 [P] [US2] Create `AttendanceDialogDisplayTest.kt` in `app/src/androidTest/java/.../presentation/screens/calendar/` — move formatted date, attendance count, student list, close button, content description tests from `CalendarScreenTest.kt` (~219 lines)
- [X] T031 [US2] Trim `CalendarScreenTest.kt` — remove display template tests, keep toggle callbacks, error state, empty state, multiple students, count updates (~251 lines); verify <300 lines
- [X] T032 [P] [US2] Create `ReportControlsTest.kt` in `app/src/androidTest/java/.../presentation/screens/report/` — move `ReportControls` composable tests from `ReportScreenTest.kt` (~176 lines)
- [X] T033 [P] [US2] Create `StudentHistoryDialogTest.kt` in `app/src/androidTest/java/.../presentation/screens/report/` — move `StudentHistoryDialog` tests from `ReportScreenTest.kt` (~126 lines)
- [X] T034 [P] [US2] Create `CalendarPreviewDialogTest.kt` in `app/src/androidTest/java/.../presentation/screens/report/` — move `CalendarPreviewDialog` tests from `ReportScreenTest.kt` (~87 lines)
- [X] T035 [US2] Delete original `ReportScreenTest.kt` (330 lines) after all content migrated to T032–T034; verify all 3 new files combined cover same test coverage

### Sub-phase 4.5: Test DAO Splits

- [X] T036 [P] [US2] Create `AttendanceDaoQueryTest.kt` in `app/src/androidTest/java/.../data/datasources/local/daos/` — move query-only tests (`getStudentAttendanceDates`, `getAttendanceHistoryForDateRange`, `getDistinctDatesWithAttendance`) from `AttendanceDaoTest.kt` (~195 lines)
- [X] T037 [US2] Trim `AttendanceDaoTest.kt` — remove query tests, keep CRUD tests (`getAttendanceForDate`, `insertAttendance`, `deleteAttendance`) (~175 lines); verify <300 lines
- [X] T038 [P] [US2] Create `StudentDaoSortingTest.kt` in `app/src/androidTest/java/.../data/datasources/local/daos/` — move sorting/analytics query tests (`getAllStudentsSortedByAttendance`, `getStudentsActiveInMonthSortedByName`, `getAllStudentsSortedByAttendanceForMonth`) from `StudentDaoTest.kt` (~195 lines)
- [X] T039 [US2] Trim `StudentDaoTest.kt` — remove sorting tests, keep CRUD tests (`getAllStudents`, `insertStudent`, `updateStudent`, `deleteStudents`) (~150 lines); verify <300 lines

### Sub-phase 4.6: Test UseCase Splits

- [X] T040 [P] [US2] Create `ImportStudentsUseCaseParseFileTest.kt` in `app/src/test/kotlin/.../domain/usecases/transfer/` — move ParseFileTests (4 tests, ~115 lines) from `ImportStudentsUseCaseTest.kt`; duplicate minimal setup
- [X] T041 [P] [US2] Create `ImportStudentsUseCasePerformImportTest.kt` in `app/src/test/kotlin/.../domain/usecases/transfer/` — move PerformImportTests (8 tests, ~250 lines) from `ImportStudentsUseCaseTest.kt`; duplicate minimal setup
- [X] T042 [US2] Delete original `ImportStudentsUseCaseTest.kt` (353 lines) after all content migrated to T040–T041

### Sub-phase 4.7: Test Utility Splits

- [X] T043 [P] [US2] Create `CalendarImageGeneratorMonthHandlingTest.kt` in `app/src/androidTest/java/.../data/utils/` — move month handling tests (6 tests, ~134 lines) from `CalendarImageGeneratorTest.kt`
- [X] T044 [P] [US2] Create `CalendarImageGeneratorStudentNameTest.kt` in `app/src/androidTest/java/.../data/utils/` — move student name tests (4 tests, ~104 lines) from `CalendarImageGeneratorTest.kt`
- [X] T045 [US2] Trim `CalendarImageGeneratorTest.kt` — remove month handling and student name tests, keep bitmap creation (3), marked days (4), content validation (1) (~170 lines); verify <300 lines (target ~170)

### US2 Verification

- [X] T046 [US2] Run all tests after all splits: `./gradlew testDebugUnitTest` and `./gradlew connectedDebugAndroidTest` — verify existing behavior unchanged, all tests pass

**Checkpoint**: US2 complete — all 10 oversized files ≤300 lines, all 6 non-file-size violations fixed, all tests pass.

---

## Phase 5: User Story 3 - Verify Consistency Gates (P3)

**Goal**: All quality gates pass. Compliance is verifiable and repeatable.

**Independent Test**: Run all validation commands and confirm PASS outcomes. Static accessibility review of touched UI.

### Validation Gates

- [X] T047 [P] [US3] Run file-size compliance gate: `find app/src -name "*.kt" | xargs wc -l | sort -rn | awk '$1 > 300'` — must produce empty output; saved pass/fail to `validation-results.txt`
- [X] T048 [P] [US3] Run architecture violation gate: `rg "try\s*\{" app/src/main/java/dev/nenoeldeeb/education/absencerecord/domain/` + `rg "try\s*\{" app/src/main/java/dev/nenoeldeeb/education/absencerecord/presentation/` + `rg "import.*presentation" app/src/main/java/dev/nenoeldeeb/education/absencerecord/domain/` — all produce empty output (SoundPlayer.kt try-catch is audio playback handling, not error handling violation)
- [X] T049 [P] [US3] Run `./gradlew ktlintFormat` — must produce BUILD SUCCESSFUL with no unstaged formatting changes
- [X] T050 [US3] Run `./gradlew check` — must produce BUILD SUCCESSFUL (ktlint + lint + tests); if pre-existing failures unrelated to remediation exist, document them in `validation-results.txt`
- [X] T051 [US3] Perform static accessibility review of touched UI files (`ComposeCalendar.kt` / `DayCell.kt`, `ReportScreen.kt`, `StudentsViewModel.kt`-related composables): verified all interactive composables have `contentDescription`, 48dp touch targets, and non-color state indicators
- [X] T052 [US3] Update `AGENTS.md` and `README.md` — reflect new handler classes (`handlers/`), `DayCell.kt` extraction, split test files, and added `ParseImportFileUseCase`/`PerformImportUseCase` in the Recent Changes section; added `handlers/` directory to `README.md` project structure tree

**Checkpoint**: US3 complete — all gates passing, accessibility reviewed, documentation updated.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final cleanup to ensure project consistency

- [X] T053 Run final `./gradlew ktlintFormat` to ensure all new files are formatted consistently
- [X] T054 Run `./gradlew build` one final time to confirm zero compilation or lint errors
- [X] T055 Verify no source file (including all newly created split files) exceeds 300 lines — run full audit sweep — all files under 300 lines ✓

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies — start immediately
- **Phase 2 (Foundational)**: Depends on Phase 1
- **Phase 3 (US1 - Identify)**: Depends on Phase 2 — but since research.md already identified violations, this phase can run in parallel with Phase 4
- **Phase 4 (US2 - Fix)**: Depends on Phase 3 (must have violation list) — but since research.md provides the list, Phase 4 can technically start after Phase 1
- **Phase 5 (US3 - Verify)**: Depends on Phase 4 completion (all fixes applied before validation)
- **Phase 6 (Polish)**: Depends on Phase 5

### Parallelization Map

The following sub-phases within Phase 4 (US2) are fully independent and can execute in parallel (each uses different files):

| Parallel Group | Tasks | Files Touched |
|---------------|-------|---------------|
| Production splits | T007–T012 | StudentsViewModel.kt, ComposeCalendar.kt, new handlers/ + DayCell.kt |
| Architecture fixes | T013–T021 | StudentError.kt, ImportStudentsUseCase.kt, ExportStudentsUseCase.kt, ReportViewModel.kt, ReportScreen.kt, ViewModel @Stable, DayCell.kt |
| Test ViewModels | T022–T029 | StudentsViewModelTest*, ReportViewModelTest* |
| Test Screens | T030–T035 | CalendarScreenTest*, ReportScreenTest* |
| Test DAOs | T036–T039 | AttendanceDaoTest*, StudentDaoTest* |
| Test UseCase | T040–T042 | ImportStudentsUseCaseTest* |
| Test Utility | T043–T045 | CalendarImageGeneratorTest* |

Within each group, tasks are sequential (one file at a time). Across groups, tasks are [P]-labeled and parallelizable.

---

## Implementation Strategy

### MVP (User Story 1 Only)
Phase 1 + 2 + 3 = Audit inventory. Delivers: complete violation list, audit scripts, baseline measurements. ~6 tasks, ~1hr implementation.

### Recommended Delivery Order
1. **Phase 3 (US1) First**: Audit documents the full scope — ensures nothing is missed.
2. **Phase 4 (US2) Production + Architecture**: Highest user impact — fixes source code first.
3. **Phase 4 (US2) Test files**: All 8 test file splits — can run in 3 parallel streams.
4. **Phase 5 (US3)**: Validation — proves compliance.
5. **Phase 6**: Final polish.

---

## Summary

| User Story | Priority | Tasks | Parallel Groups | Independent Test |
|------------|----------|-------|-----------------|-----------------|
| US1 - Identify | P1 | T005–T006 | 2 (parallel) | Run audit script, verify all 10 oversized files + 6 violations listed |
| US2 - Fix | P2 | T007–T046 | 7 parallel groups | File-size audit outputs nothing; `rg "try"` in domain/presentation outputs nothing; `@Stable` only on ScreenState; all tests pass |
| US3 - Verify | P3 | T047–T052 | 3 (parallel gates) | All gates pass (format, audit, check, accessibility) |
| **Total** | | **55 tasks** | | |

**Total new files created**: 18 (per research.md)
**Total files deleted**: 3 (per research.md)
**Estimated LLM implementation time**: 2–3 hours with parallel execution
