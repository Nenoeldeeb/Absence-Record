# Contract: Backup File Format

**Feature**: `specs/008-duration-based-scheduling`
**Status**: versioning contract for the app's only inter-device interface.
**Spec**: FR-042…FR-047, FR-040, FR-059 — [spec.md](../spec.md)

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

| Constant | Value |
|----------|-------|
| `MAX_IMPORT_STUDENTS` | 10,000 |
| `MAX_IMPORT_HOURS` | 1,008 |
| `MAX_IMPORT_ASSIGNMENTS` | 12,096 |

Exceeding a cap skips and counts the surplus and reports it in the summary. It is
never reported as a parse failure and never aborts the remaining entries
(FR-056…FR-059).