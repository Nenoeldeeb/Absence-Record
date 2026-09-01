# Contracts — Schedule Repository & Business Rules

**Feature Branch**: `007-lesson-scheduling` | **Date**: 2026-08-08

## Data-layer contract

All repository methods return `Result<T>`. Errors never escape to
domain/presentation as exceptions; they surface as typed `StudentError` subtypes
via `onSuccess` / `onFailure`.

### Flows (auto-refresh FR-015)

| Method | Emits on |
|---|---|
| `observeHours()` | any `available_lesson_hours` change |
| `observeHoursForWeekday(w)` | any `available_lesson_hours` / `lesson_assignments` change (occupancy joins) |
| `observeAssignments()` | any `lesson_assignments` change |
| `observeBusyAppointments()` | any `busy_appointments` change |
| `observeStudentSchedule(studentId)` | any schedule change affecting that student |

### Mutations (all `@Transaction`)

| Method | Failure → error | Side effect |
|---|---|---|
| `insertHour(w, start, max)` | `HourOverlap` if `[start, start+60)` overlaps another hour on `w`; `Validation` if `max <= 0` or `start % 30 != 0` | — |
| `updateHour(id, w, start, max)` | `HourOverlap` (excl. self); `Validation` if `max < assignedCount` | removes assignments whose students now busy-overlap; returns `AssignmentRemovalReport` |
| `deleteHour(id)` | `Validation` if not found | cascade deletes its assignments (FK) |
| `assignStudent(hourId, studentId, w)` | `HourFull`; `AssignmentExists`; `BusyConflict`; `Validation` if hour/student missing | inserts assignment |
| `unassignStudent(hourId, studentId)` | `Validation` if not found | deletes assignment |
| `insertBusyAppointment(studentId, w, start, dur)` | `Validation` if start `% 30 != 0`, `dur < 15`, or `start + dur > 1440`; missing student | removes that student's overlapping assignments on `w`; returns report |
| `updateBusyAppointment(id, start, dur)` | same `Validation` rules | same auto-removal; returns report |
| `deleteBusyAppointment(id)` | `Validation` if not found | — |

### Overlap predicate (single source of truth)

`overlaps(aStart, aEnd, bStart, bEnd) = aStart < bEnd && aEnd > bStart`

Rule mapping (spec):
- Lesson vs lesson: `[s, s+60)` may not overlap another hour on the same weekday.
- Lesson vs busy: a lesson at `s` is **blocked** iff `overlaps(s, s+60, busyStart, busyEnd)`.
  Exact boundary: busy ending exactly at `s` or starting exactly at `s+60` does **not** block.
- Busy vs busy: allowed to overlap (spec only constrains lessons).

### DB invariants

- `(studentId, weekday)` unique in `lesson_assignments` (DB backstop for FR-008).
- FK `CASCADE` everywhere a schedule row references a student/hour (FR-013/14).
- Hour start ∈ `0..1380`, busy start ∈ `0..1410`, busy `duration ∈ [15, 1440-start]`.

## Use cases (each a single-action `operator fun invoke` in `domain/usecases/schedule/`)

- `ObserveHoursForWeekdayUseCase(w): Flow<Result<List<HourWithOccupancy>>>`
- `ObserveBusyAppointmentsForWeekdayUseCase(w): Flow<Result<List<BusyAppointment>>>`
- `ObserveStudentScheduleUseCase(studentId): Flow<Result<StudentScheduleView>>`
- `InsertHourUseCase`, `UpdateHourUseCase`, `DeleteHourUseCase`
- `AssignStudentUseCase`, `UnassignStudentUseCase`
- `InsertBusyAppointmentUseCase`, `UpdateBusyAppointmentUseCase`, `DeleteBusyAppointmentUseCase`

Grouped into `data class ScheduleUseCases(...)`, provided by `AppContainer`.

## Backup/restore contract (rev 3, domain/usecases/transfer/)

Reuses the existing Students-screen import/export flow (same `StorageRepository`,
same per-student selection on import). Schedule data is carried in the versioned
object format described in `data-model.md`.

| Use case | Contract |
|---|---|
| `ExportStudentsUseCase(uri, selectedIds, students)` | writes `BackupExportData{version=2, students, availableHours}`; `availableHours` is the FULL plan (all weekdays, FR-027) via `observeHours()`; each selected student carries `lessonAssignments` (via `observeAssignments()`) and `busyAppointments` (via `observeBusyAppointments()`); natural-key (`weekday` + `startMinutes`) references, ids rebuilt on import |
| `ParseImportFileUseCase(uri)` | object-first decode of `BackupExportData`, fallback to legacy flat array (FR-023); `version != 2` → `StudentError.UnsupportedBackupVersion(version)` (FR-032); blank file → empty `ParsedImportData` |
| `PerformImportUseCase(data, selectionMap)` | additive merge (FR-024/FR-025): existing students/hours kept; imported hours overlapping an existing hour on the same weekday skipped (FR-025); busy imported first — wins over existing lessons (FR-028), busy-vs-busy overlaps allowed; imported lessons planned against post-busy hours, raising existing-hour capacity to fit (FR-029); orphaned (absent/skipped hour, FR-026), duplicate same-weekday lessons (FR-030), and malformed entries (FR-031: busy duration outside 15–240, cross-midnight, off-grid start, non-positive capacity, unknown weekday) skipped individually and counted |

`ImportResult` extends the student/date counts with `hoursAddedCount`,
`hoursSkippedOverlapCount`, `lessonsAddedCount`, `lessonsRemovedBusyWinsCount`,
`lessonsSkippedCount`, `malformedEntriesSkippedCount` (default `0`), reported by
`ImportExportDelegate.buildImportResultMessage` as `UiText` plurals (FR-033).

## Error → UI mapping (`StudentErrorUiMapper` extension)

`HourOverlap` → "This hour overlaps an existing appointment." (string res)
`HourFull` → "This hour is already full."
`AssignmentExists` → "This student already has a lesson that day."
`BusyConflict` → "This hour overlaps a busy appointment of this student."
`UnsupportedBackupVersion(version)` → "This backup file is not supported." (string res, FR-032)
`Validation(msg)` → `msg` (pre-localized `UiText`).

Removal notifications: `AssignmentRemovalReport.removedStudentIds` → names looked up
in VM state → pluralized `UiText` toast, e.g. "Removed from 9:00: Ali, Sara".

> UI-placement note (2026-08-09 spec revision): the repository contract above is
> UNCHANGED — busy insert/update still auto-remove overlapping assignments and
> return `AssignmentRemovalReport` at the data layer. Only the UI flow changed:
> the Busy dialog now PREVIEWS the conflicting lessons and the teacher confirms
> BEFORE `insertBusyAppointmentUseCase` / `updateBusyAppointmentUseCase` is
> called (FR-011), so no removal is reported after the fact for busy. Hour-edit
> removals (FR-017) still surface as the toast above.
