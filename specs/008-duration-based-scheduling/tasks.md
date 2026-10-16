---

description: "Task list for 008-duration-based-scheduling"
---

# Tasks: Duration-Based Lesson Scheduling

**Input**: Design documents from `/specs/008-duration-based-scheduling/`

**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅,
contracts/backup-file-format.md ✅, quickstart.md ✅

**Tests**: Included. Constitution Principle IV requires both `onSuccess` and
`onFailure` paths for every use case and ViewModel touched, so test tasks are
mandatory here, not optional.

**Organization**: Tasks are grouped by user story so each story is independently
implementable and testable.

## Format: `[ID] [P?] [Story?] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[TDD]**: Must follow RED → GREEN → REFACTOR
- **[REVIEW]**: Pause for human code review before proceeding
- **[SUBAGENT]**: Can be dispatched to a parallel subagent
- **[Story]**: Traces the task to a user story in spec.md

## Path Conventions

Base package: `app/src/main/java/dev/nenoeldeeb/education/absencerecord/`

- **Domain**: `domain/models/`, `domain/repositories/`, `domain/services/`, `domain/usecases/[group]/`
- **Data**: `data/datasources/local/{entities,daos}/`, `data/repositories/`, `data/mappers/`
- **Presentation**: `presentation/screens/[feature]/`, `presentation/screens/components/`
- **Unit tests**: `app/src/test/java/dev/nenoeldeeb/education/absencerecord/`
- **Instrumented tests**: `app/src/androidTest/java/dev/nenoeldeeb/education/absencerecord/`
- **Strings**: `app/src/main/res/values/strings.xml` and `app/src/main/res/values-ar/strings.xml`

Dependency direction: `presentation/` → `domain/` ← `data/`.

> **Refinement over plan.md**: the plan proposed extracting a slot display out of
> `HourRow.kt` (289 lines). `HourAssignedStudents.kt` is only 80 lines and already
> receives `List<Student>`, so slot ranges go there (T047). `HourRow.kt` is then
> left alone, which is strictly safer than refactoring a near-cap file.

### Build & Test Commands

```bash
./gradlew ktlintFormat
./gradlew check
./gradlew testDebugUnitTest --tests "PackageOrClass"
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class="PackageOrClass"
```

> AGP 9 rejects `--tests` on `connectedDebugAndroidTest`.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Baseline the repo and stage strings so no code work is blocked later.

- [x] **T001** `[SUBAGENT]` Create `signing.properties` from `signing.properties.example` (`cp signing.properties.example signing.properties`) — `app/build.gradle.kts` reads it for both signing configs and **no Gradle task can configure without it**
- [x] **T002** `[SUBAGENT]` Run `./gradlew check` on the untouched tree and confirm it is green. Record the result — a pre-existing failure must not be blamed on this feature
- [x] **T003** Capture the `maxStudents` blast radius as a baseline list: `grep -rln "maxStudents\|MaxStudents" --include=*.kt app/src` → expected **22 main + 21 test files, 62 occurrences in main**. This is the checklist every later phase verifies against
- [x] **T004** `[P] [SUBAGENT]` Add all new English strings to `app/src/main/res/values/strings.xml`: length label, derived-capacity label, slot range (`%1$s–%2$s`), replan keep/drop dialog title + body, `schedule_students_count` plural, `schedule_dropped_student` item, `schedule_confirm_replan` / `schedule_cancel_replan`, `schedule_slider_description`, and the four refusal messages (FR-050)
- [x] **T005** `[P] [SUBAGENT]` Add matching Arabic translations to `app/src/main/res/values-ar/strings.xml` for every string added in T004 — LZ-001 requires parity, not a subset

**Checkpoint**: baseline green, strings staged. No code changes yet.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: The pure scheduling rules, the domain contracts and the schema
migration. **No user story can begin until this completes.**

> **Why the planner is foundational, not US2**: US1's shorten/move-start confirm
> flow must decide *which* N students to keep, and that decision is the ordering
> rule. So the planner must land before US1, not inside US2.

### 2a — Pure domain rules (TDD)

- [x] **T006** `[P] [TDD] [SUBAGENT]` Write `app/src/test/java/.../domain/services/ScheduleRulesTest.kt` cases for duration validity: 10 and 120 accepted; 9, 111, 121 rejected; 35 rejected (not a multiple of 10); `startMinutes + durationMinutes <= 1440` boundary at 1440 accepted, 1441 rejected. **Verify RED** (FR-002, FR-007)
- [x] **T007** `[P] [TDD] [SUBAGENT]` Write `app/src/test/java/.../domain/services/LessonSlotPlannerTest.kt` covering **all 10 rows of quickstart.md §2**(FR-016, FR-020, FR-021, FR-022, FR-023, FR-029), plus: commitment later inside the block does not reject (FR-027); commitment ending exactly at `blockStart` is ignored by *next relevant commitment* (FR-019); slot *n* starts at `blockStart + (n-1)*10` (FR-017); sorting is independent of input list order; recomputation with identical input is byte-identical (FR-060) SC-004). **Verify RED**
- [x] **T008** `[TDD]` Implement duration predicates in `domain/services/ScheduleRules.kt` — `isHourDurationValid`, `isHourStartValid(start, duration)`, `MIN/MAX/STEP_HOUR_DURATION`, derived-capacity helper, new `DEFAULT_NEW_HOUR_DURATION_MINUTES = 60`(FR-002, FR-003, FR-005). Remove `MAX_HOUR_CAPACITY = 99`, `DEFAULT_NEW_HOUR_MAX_STUDENTS` and `parseMaxStudents` (no longer reachable). **Verify GREEN** (T006)
- [x] **T009** `[TDD]` `[REVIEW]` Implement `domain/services/LessonSlotPlanner.kt` as a pure object: `plan(blockStart, blockDuration, students)` returning `PlannedHour`, following steps 1–5 of data-model.md exactly(FR-016, FR-020, FR-021, FR-022, FR-023, FR-030). No Android, Room or Compose imports. **Verify GREEN** (T007). **Pause here — this is the highest-leverage gate in the feature**
- [x] **T010** `[TDD]` Add a randomness/duplicates regression test to `LessonSlotPlannerTest`: two students with an identical name AND identical commitment order by id (FR-024)

### 2b — Domain models and contracts

- [x] **T011** `[P] [SUBAGENT]` Update `domain/models/ScheduleModels.kt`: `AvailableLessonHour.maxStudents` → `durationMinutes` with `maxStudents` as a **computed** property (FR-004); `HourWithOccupancy` gains `durationMinutes` and computes `maxStudents`/`remainingSlots`/`isFull` from it(FR-003, FR-004, FR-006)
- [x] **T012** `[P] [SUBAGENT]` Add new value types in a new `domain/models/LessonPlanning.kt`: `PlannedSlot`, `PlannedHour`, `SlotConflict`, `DroppedStudent`, `DropReason`, `HourReplanResult` (sealed: `Applied` / `NeedsConfirmation`) — signatures per data-model.md
- [x] **T013** `[P] [SUBAGENT]` Update `domain/models/StudentError.kt`: remove `MaxBelowAssigned`; add variants for per-slot busy conflict, midnight-crossing, and unsupported/rejected import version as needed by FR-047
- [x] **T014** `[REVIEW]` `[SUBAGENT]` Update the `domain/repositories/ScheduleRepository.kt` interface: `insertHour`/`updateHour` take `durationMinutes`; `updateHour` returns `Result<HourReplanResult>`; add `suspend fun previewHourUpdate(...)` and a `confirmed` flag for the two-phase flow (research D5). **Review gate — widest contract in the change**
- [x] **T015** `[P] [SUBAGENT]` Update `domain/usecases/schedule/InsertHourUseCase.kt`, `UpdateHourUseCase.kt` and `AssignStudentUseCase.kt` for the new signatures; `UpdateHourUseCase` exposes both the unconfirmed and confirmed paths

### 2c — Data layer and migration

- [x] **T016** `[P] [SUBAGENT]` Rename the column in `data/datasources/local/entities/AvailableLessonHourEntity.kt`: `maxStudents` → `durationMinutes`. **This task unblocks T018**
- [x] **T017** `[P] [SUBAGENT]` Update `data/mappers/ScheduleMappers.kt` for the renamed field and the derived capacity
- [x] **T018** `[REVIEW]` Add `MIGRATION_3_4` to `data/datasources/local/StudentDatabase.kt`: bump `version = 4`, register in the `addMigrations(...)` chain, and run (1) `ALTER TABLE available_lesson_hours RENAME COLUMN maxStudents TO durationMinutes` then (2) `UPDATE ... SET durationMinutes = MIN(120, MAX(10, durationMinutes * 10))` (research D3)(FR-040, FR-057). Non-destructive only — `fallbackToDestructiveMigration` is prohibited
- [x] **T019** `[SUBAGENT]` **Delete the orphaned schema directory** `app/schemas/dev.nenoeldeeb.education.absencerecord.AppDatabase/` (tracked in git; its `4.json` describes an unrelated `notes`/`notes_with_dates` schema). Verify the real `4.json` lands in `...data.datasources.local.AppDatabase/` and that **exactly one** `4.json` exists
- [x] **T020** `[P] [TDD] [SUBAGENT]` Add `migrate3To4_preservesAssignmentsAndConvertsCapacity` to `app/src/androidTest/.../daos/ScheduleDaoMigrationTest.kt` following the existing `MigrationTestHelper` pattern: seed an hour with `maxStudents = 6`, migrate, assert `durationMinutes = 60` **and** that `lesson_assignments` survived. Add a second case seeding `maxStudents = 99` asserting the clamp to `120` (SC-006)(FR-040, FR-057)
- [x] **T021** `[P] [SUBAGENT]` Update `data/datasources/local/daos/ScheduleDaoHelpers.kt` with the per-slot overlap helper — pass `[slotStart, slotEnd)` rather than `[hourStart, hourEnd)` to the existing `overlaps()` (research D7). The existing `overlaps(aStart,aEnd,bStart,bEnd) = aStart < bEnd && aEnd > bStart` already implements half-open semantics (FR-028); **do not modify it**(FR-028, FR-030)
- [x] **T022** `[TDD]` Update `data/datasources/local/daos/ScheduleDao.kt` transactions as pure orchestration — read inputs, call the planner, act on its verdict, write: `updateHour` returns `HourReplanResult` and no longer throws `MaxBelowAssigned`; `assignStudent` re-plans the **whole roster** and narrows the busy check to the student's own slot. **Preserve the existing one-lesson-per-day constraint** — the unique index on `(studentId, weekday)` and the `getAssignmentsForStudentAndWeekday` guard in `assignStudent` MUST survive this rewrite untouched (FR-049). Also cover: refusal when the block already holds its derived maximum (FR-034), and refusal when any assigned — new or existing — student would conflict (FR-035), never an append (FR-032). Must stay under 300 lines (research D6)
- [x] **T023** `[TDD]` Update `data/repositories/ScheduleRepositoryImpl.kt`: wrap derivation in `withContext(Dispatchers.Default)` (FR-061, research D8); catch and return `Result.failure` here — no `try-catch` may escape to domain or presentation (Principle I)
- [x] **T024** `[P] [SUBAGENT]` Verify `app/AppContainer.kt` and `app/AppViewModelProvider.kt` — record whether the signature changes require wiring updates; make them if so. DI is manual, so this is an explicit check rather than an assumption
- [x] **T025** `[SUBAGENT]` Run `./gradlew testDebugUnitTest --tests "*LessonSlotPlannerTest"` and `--tests "*ScheduleRulesTest"` — both must be green before any story begins

**Checkpoint**: planner green, schema migrated, contracts reviewed. User stories may now begin.

---

## Phase 3: User Story 1 - Set a lesson block's length, not a headcount (Priority: P1) 🎯 MVP

**Goal**: The teacher picks a length from 10–120 minutes on a slider; capacity is
derived and shown live but cannot be edited. Shortening or moving the start runs
the confirm-first re-plan. Growing changes nobody.

**Independent Test**: Edit a block's length across all 12 values and confirm the
reported maximum equals length ÷ 10 and no independent control exists; then
shorten below occupancy and confirm the keep/drop dialog appears, that declining
changes nothing, and that growing leaves every slot untouched.

### Tests for User Story 1 ⚠️

> Write first, verify they FAIL, then implement (Constitution IV).

- [x] **T026** `[US1] [P] [TDD] [SUBAGENT]` Rewrite `app/src/test/java/.../presentation/screens/schedule/ScheduleViewModelCapacityTest.kt` — it currently asserts the removed `MaxBelowAssigned` behaviour. Assert instead: derived capacity for each of the 12 durations (SC-001, SC-002, SC-016); `NeedsConfirmation` returned (not applied) when shrinking below occupancy; declining leaves state untouched; growing leaves every slot unchanged; both `onSuccess` and `onFailure`(FR-006, FR-014)
- [x] **T027** `[US1] [P] [TDD] [SUBAGENT]` Extend `app/src/test/java/.../presentation/screens/schedule/ScheduleViewModelHourTest.kt` for `hourDurationMinutes` state, midnight rejection, and start-move re-plan (FR-007, FR-015) SC-012, SC-015)(FR-009, FR-012)
- [x] **T028** `[US1] [P] [TDD] [SUBAGENT]` Update `app/src/androidTest/java/.../screens/schedule/HourDialogTest.kt` for the slider. The tags `hour_capacity_increase` / `hour_capacity_decrease` no longer exist (FR-051); use the `ScheduleScreenContent` + harness pattern(FR-052) — `mockk-android` cannot mock final ViewModels on device
- [x] **T029** `[US1] [P] [TDD] [SUBAGENT]` Add a `ReplanConfirmDialogTest` asserting the dialog names every dropped student, that confirm and cancel are both reachable by TalkBack, and that cancel dispatches no write(FR-012, FR-013)

### Implementation for User Story 1

- [x] **T030** `[US1] [SUBAGENT]` Create `presentation/screens/schedule/components/LengthSlider.kt`: slider 10→120 step 10, derived student count rendered beside it and updating live (FR-053). Spoken label announces **both** minutes and count; switch-access and keyboard move in 10-minute steps (FR-054) SC-017). ≥48dp target, no colour-only state(FR-052, FR-053, FR-054)
- [x] **T031** `[US1] [SUBAGENT]` Update `presentation/screens/schedule/dialogs/HourDialog.kt` — remove the free-typed headcount field and its steppers, embed `LengthSlider`, keep the start-time picker. Must stay under 300 lines (currently 270)
- [x] **T032** `[US1] [SUBAGENT]` Update `presentation/screens/schedule/ScheduleScreenState.kt`: `hourMaxStudents: String` → `hourDurationMinutes: Int`; add `pendingReplan: HourReplanResult.NeedsConfirmation?`
- [x] **T033** `[US1] [SUBAGENT]` Update `presentation/screens/schedule/ScheduleScreenEvent.kt`: `SetHourMaxStudents(String)` → `SetHourDuration(Int)`; add `ConfirmReplan` and `DismissReplan`
- [x] **T034** `[US1] [SUBAGENT]` Create `presentation/screens/schedule/delegates/ReplanDelegate.kt` — owns the pending proposal and drives confirm/cancel through `_uiState.update { it.copy(...) }`. `ScheduleViewModel` composes it (constitution delegate pattern)
- [x] **T035** `[US1] [SUBAGENT]` Create `presentation/screens/schedule/dialogs/ReplanConfirmDialog.kt` — shows the resulting order plus the named drop list, and says so plainly when *every* student would drop (SC-014)(FR-010, FR-011, FR-013). Reused unchanged by US3
- [x] **T036** `[US1] [SUBAGENT]` Wire the events in `presentation/screens/schedule/ScheduleViewModel.kt` and `delegates/HourManagementDelegate.kt`; state mutated **only** via `_uiState.update` (Principle II)
- [x] **T037** `[US1] [SUBAGENT]` Update `presentation/screens/schedule/components/HourRow.kt` to show length and derived capacity (`x / y seats` already exists). **Minimal edit — do not grow this 289-line file**(FR-008, FR-055)
- [x] **T038** `[US1] [SUBAGENT]` Update `presentation/screens/studentdetail/dialogs/AddLessonDialog.kt` to reflect derived eligibility from duration (currently 258 lines — watch the cap)(FR-055)
- [x] **T039** `[US1] [SUBAGENT]` Verify every touched file is under 300 lines; extract a delegate if not. Then run `./gradlew ktlintFormat check`

**Checkpoint**: US1 fully functional and independently testable. **This is the MVP.**

---

## Phase 4: User Story 2 - Students are sequenced by who has to leave first (Priority: P2)

**Goal**: Students are ordered by next relevant commitment and each occupies one
visible 10-minute slot, validated against their own commitments rather than the
whole block.

**Independent Test**: Build a 10:00 block for students committed at 10:30, 10:40
and 11:00 and confirm slots 10:00–10:10, 10:10–10:20, 10:20–10:30; then confirm a
student with a 10:30 commitment is **accepted** into a 10:00–11:00 block.

> The planner already exists from Phase 2. This phase is the **presentation and
> read-path wiring** that makes the derived order visible and validates per-slot.

### Tests for User Story 2 ⚠️

- [x] **T040** `[US2] [P] [TDD] [SUBAGENT]` Add ordering integration tests to `app/src/test/java/.../data/repositories/ScheduleRepositoryImplObserveTest.kt` — derived order is emitted on read, unchanged by input list order, and identical across repeated reads (FR-025, FR-060) SC-005)
- [x] **T041** `[US2] [P] [TDD] [SUBAGENT]` Extend `app/src/test/java/.../domain/usecases/schedule/ScheduleObserveUseCasesTest.kt` for slot derivation on the observe path
- [x] **T042** `[US2] [P] [TDD] [SUBAGENT]` Add per-slot validation tests to `app/src/androidTest/java/.../daos/ScheduleDaoTest.kt`: commitment at slot start rejected; commitment at slot end accepted; commitment spanning `blockStart` rejected (FR-026); commitment later inside the block **not** rejected (FR-027); slot-start/slot-end boundaries (FR-028) SC-003) — both `onSuccess` and `onFailure`
- [x] **T043** `[US2] [P] [TDD] [SUBAGENT]` Extend `app/src/androidTest/java/.../screens/schedule/ScheduleScreenRefinementTest.kt` to assert each student's slot range renders on the expanded block row

### Implementation for User Story 2

- [x] **T044** `[US2] [SUBAGENT]` Thread `durationMinutes` and the derived plan through `ScheduleRepositoryImpl`'s observe path, computing slots off the main thread (FR-061)(FR-039)
- [x] **T045** `[US2] [SUBAGENT]` Update `presentation/screens/schedule/components/HourAssignedStudents.kt` to render each student's slot range beside their name (FR-018) SC-011). **This 80-line file is the intended home** — it keeps `HourRow.kt` untouched
- [x] **T046** `[US2] [SUBAGENT]` Update `presentation/screens/studentdetail/components/StudentScheduleSection.kt` and `StudentScheduleLessonsTab.kt` to surface the student's derived slot for the day
- [x] **T047** `[US2] [SUBAGENT]` Confirm the slot-range string is spoken correctly under RTL in `values-ar/` and that slot text uses content direction, not layout direction
- [x] **T048** `[US2] [SUBAGENT]` Run `./gradlew testDebugUnitTest --tests "*ScheduleRepositoryImplObserveTest"`; verify every touched file is under 300 lines

**Checkpoint**: US1 and US2 both independently working.

---

## Phase 5: User Story 3 - Adding a student re-plans the whole block, or changes nothing (Priority: P3)

**Goal**: Adding a student re-plans the entire roster rather than appending; it is
refused if any resulting slot conflicts, and a refusal leaves the block
byte-for-byte unchanged.

**Independent Test**: Add a student with an earlier commitment than all incumbents
to a partly filled block and confirm they take an earlier slot and incumbents
shift forward; then fill a 120-minute block to 12 and confirm a 13th is refused.

### Tests for User Story 3 ⚠️

- [x] **T049** `[US3] [P] [TDD] [SUBAGENT]` Extend `app/src/test/java/.../domain/usecases/schedule/ScheduleMutationUseCasesTest.kt`: earlier-comm newcomer takes the earlier slot; every existing student revalidated; any conflict refuses the whole addition; validate every student (FR-033); refusal leaves the roster and order unchanged (FR-036) SC-013) — both `onSuccess` and `onFailure`(FR-034, FR-035, FR-039)
- [x] **T050** `[US3] [P] [TDD] [SUBAGENT]` Add ViewModel tests in `app/src/test/java/.../presentation/screens/studentdetail/StudentDetailViewModelScheduleTest.kt` for the add-lessons refusal paths and the refusal message binding (FR-050)
- [x] **T051** `[US3] [P] [TDD] [SUBAGENT]` Add a removal test — removing a student re-plans the remainder with no gap in the running order (FR-037)
- [x] **T052** `[US3] [P] [TDD] [SUBAGENT]` Extend `app/src/androidTest/java/.../screens/studentdetail/AddLessonDialogTest.kt` for derived eligibility and refusal messaging

### Implementation for User Story 3

- [x] **T053** `[US3] [SUBAGENT]` **VERIFIED** — already implemented in T022; confirmed against FR-031/033/035 by `ScheduleDaoSlotValidationTest`.  to build the plan over the **whole roster** (newcomer + incumbents) and refuse if any slot conflicts (FR-031, FR-033) — done in T022; verify here against these requirements
- [x] **T054** `[US3] [SUBAGENT]` Implement re-plan-on-removal in `ScheduleDao.unassignStudent` (FR-037)
- [x] **T055** `[US3] [SUBAGENT]` **FR-038 was NOT implemented** — both helpers tested the whole block. Rewritten to per-slot via the derived plan.  to each affected student's own slot rather than the block's full extent (FR-038), preserving the existing preview-and-confirm removal report
- [x] **T056** `[US3] [SUBAGENT]` Wire the add/remove flows in `StudentDetailViewModel` and `StudentScheduleTabsDelegate`; refusals surface the specific reason (FR-050)
- [x] **T057** `[US3] [SUBAGENT]` Run `./gradlew testDebugUnitTest --tests "*ScheduleMutationUseCasesTest"`; verify every touched file is under 300 lines

**Checkpoint**: All three behavioural stories independently functional.

> **Phase 5 notes**
> - **FR-038 was never implemented.** Both re-validation helpers tested the
>   whole block, so a commitment later inside a block evicted a student whose
>   own ten minutes it never touched — the same defect FR-027 removed on the
>   assignment path. Rewritten to judge the derived slot (T055).
> - **The same over-broad check hid eligible blocks** in `AddLessonDialog`,
>   which marked a block unavailable whenever any commitment overlapped it. The
>   newcomer's prospective slot now comes from `HourWithOccupancy
>   .prospectiveSlotFor`, because where they land depends on where everyone
>   else lands (T052).
> - **`MIGRATION_3_4` crashed on API 26–29.** `ALTER TABLE … RENAME COLUMN`
>   needs SQLite 3.25 (API 30) and `minSdk` is 26. Rewritten as a rebuild, with
>   `PRAGMA legacy_alter_table` so the child foreign key is not rewritten and
>   the discarded table cannot cascade-delete assignments. Covered by
>   `ScheduleDaoMigrationRebuildTest` (5 tests, green on both devices).
> - **The API 26 emulator exposed all of this.** A second device was attached
>   during this phase; single-device testing had been reporting green for a
>   migration that crashes on a supported API.
> - **`CalendarScreenTest` was the only `mockk<ViewModel>` in the device
>   suite**, contradicting AGENTS.md. It passed on API 35 and failed on API 26.
>   Fixed outside the feature's scope: `CalendarScreenContent` extracted so the
>   tests drive state directly; split into `AttendanceDialogTest` (5) and
>   `CalendarScreenContentTest` (2). No ViewModel is mocked on device any more.

---

## Phase 6: User Story 4 - Existing plans and backups survive the change (Priority: P4)

**Goal**: Stored blocks and assignments load unchanged after upgrade, and backups
from the previous version still import.

**Independent Test**: Seed blocks on the previous version, upgrade, confirm every
block and assignment survives with a valid length; then import a v2 file and a
`version: 99` file and confirm the first loads and the second is refused.

### Tests for User Story 4 ⚠️

- [x] **T058** `[US4] [P] [TDD] [SUBAGENT]` Update `app/src/test/java/.../domain/usecases/transfer/ParseImportFileUseCaseTest.kt`. Line 184 asserts `assertEquals(2, BACKUP_VERSION)` — becomes `3`. Add cases: v3 input, v2 input converted, legacy flat array, `version: 99` refused with nothing written, entry with **both** fields skipped+counted, entry with neither skipped+counted, out-of-range `durationMinutes` skipped+counted (FR-042…FR-048) SC-007, SC-009)(FR-043, FR-044, FR-045, FR-046)
- [x] **T059** `[US4] [P] [TDD] [SUBAGENT]` Update `app/src/test/java/.../domain/usecases/transfer/ExportStudentsUseCaseScheduleTest.kt` — assert the v3 shape contains `durationMinutes` and **no** `maxStudents` anywhere (FR-042)
- [x] **T060** `[US4] [P] [TDD] [SUBAGENT]` Add the export→import round-trip case (SC-008) to the above: every block returns at the length it was saved with
- [x] **T061** `[US4] [P] [TDD] [SUBAGENT]` Extend `app/src/test/java/.../domain/usecases/transfer/PerformImportHoursMergeTest.kt` and `PerformImportScheduleMergeTest.kt` for duration-aware merging
- [x] **T062** `[US4] [P] [TDD] [SUBAGENT]` Add cap tests to `PerformImportUseCaseTest.kt`: exceeding a cap skips and counts, does not abort the remainder, and is **not** reported as a parse failure (FR-056…FR-059) SC-010)(FR-057, FR-058)

### Implementation for User Story 4

- [x] **T063** `[US4] [SUBAGENT]` Update `domain/models/ScheduleBackupData.kt`: `BACKUP_VERSION = 3`; `AvailableHourExportData.maxStudents` → `durationMinutes` (FR-042, D4)
- [x] **T064** `[US4] [SUBAGENT]` Update `domain/usecases/transfer/ExportStudentsUseCase.kt` to write the v3 shape
- [x] **T065** `[US4] [SUBAGENT]` Update `domain/usecases/transfer/ParseImportFileUseCase.kt` — replace strict `version != BACKUP_VERSION` with a three-way branch: v3 direct, v2 converted via `min(120, max(10, value * 10))`, anything else `UnsupportedBackupVersion`. **Preserve the legacy flat-array fallback in the failure branch**
- [x] **T066** `[US4] [SUBAGENT]` Add the import caps to `domain/services/ImportMergeEngine.kt`: `MAX_IMPORT_STUDENTS = 10_000`, `MAX_IMPORT_HOURS = 1_008`, `MAX_IMPORT_ASSIGNMENTS = 12_096` (research D9). File is 223 lines — **extract the cap enforcement into its own file if it would cross 300**(FR-056, FR-057, FR-058)
- [x] **T067** `[US4] [SUBAGENT]` Surface cap and skip counts in the post-import summary message via `PerformImportUseCase.kt` and `ImportResult.kt`
- [x] **T068** `[US4] [SUBAGENT]` Confirm the v2→v3 conversion clamps legacy data outside 10–120 (FR-041) on both the backup path and the migration path
- [x] **T069** `[US4] [SUBAGENT]` Update the post-import message strings in **both** `values/` and `values-ar/`
- [x] **T070** `[US4] [SUBAGENT]` Run the full backup test set; verify every touched file is under 300 lines

**Checkpoint**: Continuity verified — no teacher loses data.

> **Phase 6 notes**
> - **Backup v3.** `BACKUP_VERSION = 3` and the block field is renamed to
>   `durationMinutes`, so the shape change is signalled twice over (FR-043).
>   One model carries both signals, with `resolvedDurationMinutes()` refusing
>   an entry that has both or neither rather than guessing (FR-046, SC-009).
> - **v2 files still import.** `SUPPORTED_BACKUP_VERSIONS = {3, 2}`; a legacy
>   count is multiplied by ten and clamped at the merge boundary (FR-044).
>   Version 1 and version 99 are both refused with nothing written (FR-047).
> - **The size guard is the only real protection against the crash** the record
>   caps were written to prevent, and it is enforced in `StorageRepository`
>   *before* decoding (FR-063, SC-010). Verified on device, including a
>   multi-byte case where character count and byte count disagree.
> - **`ImportMergeEngine` crossed the 300-line cap**, so cap admission was
>   extracted to `ImportCapEnforcement.kt` (T066).

---

## Phase 7: Polish & Cross-Cutting Concerns

- [x] **T071** Delete the four obsolete strings (`error_capacity_invalid`, `schedule_decrease_capacity`, `schedule_increase_capacity`, `schedule_max_students`) from **both** `values/strings.xml` and `values-ar/strings.xml`. **Only now** — deleting earlier would break compilation while the old UI still references them
- [x] **T072** Verify against the T003 baseline that **zero** `maxStudents` references remain in main source, and that every hit in tests is either a legacy-fixture or an intentional assertion
- [x] **T073** Run `./gradlew ktlintFormat` then `./gradlew check` — both MUST pass
- [x] **T074** Verify every new and modified source file is under 300 lines (Constitution V), with particular attention to `ScheduleDao.kt`, `HourRow.kt`, `HourDialog.kt`, `ImportMergeEngine.kt`, `AddLessonDialog.kt`
- [x] **T075** Confirm every new string exists in **both** `values/strings.xml` and `values-ar/strings.xml` with no hardcoded strings in composables
- [x] **T076** Accessibility pass: spoken slider label announces minutes **and** derived count; switch-access and keyboard parity at 10-minute steps; 48dp targets; no colour-only state; contrast ≥4.5:1; TalkBack walkthrough per quickstart.md §6
- [x] **T077** Confirm derivation is wrapped in `withContext(Dispatchers.Default)` and never on the main thread (FR-061, Principle V)
- [x] **T078** `[SUBAGENT]` **MEASURED 2026-10-16** — derivation **3.3 ms** median over a full 1,008-block week (JVM, exact); render **33 ms** for a 144-block day (device, warm). Budget 1,000 ms. Cold-start is 1.2–1.7 s and is reported separately as meaningless — AGP 9 creates no `connectedReleaseAndroidTest`. See `audit.md`. With a fully populated week (every day holding the maximum number of non-overlapping blocks, each carrying the maximum 12 students with commitments), time opening a day of the schedule on a mid-range device and record wall-clock time to correctly ordered render plus frame-drop count. MUST meet SC-019 (< 1 s, no dropped frames attributable to derivation). Run against the release build, not debug — debug timings are meaningless. Record the measured numbers in the task checkbox when done. If it fails, the cause is almost certainly derivation on the main thread (T023/T044) or an unintended cache invalidation storm — fix before signing off
- [x] **T079** Confirm no `try-catch` escapes the data layer (Principle I) and no `java.time` import was introduced — `grep -rn "java.time" --include=*.kt app/src | wc -l` must be `0`
- [x] **T080** Confirm exactly one `4.json` exists, in `app/schemas/dev.nenoeldeeb.education.absencerecord.data.datasources.local.AppDatabase/`, and that it is committed
- [x] **T081** Run `./gradlew connectedDebugAndroidTest` and confirm the schedule and student-detail instrumented suites pass. This does **not** cover the untouched screens — see T082 (SC-018)
- [x] **T082** `[SUBAGENT]` **SC-018 regression check — the untouched screens.** This feature must not change the behaviour of the attendance, class-filter, student-detail or reporting screens. Run their instrumented suites explicitly — `AttendanceDaoQueryTest`, `AttendanceDaoTest`, `StudentDaoSortingTest`, `StudentDaoTest`, `StudentsScreenTest`, `ScheduleNavigationTest` — and confirm they pass unmodified (SC-018). T081's scope is the schedule and student-detail suites only, so these are NOT covered by it. If any fails, the cause is almost certainly a shared file touched by this feature (`AppContainer`, a mapper, or `ClassFilterRepositoryImpl`) — trace it before fixing the test
- [x] **T083** Run the full quickstart.md validation, including the backward-compatibility walkthrough in §7
- [x] **T084** Re-read spec.md and confirm every one of FR-001…FR-062 and SC-001…SC-019 is implemented or explicitly deferred — with a written reason for any deferral
- [x] **T085** `[SUBAGENT]` Done — plus rules added for the SQLite-version hazard, the two-device rule, and the import bounds. to reflect the duration-based model — the constitution's Governance section requires both before the feature is complete
- [x] **T086** **SETTLED 2026-10-16.** User confirmed: cap students AND dates. `MAX_IMPORT_STUDENTS = 10_000` retained (FR-058, judgement-based, documented as such in FR-062/Assumptions). Added `MAX_IMPORT_ATTENDANCE_DATES_PER_STUDENT = 3_650` (10 years daily, derived) and a **32 MB `MAX_IMPORT_FILE_BYTES` guard enforced before decode** (new FR-063) — record caps alone cannot prevent an OOM because memory is already spent by decode time. research D9 rewritten with the measurements.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — starts immediately
- **Foundational (Phase 2)**: Depends on Phase 1 — **BLOCKS all user stories**
- **User Stories (Phases 3–6)**: All depend on Phase 2 completion
- **Polish (Phase 7)**: Depends on all desired stories; T071 additionally depends on Phase 3 (the old UI must be gone before its strings are)

### User Story Dependencies

- **US1 (P1)**: After Phase 2 — no dependency on other stories
- **US2 (P2)**: After Phase 2 — extends the read path from US1 but is independently testable
- **US3 (P3)**: After Phase 2 — reuses `ReplanConfirmDialog` built in US1, and US2's slot display
- **US4 (P4)**: After Phase 2 — independent of US1–US3 at the code level; T066 touches `ImportMergeEngine` only

### Within Each User Story

- Tests MUST be written and FAIL before implementation
- Domain → data → presentation
- Core implementation before integration
- Story complete before moving to the next priority

### Parallel Opportunities

- **T006+T007** run in parallel with each other; **T008** follows T006 and **T009** follows T007, so the two chains parallelise
- **T011, T012, T013** parallelise; **T016, T017, T020** parallelise once T018 is sequenced
- **T004 + T005** parallelise with all Kotlin work — strings never block code
- After Phase 2, US1–US4 can be staffed in parallel; they touch disjoint files
  except `ScheduleDao.kt` (US1/US2/US3) and `ReplanConfirmDialog.kt` (US1→US3)
- **[SUBAGENT]**-marked tasks are safe to dispatch to parallel agents: they touch
  separate files and none depend on in-flight state

---

## Implementation Strategy

### MVP First (User Story 1 only)

1. Phase 1: Setup
2. Phase 2: Foundational (**critical — blocks all stories**)
3. Phase 3: User Story 1
4. **STOP and VALIDATE** — verify US1 acceptance scenarios independently
5. Demo if ready

### Incremental Delivery

1. Setup + Foundational → foundation ready
2. US1 → test independently → demo (MVP: teacher can set lengths and see capacity)
3. US2 → test independently → demo (running order and slots become visible)
4. US3 → test independently → demo (re-planning and refusal)
5. US4 → test independently → demo (backward compatibility closes the loop)

### Highest-Risk Gate

**T009 — the planner.** Every other story depends on its output being right, and
the rules it encodes (half-open boundaries, three-level tie-break, keep-first-N)
are where the subtle bugs live. Get T007's 10 scenarios green before building
anything on top.

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] labels map tasks to spec.md user stories for traceability
- Verify tests fail before implementing
- Commit per task or logical group, Conventional Commits **with a scope** —
  e.g. `feat(schedule): add duration-based lesson planner`
- Stop at every checkpoint to validate the story independently
- Avoid: vague tasks, same-file conflicts, cross-story dependencies that break independence