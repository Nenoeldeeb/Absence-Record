# Research & Design Decisions — 008 Duration-Based Scheduling

**Feature**: `specs/008-duration-based-scheduling`
**Date**: 2026-10-13
**Spec**: [spec.md](./spec.md) — FR-001…FR-062, SC-001…SC-019

Every `NEEDS CLARIFICATION` in the Technical Context is resolved below. No
remaining unknowns block planning.

---

## D1 — Where the ordering algorithm lives

**Decision**: A new pure-Kotlin file, `domain/services/LessonSlotPlanner.kt`,
exposing a single object with a pure function that maps
`(blockStart, blockDuration, List<StudentWithCommitments>)` to
`List<PlannedSlot>` plus a list of conflicts.

**Rationale**: The ordering rules (FR-019…FR-025) are the heart of this feature
and have no Android, Room, or Compose dependency. Putting them in `domain/services/`
satisfies Principle I by construction — the planner cannot reach persistence even
by accident. It is trivially unit-testable with no mocks, which matters because
this is the logic with the most edge cases (half-open intervals, tie-breaks,
cross-midnight starts).

**Alternatives considered**:
- *In the DAO (`ScheduleDaoHelpers.kt`)* — rejected. Business rules in the data
  layer invert Principle I and would make the rules untestable without Room.
- *In a ViewModel delegate* — rejected. The same rules must be enforced on the
  Student Detail "Add lesson" path too; a ViewModel would duplicate them.
- *In a use case* — rejected. It is not a single action, it is a pure function.

---

## D2 — How capacity is represented

**Decision**: `maxStudents: Int` is replaced by `durationMinutes: Int` on the
entity, mapper, domain model, repository interface and backup. Capacity becomes a
**computed** property (`durationMinutes / 10`) exposed on the domain model, never
a stored or independently editable field.

**Rationale**: FR-004 demands a single source of truth. A computed property
makes "two independently editable values" structurally impossible rather than
merely discouraged. The derivation is one integer division, so there is no
reason to persist it.

**Alternatives considered**:
- *Keep both columns and sync them* — rejected. Exactly the failure mode FR-004
  prohibits; guarantees drift.
- *Store capacity only, derive duration* — rejected. FR-002/FR-035 require the
  exported and stored unit to be duration; capacity is not selectable by the user.

---

## D3 — Schema migration 3 → 4

**Decision**: `MIGRATION_3_4` performs, in order:
1. `ALTER TABLE available_lesson_hours RENAME COLUMN maxStudents TO durationMinutes`
2. `UPDATE available_lesson_hours SET durationMinutes = MIN(120, MAX(10, durationMinutes * 10))`

then registers in the `addMigrations(...)` chain and bumps `version = 4`.

**Rationale**: SQLite's `RENAME COLUMN` preserves the stored values; the
following `UPDATE` converts each legacy capacity to a duration and clamps into
the 10–120 selectable range required by FR-041. Doing it as two statements
avoids a table rebuild, which would be the only way to avoid the clamp and would
put the FK targets from `lesson_assignments` at risk. `MIN`/`MAX` are scalar
functions available in every SQLite build Room supports.

**Alternatives considered**:
- *Table rebuild (create/copy/drop/rename)* — rejected. Much larger, and the
  `lesson_assignments` foreign key references `available_lesson_hours(id)`.
- *Migrate to a raw value and clamp on read* — rejected. FR-041 requires the
  stored value itself to be in range; clamping only at read time would let a
  legacy 99-student block render as 990 minutes and blow past midnight.

**Housekeeping required**: `app/schemas/dev.nenoeldeeb.education.absencerecord.AppDatabase/`
is an orphaned directory left over from a different database. It contains a
`4.json` whose entities are `notes` and `notes_with_dates` — nothing to do with
this app. It is tracked in git (added in commit `90b7389`). The live directory is
`dev.nenoeldeeb.education.absencerecord.data.datasources.local.AppDatabase/`.
The orphan must be deleted and the real `4.json` committed there, or a reader
will pick up the wrong schema for version 4.

---

## D4 — Backup format versions

**Decision**: `BACKUP_VERSION` becomes `3`. `AvailableHourExportData.maxStudents`
is replaced by `durationMinutes`. `ParseImportFileUseCase` branches on version:
`3` → read `durationMinutes` directly; `2` → read `maxStudents`, multiply by 10,
clamp; anything else → `UnsupportedBackupVersion`, as today.

**Rationale**: FR-043 requires the change to be signalled twice over, and FR-046
requires refusing to guess when both fields are present. A discriminated read on
the declared version gives both. The legacy flat-array fallback (a bare JSON
array of students, pre-`version`) must keep working and stays in the failure
branch, exactly as it does today.

**Alternatives considered**:
- *Same version, rename field only* — rejected. FR-043 requires two signals, and a
  reader that trusts only the field name could misread a partially-edited file.
- *Same version and field, reinterpret the number* — rejected. A capacity of `6`
  and a duration of `6` are indistinguishable; this would silently corrupt data.

---

## D5 — The confirm-first re-plan is two-phase

**Decision**: `updateHour` stops throwing `StudentError.MaxBelowAssigned`. It
returns a new domain type instead:

```kotlin
sealed interface HourReplanResult {
    data class Applied(val removedStudentIds: List<Int>) : HourReplanResult
    data class NeedsConfirmation(
        val keptStudentIds: List<Int>,
        val droppedStudents: List<DroppedStudent>
    ) : HourReplanResult
}
```

The first call evaluates and returns `NeedsConfirmation` **without writing**; the
UI shows the keep/drop list; a second call with `confirmed = true` commits.

**Rationale**: FR-011 and FR-013 require the teacher to see the consequences and
confirm, and require all-or-nothing application. A single call cannot both
preview and commit without risking a write before consent. Returning a result
type (rather than throwing) keeps it inside the `Result<T>` convention of
Principle I and gives the UI something to render.

**Alternatives considered**:
- *Throw a structured error carrying the plan* — rejected. Errors here are an
  expected outcome, not a failure; throwing would make every caller write a
  `try`/`catch` and would blur FR-013's "applied" case.
- *Let the ViewModel compute the plan, then call update* — rejected. It would put
  the ordering rules in the presentation layer and duplicate them.

---

## D6 — The DAO transaction bodies must shrink, not grow

**Decision**: `ScheduleDao` (currently 245 lines) does **not** receive the planner
inline. `@Transaction` methods keep orchestrating only: read inputs, call the
domain planner, act on its verdict, write. The per-slot overlap checks move into a
new `domain/services/LessonSlotPlanner` companion, and the SQL-facing helpers stay
in `ScheduleDaoHelpers.kt` (105 lines).

**Rationale**: Principle V caps files at 300 lines. `ScheduleDao` must gain the
two-phase update, the narrowed busy check and the per-slot validation. Inlining
the planner would blow the cap. Keeping orchestration in the DAO and rules in the
domain keeps both files cohesive.

---

## D7 — Narrowing the busy check (the behavioural core)

**Decision**: Replace `hasBusyConflict(studentId, weekday, hour.startMinutes)`
(which tests the whole hour extent) with a check of **only the slot the student
would occupy**, computed from their position in the derived order.

**Rationale**: This is FR-025's entire purpose — a student with a commitment at
10:30 must be assignable to a block running 10:00–11:00 if their own slot is
10:00–10:10. The existing whole-extent check rejects them and is the single most
important defect this feature fixes. `ScheduleDaoHelpers.kt` already holds the
overlap helper; the change is to pass `[slotStart, slotEnd)` instead of
`[hourStart, hourEnd)`.

**Boundary rule** (FR-030): ranges are half-open, so `slotEnd == busyStart` is
**not** a conflict and `slotStart == busyStart` **is**. The existing
`overlaps(aStart, aEnd, bStart, bEnd) = aStart < bEnd && aEnd > bStart` already
implements this exactly — it needs no change, only correct arguments.

---

## D8 — Derivation runs off the main thread, uncached

**Decision**: `ScheduleRepositoryImpl` wraps the derivation in
`withContext(Dispatchers.Default)`. No cache is introduced (FR-060).

**Rationale**: FR-061 and Principle V. FR-060 rules out caching because the order
legitimately changes whenever any of a student's commitments changes, so a cache
needs invalidation on every commitment write; a missed invalidation would display
a plan the rules would never produce. The work is bounded — at most 12 students
per block, and a day's blocks cannot exceed what fits in 24 hours — so recompute-
always is affordable.

---

## D9 — Record caps and the file-size guard (FR-056…FR-059)

**Decision**: Two independent layers.

*Record caps* (bound what reaches the database), in the domain next to the merge
logic:

| Constant | Value | Basis |
|---|---|---|
| `MAX_IMPORT_STUDENTS` | 10,000 | Judgement — far beyond one teacher's classes (FR-058) |
| `MAX_IMPORT_HOURS` | 1,008 | **Derived**: 7 days x 1440 min / 10 min = 1,008 |
| `MAX_IMPORT_ASSIGNMENTS` | 12,096 | **Derived**: 1,008 x 12 (max capacity) |
| `MAX_IMPORT_ATTENDANCE_DATES_PER_STUDENT` | 3,650 | **Derived**: 10 years of daily attendance |

*File-size guard* (bounds what is parsed), checked **before** decoding:

| Constant | Value | Basis |
|---|---|---|
| `MAX_IMPORT_FILE_BYTES` | 32 MB | 4.6x the realistic worst case (see below) |

**Why the record caps alone are not enough**: the import path reads the whole
file into a `String` and materialises every record before any merge-time cap can
apply. A cap applied *after* `decodeFromString` bounds the **database write**,
not the **parse** — an `OutOfMemoryError` occurs while decoding, long before
`MAX_IMPORT_STUDENTS` is consulted.

**Why attendance dates had to be capped**: `StudentExportData.dates` is a
`List<String>` of unbounded length and is the dominant term in memory. Measured
peak heap for the read path (raw bytes + UTF-16 `String` + decoded objects, ~3x
file size):

| File | Size | Peak heap |
|---|---|---|
| 1,500 students x 365 dates | 6.9 MB | ~21 MB |
| 10,000 students x 180 dates | 26 MB | ~78 MB |
| 10,000 students x 1,000 dates | 143 MB | **~430 MB** |

Lowering the student cap does **not** bound memory: at a 1,000-student cap an
oversized file simply sends more dates per student instead. The date count is the
binding constraint, so it is capped in its own right.

**How the guard is enforced**: `StorageRepository.readTextFromUri` reads at most
`MAX_IMPORT_FILE_BYTES + 1` bytes and fails with `StudentError.ImportFileTooLarge`
if it receives the extra byte. The file is never fully materialised, so a
hostile 2 GB file costs 32 MB, not 2 GB.

**Why 32 MB**: the realistic worst case for a single teacher's own device — a
whole secondary school with a full year of daily attendance — is ~7 MB. The cap
gives ~4.6x headroom while capping peak heap near ~96 MB, which is survivable on
a mid-range device. The student cap of 10,000 is retained per FR-058 and is
documented as the judgement-based figure it is.

---

## D10 — Accessibility of the length slider

**Decision**: One continuous slider, 10→120, step 10, with the derived student
count rendered beside it. Its spoken label announces **both** the minutes and the
resulting count, and it is operable by switch access and keyboard at the same
granularity.

**Rationale**: FR-048…FR-051. A slider with only a numeric label leaves a
screen-reader user unable to tell what the number means; announcing the derived
capacity is what makes the control self-explanatory without sight.

**Removed strings**: `error_capacity_invalid`, `schedule_decrease_capacity`,
`schedule_increase_capacity`, `schedule_max_students` become unused and are
deleted rather than left as dead resources.

---

## Resolved unknowns from Technical Context

| Unknown | Resolution |
|---------|------------|
| Backup dual-version acceptance — does it already work? | **No.** `ParseImportFileUseCase` uses strict equality `version != BACKUP_VERSION`. Dual-version read is new work (D4). |
| Does a slot-position column need to be added? | **No.** FR-025 stores no position; the schema change is limited to the capacity→duration swap (D2, D3). |
| Which schema directory does `4.json` belong in? | The `data.datasources.local.AppDatabase` one. The sibling directory is orphaned (D3). |
| Where does the `MaxBelowAssigned` error go? | Replaced by the two-phase `HourReplanResult` (D5). |
| Does `assignStudent` need to revalidate existing students? | Yes — inserting someone earlier pushes later students toward their own commitments and can create a conflict (FR-033). Handled by the single whole-roster plan. |
---

## D11 — Historical schema JSONs must not be regenerated (found on device)

**Decision**: `app/schemas/.../3.json` records the *pre-migration* shape and MUST
keep `maxStudents`. Only `4.json` is new. If `3.json` is regenerated from the
current entity it will claim `durationMinutes`, and two things break at once:

- `MigrationTestHelper.createDatabase(name, 3)` builds the starting database from
  that JSON, so a 3→4 test cannot even insert legacy data.
- `MIGRATION_2_3` then validates against a v3 schema it does not produce, breaking
  the pre-existing `migrate2To3` test.

**Why it matters**: both failures are silent in the sense that the build still
succeeds; only the device test reveals them. The migration is the only
irreversible change in this feature, so "it compiles" is not evidence it works.

**Verified**: `./gradlew clean && :app:assembleDebug` leaves `3.json` with
`maxStudents` — the corruption came from the one build that introduced the
version bump, not from every build. If a future build clobbers it, re-commit the
historical file rather than regenerating it.
