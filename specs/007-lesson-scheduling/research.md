# Phase 0 Research: Lesson Scheduling

**Feature Branch**: `007-lesson-scheduling` | **Date**: 2026-08-08

## Executive Summary

The feature adds weekly lesson scheduling to the existing Absence Record app. The
research below resolves every technical unknown against the current codebase
(`HorizontalPager` navigation, Room data layer, MVI presentation, JUnit6 + MockK
testing) and the project constitution (`kotlinx-datetime` only, `Result<T>`
return types, `strings.xml`/`UiText`, max 300 lines per file, accessibility).

**The user's navigation requirement** — *"make sure appointments and busy
appointments sections next to attendance report navigation via horizontal
pager"* — is realized by adding a third page to the existing main
`HorizontalPager`: page order becomes **0: Students, 1: Calendar (attendance
report), 2: Schedule**. The Schedule page hosts the two sections — **Appointments**
(teacher's weekly available lesson hours + student assignments) and **Busy
Appointments** (per-student blocked times) — switched by a Material 3
`SecondaryTabRow`.

---

## Technical Decisions & Rationale

### 1. Navigation: third page in the existing `HorizontalPager` (next to the attendance report)

- **Decision**: Extend `MainScreen` (`app/MainActivity.kt`) from a 2-page to a
  **3-page** `HorizontalPager`:
  - Page 0: `StudentsScreen`
  - Page 1: `CalendarScreen` (attendance report — unchanged)
  - Page 2: `ScheduleScreen` (new)
  - `AppNavigation.kt` changes `rememberPagerState(initialPage = 1, pageCount = { 2 })`
    to `pageCount = { 3 }`. `initialPage = 1` is preserved so the app still opens on
    Calendar. No `AppDestination` back-stack change: the Schedule page is a pager
    page, not a pushed destination.
- **Rationale**:
  - Directly satisfies the explicit user requirement ("sections next to attendance
    report navigation via horizontal pager").
  - The existing state-driven navigation (`AppNavigation` back-stack +
    `AnimatedContent`) is untouched; `StudentDetail` push/pop continues to work
    above the whole pager.
  - Reuses the already-present foundation `HorizontalPager` (no new dependency;
    the legacy `accompanist-pager` artifact in `libs.versions.toml` is unused by
    `MainActivity` and is NOT needed).
- **Alternatives Considered**:
  - *New top-level `AppDestination.Schedule` pushed on the back-stack*: Rejected —
    the user explicitly wants swipe navigation next to the attendance report, and a
    pushed destination would hide the pager.
  - *Replace pager with Navigation-3 / nav library*: Rejected — the project
    deliberately avoids navigation libraries (see `006-student-detail-screen`).

### 2. Schedule page internal structure: shared weekday selector + two sections

- **Decision**: Inside `ScheduleScreen`, a shared **weekday selector** (7 chips,
  **Saturday-first** to match `ComposeCalendar.kt`'s week ordering) scopes both
  sections, followed by a Material 3 `SecondaryTabRow` with two tabs:
  - **Appointments** — LazyColumn of the selected weekday's available hours, each
    showing start–end range (fixed 60 min) and occupancy `assigned / max`, with
    edit / delete / "assign students" actions and an empty state.
  - **Busy Appointments** — a student picker (dropdown of all students) + that
    student's busy appointments for the selected weekday, with add / edit / delete
    and an empty state.
- **Rationale**:
  - The spec's availability plan, the one-lesson-per-day rule, and busy-appointment
    recurrence are all **per-weekday**, so one weekday selector is the natural
    shared scoping control.
  - A `TabRow` (not a nested pager) is the standard Material 3 pattern for two
    sibling sections and keeps state trivial.
  - "Sections next to attendance report" is satisfied by the page placement in
    Decision 1; tabs provide the two sections inside that page.
- **Alternatives Considered**:
  - *Nested `HorizontalPager` inside the Schedule page*: Rejected — an unlabeled
    swipe between two dense management lists hurts discoverability and
    accessibility versus labeled tabs.
  - *A single flat weekly grid view*: Rejected — it complicates the add/edit
    dialogs and the per-weekday flow the teacher actually follows.

### 3. Time representation: minutes-of-day `Int`, snapped to :00/:30 (no `java.time`)

> **Superseded (2026-08-11)**: the `:00`/`:30` snap rule was removed — start
> times are exact minute-of-day (see spec.md Brainstorm Log, "Normal time picker
> + extended busy duration"). The minutes-of-day model below still stands.

- **Decision**: Store and compute all wall-clock times as **minutes since
  midnight (`Int`)**:
  - `startMinutes` for hours ∈ `0..1380` (23:00) with `startMinutes % 30 == 0`
    (60-min lesson must fit in the day).
  - `startMinutes` for busy appointments ∈ `0..1410` (23:30), `% 30 == 0`
    (FR-010 / clarification: start times snap to :00/:30).
  - `durationMinutes` for busy appointments ∈ `[15, 1440 - startMinutes]`
    (FR-010: at least 15 min; must not cross midnight).
  - Lesson duration is the constant `60` — **not stored**, per the clarified spec
    ("fixed 60-minute lessons").
- **Rationale**:
  - Integer minutes make the :00/:30 snap, duration math, and overlap checks
    trivial and unit-testable, and serialize to Room without any converter.
  - `kotlinx-datetime` has no `Duration`-of-day arithmetic story suitable here
    (its docs recommend using `Instant` for arithmetic), and `java.time` is
    **prohibited** by the constitution — minutes-of-day is the simplest compliant
    model.
- **Alternatives Considered**:
  - *`kotlinx.datetime.LocalTime` fields*: Rejected — needs a Room `TypeConverter`,
    and time math (end = start + duration) is more awkward than `Int`.
  - *`java.time.LocalTime`*: Rejected — violates constitution Principle III.

### 4. Time display: localized via `java.util.Calendar` + `android.text.format.DateFormat`

- **Decision**: Add `TimeFormatter` (`presentation/utils/TimeFormatter.kt`) that
  formats `minutes: Int` to a locale-aware string using
  `android.text.format.DateFormat.getTimeFormat(context)` with a
  `java.util.Calendar` set to the hour/minute. It honors the user's 12/24-hour
  system preference and Arabic AM/PM labels ("9:00 ص") without any `java.time`
  dependency. Called from composables (which already have `LocalContext`), wrapped
  in `UiText.DynamicString` or formatted inline via `remember(minutes)`.
- **Rationale**:
  - `kotlinx-datetime` provides no localized *display* formatter (research:
    kotlinx-datetime formatting is ISO-oriented, no CLDR skeleton support on
    Android). `java.util.Calendar`/`java.text.DateFormat` are not `java.time` and
    are permitted.
  - The project already gateways all date/day display through
    `DateFormatter.kt` + `strings.xml`; `TimeFormatter` is the analogous, narrow
    utility for wall-clock times.
- **Alternatives Considered**:
  - *Hand-rolled `HH:mm` string*: Rejected — breaks 12/24-hour preference and
    Arabic-Indic digit rendering.
  - *`SimpleDateFormat` patterns*: Rejected (redundant) — `getTimeFormat` already
    returns the correct localized `DateFormat`.

### 5. Time selection UX: `ExposedDropdownMenuBox` time-slot list (no Material3 TimePicker)

> **Superseded (2026-08-11)**: start times are now entered with a standard
> Material3 `TimePickerDialog` at exact-minute granularity, and busy durations
> with a 0.5h–6h slider in 30-minute steps (see spec.md Brainstorm Log, "Normal
> time picker + extended busy duration"). Kept below as the historical record.

- **Decision**: Start-time entry uses an `ExposedDropdownMenuBox` listing all
  :00/:30 slots (48 entries, locale-formatted by `TimeFormatter`). Duration entry
  for busy appointments uses a dropdown of common durations
  (15, 30, 45, 60, 90, 120, 150, 180, 240 min).
- **Rationale**:
  - **Research finding**: `rememberTimePickerState(initialHour, initialMinute,
    is24Hour)` has **no minute-step parameter**, so Material3's `TimePicker` cannot
    enforce the :00/:30 snap natively; snapping on OK would silently rewrite a
    teacher's chosen :15 to :00.
  - A restricted slot list **enforces the snap by construction** — no post-hoc
    rounding, no invalid input states, no `ExperimentalMaterial3Api` surface.
  - Matches the app's established `ExposedDropdownMenuBox` pattern
    (`AttendanceReportSection.kt` month picker).
  - Trivially testable in Compose UI tests and keyboard/switch-accessible.
- **Alternatives Considered**:
  - *Material3 `TimePicker` + snap on confirm*: Rejected per research finding
    (silent rounding, larger experimental API surface).
  - *Free-text `OutlinedTextField` ("9:00") + validation*: Rejected — Arabic input
    layout + parse errors add friction for a constrained value set.

### 6. Data model: three new Room tables, DB version 2 → 3

- **Decision**: New entities (Room `@Entity`, FK `CASCADE` mirrors the existing
  `StudentAttendanceEntity` pattern):
  - `available_lesson_hours` (`id`, `weekday: Int` = `DayOfWeek.isoDayNumber`,
    `startMinutes: Int`, `maxStudents: Int`) — index on `weekday`.
  - `lesson_assignments` (`id`, `availableHourId` FK→hours CASCADE,
    `studentId` FK→students CASCADE, `weekday: Int` denormalized from the hour)
    — **unique index `(studentId, weekday)`** as the DB backstop for the
    one-lesson-per-student-per-day rule (FR-008); indices on `availableHourId`,
    `studentId`.
  - `busy_appointments` (`id`, `studentId` FK→students CASCADE, `weekday: Int`,
    `startMinutes: Int`, `durationMinutes: Int`) — index on `(studentId, weekday)`.
  - Add `MIGRATION_2_3` (CREATE TABLEs + indices) to `StudentDatabase.kt`;
    bump version to 3. `exportSchema = true` continues to emit
    `app/schemas/.../3.json`.
- **Rationale**:
  - FK `CASCADE` gives **FR-013** (delete hour ⇒ assignments removed) and
    **FR-014** (delete student ⇒ assignments + busy appointments removed) for free,
    matching the existing attendance-cascade behavior and the
    `DeleteStudentsUseCase`/`AttendanceDao` pattern.
  - Denormalizing `weekday` onto the assignment makes the unique constraint and
    all per-weekday queries (eligibility, conflict checks) simple SQL.
- **Alternatives Considered**:
  - *Storing times as `String`/`LocalTime`*: Rejected (see Decision 3).
  - *Composite key instead of surrogate ids*: Rejected — the codebase consistently
    uses auto-generated `Int` PKs.

### 7. Business rules: pure `ScheduleRules` in `domain/services` + `@Transaction` orchestration in the data layer

- **Decision**:
  - `domain/services/ScheduleRules.kt` — a pure, dependency-free object holding the
    constants and predicates: `LESSON_DURATION_MINUTES = 60`, `SNAP = 30`,
    `DAY_MINUTES = 1440`, `MIN_BUSY_DURATION_MINUTES = 15`,
    `overlaps(aStart, aEnd, bStart, bEnd)` (`aStart < bEnd && aEnd > bStart`),
    `fitsInDay(start, duration)`, `isSnappedToHalfHour(minutes)`, hour/busy start
    validity. This is where the spec's conflict definition ("lesson must start
    before the busy appointment ends and end after the busy appointment starts")
    is encoded.
  - `ScheduleDao` exposes `@Transaction` (concrete DAO) methods for every
    read-check-write flow so invariants hold atomically:
    - `insertHour`/`updateHour` — reject overlap on the same weekday
      (`countHoursOverlapping(weekday, start, start+60, excludeId)`); on
      `updateHour`, enforce `maxStudents >= assignedCount` (FR-004) and
      auto-remove assignments whose students now overlap a busy appointment
      (FR-017), returning removed student ids.
    - `insertAssignment` — reject when the hour is full (FR-007), when the
      student already has a lesson that weekday (FR-008), or when the hour
      overlaps a busy appointment (FR-009).
    - `insertBusyAppointment`/`updateBusyAppointment` — delete overlapping
      assignments of that student on that weekday (FR-011), returning removed
      student ids.
  - Repository interface + impl keep the existing
    `Flow<Result<T>>` / `Result<T>` shapes (`catch { emit(Result.failure(...)) }`,
    `runCatching`), mirroring `AttendanceRepositoryImpl`/`StudentClassRepositoryImpl`
    (which already validates `DuplicateClass` inside the impl — precedent for
    data-layer invariant enforcement).
- **Rationale**:
  - The overlap/conflict *definitions* stay in the Domain layer (pure, unit-tested,
    readable), while the *atomic orchestration* lives where Room transactions
    exist — consistent with the codebase's existing pattern and the constitution's
    "no try/catch escaping to domain/presentation" rule.
  - Typed failures (`HourConflict`, `HourFull`, etc.) keep the ViewModel error
    mapping to `UiText` mechanical.
- **Alternatives Considered**:
  - *All rules in use cases with separate repository calls*: Rejected — the
    read-check-write sequences (e.g. busy-add auto-unassignment) would race between
    separate calls; `@Transaction` is the only safe place.
  - *SQL triggers / CHECKs for overlap*: Rejected — overlap needs set-based logic
    across rows plus rich per-student messaging; Room-level Kotlin in `@Transaction`
    is clearer and testable.

### 8. "Teacher informed of removals" = typed success payloads

- **Decision**: Mutating ops that can auto-remove assignments return
  `Result<AssignmentRemovalReport>` where `AssignmentRemovalReport(removedStudentIds:
  List<Int>)`. The ViewModel resolves names from the loaded `allStudents` list and
  surfaces `UiText` (pluralized) like *"Removed from 9:00: Ali, Sara (busy
  conflict)"*. FR-011/FR-017 messaging is therefore deterministic and testable.
- **Rationale**: A success-with-payload keeps the flow non-blocking (matches the
  app's Snackbar toast pattern) and avoids encoding names in the data layer.
- **Alternatives Considered**:
  - *Side-effect return via SharedFlow*: Rejected — simpler to thread the payload
    through `Result<T>`.
  - *Names resolved in the repository*: Rejected — leaks presentation strings into
    data.

### 9. MVI presentation with explicit file decomposition (300-line limit)

- **Decision**: `presentation/screens/schedule/` is decomposed to keep every file
  ≤ 300 lines, following the `students/` pattern (delegates + handlers):
  - `ScheduleViewModel.kt` (state, load, event routing)
  - `delegates/` — `HourManagementDelegate`, `AssignmentDelegate`,
    `BusyManagementDelegate` (CRUD + payload→UiText mapping)
  - `components/` — `WeekdaySelector.kt`, `AppointmentsTab.kt`, `HourRow.kt`,
    `BusyTab.kt`
  - `dialogs/` — `HourDialog.kt`, `AssignStudentsDialog.kt`,
    `BusyAppointmentDialog.kt`, `ScheduleConfirmDialog.kt`
  - `ScheduleScreen.kt` (root scaffold + tabs), `ScheduleScreenState.kt`,
    `ScheduleScreenEvent.kt`
- **Rationale**: The state/event surface is large (two CRUD areas + assignment
  dialog); centralizing everything in one VM/one file would violate the 300-line
  gate (already enforced by `003-resolve-constitution-violations`).
- **Alternatives Considered**:
  - *One flat file per concern with no delegates*: Rejected — would exceed the
    line gate.
  - *Sub-navigation (push hour details)*: Rejected — dialogs are lighter and match
    the Calendar `AttendanceDialog` precedent.

### 10. Student profile schedule view (User Story 4 / FR-015, P3)

- **Decision**: `StudentDetailBody` gains a `StudentScheduleSection` (read-only)
  listing the student's weekly lessons (weekday + time range) and busy
  appointments, sourced from `observeStudentScheduleUseCase(studentId)`. The
  section auto-refreshes because Room flows re-emit on any schedule change
  (FR-015 "changes reflected immediately"). `StudentDetailViewModel` stays under
  300 lines by moving schedule loading into a `StudentScheduleDelegate`.
- **Rationale**: Closes the loop required by the spec while keeping the detail
  screen's attendance calendar intact.
- **Alternatives Considered**: *Separate pushed screen from the profile*: Rejected
  — the spec says the schedule is displayed *on* the profile.

### 11. No class filter on the Schedule screen

- **Decision**: The Schedule screen shows **all students** (loaded via
  `getAllStudentsUseCase(SortType.ByName, null)`). The `ClassFilterRepository`
  multi-select filter is **not** applied.
- **Rationale**: Explicit spec assumption: *"The schedule applies to all students
  in the register; class-level filtering of scheduling is out of scope for v1."*
- **Alternatives Considered**: *Reusing `ClassCheckboxFilter`*: Rejected — out of
  scope per spec and would couple scheduling to the Calendar/Students filter state.

### 12. DI wiring

- **Decision**: `AppContainer` gains `scheduleRepository: ScheduleRepository`
  (`ScheduleRepositoryImpl(ScheduleDao)`) and `scheduleUseCases: ScheduleUseCases`.
  `AppViewModelProvider.factory` gains a `ScheduleViewModel` initializer;
  `studentDetailFactory(studentId)` gains `scheduleUseCases`.
- **Rationale**: Follows the manual-DI pattern (Principle III) exactly as done for
  `CalendarViewModel` and `StudentDetailViewModel`.

### 13. Testing strategy

- **Unit (JUnit6 + MockK, `runTest` + `MainDispatcherRule`)**, testing **both
  `onSuccess` and `onFailure`** paths per constitution:
  - `ScheduleRulesTest` — snap/boundary/overlap matrix including the exact-boundary
    case "busy ends at 9:00 ⇒ 9:00 lesson assignable".
  - `ScheduleRepositoryImplTest` — success + each `StudentError` subtype.
  - Use-case tests per rule (capacity, one-per-day, busy conflict, auto-removal
    payloads, cascade).
  - `ScheduleViewModelTest` (+ base) and delegate tests — state updates, tab/weekday
    switching, dialog flows, removal toasts, error bindings.
  - `StudentDetailViewModelScheduleTest` — profile section load.
- **Instrumented (`androidTest`)**: `ScheduleDaoTest` (Room in-memory; inserts,
  overlap rejection, occupancy joins, `@Transaction` auto-unassignment, FK cascade);
  `ScheduleScreenTest` (tabs, weekday selector, add/edit hour, assign dialog
  eligibility rendering, busy add/edit/delete, a11y descriptions); `MainScreenPagerTest`
  (exactly 3 pages, swipe to Schedule); `TimeFormatterTest` (locale formatting).
- **Rationale**: Matches existing suites (`CalendarViewModelTestBase`,
  `StudentsViewModelTestBase`, DAO instrumented tests).
- **Alternatives Considered**: *Only ViewModel tests*: Rejected — the 300-line rule
  plus rule-heavy domain make dedicated `ScheduleRules` and DAO tests mandatory.

---

## Code Quality & Accessibility Constraints

- Every new file (main + tests) ≤ 300 lines.
- All new user-facing strings in `strings.xml` + `values-ar/strings.xml`, wrapped
  in `UiText` (no hardcoded strings).
- `kotlinx-datetime` only for dates/days (`DayOfWeek`); **no `java.time`**; time
  *display* via `TimeFormatter`.
- Accessibility: weekday chips and every icon get meaningful `contentDescription`;
  touch targets ≥ 48dp (Material defaults); state never conveyed by color alone
  (occupancy shown as text "2 / 5"; selected chip also read aloud via semantics).
