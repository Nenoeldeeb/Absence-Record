# Implementation Plan: Duration-Based Lesson Scheduling

**Branch**: `008-duration-based-scheduling` | **Date**: 2026-10-13 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `specs/008-duration-based-scheduling/spec.md`

## Summary

Replace the manually-entered "max students" on a lesson block with a lesson
**length** (10–120 minutes, step 10), from which capacity is derived as
`durationMinutes / 10`. Each assigned student occupies one contiguous 10-minute
slot. Students are sequenced automatically by their next busy appointment, so a
student who must leave soon gets an early slot, and a student's slot — not the
whole block — is what gets validated against their commitments. Adding a student
re-plans the entire block and is refused outright if any resulting slot conflicts.
Slot position is never stored; the order is derived on every read.

## Technical Context

**Language/Version**: Kotlin 2.4.21, JVM target 25
**Primary Dependencies**: AGP 9.4.1, KSP 2.3.6, Compose BOM 2026.09.00
(Material 3), Room 2.8.5, kotlinx-datetime 0.8.0, kotlinx-serialization-json
1.11.0, kotlinx-coroutines 1.11.0, accompanist-pager 0.36.0
**Storage**: Room / SQLite, single-module `:app`, database **version 3 → 4**
**Testing**: JUnit 6 (`junit-jupiter` 6.1.3) + MockK 1.14.11 for JVM;
AndroidJUnit4 + `androidx.room.testing` for instrumented
**Target Platform**: Android, minSdk 26, compileSdk/targetSdk 37
**Project Type**: single-module offline Android app — no server, no network, no
accounts
**Performance Goals**: derive a block's order in well under 1 s; show a full day
of blocks correctly ordered in under 1 s with no jank (SC-019); refuse a full
block immediately (SC-013)
**Constraints**: `java.time` prohibited — `kotlinx-datetime` only; every
user-facing string in `strings.xml` + `values-ar/`; manual DI via `AppContainer`
/ `AppViewModelProvider`; no CI, so every gate is run locally and manually;
source files capped at 300 lines

**No `NEEDS CLARIFICATION` remain.** All resolved in
[research.md](./research.md).

## Constitution Check

*GATE: passed before proceeding. Re-checked after the design phase — see the
bottom of this table.*

| Principle | Status | Notes |
|-----------|--------|-------|
| **I. Clean Architecture** | PASS | The ordering algorithm lives in `domain/services/LessonSlotPlanner.kt` as a pure function with no Android/Room imports, so it cannot violate the inward dependency rule. The DAO orchestrates read → plan → write; it holds no business rules. All use cases return `Result<T>`; no `try-catch` escapes to domain or presentation. |
| **II. MVI & Stable State** | PASS | `ScheduleScreenState` stays a single `@Stable` data class mutated only via `_uiState.update`. The confirm-first flow adds a `pendingReplan` field rather than a new mutable holder. `ScreenEvent` gains events for confirm/cancel; all async work runs in `viewModelScope.launch`. |
| **III. Androidx & Jetpack-First** | PASS | Material 3 slider; `kotlinx-datetime` for `DayOfWeek`; `kotlinx-serialization` for the backup; manual DI untouched. New and changed strings added to `strings.xml` **and** `values-ar/`; four obsolete capacity strings deleted. `java.time` usage remains 0. |
| **IV. Verification & Test Discipline** | PASS | The planner is pure and covered by JUnit 6 + MockK tests including both success and refusal branches. Migration covered by `ScheduleDaoMigrationTest`. Instrumented UI tests use the existing `ScheduleScreenTestHarness` harness, since `mockk-android` cannot mock final ViewModels on device. |
| **V. Simplicity, Performance, Consistency** | **NEEDS ATTENTION** | Three files are close to the 300-line cap and this feature grows them: `HourRow.kt` (289, gains per-student slot ranges), `ScheduleDao.kt` (245, gains two-phase update and per-slot validation), `ImportMergeEngine.kt` (223, gains cap enforcement). **Mitigation is mandatory and listed in the Source Code section below** — the planner is a new file, the DAO delegates to it rather than inlining, and `HourRow`'s new slot display is extracted into its own composable. Derivation is wrapped in `withContext(Dispatchers.Default)`. |
| **VI. Accessibility** | PASS | FR-051 requires the slider's spoken label to announce both the minutes and the derived student count, and requires switch-access/keyboard parity at the same 10-minute granularity. Touch target ≥ 48 dp, no colour-only state, 4.5:1 contrast, TalkBack-checked per [quickstart.md](./quickstart.md) §6. |
| **Room Migrations & Schema Export** | **NEEDS ATTENTION** | Version 3 → 4 with an explicit non-destructive `MIGRATION_3_4`; `4.json` committed; `androidTest` migration test required. **Additional finding**: `app/schemas/dev.nenoeldeeb.education.absencerecord.AppDatabase/` is an orphaned directory whose `4.json` describes an unrelated schema (`notes`, `notes_with_dates`). Leaving it would produce two conflicting `4.json` files. Cleanup is a required task. |
| **AGENTS.md / README.md update** | PASS | Required by Governance before the feature is considered complete; listed in the task breakdown. |

**Gate result**: no VIOLATIONs. Two NEEDS ATTENTION items carry mandatory,
specified mitigations rather than being waved through.

**Post-design re-check**: the design phase added the planner, the two-phase
replan result and the import caps. All sit in `domain/`, so Principle I is
strengthened rather than strained. The Principle V and schema risks above are
unchanged and remain the two things to watch during execution.

## Project Structure

### Documentation (this feature)

```text
specs/008-duration-based-scheduling/
├── spec.md                       # 64 requirements, 19 success criteria, Q1–Q8 resolved
├── plan.md                       # this file
├── research.md                   # D1–D10 decisions + rationale + alternatives
├── data-model.md                 # entities, ordering algorithm, caps
├── contracts/
│   └── backup-file-format.md     # v1 / v2 / v3 compatibility contract
├── quickstart.md                 # runnable validation guide
├── progress.yml                  # superspec phase tracking
├── tasks.md                      # /speckit.superspec.tasks output
└── checklists/requirements.md    # 16/16 passing
```

### Source Code (repository root)

```text
app/src/main/java/dev/nenoeldeeb/education/absencerecord/
├── domain/
│   ├── models/
│   │   ├── ScheduleModels.kt          # MODIFIED — durationMinutes replaces maxStudents;
│   │   │                              #   maxStudents becomes computed
│   │   ├── ScheduleBackupData.kt      # MODIFIED — BACKUP_VERSION 2→3, field rename
│   │   ├── StudentError.kt            # MODIFIED — MaxBelowAssigned removed; new variants
│   │   ├── ImportResult.kt            # MODIFIED — carries cap/skip counts to the summary
│   │   └── LessonPlanning.kt           # NEW — PlannedSlot, PlannedHour, SlotConflict,
│   │                                  #   DroppedStudent, DropReason, HourReplanResult.
│   │                                  #   The feature's central domain vocabulary (D5)
│   ├── services/
│   │   ├── ScheduleRules.kt           # MODIFIED — duration rules replace capacity rules
│   │   ├── LessonSlotPlanner.kt       # NEW — pure ordering + slot + validation (D1)
│   │   └── ImportMergeEngine.kt       # MODIFIED — record caps (D9), 223→~250
│   ├── repositories/
│   │   └── ScheduleRepository.kt      # MODIFIED — durationMinutes in signatures;
│   │                                  #   updateHour returns HourReplanResult (D5)
│   └── usecases/
│       ├── schedule/
│       │   ├── InsertHourUseCase.kt        # MODIFIED
│       │   ├── UpdateHourUseCase.kt        # MODIFIED — two-phase confirm
│       │   └── AssignStudentUseCase.kt     # MODIFIED
│       └── transfer/
│           ├── ExportStudentsUseCase.kt   # MODIFIED — v3 shape
│           ├── ParseImportFileUseCase.kt   # MODIFIED — dual-version read (D4)
│           └── PerformImportUseCase.kt     # MODIFIED — cap reporting
│
├── data/
│   ├── datasources/local/
│   │   ├── StudentDatabase.kt        # MODIFIED — version 4, MIGRATION_3_4 (D3)
│   │   ├── entities/AvailableLessonHourEntity.kt   # MODIFIED — column rename
│   │   └── daos/
│   │       ├── ScheduleDao.kt        # MODIFIED — orchestration only (D6)
│   │       └── ScheduleDaoHelpers.kt # MODIFIED — per-slot overlap (D7)
│   ├── mappers/ScheduleMappers.kt    # MODIFIED
│   └── repositories/ScheduleRepositoryImpl.kt      # MODIFIED — off-main-thread (D8)
│
└── presentation/screens/
    ├── schedule/
    │   ├── ScheduleScreenState.kt    # MODIFIED — pendingReplan
    │   ├── ScheduleScreenEvent.kt    # MODIFIED — confirm/cancel events
    │   ├── ScheduleViewModel.kt      # MODIFIED (154 lines, has headroom)
    │   ├── delegates/
    │   │   ├── HourManagementDelegate.kt      # MODIFIED (173 lines)
    │   │   └── ReplanDelegate.kt             # NEW — confirm-first flow
    │   ├── components/
    │   │   ├── HourRow.kt                    # MODIFIED — show length + derived capacity, minimal edit
    │   │   ├── HourAssignedStudents.kt        # MODIFIED — renders each student's slot range
    │   │   └── LengthSlider.kt               # NEW — slider + derived count (D10)
    │   └── dialogs/
    │       ├── HourDialog.kt                 # MODIFIED (270 — capacity field → slider)
    │       └── ReplanConfirmDialog.kt        # NEW — keep/drop list
    └── studentdetail/
        ├── dialogs/AddLessonDialog.kt         # MODIFIED (258 — shows slots)
        └── components/
            ├── StudentScheduleSection.kt       # MODIFIED — surfaces the student's slot
            └── StudentScheduleLessonsTab.kt    # MODIFIED — shows derived slot per lesson

app/src/main/res/
├── values/strings.xml             # MODIFIED — +new, −4 obsolete capacity strings
└── values-ar/strings.xml          # MODIFIED — full Arabic parity

app/src/androidTest/java/.../data/datasources/local/daos/
└── ScheduleDaoMigrationTest.kt    # MODIFIED — add migrate3To4 cases

app/schemas/
├── dev.nenoeldeeb.education.absencerecord.AppDatabase/     # DELETE — orphaned (D3)
└── ...data.datasources.local.AppDatabase/4.json             # NEW — committed
```

**Structure Decision**: the existing three-layer layout is kept unchanged. The
one genuinely new architectural element is `LessonSlotPlanner` in `domain/services/`,
placed there — rather than in a use case or the DAO — because the same pure rules
are needed by three callers (add student, edit hour, re-validate on commitment
change) and must not be duplicated. Blast radius measured from the codebase:
**22 main-source files and 21 test files** reference `maxStudents` (62
occurrences in main), so this is a wide but mechanical refactor once the planner
exists.

### Mandatory extractions (Principle V)

| File | Now | Action |
|------|-----|--------|
| `HourRow.kt` | 289 | **Minimal edit only** — show length and derived capacity. Slot ranges go in `HourAssignedStudents.kt` (80 lines), which already receives `List<Student>`, so this 289-line file is not grown and not refactored |
| `ScheduleDao.kt` | 245 | Planner is called, not inlined; orchestration only |
| `ImportMergeEngine.kt` | 223 | Cap enforcement extracted into its own file if it would exceed 300 |
| `HourDialog.kt` | 270 | Capacity text field removed, slider added — net roughly neutral |

> Reconciled with `tasks.md` during `/speckit.analyze`. An earlier draft of this
> plan mandated extracting `HourSlotList.kt` out of `HourRow.kt`; the tasks
> decomposition improved on it by reusing `HourAssignedStudents.kt`, which
> avoids touching a file sitting 11 lines under the cap.

## Execution Strategy

### TDD Requirements

- [ ] **`domain/services/LessonSlotPlanner`** — mandatory RED-GREEN. Highest
  edge-case density in the feature: half-open interval boundaries, three-level
  tie-break, "commitment later inside the block must not reject", and the
  keep-first-N rule. Every spec acceptance scenario in §2 of
  [quickstart.md](./quickstart.md) maps to a test written first.
- [ ] **`domain/services/ScheduleRules`** — mandatory. Duration range, step, and
  midnight-boundary predicates are pure and cheap to pin down first.
- [ ] **`ParseImportFileUseCase`** — mandatory. v3 / v2 / legacy / unknown /
  both-fields / out-of-range are six distinct branches; writing them first is
  cheaper than debugging them through the UI.
- [ ] **Migration 3 → 4** — test-first where practical: the conversion and clamp
  cases define what the migration must do before it is written.
- [ ] **DAO transaction methods** — test-first for `assignStudent` and
  `updateHour`, which gain multi-step all-or-nothing behaviour.

### Parallel Execution Opportunities

- [ ] After the planner lands: **domain layer** (rules, models, use cases) and
  **backup format** (v3 export + dual-version import) touch disjoint files and
  can proceed in parallel.
- [ ] **UI thread** (slider, dialogs, slot display, confirm flow) depends only on
  the domain interfaces, so it can start once the planner's signatures are fixed
  — before the DAO work completes.
- [ ] **Migration 3→4 + schema cleanup** is independent of everything except the
  entity field rename, and can be dispatched immediately after that rename.
- [ ] **String resources** (`values/` and `values-ar/`) are independent of all
  Kotlin work.

### Human Checkpoints

1. **After the planner is green** — verify the ordering rules against §2 of
   [quickstart.md](./quickstart.md) before any layer above it is built on top.
   This is the highest-leverage gate in the feature.
2. **After the migration lands** — confirm exactly one `4.json` exists in the
   correct directory and the orphaned directory is gone.
3. **After each user story** — verify behaviour against the spec's acceptance
   scenarios.
4. **Before merge** — run `./gradlew check`, TalkBack pass, and re-read the spec
   for anything unimplemented.

### Review Gates

- [ ] **Data model / migration**: review `MIGRATION_3_4` and the schema cleanup
  before the build is run — a wrong migration is unrecoverable for user data.
- [ ] **`ScheduleRepository` interface**: review signature changes before
  consumers are updated; it is the widest contract in the change.
- [ ] **Backup format contract**: review [contracts/backup-file-format.md](./contracts/backup-file-format.md)
  against `ScheduleBackupData.kt` before export/import ships — a silent
  misreading corrupts a teacher's roster.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

No violations. The two NEEDS ATTENTION items are risks with specified
mitigations, not exceptions:

| Risk | Mitigation |
|------|------------|
| Principle V — three files near the 300-line cap | Mandatory extractions listed in the Source Code section; planner kept in a new file so the DAO does not absorb the logic |
| Schema — duplicate `4.json` in an orphaned directory | Directory deleted as a required task; quickstart §5 asserts exactly one `4.json` in the live directory |
| Rule derivation on every read could be assumed cheap | Bounded by 12 students/block; wrapped in `Dispatchers.Default`; SC-019 gives a measurable budget |
| `MAX_IMPORT_STUDENTS = 10,000` is a judgement call, not derived | Flagged in spec Assumptions and research D9; confirm with the user before it hardens into a test assertion |

## Next Steps

1. `/speckit.superspec.tasks` — decompose into phased tasks with `[TDD]`,
   `[REVIEW]`, `[SUBAGENT]` and `[P]` markers.
2. Confirm the `MAX_IMPORT_STUDENTS` value (see Complexity Tracking).