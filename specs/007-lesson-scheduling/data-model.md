# Phase 1 Data Model: Lesson Scheduling

**Feature Branch**: `007-lesson-scheduling` | **Date**: 2026-08-08

## Constants

| Name | Value | Source |
|---|---|---|
| `LESSON_DURATION_MINUTES` | `60` | spec (fixed 60-min lessons) |
| `DAY_MINUTES` | `1440` | midnight boundary |
| `MIN_BUSY_DURATION_MINUTES` | `30` | FR-010 (0.5h minimum) |
| `MAX_BUSY_DURATION_MINUTES` | `360` | FR-010 (6h maximum) |
| Hour start range | `0..1380` (23:00) | must fit a 60-min lesson |
| Busy start | any exact minute | bounded by `start + duration <= 1440` |
| Busy duration | `30..360`, multiple of 30 | FR-010 (0.5h–6h slider in 30-min steps); must not cross midnight |
| `DayOfWeek.isoDayNumber` | `1..7` | Mon=1 … Sun=7 (DB `weekday` values) |

All times are **minutes since midnight (`Int`)** at exact-minute granularity (the
`:00`/`:30` snap rule was removed, 2026-08-11). Lesson duration is **not
stored**.

---

## Room Schema (DB version 2 → 3)

### `available_lesson_hours`
```
id          INTEGER PK AUTOINCREMENT
weekday     INTEGER NOT NULL              -- DayOfWeek.isoDayNumber
startMinutes INTEGER NOT NULL              -- exact minute-of-day
maxStudents INTEGER NOT NULL               -- > 0
```
- Index: `idx_available_lesson_hours_weekday` on `weekday`.
- CHECKs (DAO-enforced, not SQL): `startMinutes + 60 <= 1440`, `maxStudents > 0`,
  no two hours overlap on the same weekday.

### `lesson_assignments`
```
id             INTEGER PK AUTOINCREMENT
availableHourId INTEGER NOT NULL FK -> available_lesson_hours.id ON DELETE CASCADE
studentId      INTEGER NOT NULL FK -> students.id ON DELETE CASCADE
weekday        INTEGER NOT NULL          -- denormalized from the hour
```
- **Unique index** `idx_lesson_assignments_studentId_weekday` on `(studentId, weekday)` —
  DB backstop for "one lesson per student per day" (FR-008).
- Indices: `idx_lesson_assignments_availableHourId` on `availableHourId`,
  `idx_lesson_assignments_studentId` on `studentId`.

### `busy_appointments`
```
id              INTEGER PK AUTOINCREMENT
studentId       INTEGER NOT NULL FK -> students.id ON DELETE CASCADE
weekday         INTEGER NOT NULL
startMinutes    INTEGER NOT NULL          -- exact minute-of-day
durationMinutes INTEGER NOT NULL          -- 30..360, multiple of 30
```
- Index: `idx_busy_appointments_studentId_weekday` on `(studentId, weekday)`.

### Migration `MIGRATION_2_3`
Plain `CREATE TABLE` + `CREATE INDEX` statements (no data transformation).
Register in `StudentDatabase.kt`, bump `version = 3`.

---

## Domain Models

```kotlin
data class AvailableLessonHour(
    val id: Int,
    val weekday: DayOfWeek,
    val startMinutes: Int,
    val maxStudents: Int,
)

data class LessonAssignment(
    val id: Int,
    val availableHourId: Int,
    val studentId: Int,
    val weekday: DayOfWeek,
)

data class BusyAppointment(
    val id: Int,
    val studentId: Int,
    val weekday: DayOfWeek,
    val startMinutes: Int,
    val durationMinutes: Int,
)

data class AssignmentRemovalReport(
    val removedStudentIds: List<Int>,   // students auto-unassigned by an edit/insert
)
```

### Views (screen-level, built in use cases / VM from flows)

```kotlin
data class HourWithOccupancy(
    val hour: AvailableLessonHour,
    val endMinutes: Int,            // startMinutes + LESSON_DURATION_MINUTES
    val assignedStudentIds: List<Int>,
    val assignedCount: Int,
    val maxStudents: Int,
    val remainingSlots: Int,        // maxStudents - assignedCount
    val isFull: Boolean,
)
```
Occupancy is derived from `lesson_assignments` joins; **not** stored.

---

## New `StudentError` subtypes

```kotlin
sealed interface StudentError {
    // existing: Database, DuplicateClass, FileRead, FileWrite,
    //           ImportParse, ReportGeneration, Validation(message), Cancelled

    data object HourOverlap         // new hour overlaps an existing one on same weekday
    data object HourFull            // assignment rejected: capacity reached
    data object AssignmentExists    // assignment rejected: student already has a lesson that day
    data object BusyConflict        // assignment rejected: hour overlaps a busy appointment
    data class Validation(message: String) // reuse for generic validation
}
```

---

## Repository contract (shape)

```kotlin
interface ScheduleRepository {
    fun observeHours(): Flow<Result<List<AvailableLessonHour>>>
    fun observeHoursForWeekday(weekday: DayOfWeek): Flow<Result<List<HourWithOccupancy>>>
    fun observeAssignments(): Flow<Result<List<LessonAssignment>>>
    fun observeBusyAppointments(): Flow<Result<List<BusyAppointment>>>
    fun observeStudentSchedule(studentId: Int): Flow<Result<StudentScheduleView>>

    suspend fun insertHour(weekday: DayOfWeek, startMinutes: Int, maxStudents: Int): Result<Int>
    suspend fun updateHour(id: Int, weekday: DayOfWeek, startMinutes: Int, maxStudents: Int): Result<AssignmentRemovalReport>
    suspend fun deleteHour(id: Int): Result<Unit>

    suspend fun assignStudent(hourId: Int, studentId: Int, weekday: DayOfWeek): Result<Unit>
    suspend fun unassignStudent(hourId: Int, studentId: Int): Result<Unit>

    suspend fun insertBusyAppointment(studentId: Int, weekday: DayOfWeek, startMinutes: Int, durationMinutes: Int): Result<AssignmentRemovalReport>
    suspend fun updateBusyAppointment(id: Int, startMinutes: Int, durationMinutes: Int): Result<AssignmentRemovalReport>
    suspend fun deleteBusyAppointment(id: Int): Result<Unit>
}
```

### `@Transaction` invariants

- **insertHour / updateHour**: reject if the new range
  `[start, start + 60)` overlaps any other hour on the same `weekday` → `HourOverlap`.
  On update, if `maxStudents` drops below current `assignedCount` → `Validation`.
  On update/delete, assignments whose students now have a busy overlap are removed
  (removals returned in the report).
- **assignStudent**: hour exists + not full (`HourFull`) + no existing assignment
  for `(studentId, weekday)` (`AssignmentExists`) + hour does not overlap any of
  the student's busy appointments on that weekday (`BusyConflict`).
- **insertBusyAppointment / updateBusyAppointment**: student exists, duration a
  multiple of 30 within `30..360`, `start + duration <= 1440` (range does not cross
  midnight); start is any exact minute. Overlapping
  assignments of that student on that weekday are removed (returned in the report).
  The new busy appointment may **overlap** other busy appointments of the same
  student (spec only constrains lessons).

### Eligibility for the "Add lesson" dialog (Student Detail Lessons tab)

Assignment is per student from the profile (FR-006). For the open student and the
selected weekday, each hour in `hoursForWeekday` is offered iff it has capacity
remaining — `remainingSlots > 0` (FR-007) — AND it does not overlap any of that
student's busy appointments on that weekday:
`overlaps(hourStart, hourStart + 60, busyStart, busyStart + busyDuration)` is
false (FR-009). A second lesson that day is prevented by hiding/blocking the
"Add lesson" action (FR-008/FR-019), so no second-lesson filtering is needed
inside the dialog. Computed in the `AddLessonDialog` composable from the
`observeHoursForWeekdayUseCase` occupancy + the student's busy list — no DAO
round-trip per row.

---

## Backup / restore export format (rev 3, FR-022…FR-033)

The backup file is a single versioned JSON object (kotlinx.serialization), defined
in `domain/models/ScheduleBackupData.kt`:

```kotlin
const val BACKUP_VERSION = 2

@Serializable
data class BackupExportData(
    val version: Int,                                   // = BACKUP_VERSION
    val students: List<StudentExportData>,
    val availableHours: List<AvailableHourExportData>,  // FULL plan (all weekdays)
)

@Serializable
data class AvailableHourExportData(val weekday: Int, val startMinutes: Int, val maxStudents: Int)

@Serializable
data class LessonAssignmentExportData(val weekday: Int, val startMinutes: Int)

@Serializable
data class BusyAppointmentExportData(val weekday: Int, val startMinutes: Int, val durationMinutes: Int)
```

- `StudentExportData` keeps `name` / `className` / `dates` and gains
  `lessonAssignments: List<LessonAssignmentExportData>` and
  `busyAppointments: List<BusyAppointmentExportData>`.
- Schedule rows are referenced by **natural key** (`weekday` as `DayOfWeek.isoDayNumber`
  + `startMinutes`) — ids are rebuilt on import. `weekday` is `1..7` (Mon=1 … Sun=7).
- `ExportStudentsUseCase` always writes the FULL `availableHours` plan regardless of
  which students are selected (FR-027); each **selected** student carries their own
  assignments + busy.
- `ParseImportFileUseCase` tries the object format first; on failure it falls back to
  the legacy flat array (`[{name, className, dates}]`, students + dates only, FR-023).
  A parsed `version != 2` → `StudentError.UnsupportedBackupVersion(version)` (FR-032);
  a blank file → empty `ParsedImportData`.
- `PerformImportUseCase` merges additively (FR-024/FR-025/FR-028/FR-029):
  - existing students keep their schedule; only non-conflicting imported entries are added;
  - imported hours overlapping an existing hour on the same weekday are skipped;
  - imported busy wins over an existing lesson (lesson removed), imported lessons never
    delete busy, busy-vs-busy overlaps are allowed;
  - imported lessons are planned against the **post-busy** hours; an hour's capacity is
    raised via `updateHour` to `max(current, currentAssigned + importedNewDistinct)` (FR-029);
  - orphaned/duplicate/busy-overlapping/invalid lessons are skipped and counted (FR-026/FR-030/FR-031).
- `ImportResult` reports schedule outcomes alongside student/date counts:
  `hoursAddedCount`, `hoursSkippedOverlapCount`, `lessonsAddedCount`,
  `lessonsRemovedBusyWinsCount`, `lessonsSkippedCount`, `malformedEntriesSkippedCount`
  (all default `0`) → localized in `ImportExportDelegate.buildImportResultMessage` (FR-033).

---

## File map (new)

| File | Purpose |
|---|---|
| `data/datasources/local/entities/AvailableLessonHourEntity.kt` | Room entity |
| `data/datasources/local/entities/LessonAssignmentEntity.kt` | Room entity |
| `data/datasources/local/entities/BusyAppointmentEntity.kt` | Room entity |
| `data/datasources/local/daos/ScheduleDao.kt` | queries + `@Transaction` orchestration |
| `data/datasources/local/StudentDatabase.kt` | version 3 + `MIGRATION_2_3` |
| `data/mappers/ScheduleMappers.kt` | entity ↔ domain |
| `data/repositories/ScheduleRepositoryImpl.kt` | repository impl |
| `domain/repositories/ScheduleRepository.kt` | interface |
| `domain/models/ScheduleModels.kt` | `AvailableLessonHour`, `LessonAssignment`, `BusyAppointment`, `AssignmentRemovalReport`, `HourWithOccupancy`, `StudentScheduleView` |
| `domain/services/ScheduleRules.kt` | constants + overlap/validity predicates |
| `domain/usecases/schedule/…` | single-action use cases + `ScheduleUseCases` group |
| `presentation/screens/schedule/**` | screen (see research.md Decision 9) |
| `presentation/utils/TimeFormatter.kt` | localized wall-clock formatting |

`StudentScheduleView(studentId, lessons: List<(weekday, startMinutes, endMinutes)>,
busy: List<BusyAppointment>)` — aggregate backing the interactive Lessons/Busy
tabs on Student Detail (FR-015); lessons and busy appointments are filtered per
selected weekday in the tabs.
