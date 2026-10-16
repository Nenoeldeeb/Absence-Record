# Quickstart — Validating 008 Duration-Based Scheduling

**Feature**: `specs/008-duration-based-scheduling`

A runnable guide to prove the feature works end-to-end. It lists **what to run
and what to observe** — the implementation itself belongs in `tasks.md`.

> **Prerequisite**: `cp signing.properties.example signing.properties` — the
> `app/build.gradle.kts` signing config reads this file and **no Gradle task can
> configure without it**.

---

## 1. Build and static gates

```bash
./gradlew ktlintFormat      # formatting; also runs as a preBuild dependency
./gradlew check             # ktlint + lint + unit tests
```

**Expected**: BUILD SUCCESSFUL. Any `MaxLineCount` failure here means a source
file passed 300 lines — the constitution's Principle V cap.

---

## 2. Pure scheduling rules — the fastest signal

The ordering algorithm is a pure function, so it needs no device and no mocks.

```bash
./gradlew testDebugUnitTest --tests "*LessonSlotPlannerTest"
```

**Expected**: green. The test class should cover, at minimum, the acceptance
scenarios named in the spec:

| # | Assert |
|---|-------|
| 1 | a 10-minute block yields `maxStudents == 1` |
| 2 | a 120-minute block yields `maxStudents == 12` |
| 3 | block 10:00–10:10, commitment at 10:30 → **accepted** (FR-025) |
| 4 | commitments 10:30 / 10:40 / 11:00 → slots 10:00, 10:10, 10:20 |
| 5 | three students tied at 10:30 → alphabetical, then by id |
| 6 | commitment starting exactly at slot **end** → accepted (half-open) |
| 7 | commitment starting exactly at slot **start** → rejected |
| 8 | commitment 09:50–10:20 vs block 10:00 → rejected |
| 9 | adding to a full block → refused, roster unchanged |
| 10 | 13 students into a 120-minute block → first 12 kept by order |

---

## 3. Rules, capacity and migration

```bash
./gradlew testDebugUnitTest --tests "*ScheduleRulesTest"
./gradlew testDebugUnitTest --tests "*ScheduleViewModelCapacityTest"
```

**Expected**: green, and `ScheduleViewModelCapacityTest` renamed or rewritten to
assert derived capacity — it currently asserts the removed `MaxBelowAssigned`
behaviour.

---

## 4. Backup format compatibility

```bash
./gradlew testDebugUnitTest --tests "*ParseImportFileUseCaseTest"
./gradlew testDebugUnitTest --tests "*ExportStudentsUseCaseScheduleTest"
./gradlew testDebugUnitTest --tests "*PerformImportHoursMergeTest"
```

**Expected**: green. `ParseImportFileUseCaseTest` currently asserts
`assertEquals(2, BACKUP_VERSION)` at line 184 — that becomes `3`, and the class
gains v2-input and round-trip cases.

---

## 5. Database migration 3 → 4 — requires a device

```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class="*ScheduleDaoMigrationTest"
```

> AGP 9 rejects `--tests` on `connectedDebugAndroidTest`. Use the
> `-Pandroid.testInstrumentationRunnerArguments.class=` form only.

**Expected**: `migrate3To4_preservesAssignmentsAndConvertsCapacity` green —
seed an hour with `maxStudents = 6`, migrate, assert `durationMinutes = 60` and
that the assignment survived. A second case seeds `maxStudents = 99` and asserts
the clamp to `120`.

**Schema files**: exactly one `4.json`, at
`app/schemas/dev.nenoeldeeb.education.absencerecord.data.datasources.local.AppDatabase/4.json`.
If a `4.json` appears in the orphaned sibling directory instead, the cleanup
described in [research.md](./research.md) D3 was not done.

---

## 6. UI — the slider and the slot display

```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class="*HourDialogTest"
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class="*ScheduleScreenRefinementTest"
```

**Expected**: green. The old `hour_capacity_increase` / `hour_capacity_decrease`
test tags no longer exist — tests reference the slider instead.

**Manual check** (Principle VI):

1. Open a lesson hour → the length slider spans 10…120, stepped by 10.
2. Drag it → the student count beside it updates live; it never lands on a
   non-multiple of 10.
3. Turn on TalkBack, swipe to the slider → the label announces **both** the
   minutes and the student count, and arrow keys move it in 10-minute steps.
4. Assign three students with commitments at 10:30 / 10:40 / 11:00 to a 10:00
   block → slots read 10:00–10:10, 10:10–10:20, 10:20–10:30.
5. Drag the slider to shrink the block below the number assigned → a confirm
   dialog names who would be dropped; **declining changes nothing**.

---

## 7. Backward compatibility walkthrough

1. Install the previous release, create a block and assign two students.
2. Upgrade to this build and open the same day.
3. The block appears with a valid length; both students remain assigned.
4. Export → inspect the JSON: `"version": 3` and `"durationMinutes"` present,
   no `maxStudents` anywhere.
5. Import the **previous** version's exported file → it loads, converting
   capacity to duration.
6. Import a file with `"version": 99` → refused with a message, nothing written.

---

## Definition of done

- [ ] `./gradlew check` passes
- [ ] `LessonSlotPlannerTest` covers all 10 rows in §2
- [ ] `ScheduleDaoMigrationTest` covers 3→4 including the clamp case
- [ ] Exactly one `4.json`, in the correct schema directory
- [ ] No `[NEEDS CLARIFICATION]` in plan.md, research.md, or tasks.md
- [ ] Every source file ≤ 300 lines
- [ ] `strings.xml` and `values-ar/strings.xml` both updated; no hardcoded strings
- [ ] `AGENTS.md` and `README.md` updated (constitution Governance requirement)