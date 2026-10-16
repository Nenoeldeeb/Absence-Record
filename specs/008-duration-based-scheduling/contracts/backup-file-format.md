# Contract: Backup File Format

**Feature**: `specs/008-duration-based-scheduling`
**Status**: versioning contract for the app's only inter-device interface.
**Spec**: FR-040, FR-042…FR-048, FR-056…FR-059, FR-062, FR-063 — [spec.md](../spec.md)

The backup JSON is the sole transfer mechanism in this offline app, so its
compatibility rules are a hard contract rather than an implementation detail.

---

## Version 3 (written by this version of the app)

```json
{
  "version": 3,
  "students": [
    {
      "name": "Layla",
      "classId": 1,
      "dates": ["2026-09-01", "2026-09-02"],
      "lessonAssignments": [ { "weekday": 6, "startMinutes": 600 } ],
      "busyAppointments": [
        { "weekday": 6, "startMinutes": 630, "durationMinutes": 60 }
      ]
    }
  ],
  "availableHours": [
    { "weekday": 6, "startMinutes": 600, "durationMinutes": 60 }
  ]
}
```

### Field rules

| Field | Type | Rule |
|-------|------|------|
| `version` | int | MUST be `3` |
| `students[].name` | string | required |
| `students[].classId` | int / null | optional |
| `students[].dates` | string[] | `YYYY-MM-DD` |
| `students[].lessonAssignments[]` | object | natural key `(weekday, startMinutes)`; ids are rebuilt on import |
| `students[].busyAppointments[]` | object | `durationMinutes` 30…360, step 30 |
| `availableHours[].weekday` | int | 1…7 |
| `availableHours[].startMinutes` | int | 0…1440 |
| `availableHours[].durationMinutes` | int | **10…120, multiple of 10** |

**No `maxStudents` field may appear anywhere in a v3 file** (FR-042).

---

## Version 2 (written by the previous version — still accepted)

Identical in every respect except one field name:

| Version 2 | Version 3 |
|-----------|-----------|
| `availableHours[].maxStudents` | `availableHours[].durationMinutes` |

On import, `durationMinutes = min(120, max(10, maxStudents * 10))`.

`lessonAssignments[].startMinutes` refers to the block's **start**, unchanged
between versions — it is not an offset and is not rescaled.

---

## Version 1 / legacy flat array (still accepted)

A bare JSON array of student objects, with no `version` field and no schedule:

```json
[ { "name": "Layla", "dates": ["2026-09-01"] } ]
```

Restores students and dates only. No blocks, no lessons, no appointments. This
is tried when the v3 envelope fails to decode.

---

## Compatibility matrix

| Declared version | Shape | Outcome |
|------------------|-------|---------|
| `3` | v3 envelope, `durationMinutes` | full restore |
| `2` | v2 envelope, `maxStudents` | full restore, capacity → duration |
| `1` / absent, bare array | legacy array | students + dates only |
| `4` or any future | unknown | **refused**, `UnsupportedBackupVersion`, nothing written |
| any, entry has **both** fields | ambiguous | that entry skipped + counted, import continues |
| any, entry has **neither** field | malformed | that entry skipped + counted, import continues |
| any, `durationMinutes` outside 10…120 or not a multiple of 10 | out of range | that entry skipped + counted |

**Refusal is total.** When the version is unrecognised, nothing is written — not
students, not blocks, not a partial schedule (FR-047).

**Skipping is partial and reported.** An individual bad entry never aborts the
import; the count appears in the post-import summary alongside the existing
malformed/duplicate/orphan counts (FR-048, FR-059).

---

## Round-trip guarantee

Export → import → export MUST be lossless for every block length (SC-008):
a block saved at 90 minutes returns at 90 minutes.

---

## Import caps

| Constant | Value | Basis |
|----------|-------|-------|
| `MAX_IMPORT_FILE_BYTES` | 33,554,432 (32 MB) | **Pre-decode guard.** Read first, before the file is decoded (FR-063) |
| `MAX_IMPORT_STUDENTS` | 10,000 | The one judgement-based figure (FR-058, FR-062) |
| `MAX_IMPORT_HOURS` | 1,008 | 7 × 1440 ÷ 10 — every non-overlapping ten-minute block in a week (FR-057) |
| `MAX_IMPORT_ASSIGNMENTS` | 12,096 | 1,008 × 12 — every block at its maximum capacity (FR-057) |
| `MAX_IMPORT_ATTENDANCE_DATES_PER_STUDENT` | 3,650 | Ten years of daily attendance (FR-056, FR-058) |

The two layers behave differently on breach, and the difference matters.

**Record caps** — `MAX_IMPORT_STUDENTS`, `MAX_IMPORT_HOURS`,
`MAX_IMPORT_ASSIGNMENTS`, `MAX_IMPORT_ATTENDANCE_DATES_PER_STUDENT`. Exceeding
one skips and counts the surplus and reports it in the summary. It is never
reported as a parse failure and never aborts the remaining entries (FR-059).

**The file-size guard** — `MAX_IMPORT_FILE_BYTES`. This is the exception: it
refuses the **whole file** and writes nothing, because by the time it is
consulted there is no meaningful "the rest" left to import. It surfaces as
`ImportFileTooLarge`, distinct from `ImportParse` — an oversize file is not
malformed, merely refused.

### Why there are two layers

The record caps and the size guard stop different things, and neither substitutes
for the other.

**The size guard runs first, before decoding.** It is the only layer that
actually prevents an out-of-memory failure: a record cap applied *after* decode
bounds what reaches the database but not what was parsed, and by the time the cap
is consulted the memory has already been spent. Reading is chunked and gives up
the moment the cumulative limit is crossed, so an implausibly large file is never
materialised at all.

**The record caps run after decoding**, bounding the write. Attendance dates are
capped per student rather than globally, because the date list is what dominates
import memory: a small file of few students with deep history costs more than a
large file of shallow ones.

`MAX_IMPORT_STUDENTS` is the single judgement-based number in this feature; every
other cap is derived arithmetically. It is set far above any real class size so
that no legitimate backup can reach it, and hitting it is stated explicitly in
the import summary rather than passing unnoticed.