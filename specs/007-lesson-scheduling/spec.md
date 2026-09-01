# Feature Specification: Lesson Scheduling

**Feature Branch**: `007-lesson-scheduling`

**Created**: 2026-08-08

**Status**: Draft

**Input**: User description: "I want to add a new feature for students. This feature allows students to schedule specific times for their lessons on designated days. The teacher will work as follows: each day of the week, the teacher has a certain number of hours available for teaching. Each of these hours can accommodate multiple students. Each student has one lesson on their assigned days. The teacher will determine the available hours so that students can be assigned to them. When the teacher wants to assign one or more students to a particular hour, these hours will appear along with the number of students assigned to each, so the teacher knows the maximum number of students allowed per hour. After the teacher registers a student for an available hour, this appointment will appear on the student's profile. We will also add a necessary feature to make this function easier: adding specific times for each student. These times cannot be used for regular classes. These times will be specified by the start time, for example, 9:00, followed by the duration of the appointment. For example, if the teacher wants to register the student for the 9:00 class and the student has a busy appointment at 8:00 that extends for an hour and a half, the 9:00 appointment will not appear for him."

## Clarifications

### Session 2026-08-08

- Q: Lesson duration — fixed 60-minute lessons or configurable per hour? → A: Fixed 60-minute lessons (duration is a system constant, not a per-hour field).
- Q: Busy appointment weekday scope — repeat every weekday or per-weekday? → A: Per-weekday recurring (a busy block defined for one weekday only affects lessons on that weekday).
- Q: Editing an available hour's start time when assigned students then conflict with busy appointments? → A: The edit is saved; conflicting students are automatically unassigned and the teacher is informed of each removal.
- Q: Does lesson scheduling link to the existing attendance records? → A: No link in v1 (scheduling is planning-only; attendance module untouched).
- Q: Start-time granularity — 15-min, 30-min, whole hours, or exact minute? → A: 30-minute increments (start times for both lesson hours and busy appointments snap to :00 / :30).
- Q: May time blocks cross midnight? → A: No — a busy appointment or available hour must fit entirely within a single day (start time + duration must not pass 24:00).
- Q: Which durations are offered for busy appointments? → A: A fixed preset list — 15/30/45/60/90/120/150/180/240 minutes (minimum 15, maximum 240). The domain validation rule (duration ≥ 15 minutes, start + duration ≤ 24:00) is a permissive superset of the preset list.

### Session 2026-08-09 — Moved assignment & busy management to the Student Detail screen

- Q: What remains on the Schedule page after busy appointments and lesson assignments move to Student Detail? → A: The weekly availability plan only — weekday selector + list of available hours with occupancy (e.g., "3 / 5"), add/edit/delete hours. Each hour row is expandable to show the assigned student names (read-only); tapping a name navigates to that student's profile. The Appointments / Busy Appointments tabs are removed.
- Q: How is schedule management organized on the Student Detail screen? → A: A Saturday-first weekday selector (same component as the Schedule page) plus two tabs — "Lessons" and "Busy" — replacing the read-only schedule card. The Busy tab manages the selected student's busy appointments; the Lessons tab manages their lessons.
- Q: How does the teacher assign THIS student to a lesson? → A: Per-student from the Lessons tab: the student's assigned lessons for the selected weekday are listed (each with unassign); an "Add lesson" button opens a dialog scoped to that weekday that lists only eligible hours (capacity available, no busy overlap, not a second lesson that day). If no hours exist that weekday, the dialog shows a hint pointing to the Schedule page.
- Q: How are busy-conflict lesson removals presented (FR-011)? → A: Previewed in the dialog: when a busy appointment would overlap an existing lesson, the dialog warns and lists the lesson(s) that will be removed; the teacher confirms to save (busy always wins). Changed from the previous save-and-toast behavior.
- Q: Is the hour-centric bulk assignment UI retained? → A: No — assignment is strictly per-student from the profile. The multi-select Assign Students dialog is removed; the Schedule page hours list is read-only with respect to assignments.

### Session 2026-08-09 (rev 2) — Student Detail sections become a swipeable pager

- Q: Should the Student Detail screen present its two sections (weekly schedule and attendance report) as horizontally swipeable pages? → A: Yes — a `HorizontalPager` with exactly two pages: the Attendance Report and the Schedule. The Lessons / Busy sub-tabs remain inside the Schedule page.
- Q: Which page is shown first? → A: The Attendance Report is page 0 and the default on every open.
- Q: How does the teacher switch between the pages? → A: Swipe-only (no tab row), consistent with the top-level Students/Calendar/Schedule pager; subtle page-position dots below the header indicate the current page.
- Q: Where does the student header (name + class) live? → A: Pinned above the pager, always visible on both pages.
- Q: Is the swiped page remembered across navigation? → A: No — reopening a profile always starts on the Attendance page (the detail screen's state resets on re-entry; rotation/config changes are preserved by `rememberPagerState`'s built-in saver).

### Session 2026-08-09 (rev 3) — Backup/restore (import/export) covers schedule data

- Q: Should the importer also accept the OLD flat-array backup format (files created before this change)? → A: Yes — the importer tries the new object format first and falls back to the legacy flat array, so pre-existing backups still restore (students + dates only, no schedule data).
- Q: When an imported student already exists (same name), how should their existing lessons/busy appointments be treated? → A: Merge additively, skipping conflicts — the existing schedule is kept and only imported lessons/busy that do not conflict are added; nothing is deleted. New students get their full imported schedule.
- Q: If an imported available hour overlaps an hour already in the app on the same weekday, what should the importer do? → A: Merge additively, skipping overlaps — existing hours are kept and only non-conflicting imported hours are added; the availability plan is never deleted wholesale.
- Q: What should happen to an imported lesson assignment whose hour is not in the imported availability plan? → A: Skip it and report the count in the import result message (the teacher is told how many lessons were skipped for missing hours).
- Q: When the teacher exports a subset of students, how much of the availability plan should be written to the backup? → A: Always the full availability plan — the plan is teacher-owned and exported whole regardless of the student selection.
- Q: On import, if an imported busy appointment overlaps a lesson already in the app, what should happen? → A: Busy wins — the overlapping existing lesson is removed and the imported busy is added, mirroring FR-011's in-app rule. (Symmetric note: an imported LESSON that overlaps an existing busy is skipped, never deleting busy; imported busy may overlap existing busy since busy-vs-busy is allowed.)
- Q: When an imported lesson targets an existing hour that is already full, what should happen? → A: Raise the existing hour's capacity to fit the imported assignments — nothing is skipped; the capacity becomes at least `max(currentCapacity, currentAssigned + importedNewDistinct)`.
- Q: When an imported lesson duplicates an existing lesson for the same student on the same weekday, what should happen? → A: Skip the imported lesson (one lesson per weekday is already satisfied) and count it in the result message.
- Q: How should the importer handle malformed schedule entries (bad duration, cross-midnight, off-grid start, bad capacity, unknown weekday)? → A: Skip each invalid entry and count it in the result message, exactly like malformed dates are handled today; the rest of the file still imports.
- Q: Should the new backup format include an explicit `version` field? → A: Yes — the new object format carries a `version` field; unknown/higher versions are rejected with a clear "unsupported backup" error, and versioned files + the legacy flat array are both accepted.

### Session 2026-08-11 — Normal time picker + extended busy duration (0.5h–6h)

> **Supersedes** two earlier decisions from Session 2026-08-08: the start-time granularity rule (:00/:30 snap) and the busy-duration preset list (15–240 min).

- Q: Replace the dropdown slot lists with a standard time picker? → A: Yes — a Material3 `TimePicker` (clock face + keyboard tab) is used for the two places the teacher actually types a time: available-hour start times and busy-appointment start times. It is launched from a read-only start-time field inside the existing Hour / BusyAppointment dialogs. The Add Lesson dialog is unchanged — it still lists the teacher's eligible available hours with occupancy (a lesson binds to a defined available hour, so there is no free-form lesson time to pick).
- Q: What minute granularity should the time picker allow? → A: Exact minute — the `:00`/`:30` snap rule is removed entirely. Any minute of the hour is legal for both available-hour and busy-appointment start times. The Material3 `TimePicker` has no minute-step API, so this is the natural behavior.
- Q: How should the time picker present hours? → A: Follow the system locale (`is24Hour = null`), matching `TimeFormatter`'s existing locale-aware output (12-hour AM/PM or 24-hour per device).
- Q: What is the busy-appointment duration range? → A: 30 minutes (0.5h) minimum, 360 minutes (6h) maximum, in 30-minute steps. The old 15-minute minimum and the preset list (15/30/…/240) are removed — a busy appointment can no longer be shorter than half an hour.
- Q: What UI control presents the busy duration? → A: A Material slider over 0.5h–6h in 0.5h steps with a live "Ends at HH:mm" preview that turns red (and blocks save) when the range would cross midnight.
- Q: What should the importer do with a legacy backup busy appointment whose duration is 15/45/75/… (old range allowed those)? → A: The new rule applies to imports too: durations outside 30–360 minutes (or not a multiple of 30) are skipped and counted as malformed (FR-031). No special legacy handling.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Teacher defines the weekly teaching availability (Priority: P1)

The teacher opens the schedule, chooses a day of the week (e.g., Saturday), and lists the hours at which they can teach that day. For each available hour, the teacher sets a start time (e.g., 9:00) and the maximum number of students it can hold (e.g., 5). The teacher can edit or remove these hours at any time and can repeat this for every day they teach.

**Why this priority**: Without the availability plan there is nothing to schedule students into. This is the foundation every other story depends on.

**Independent Test**: Can be fully tested by building a full week of available hours and verifying they are stored, displayed per weekday, editable, and removable — delivering a usable availability plan with no scheduling feature required.

**Acceptance Scenarios**:

1. **Given** the teacher is creating availability for a weekday, **When** they add an hour starting at 9:00 with a maximum of 5 students, **Then** the hour appears in that day's list with an occupancy of "0 / 5".
2. **Given** a saved hour, **When** the teacher changes its maximum to a number lower than the students currently assigned to it, **Then** the change is rejected and the current maximum is preserved.
3. **Given** a saved hour, **When** the teacher deletes it and confirms, **Then** the hour disappears and all student assignments to it are removed as well.
4. **Given** two hours on the same weekday whose time ranges overlap, **When** the teacher tries to save the second one, **Then** it is rejected as conflicting with the existing hour.
5. **Given** no hours exist for a day, **When** the teacher views that day, **Then** an empty state clearly invites them to add the first hour.
6. **Given** an hour with assigned students, **When** the teacher expands its row on the Schedule page, **Then** the assigned student names are listed (read-only), and tapping a name opens that student's profile while preserving the Schedule page state on return.
7. **Given** the start-time field in the hour dialog, **When** the teacher opens the time picker and picks 9:07, **Then** the hour is saved at 9:07 with occupancy "0 / 5" (exact-minute starts are legal).
8. **Given** the start-time field in the hour dialog, **When** the teacher picks a time whose 60-minute lesson would pass 24:00 (e.g. 23:30), **Then** the save is blocked with an inline error and the dialog shows the lesson's "Ends at" preview in error.

---

### User Story 2 - Assign lessons per student on the Student Detail screen (Priority: P1)

The teacher opens a student's profile, selects a weekday, and sees that student's lessons on that day in the "Lessons" tab. To register the student, the teacher taps "Add lesson" and picks an eligible hour from a dialog scoped to the selected weekday. The dialog shows each hour's occupancy (e.g., "3 / 5") and hides hours that are full, that overlap the student's busy appointments, or that would create a second lesson that day. Assigning the student is reflected immediately in the hour's occupancy on the Schedule page. The teacher can unassign the student from the Lessons tab at any time.

**Why this priority**: This is the core value of the feature — registering students into lessons with capacity and conflict awareness. It is now performed per student from the profile (see Clarifications, session 2026-08-09), so the Schedule page no longer needs an hour-centric bulk-assignment flow.

**Independent Test**: Can be fully tested by opening a student's profile, assigning them to eligible hours on several weekdays, verifying the Schedule page occupancy updates, verifying full/busy-conflicting/double-booked hours are never offered, and unassigning — with zero busy-appointment records needed.

**Acceptance Scenarios**:

1. **Given** a student's Lessons tab on a weekday with an hour at 9:00 with capacity 5 and 3 students already assigned, **When** the teacher taps "Add lesson", **Then** the dialog lists the 9:00 hour with occupancy "3 / 5" and it is selectable for this student.
2. **Given** an hour that has reached its maximum, **When** the teacher opens the "Add lesson" dialog, **Then** that hour is not offered for this student (hour full).
3. **Given** a student already assigned to an hour on a weekday, **When** the teacher views the Lessons tab for that weekday, **Then** the lesson is listed with an unassign action, and the "Add lesson" action is unavailable for that weekday (at most one lesson per day).
4. **Given** a student assigned to an hour on one weekday, **When** the teacher assigns the same student to an hour on a different weekday, **Then** the assignment succeeds.
5. **Given** an assigned lesson, **When** the teacher unassigns it from the Lessons tab, **Then** the lesson disappears from the tab and the hour's occupancy on the Schedule page decrements.
6. **Given** a weekday with no available hours, **When** the teacher opens the "Add lesson" dialog, **Then** the dialog shows a "no hours for this day" message and a hint to add hours on the Schedule page.

---

### User Story 3 - Manage busy appointments per student on the Student Detail screen (Priority: P2)

From a student's profile, the teacher opens the "Busy" tab and, for a selected weekday, records that student's busy appointment times: a start time (e.g., 8:00) plus a duration (for example, 1 and a half hours). These times are off-limits for lessons: when the teacher later opens the "Add lesson" dialog, any hour overlapping a busy appointment is not offered. If a busy appointment is added or extended after a lesson was already assigned, the dialog previews the lesson(s) that will be removed and the teacher confirms, so busy times always win.

**Why this priority**: This is what makes the schedule realistic: without conflict awareness, the teacher must manually track each student's commitments on paper. It directly supports story 2 and is prioritized because it changes which hours are offered in the assignment flow.

**Independent Test**: Can be tested with manually added busy appointments and confirm that no lesson can ever coexist with an overlapping busy appointment at any time.

**Acceptance Scenarios**:

1. **Given** a student with a busy appointment from 8:00 lasting 1.5 hours, **When** the teacher opens the "Add lesson" dialog on that day, **Then** the 9:00 hour does not appear as assignable for that student (busy range 8:00–9:30 overlaps 9:00).
2. **Given** a student with a busy appointment ending exactly at 9:00, **When** the teacher opens the "Add lesson" dialog, **Then** the 9:00 hour is still assignable (no overlap).
3. **Given** a student already assigned to a 9:00 hour, **When** the teacher adds a busy appointment from 8:00 for 1.5 hours in the Busy dialog, **Then** the dialog previews that the 9:00 lesson will be removed, the teacher confirms, and the removal takes effect.
4. **Given** a busy appointment, **When** the teacher edits its start time or duration in the Busy tab, **Then** the conflict rules are re-evaluated exactly as if the appointment were new (with the same preview-and-confirm behavior).
5. **Given** a busy appointment, **When** the teacher deletes it from the Busy tab, **Then** the previously blocked hours become assignable for that student again.
6. **Given** the Busy dialog's duration slider, **When** the teacher drags it, **Then** only durations from 0.5h to 6h in 0.5h steps are selectable and the "Ends at" preview updates live; a combination that crosses midnight shows the preview in error and blocks saving.

---

### User Story 4 - Inspect occupancy and assigned students on the Schedule page (Priority: P3)

The teacher views a weekday's list of available hours on the Schedule page, each showing how many students are assigned out of the maximum (e.g., "3 / 5"). Expanding an hour reveals the assigned student names (read-only). Tapping a student's name opens that student's profile where the lesson can be managed; returning preserves the Schedule page state.

**Why this priority**: Displaying the plan closes the loop for the teacher (the "report" of the registered appointments), at the feature level below the core flows, so it is last.

**Independent Test**: Assign someone to an hour from their profile, return to the Schedule page, expand the hour, and confirm the student's name is listed and navigates to their profile — no other screens are needed.

**Acceptance Scenarios**:

1. **Given** a weekday with hours, **When** the teacher views the Schedule page, **Then** each hour shows occupancy "assigned / maximum".
2. **Given** an hour with assigned students, **When** the teacher expands its row, **Then** the assigned student names are shown (read-only) and no assignment action is offered there.
3. **Given** an expanded hour's student name, **When** the teacher taps it, **Then** that student's profile opens, and navigating back restores the same weekday and expansion state on the Schedule page.
4. **Given** the schedule of a student, **When** an assignment is removed or a busy appointment is deleted on the profile, **Then** the Schedule page occupancy reflects the change immediately.

---

### Edge Cases

- What happens when a student is deleted? All of the student's lesson assignments and busy appointments are removed along with them.
- What happens if two busy appointments for the same student overlap each other? Both are allowed; busy appointments only constrain lessons, not each other.
- What happens if the teacher tries to delete the last available hour for a day? The day simply has no more hours and shows the empty state.
- When exactly is a lesson considered in conflict with a busy appointment? Only when their time ranges actually overlap: the lesson must start before the busy appointment ends and end after the busy appointment starts. A lesson's range is always exactly 60 minutes from its start time (see Clarifications, session 2026-08-08).
- What happens when the teacher edits an available hour's start time and assigned students now conflict with their busy appointments? See FR-017: the edit is saved, the conflicting students are automatically removed from that hour, and the teacher is informed of each removal.
- What is the maximum capacity? At least 1; any positive number up to the number of students (no modeling mandated).
- Can a time block cross midnight? No — busy appointments and available hours must fit within a single day; any input whose start time plus duration passes 24:00 is rejected (see Clarifications, sessions 2026-08-08 and 2026-08-11). With exact-minute starts, the latest legal available-hour start is 23:00 (its 60-minute lesson ends exactly at 24:00); the latest legal busy start depends on the chosen duration (start + duration ≤ 24:00, minimum duration 30 min → at most 23:30).
- What if the teacher picks a start time beyond the legal latest start? The hour/busy dialog blocks the save with an inline error and shows the "Ends at" preview in error (available hours additionally preview their fixed 60-minute end). The time picker itself offers the full 24-hour day; legality is validated on save.
- What if a busy appointment both conflicts with a lesson AND crosses midnight (e.g. a 23:00 lesson plus a busy 23:30–00:30 block)? The conflict-preview confirm button MUST remain disabled until the appointment itself is valid (duration in 30–360 min and start + duration ≤ 24:00) — a teacher must never be able to confirm a midnight-crossing busy block.
- Are exact-minute start times really allowed for both available hours and busy appointments? Yes — the `:00`/`:30` snap was removed; e.g. hours at 9:07 and 10:22 are legal, and busy blocks such as 9:07–9:37 are legal (30-min steps on the duration, arbitrary minutes on the start). Overlap checks (FR-003) still apply to arbitrary minutes.
- Can two available hours start only minutes apart (e.g. 9:00 and 9:07)? Yes, each is saved only if it does not overlap an existing hour that day — 9:00–10:00 and 9:07–10:07 overlap, so the second is rejected (FR-003).
- What happens to a legacy backup busy appointment with a 15/45/75/…-minute duration? It is skipped and counted in the import result message — the new 30–360-minute / 30-minute-step rule applies to imports as well (see Clarifications, session 2026-08-11).
- What if the teacher edits an existing busy appointment whose duration is not a multiple of 30 (data created before this change)? The slider snaps it to the nearest 30-minute step within 30–360 on open; the teacher confirms the snapped value before saving.
- What if the teacher opens a student's "Add lesson" dialog on a weekday with no available hours? The dialog shows a "no hours for this day" message and a hint to add hours on the Schedule page (see Clarifications, session 2026-08-09).
- What if the student already has a lesson on the selected weekday? At most one lesson per day (FR-008): the "Add lesson" action is unavailable (disabled or hidden) for that weekday.
- What if the student has no busy appointments on the selected weekday? The Busy tab shows an empty state inviting the teacher to add the first busy appointment for that day.
- What if a busy appointment added from the profile removes a lesson? The dialog previews the removal and the teacher confirms before saving (see Clarifications, session 2026-08-09); after saving, the Schedule page occupancy and the Lessons tab both update immediately.
- What if the teacher taps an assigned student's name on the Schedule page? It navigates to that student's profile; returning restores the same weekday and expanded-hour state on the Schedule page.
- What if a student is deleted after the schedule was inspected? Their name disappears from every expanded hour on the Schedule page (cascade, FR-014) as well as from the register.
- What if the teacher swipes horizontally over the weekday chips row on the Schedule page? The chips row is itself horizontally scrollable (`horizontalScroll`), so a drag over the chips scrolls the chips instead of flipping the pager page; the pager swipe works everywhere else on the page. This is the intended gesture behavior (a swipe anywhere outside the chips row flips pages).
- What happens to the detail pager position when the teacher leaves a profile and returns? It resets to the Attendance page (page 0); every visit starts fresh. Rotation or configuration changes preserve the current page via `rememberPagerState`'s saver (see Clarifications, session 2026-08-09 rev 2).
- What if a student has no attendance records and no scheduled hours? The Attendance page shows its existing "no attendance records" empty state; the Schedule page shows the existing Lessons/Busy empty/hint states. No new combined empty state is required.
- How does TalkBack expose the two swipeable pages? `HorizontalPager` provides pager semantics (page position and scroll actions), so the teacher can swipe between the Attendance and Schedule pages with TalkBack without needing a tab row.
- Is the Material3 time picker accessible? Yes — `TimePickerDialog` is fully accessible via TalkBack (clock face announcements + a keyboard-input tab), and the start-time field that opens it is a clickable read-only field (`StartTimeField`, a `Surface(onClick=...)`) announced as a button with its time value. The duration slider is exposed as a seek control with its current value announced; the live "Ends at" preview provides a non-color text signal for invalid ranges (color is never the sole indicator). (2026-08-11: a plain `readOnly` `OutlinedTextField` wrapped in `Modifier.clickable` did NOT open the picker on real devices — the field's internal tap handling consumed the gesture, see Brainstorm Log, Session 2026-08-11 bugfix.)
- What happens when a pre-change (legacy flat-array) backup file is imported after this feature ships? The importer detects the legacy shape and restores students + dates only; no schedule data exists in such files and nothing is reported as missing (FR-023).
- What happens if the backup file's `availableHours` list is missing or empty (teacher never set up hours)? The list is treated as empty — no hours are imported, and any lesson assignments referencing hours are skipped and counted as orphaned (FR-026).
- What happens when the same student appears twice in one backup file? The existing name-keyed import behavior applies: they are merged into a single student; their dates/schedule data are both applied against that one record.
- What if an imported lesson's hour exists in the file but that hour was itself skipped on import (overlap with an existing hour)? The lesson becomes orphaned and is skipped + counted (FR-026).
- What if an imported busy appointment would delete an existing lesson (FR-028)? This is the intended "busy wins" behavior; the removed lesson is reported in the import result message so the teacher sees the deletion.
- What if a backup file contains schedule entries whose weekday/hour fields are internally inconsistent (e.g. an assignment on a weekday with no matching hour)? Each affected entry is handled by the orphan rule (FR-026) or the malformed rule (FR-031), and counted.
- What if an imported hour has a capacity that is valid but the app's matching hour already has students assigned? The imported hour is skipped (overlap by natural key); the app's hour is kept and its capacity may be raised per FR-029.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST allow the teacher to configure a weekly availability plan, with the ability to create, view, edit, and delete available lesson hours per weekday independently.
- **FR-002**: Each available hour MUST have a start time and a maximum number of students; the maximum MUST be a positive number. The lesson duration is fixed at exactly 60 minutes (system constant, not a per-hour field). Start times MUST be entered with a standard Material3 time picker at exact-minute granularity (the `:00`/`:30` snap rule is removed); a start whose 60-minute lesson would pass 24:00 MUST be rejected (see Clarifications, session 2026-08-11).
- **FR-003**: Available hours on the same weekday MUST NOT overlap in time; the system MUST prevent saving a conflicting hour.
- **FR-004**: The system MUST NOT allow reducing an hour's capacity below the number of students currently assigned to it.
- **FR-005**: The system MUST display each available hour with its current student count and its maximum total student count, so the teacher can tell how full it is at a glance.
- **FR-006**: The teacher MUST be able to assign a student to an available hour from that student's profile, and unassign the student from there. Assignment is per-student; there is no hour-centric bulk-assignment UI (see Clarifications, session 2026-08-09).
- **FR-007**: The system MUST prevent assigning a student when the hour has reached its maximum.
- **FR-008**: The system MUST prevent a student from being in more than one hour on the same weekday, and MUST allow the student on several different weekdays.
- **FR-009**: The system MUST not present an hour as assignable for a student when that hour overlaps one of the student's busy appointments.
- **FR-010**: From the student's profile, the teacher MUST be able to define and remove that student's busy appointments per weekday, each with a start time (entered with a standard Material3 time picker at exact-minute granularity) and a duration chosen with a slider from 30 to 360 minutes (0.5h to 6h) in 30-minute steps, with a live "Ends at" preview; a busy appointment recurs weekly on its weekday only and never affects other weekdays. A busy appointment MUST NOT extend past midnight (start + duration ≤ 24:00). The old 15-minute minimum and preset duration list are removed (see Clarifications, session 2026-08-11).
- **FR-011**: When a busy appointment is added or edited so that it overlaps one of the student's existing lessons, the system MUST preview the conflicting lessons in the dialog before saving, remove them once the teacher confirms, and MUST inform the teacher of each removal (see Clarifications, session 2026-08-09).
- **FR-012**: Removing a busy appointment MUST immediately restore the affected hours as assignable for that student.
- **FR-013**: Deleting an available hour MUST remove all student assignments to it, after teacher confirmation.
- **FR-014**: Deleting a student MUST also remove that student's assignment and busy appointments.
- **FR-015**: The student's profile MUST provide the interactive weekly schedule section (weekday selector with "Lessons" and "Busy" tabs) through which lessons and busy appointments are displayed and managed, with changes reflected immediately.
- **FR-016**: The schedule for a student MUST be based on the day-of-week availability, applying every week (recurring, until the teacher changes it).
- **FR-017**: When the teacher edits an available hour's start time and one or more assigned students would then overlap one of their busy appointments, the system MUST save the edit, automatically remove the conflicting students from that hour, and inform the teacher of each removal.
- **FR-018**: The "Lessons" tab MUST list the student's lessons for the selected weekday (each with an unassign action) and provide an "Add lesson" action that opens a dialog scoped to that weekday. The dialog MUST offer only eligible hours — capacity available (FR-007), no overlap with the student's busy appointments (FR-009), and not a second lesson that day (FR-008). If the selected weekday has no available hours, the dialog MUST show a "no hours for this day" message and a hint to add hours on the Schedule page.
- **FR-019**: The "Add lesson" action MUST be unavailable (disabled or hidden) on a weekday where the student already has a lesson (FR-008).
- **FR-020**: Each hour row on the Schedule page MUST be expandable to show the assigned student names (read-only); tapping a name MUST navigate to that student's profile, and returning MUST restore the Schedule page's weekday and expanded-hour state.
- **FR-021**: The Student Detail screen MUST present its attendance report and its interactive weekly schedule as two horizontally swipeable pages (`HorizontalPager`, Attendance first). The student header (name + class) MUST stay pinned above the pager, subtle page-position dots MUST indicate the current page, and the Lessons / Busy sub-tabs MUST remain inside the Schedule page. Reopening a profile MUST reset to the Attendance page.
- **FR-022**: The export/backup file MUST be a single JSON object that carries the teacher's `availableHours` list alongside the `students` list; each student object MUST carry their `lessonAssignments` and `busyAppointments` lists next to `name`/`className`/`dates` (see Clarifications, session 2026-08-09 rev 3).
- **FR-023**: The importer MUST accept BOTH the new object format and the legacy flat-array format; a legacy file restores students and dates only (no schedule data).
- **FR-024**: On import, a NEW student MUST receive their full imported lesson assignments and busy appointments. An EXISTING student (matched by name) MUST be merged additively: their current lessons/busy are kept, and only imported lessons/busy that do not conflict are added — no wholesale deletion, with the single exception of FR-028 (an imported busy appointment over an existing lesson) (see Clarifications, session 2026-08-09 rev 3).
- **FR-025**: On import, the availability plan MUST be merged additively: existing hours are kept, and only imported hours that do not overlap an existing hour on the same weekday are added — the plan is never deleted wholesale (see Clarifications, session 2026-08-09 rev 3).
- **FR-026**: An imported lesson assignment whose referenced hour is absent from the imported availability plan (missing from the file, or skipped on import) MUST be dropped and counted in the import result message, so the teacher knows lessons were skipped (see Clarifications, session 2026-08-09 rev 3).
- **FR-027**: The export MUST always write the FULL availability plan (all weekdays, all hours) into the backup, independent of which students are selected for export (see Clarifications, session 2026-08-09 rev 3).
- **FR-028**: On import, an imported busy appointment that overlaps an existing lesson MUST win: the overlapping existing lesson is removed and the busy is added (mirroring FR-011). Imported lessons that overlap an existing busy appointment MUST be skipped (busy is never deleted by an import); imported busy may overlap existing busy (busy-vs-busy is allowed) (see Clarifications, session 2026-08-09 rev 3).
- **FR-029**: On import, if an existing hour lacks capacity for the imported lessons being added to it, the hour's capacity MUST be raised to at least `max(currentCapacity, currentAssignedCount + importedNewDistinctCount)` — imported lessons are never skipped for capacity (see Clarifications, session 2026-08-09 rev 3).
- **FR-030**: An imported lesson that duplicates an existing lesson for the same student on the same weekday MUST be skipped and counted in the import result message (see Clarifications, session 2026-08-09 rev 3).
- **FR-031**: Malformed schedule entries (busy duration outside 30–360 minutes or not a multiple of 30, time range crossing midnight, non-positive capacity, unknown weekday) MUST be skipped individually and counted in the import result message — never aborting the rest of the file. Start times are exact-minute by design, so there is no "off-grid start" check anymore (see Clarifications, session 2026-08-11).
- **FR-032**: The new backup object format MUST include an explicit `version` field. Files with an unknown/higher version MUST be rejected with a clear "unsupported backup" error. Versioned files and the legacy flat array are both accepted (see Clarifications, session 2026-08-09 rev 3).
- **FR-033**: The import result message MUST report schedule outcomes alongside the existing student/date counts: available hours added, hours skipped (overlap), lessons added, lessons removed (busy wins), and lessons skipped (orphaned hour / duplicate / busy overlap / invalid) (see Clarifications, session 2026-08-09 rev 3).
- **FR-034**: The system MUST offer a standard Material3 `TimePickerDialog` (clock + keyboard, hour format following the system locale) for available-hour and busy-appointment start times, launched from a read-only start-time field in the Hour and BusyAppointment dialogs. The busy-appointment duration MUST be chosen with a 0.5h–6h slider in 0.5h steps with a live "Ends at" preview; the available-hour dialog MUST preview its fixed 60-minute lesson end. The Add Lesson hour-selection dialog (FR-018) is unchanged. Where a busy conflict-preview is shown (FR-011), its confirm action MUST be disabled until the appointment itself is valid (duration in range and start + duration ≤ 24:00). The 2026-08-11 validation rule (30–360 min, 30-min steps) applies to imports as well as in-app entry (see Clarifications, session 2026-08-11).

### Key Entities *(include if feature involves data)*

- **Available Lesson Hour**: A teaching slot on a specific weekday defined by a start time (exact-minute granularity, via a Material3 time picker), a fixed 60-minute duration, and a maximum number of students.
- **Lesson Assignment**: The registration of a specific student in a specific lesson hour on a specific weekday.
- **Student Profile**: Existing student entity extended with an interactive weekly schedule section (weekday selector + "Lessons" / "Busy" tabs).
- **Busy Appointment**: A per-weekday recurring time block for a student defined by a weekday, a start time (exact-minute granularity), and a duration from 30 to 360 minutes (0.5h–6h) in 30-minute steps; blocks that student's lesson options on that weekday only.
- **Backup File (new format)**: A single JSON object, `{ "version": 2, "students": [...], "availableHours": [...] }`. Each `students` entry is `{ "name", "className", "dates": [...], "lessonAssignments": [...], "busyAppointments": [...] }`; a `lessonAssignments` entry is `{ "weekday", "startMinutes" }` (the hour referenced by natural key — ids are rebuilt on import), a `busyAppointments` entry is `{ "weekday", "startMinutes", "durationMinutes" }`, and each `availableHours` entry is `{ "weekday", "startMinutes", "maxStudents" }`. The legacy flat-array format (`[{ "name", "className", "dates" }]`) is also importable (FR-023). `startMinutes` is an exact minute-of-day; `durationMinutes` must be a multiple of 30 within 30–360 to import (legacy 15/45/75-minute busy entries are skipped and counted, see Clarifications session 2026-08-11).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: The teacher can create a complete week of availability (all days, all hours) in under 5 minutes.
- **SC-002**: Assigning a student to an hour of an existing plan (from the student's profile) takes less than 10 seconds.
- **SC-003**: 100% of lessons that would overlap a student's busy appointment are correctly hidden from or refused for that student (a busy-rule violation can never be reached).
- **SC-004**: The occupancy number displayed for any hour is always exactly the number of students actually registered in it (never exceeds capacity, never lower than the true count).
- **SC-005**: A lesson registered from the profile appears immediately in that student's Lessons tab and in the hour's occupancy on the Schedule page, and disappears immediately if the slot is removed.
- **SC-006**: The teacher can add or edit a busy appointment from a student's profile in under 30 seconds, with the conflict rules applied (preview + confirm).
- **SC-007**: The teacher can switch between a student's attendance report and weekly schedule with a single horizontal swipe in under 3 seconds and without any tab or button press; the current page is always identifiable via the page-position dots.
- **SC-008**: A backup exported on any device, when imported on an empty app, reproduces the full availability plan, every student's lessons, and every student's busy appointments exactly (round-trip fidelity) — with no orphaned lessons and no capacity overruns.
- **SC-009**: Importing the same backup file twice (or on top of partially restored data) never violates a scheduling invariant: no lesson overlaps a busy appointment, no hour exceeds its capacity, and at most one lesson per student per weekday.

## Assumptions

- The teacher is the sole user and manager — students have no accounts; "student profile" is the internal record of the existing attendance app.
- Lesson assignments and busy appointments are managed per student from the student's profile; the Schedule page hours list is read-only with respect to assignments (no hour-centric bulk assignment UI in v1) and additionally shows assigned student names for inspection (see Clarifications, session 2026-08-09).
- The availability plan is weekly and recurring: the same hours apply every week for that weekday until the teacher edits it; there are no one-time (single-date) effects in v1.
- A lesson lasts one 60-minute hour; the teacher's "hour" is a literal 60-minute block (as in "each hour" of the availability). The duration is a fixed system constant — not configurable per hour. Durations such as "an hour and a half" apply only to busy appointments, not lessons.
- Start times are entered with the standard Material3 time picker at exact-minute granularity (the `:00`/`:30` snap is removed) and presented in the system locale's hour format. Busy durations are chosen with a 0.5h–6h slider in 0.5h steps; the minimum busy duration is now 30 minutes, so blocks shorter than half an hour are no longer possible (see Clarifications, session 2026-08-11).
- Busy appointments are per-weekday and recur weekly on that weekday only: a busy block defined for one weekday does not affect lessons on other weekdays.
- Lesson scheduling is planning-only in v1: lesson assignments and busy appointments do not create, modify, or read attendance records; the existing date-based attendance module is unaffected.
- The schedule applies to all students in the register; class-level filtering of scheduling is out of scope for v1.
- The teacher can set a lesson capacity of any positive number up to the number of registered students.
- If a student is removed from the register, their schedule data (assignments and busy appointments) is removed with them.
- On the Student Detail screen, the attendance report and the weekly schedule are presented as two horizontally swipeable pages (`HorizontalPager`, Attendance first, swipe-only with page-position dots, header pinned above). This is a presentation-only change: it does not alter any underlying data model or domain logic, and the attendance module remains untouched.
- Backup/restore reuses the existing Students-screen import/export flow (same storage repository, same per-student selection on import). It is merged additively by design — a backup restores fully onto an empty app, but never deletes existing data except where a busy appointment wins over an existing lesson (FR-028).
- Import is tolerant and partial (mirroring the existing date behavior): malformed, orphaned, and conflicting schedule entries are skipped and counted rather than aborting the file.

## Brainstorm Log

### Session 2026-08-09 — Moved assignment & busy management to the Student Detail screen

**Insight**: The teacher's own flow is student-centric ("I manage THIS student's week"), so lesson-assignment and busy-appointment management belong on the Student Detail screen rather than on the hour-centric Schedule page. The Schedule page is reduced to the weekly availability plan plus read-only occupancy/assigned-names inspection.

**Decisions captured**:
1. Schedule page scope: hours list (weekday selector + occupancy) with expandable, read-only assigned student names; tapping a name opens that student's profile; Appointments/Busy tabs removed.
2. Student Detail: Saturday-first weekday selector + "Lessons" / "Busy" tabs replace the read-only schedule card.
3. Assignment UX: per-student "Add lesson" dialog scoped to the selected weekday, offering only eligible hours; "Add lesson" unavailable on a weekday that already has a lesson (FR-008).
4. Busy conflicts: preview removals in the Busy dialog with teacher confirmation (changed from save-and-toast; FR-011).
5. Hour-centric bulk assignment (multi-select Assign Students dialog) removed — assignment is strictly per-student.

**Impact on plan/tasks**: `plan.md` and `tasks.md` reflect the previous hour-centric placement (Schedule page tabs, Assign Students dialog, read-only detail card). They must be re-planned / re-tasked (`/speckit-converge`) to match this spec: remove the Schedule tabs, move assignment + busy use cases and dialogs into the Student Detail screen, add the expandable assigned-names list + navigation on the Schedule page, and change the busy-conflict flow to preview-and-confirm.

### Session 2026-08-09 (rev 2) — Student Detail sections become a swipeable pager

**Insight**: The Student Detail screen had two stacked sections — the interactive weekly schedule (weekday selector + Lessons/Busy tabs) and the attendance report (month calendar + share). Because they are two equally important, mutually exclusive views of one student, a horizontal pager is the natural fit: it matches the app's existing swipe-only top-level pager (Students/Calendar/Schedule) and avoids a vertically cramped column.

**Decisions captured**:
1. Two pages only: Attendance Report (page 0, default) | Schedule. Lessons/Busy remain sub-tabs inside the Schedule page (no third/fourth pager pages).
2. Swipe-only navigation with a subtle two-dot page indicator below the pinned student header — no tab row, consistent with the top-level pager.
3. Student header (name + class) stays pinned above the pager on both pages.
4. Page position resets to Attendance on every open of a profile (matching the detail screen's existing reset-on-re-entry behavior); rotation/config changes preserve the page via `rememberPagerState`'s saver.
5. Gesture note: the Schedule page's weekday chips row is `horizontalScroll`, so horizontal drags over the chips scroll the chips rather than flip pages; swiping anywhere else flips pages. Intended behavior.
6. Accessibility: `HorizontalPager`'s pager semantics let TalkBack swipe between the two pages without a tab row; page dots are decorative (page position also announced by pager semantics).

**Impact on plan/tasks**: `plan.md` / `tasks.md` currently specify `StudentDetailBody` as a stacked Column (schedule card above attendance report). They must be re-tasked (`/speckit-converge`) to: wrap `AttendanceReportSection` + `StudentScheduleSection` in a `HorizontalPager` (Attendance first), hoist a fixed header + page dots, add the pager's `PagerState` (hoisted or remembered in the detail composable), and add coverage in `StudentDetailScreenTest` for swiping between pages and for the weekday-chips gesture behavior.

### Session 2026-08-09 (rev 3) — Backup/restore covers the new schedule data

**Insight**: The schedule feature's data must ride the app's existing import/export (backup/restore) flow so a teacher can migrate it with the student register. The file changes from a flat array to a versioned object — `availableHours` sits beside the `students` list, and each student carries `lessonAssignments` + `busyAppointments` beside name/dates. Because DB ids are rebuilt on import, assignments reference hours by natural key (weekday + startMinutes). The import stays tolerant and partial (matching today's date handling), merging additively so a backup restores fully onto an empty app but never wipes existing data.

**Decisions captured**:
1. Legacy compatibility: the importer accepts BOTH the new versioned object and the old flat array; legacy files restore students + dates only (FR-023).
2. Format: `{ version: 2, students: [...], availableHours: [...] }`; students gain `lessonAssignments` (`{weekday, startMinutes}`) and `busyAppointments` (`{weekday, startMinutes, durationMinutes}`); hours are `{weekday, startMinutes, maxStudents}`; unknown/higher versions are rejected (FR-022, FR-032).
3. Additive merge (no wholesale deletion): existing students keep their schedule and only non-conflicting imported entries are added (FR-024); the availability plan merges the same way — imported hours that overlap an existing hour on the same weekday are skipped (FR-025).
4. Availability plan is teacher-owned: export always writes the FULL plan regardless of student selection (FR-027).
5. Conflict resolution: imported busy beats an existing lesson (lesson removed, FR-028 — mirrors FR-011); imported lessons never delete busy; busy-vs-busy overlaps are allowed; imported lessons that overlap existing busy or duplicate a same-weekday lesson are skipped + counted (FR-028/FR-030).
6. Capacity: existing hours are never over capacity — an imported lesson onto a full existing hour raises that hour's capacity to fit (FR-029); orphaned lessons (hour absent/skipped) are skipped + counted (FR-026).
7. Tolerance: malformed entries are skipped individually and counted (FR-031); the import result message reports hours/lessons/busy outcomes alongside today's student/date counts (FR-033).
8. Round-trip guarantees: import onto an empty app reproduces the backup exactly; re-imports never violate lesson/busy/capacity invariants (SC-008/SC-009).

**Impact on plan/tasks**: `plan.md` / `tasks.md` do not yet cover backup/restore. They must be re-tasked (`/speckit-converge`) to: redesign `StudentExportData` → versioned object + `AvailableHourExportData`/`LessonAssignmentExportData`/`BusyAppointmentExportData`, extend `ExportStudentsUseCase` to include the full plan + per-student schedule, extend `ParseImportFileUseCase` to detect versioned vs legacy and reject unknown versions, extend `PerformImportUseCase` (+ `ImportResult`) with the merge/skip/capacity rules above, and extend `ImportExportDelegate`/`ImportSelectionDialog`/strings for the new result report.

### Session 2026-08-11 — Normal time picker + extended busy duration (0.5h–6h)

**Insight**: The slot-list dropdowns (`ExposedDropdownMenuBox` of `:00`/`:30` entries) were chosen originally because Material3's `TimePicker` has no minute-step API — but that API limitation is exactly why the "normal time picker" the teacher asked for is the right call: it makes start times exact-minute and removes an arbitrary snapping constraint the teacher never asked for. The same request re-scoped busy durations: the old 15–240 min preset list becomes a 0.5h–6h range in 0.5h steps (matching the "0.5 hour to 6 hours" phrasing) on a Material slider. Lessons are unaffected — they still bind to the teacher's defined available hours, so "lesson time choosing" stays the eligible-hours list in the Add Lesson dialog.

**Decisions captured**:
1. Start times (available hours + busy appointments) are entered with a standard Material3 `TimePickerDialog` (clock + keyboard) launched from a read-only field inside the Hour / BusyAppointment dialogs; the `:00`/`:30` snap rule is removed — exact-minute starts are legal (supersedes Session 2026-08-08 granularity decision). Hour format follows the system locale (`is24Hour = null`), matching `TimeFormatter`.
2. Busy duration: Material slider 0.5h–6h in 0.5h steps (30–360 min) with a live "Ends at" preview; the 15-min minimum and the preset list are removed (supersedes Session 2026-08-08 duration decision).
3. Add Lesson dialog unchanged — it still lists eligible teacher-defined hours with occupancy (FR-018); there is no free-form lesson time to pick.
4. Boundary/legality: available-hour start must allow its fixed 60-minute lesson to end by 24:00 (latest 23:00); busy start + duration must be ≤ 24:00 (latest start depends on duration). Illegal picks are blocked on save with inline error + red "Ends at" preview.
5. Conflict-preview confirm (FR-011) must be disabled while the busy appointment itself is invalid (e.g. midnight-crossing) — prevents confirming an illegal block.
6. Legacy backups: the new duration rule applies to imports — 15/45/75-minute busy entries are skipped and counted (FR-031 updated); exact-minute starts mean no "off-grid start" malformed check anymore.
7. Off-grid busy data created before this change snaps to the nearest 30-minute step (within 30–360) when edited.
8. Accessibility: `TimePickerDialog` and the slider are TalkBack-accessible; invalid "Ends at" is signaled by text/error state, not color alone.

**Impact on plan/tasks**: `plan.md` / `tasks.md` currently specify `ExposedDropdownMenuBox` slot lists for start times (research.md rejected Material3 `TimePicker` "no minute-step API"), `BUSY_DURATION_OPTIONS = 15/…/240`, and `ScheduleRules` constants `SNAP_MINUTES = 30` / `MIN_BUSY_DURATION_MINUTES = 15` plus `isSnappedToHalfHour`. They must be re-tasked (`/speckit-converge`) to: replace the start-time dropdowns with a `TimePickerDialog` in `HourDialog`/`BusyAppointmentDialog`, replace the duration dropdown with a 30–360 min / 30-min-step slider, remove the snap from `ScheduleRules` (drop `SNAP_MINUTES`, `isSnappedToHalfHour`, `MAX_HOUR_START_MINUTES`/`MAX_BUSY_START_MINUTES` become bounds checked against the 60-minute lesson / chosen duration), set `MIN_BUSY_DURATION_MINUTES = 30` + `MAX_BUSY_DURATION_MINUTES = 360` + a 30-minute-step check, gate the FR-011 conflict confirm on validity, update `PerformImportUseCase` validation (FR-031) and the related unit/UI tests (`ScheduleRulesTest`, `BusyAppointmentDialogTest`, `HourDialog` coverage, `TimeFormatterTest` unaffected).

### Session 2026-08-11 (bugfix) — Start-time field must be a clickable Surface, not a read-only TextField

**Insight**: On a real device, tapping the Hour dialog's start-time field did NOT open the `TimePickerDialog`. Root cause: the field was a `readOnly` `OutlinedTextField` with `Modifier.clickable` — the text field's internal selection/cursor gesture handling consumes the tap in the pointer-input Main pass (inner nodes first), so the outer `clickable` never fires. The Compose UI test passed only because `performClick()` invokes the merged `OnClick` semantics action directly instead of dispatching a real touch gesture, which is why the bug slipped through.

**Decisions captured**:
1. The time-picker launcher is a dedicated `StartTimeField` composable — a `Surface(onClick=...)` with no editable text field inside — extracted to `presentation/screens/components/` and shared by BOTH the Hour dialog (`HourDialog.kt`) and the Busy dialog (`BusyAppointmentDialog.kt`, which already used this pattern successfully).
2. `StartTimeField` gains an `isError: Boolean = false` flag that renders an error-colored border, so the Hour dialog keeps signaling a midnight-crossing start (previously done via `OutlinedTextField.isError`).
3. Accessibility is preserved/improved: the field is announced as a button with its time value (not a stale editable text field).

**Impact on plan/tasks**: `plan.md` / `tasks.md` wording "read-only start-time field" remains semantically correct, but any implementation reference should use the `Surface(onClick=...)` `StartTimeField` rather than a `readOnly` `OutlinedTextField` + `Modifier.clickable`. `HourDialogTest` (`hourDialog_tappingStartField_opensTimePicker`) continues to pass and now exercises a real click target.