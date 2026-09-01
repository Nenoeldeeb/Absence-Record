# Implementation Plan: 007 Lesson Scheduling

**Branch**: `007-lesson-scheduling` | **Date**: 2026-08-08 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/007-lesson-scheduling/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

## Summary

The teacher defines **weekly lesson hours** (60-min fixed lessons, per weekday,
with a max-student capacity) on the **Schedule page** and manages **per-student
busy appointments** (recurring per weekday, `startTime` + `duration` 0.5h–6h in
30-min steps) from the **Student Detail** screen. The Schedule page is the weekly availability
plan only: a Saturday-first weekday selector + a list of hours with occupancy
"x / y"; each hour row is expandable to show the assigned student names
(read-only) and tapping a name opens that student's profile. Assignment and busy
management live on Student Detail (Lessons / Busy tabs, scoped to the selected
weekday): a student is assigned via the "Add lesson" dialog subject to hour
capacity (FR-007), one lesson per student per day (FR-008), and no overlap with
that student's busy appointments (FR-009). Busy add/edit **previews** conflicting
lessons in the dialog and removes them only after the teacher confirms (FR-011);
deleting an hour or student cascades to its assignments (FR-013/014).

**The user's navigation requirement** — "appointments and busy appointments
sections next to attendance report navigation via horizontal pager" — is met by
extending the existing main `HorizontalPager` to **3 pages** (0: Students, 1:
Calendar/attendance report, 2: Schedule). The Schedule page is the weekly
availability plan (weekday selector + hours list with occupancy and expandable,
read-only assigned names). Assignment and busy-appointment management moved to
the **Student Detail** screen (2026-08-09 spec revision): a Saturday-first
weekday selector plus a `SecondaryTabRow` with **Lessons** and **Busy** tabs
(scoped to the open student).

**Technical approach** (see [research.md](./research.md)):
- Times stored as **minutes-of-day `Int`** at exact-minute granularity (no
  `java.time`); lesson duration is the constant `60`, not stored.
- Three new Room tables (`available_lesson_hours`, `lesson_assignments`,
  `busy_appointments`), DB v3 + `MIGRATION_2_3`, FK `CASCADE`, unique
  `(studentId, weekday)` index.
- Conflict/capacity rules as pure `ScheduleRules` (domain/services) orchestrated
  atomically in `ScheduleDao @Transaction` methods; typed `StudentError` subtypes;
  auto-removals returned as `AssignmentRemovalReport` (hour-edit removals →
  localized toast, FR-017; busy removals → preview-and-confirm in the dialog
  before the repository call, FR-011).
- Time entry via a standard Material3 `TimePickerDialog` at exact-minute
  granularity (launched from a read-only start-time field, `is24Hour = null` per
  system locale) for both available-hour and busy-appointment starts (2026-08-11
  spec revision); busy durations via a 0.5h–6h slider in 30-minute steps with a
  live "Ends at" preview and midnight-boundary validation (start + duration ≤
  24:00; latest hour start 23:00).
- Display via new `TimeFormatter` (`android.text.format.DateFormat` + Calendar) for
  system-locale 12/24h + Arabic AM/PM; all strings via `strings.xml`/`UiText`.
- MVI `ScheduleViewModel` (keeps `HourManagementDelegate` only) and
  `StudentDetailViewModel` decomposed into delegates to honor the 300-line rule.
- **Backup/restore (rev 3, 2026-08-09)**: the existing Students-screen import/export
  flow now carries schedule data — a single versioned JSON object
  `{ "version": 2, "students": [...], "availableHours": [...] }` where hours,
  per-student `lessonAssignments`, and `busyAppointments` are referenced by natural key
  (`weekday` + `startMinutes`, ids rebuilt on import). Export always writes the FULL
  availability plan regardless of student selection (FR-027); import merges additively —
  no wholesale deletion except busy-wins (FR-028), capacity raised to fit imported
  lessons (FR-029), orphan/duplicate/malformed entries skipped + counted (FR-026/030/031),
  legacy flat-array files still import students + dates only (FR-023), unknown/higher
  versions rejected (FR-032). The post-import message reports hours/lessons/busy outcomes
  (FR-033). See `data-model.md` "Backup / restore export format" for the DTOs and rules.

## Technical Context

**Language/Version**: Kotlin 2.4.10 / Kotlin JVM 25 (project uses Kotlin 2.4.0+)

**Primary Dependencies**: Jetpack Compose (BOM 2026.06.00, foundation `HorizontalPager`), Room 2.8.4, Kotlinx Datetime 0.8.0, Kotlinx Serialization

**Storage**: Room SQLite database (v2 → v3 migration)

**Testing**: JUnit 6, MockK, Compose rules, `runTest`, MainDispatcherRule

**Target Platform**: Android (API 26+)

**Project Type**: Android App Module (Clean + MVI)

**Performance Goals**: Responsive UI at 60+ fps, no Main-thread blocking for CPU-intensive operations (parsing/db/serialization); schedule flows are Room-backed `Flow`s (no polling).

**Constraints**: Main-safety via dispatchers, manual DI (AppContainer), Arabic localization support (`values-ar`), `kotlinx-datetime` only (NO `java.time`), every source file ≤ 300 lines.

**Scale/Scope**: Local offline-first architecture.

## Constitution Check

_GATE: Must pass before Phase 0 research. Re-check after Phase 1 design._

- [x] **Clean Architecture Check**: Domain (models, `ScheduleRepository` interface, `ScheduleRules` service, single-action use cases), Data (Room entities/DAOs, `ScheduleRepositoryImpl`, mappers), Presentation (schedule + studentdetail sections) layers separated — mirrored from `StudentRepository`/`AttendanceRepository`/`CalendarScreen`.
- [x] **MVI / UDF Check**: Single `@Stable` `ScheduleScreenState` mutated only via `_uiState.update`; `sealed interface ScheduleScreenEvent`; collection via `collectAsStateWithLifecycle()`; dialogs/tabs/weekday derived from state.
- [x] **Technology Check**: `kotlinx-datetime` (`DayOfWeek`, `LocalDate`) only; Room entities + DAO; new strings in `strings.xml` + `values-ar` wrapped in `UiText`; no `java.time` (time display via `TimeFormatter` using `java.util.Calendar`/`DateFormat`, which are not `java.time`).
- [x] **Test Check**: Both `onSuccess` and `onFailure` paths planned for repository, use cases, and `ScheduleViewModel`; `ScheduleRulesTest` covers the overlap matrix; DAO + screen instrumented tests (see [quickstart.md](./quickstart.md)).
- [x] **Main-Safety Check**: All DB work inside Room (suspend/@Transaction on its own executor); no computation on Main beyond light state derivation; ViewModel uses `viewModelScope.launch`.
- [x] **File Size Check**: Every new file (main + test) ≤ 300 lines via delegate decomposition (`delegates/`, `components/`, `dialogs/`) and separate mapper/entity files.
- [x] **Accessibility Check**: `contentDescription` on all icon actions, 48dp targets, occupancy as text (not color alone), `SecondaryTabRow` + chips expose selection to TalkBack; dialogs are proper `Dialog`s with focus management.

**Result**: PASS — no violations requiring Complexity Tracking justification.

## Project Structure

### Documentation (this feature)

```text
specs/007-lesson-scheduling/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
│   ├── schedule-repository-contract.md
│   └── ui-schedule-contracts.md
├── checklists/requirements.md   # all-pass clarification checklist
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
app/src/main/java/dev/nenoeldeeb/education/absencerecord/
├── app/                          # AppContainer, AppViewModelProvider, AppNavigation, MainActivity (pager → 3 pages)
├── domain/
│   ├── models/
│   │   ├── StudentError.kt       # + HourOverlap, HourFull, AssignmentExists, BusyConflict
│   │   └── ScheduleModels.kt     # AvailableLessonHour, LessonAssignment, BusyAppointment,
│   │                             #   AssignmentRemovalReport, HourWithOccupancy, StudentScheduleView
│   ├── repositories/
│   │   └── ScheduleRepository.kt # interface (Flows + suspend mutations → Result<T>)
│   ├── services/
│   │   └── ScheduleRules.kt      # constants + overlap/validity predicates (pure)
│   └── usecases/schedule/        # ObserveHoursForWeekday, ObserveBusyAppointmentsForWeekday,
│                                 #   ObserveStudentSchedule, InsertHour, UpdateHour, DeleteHour,
│                                 #   AssignStudent, UnassignStudent, InsertBusyAppointment,
│                                 #   UpdateBusyAppointment, DeleteBusyAppointment + ScheduleUseCases group
├── data/
│   ├── datasources/local/
│   │   ├── StudentDatabase.kt    # version 3 + MIGRATION_2_3
│   │   ├── entities/AvailableLessonHourEntity.kt
│   │   ├── entities/LessonAssignmentEntity.kt
│   │   ├── entities/BusyAppointmentEntity.kt
│   │   └── daos/ScheduleDao.kt   # queries + @Transaction orchestration
│   ├── repositories/ScheduleRepositoryImpl.kt
│   └── mappers/ScheduleMappers.kt
└── presentation/
    ├── screens/
    │   ├── schedule/                   # availability-only (2026-08-09 spec)
    │   │   ├── ScheduleScreen.kt       # root: pager page, WeekdaySelector + hours list
    │   │   ├── ScheduleScreenState.kt  # incl. expandedHourIds (survives tap-to-profile)
    │   │   ├── ScheduleScreenEvent.kt  # hours + expand/student-click only (no tabs/assign/busy)
    │   │   ├── ScheduleViewModel.kt
    │   │   ├── delegates/              # HourManagementDelegate
    │   │   ├── components/             # WeekdaySelector, AppointmentsTab, HourRow (expandable)
    │   │   └── dialogs/                # HourDialog, ScheduleConfirmDialog
    │   └── studentdetail/              # interactive weekly schedule (FR-015/018/019)
    │       ├── StudentScheduleSection.kt      # WeekdaySelector + SecondaryTabRow "Lessons" | "Busy"
    │       ├── StudentScheduleLessonsTab.kt   # per-weekday lessons + unassign + Add lesson
    │       ├── StudentScheduleBusyTab.kt      # per-weekday busy CRUD
    │       ├── StudentScheduleDelegate.kt     # collects observeStudentScheduleUseCase
    │       ├── StudentScheduleTabsDelegate.kt # weekday/tab + add-lesson/unassign
    │       ├── BusyManagementDelegate.kt      # busy dialog + preview-and-confirm (FR-011)
    │       ├── StudentDetailReportDelegate.kt # attendance share (existing detail feature)
    │       └── dialogs/                # AddLessonDialog, BusyAppointmentDialog
    ├── utils/TimeFormatter.kt          # minutes → localized wall-clock string
    └── (existing utils/StudentErrorUiMapper.kt → extend with new error cases)
```

**Structure Decision**: Mirrors the existing three-layer Clean structure captured above (`domain/usecases/<group>/` single-action use cases, `data/datasources/local/{entities,daos}`, `presentation/screens/<feature>`). The schedule feature decomposes into delegates/components/dialogs strictly to honor the 300-line-per-file rule (same approach as `StudentsViewModel`/`CalendarViewModel` handlers). Navigation is the existing composition-based pager + back-stack in `app/navigation/` — no nav library.

## Implementation Phases (preview — detailed tasks generated by /speckit-tasks)

### Phase 0 — Research (COMPLETE)
`research.md` written; all decisions recorded (pager placement, minute-model,
time-picker + duration-slider time entry, DAO `@Transaction` orchestration,
delegate decomposition).

### Phase 1 — Design (COMPLETE)
`data-model.md`, `contracts/` (repository + UI/navigation), `quickstart.md`
written; constitution re-checked (PASS).

### Phase 2 — Implementation (via /speckit-tasks)
1. **Data layer**: entities + `MIGRATION_2_3` + DB v3; `ScheduleDao` (+ `@Transaction`
   invariants); `ScheduleMappers`; `ScheduleRepository`/`Impl`; `ScheduleRules`.
2. **Domain**: `StudentError` subtypes; `ScheduleModels`; use cases + group.
3. **DI**: `AppContainer` (repo + use cases); `AppViewModelProvider` (Schedule VM +
   studentDetail gains schedule use cases).
4. **Presentation**: availability-only `ScheduleScreenState/Event/ViewModel` +
   `HourManagementDelegate`; components (WeekdaySelector, AppointmentsTab,
   expandable `HourRow` with tap-to-profile); dialogs (HourDialog,
   ScheduleConfirmDialog); wire page 2 in `MainScreen` (`pageCount = { 3 }`);
   `TimeFormatter`; strings (en + ar); `StudentErrorUiMapper` extension;
   interactive `StudentScheduleSection` (Lessons/Busy tabs + `AddLessonDialog` +
   `BusyAppointmentDialog` with preview-and-confirm) on Student Detail.
5. **Tests**: unit (rules, repo, use cases, VM, delegates, detail VM) + androidTest
   (`ScheduleDaoTest`, `ScheduleScreenTest`, `MainScreenPagerTest`).
6. **Verify**: `./gradlew ktlintFormat`, `./gradlew check`, targeted test commands.

## Complexity Tracking

> No constitution violations to justify.
