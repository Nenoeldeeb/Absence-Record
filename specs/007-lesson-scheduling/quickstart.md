# Quickstart — Validation Scenarios

**Feature Branch**: `007-lesson-scheduling` | **Date**: 2026-08-08

How to validate the feature against the user stories after implementation.
Recommended device: Android phone, `values-ar` locale exercised via emulator
locale switch; TalkBack run-through on the Schedule page.

---

## Scenario 1 — Create a weekly lesson hour (US 1, FR-001…004)

1. Open app → swipe right from Calendar (page 1 → page 2 = Schedule). Confirm the
   pager now shows **3 pages** and Schedule sits next to the attendance report.
2. Pick **Sunday** on the weekday selector (the Schedule page is availability-only —
   no tabs).
3. Add hour → tap the start-time field → pick **9:00** in the Material3 time
   picker (exact-minute granularity; e.g. 9:07 is legal), **max students = 3** →
   save. The dialog previews the fixed 60-minute "Ends at" (9:00 → 10:00).
4. Expect: row "9:00 – 10:00, 0 / 3".
5. Add another hour at 9:30 on Sunday → expect **"overlaps an existing hour"**
   error, hour NOT saved.
6. Add 11:00 max 2 → save. Switch to another weekday → list is empty (per-weekday).

## Scenario 2 — Assign lessons per student from the profile (US 2, FR-006/007/008/009)

1. Open student **Sara** → detail screen → schedule section → **Lessons** tab,
   Sunday selected.
2. With Sunday 9:00 (max 3) present: tap **Add lesson** → dialog lists eligible
   hours with occupancy ("0 / 3" for 9:00). Select 9:00 → save.
3. Expect: Lessons tab now lists "9:00 – 10:00" with an unassign action, and
   **Add lesson is no longer available** for Sunday (FR-008/FR-019).
4. Repeat for **Omar** (Sunday, 9:00) → on the Schedule page the 9:00 row shows
   "2 / 3"; with a 3rd eligible student it shows "3 / 3" and **Full**.
5. For a student whose hour is full, the Add lesson dialog shows 9:00 **disabled
   ("hour is full")** (FR-007).
6. For a student with a busy appointment overlapping 9:00 (e.g. Ali busy
   9:30–10:30), the Add lesson dialog shows 9:00 **disabled ("overlaps busy
   appointment")** (FR-009).
7. On a weekday with no hours, Add lesson → dialog shows the **"no hours for this
   day"** hint pointing to the Schedule page (FR-018).

## Scenario 3 — Busy appointment preview & confirm (US 3, FR-010/011/012)

1. Sunday 9:00 has **Sara** assigned (from scenario 2).
2. Student **Sara** → detail → **Busy** tab → Sunday → add a busy appointment
   at 8:30 (Material3 time picker) with a 60-min duration via the 0.5h–6h slider
   in 30-minute steps (8:30–9:30, overlaps the 9:00 lesson).
3. Expect: the dialog **previews** the conflicting lesson "9:00 – 10:00" and the
   confirm action reads as a conflict confirmation — nothing is removed until the
   teacher confirms (FR-011).
4. Confirm → the lesson is removed: Lessons tab empty for Sunday, Schedule page
   9:00 occupancy back to "0 / 3".
5. Delete the busy appointment (or edit it to 9:30+) → the previously blocked
   hour becomes assignable again (FR-012).

## Scenario 4 — Edit/delete & cascades (FR-004/013/014)

1. Edit 9:00 hour → lower max below assigned count → inline error, not saved.
2. Delete 9:00 hour → confirm dialog → row gone **and** its assignments gone.
3. Delete a student (Students screen) → their assignments and busy appointments
   disappear (FK cascade).

## Scenario 5 — Student profile interactive schedule (US 2/3/4, FR-015/018/019)

1. Tap a student in Students list → detail screen → **schedule section** with a
   Saturday-first weekday selector and **Lessons / Busy** tabs.
2. Lessons tab: lists the student's lessons for the selected weekday (each with
   unassign); "Add lesson" opens the per-weekday dialog; on a weekday that
   already has a lesson the action is unavailable (FR-019).
3. Busy tab: add/edit/delete the student's busy appointments for the selected
   weekday, with preview-and-confirm on lesson conflicts (FR-011).
4. Make a change and return to the Schedule page → the hour occupancy reflects
   it immediately (flow auto-refresh).

## Scenario 6 — Localization & accessibility

1. Switch device language to **Arabic** → weekday chips, tabs, dialogs, and time
   text ("9:00 ص") render in Arabic; no layout breakage.
2. TalkBack: weekday chips, tabs, occupancy ("2 of 3"), and every icon button are
   announced; dialog focus is trapped.

## Scenario 7 — Backup/restore covers schedule data (US 4, FR-022…FR-033)

1. Set up: Sunday 9:00 max 3 with **Sara** + **Omar** assigned; Sara busy
   8:30–9:00; a busy overlapping a lesson (FR-028) and a malformed entry are
   optional extras.
2. **Export** (Students screen → Export): open the resulting file → confirm it is
   `{ "version": 2, "students": [...], "availableHours": [...] }` with the FULL
   plan (every weekday/hour) regardless of which students were selected (FR-027),
   and Sara's `lessonAssignments` + `busyAppointments` beside `name`/`dates`.
3. **Restore onto an empty app** (reinstall/clear data → Import): students, dates,
   the whole availability plan, and every lesson/busy appointment come back —
   the Schedule page occupancy matches exactly (round trip).
4. **Additive merge** (import onto data that already exists): existing hours and
   lessons are kept; imported hours overlapping an existing hour are skipped;
   an imported lesson duplicating Sara's existing Sunday lesson is skipped; an
   imported lesson onto a full existing hour is accepted and the hour's capacity
   is raised (FR-029); an imported busy over an existing lesson removes the lesson
   and the import message reports "lessons removed (busy wins)" (FR-028/FR-033).
5. **Import a legacy flat-array file** → students + dates restored, no schedule
   data, nothing reported missing (FR-023).
6. **Import a file with `version: 3`** → rejected with the "unsupported backup"
   error (FR-032); a blank file imports nothing.
7. Confirm the post-import message reports hours added, hours skipped (overlap),
   lessons added, lessons removed (busy wins), and lessons skipped — in English
   and Arabic (FR-033).

## Automated coverage map

| Layer | Coverage |
|---|---|
| `ScheduleRulesTest` | validity/boundary/overlap matrix incl. exact-boundary (busy ends exactly at 9:00 ⇒ 9:00 assignable), 30–360-minute / 30-step duration bounds, and exact-minute starts |
| `ScheduleRepositoryImplTest` | success + every `StudentError` subtype |
| Use-case tests | capacity, one-per-day, busy conflict, removal payloads, cascade |
| `ScheduleViewModelHourTest` | weekday switching, hour dialog flows, expanded-hour state, toasts, error bindings |
| `ScheduleDaoTest` (androidTest) | `@Transaction` invariants, unique index, FK cascade |
| `ScheduleScreenAppointmentsTest` (androidTest) | add/edit/delete hour, overlap error, expandable rows + read-only names, a11y descriptions |
| `StudentDetailViewModelScheduleTest` | schedule load, Lessons/Busy tab state, add-lesson eligibility, unassign, busy preview-and-confirm (both paths) |
| `StudentScheduleSectionTest` (androidTest) | Lessons/Busy tabs, Add lesson dialog eligibility + no-hours hint, busy conflict preview, a11y descriptions |
| `MainScreenPagerTest` (androidTest) | exactly 3 pages, swipe 1→2 reaches Schedule |
| `ExportStudentsUseCaseTest` / `ExportStudentsUseCaseScheduleTest` | full-plan + per-student schedule export, serializer failures, empty attendance, missing students |
| `ParseImportFileUseCaseTest` | object-first, legacy fallback, version rejection, blank file, storage failure |
| `PerformImportUseCaseTest` / `PerformImportScheduleMergeTest` | additive hour/busy/lesson merge, busy-wins, capacity raise, orphan/duplicate/malformed skip+count, round trip on empty app |
| `StudentsViewModelImportTest` / `ImportExportDelegateTest` / `StudentActionDelegateTest` | import selection dialog, schedule-aware import message, unsupported-version error binding |
