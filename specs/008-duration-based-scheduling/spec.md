# Feature Specification: Duration-Based Lesson Scheduling

**Feature Branch**: `008-duration-based-scheduling`

**Created**: 2026-10-13

**Status**: Draft

**Input**: User description: "Refactor Schedule Capacity to Duration-Based Scheduling. Replace the manually configured maximum-student count on a lesson block with a lesson length (duration). Each student occupies one 10-minute slot. The user selects a duration between 10 and 120 minutes in 10-minute increments; capacity is derived as `capacity = durationMinutes / 10`. When adding a student, consider the student's next busy appointment after the block start and validate the student's actual 10-minute slot against it — do not reject a student merely because they have an appointment later inside the block. Order assigned students by their next relevant busy appointment (earliest first), with a deterministic tie-break for equal times and an explicitly defined fallback for students with no upcoming appointment. Adding a student to a partly filled block must re-evaluate the complete schedule (recalculate order, revalidate every student, accept only if the schedule stays valid and within capacity) rather than appending to the end, and a failed addition must not leave partial state. Existing schedules and persisted data must survive via an appropriate migration or backward-compatible strategy." Follow-up clarifications: Q1 = B, Q2 = A, Q3 = A; a later brainstorm session added Q4 = A (see Open Questions).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Set a lesson block's length, not a headcount (Priority: P1)

A teacher planning their weekly availability opens a lesson block and, instead of typing "how many students fit", sets how long the block lasts. The block offers lengths from 10 to 120 minutes in 10-minute steps. As soon as the teacher picks a length, the block reports how many students it can hold, and that number cannot be edited on its own. A 10-minute block holds one student; a 120-minute block holds twelve.

**Why this priority**: The length is the single source of truth for everything else in this feature. Without it there is no slot model, no capacity, and no ordering to speak of. Every other story depends on this one, so it is the smallest slice that delivers standalone value.

**Independent Test**: Add a lesson block, pick each of the twelve available lengths, and confirm the reported maximum student count equals the length divided by 10 and that no control exists to set that count independently. Delivers value on its own: the teacher can plan blocks and see the resulting headcount.

**Acceptance Scenarios**:

1. **Given** no lesson blocks exist, **When** the teacher adds a block and chooses 30 minutes, **Then** the block is shown as holding at most 3 students.
2. **Given** a lesson block with a chosen length, **When** the teacher views the block editor, **Then** the maximum student count is visible but cannot be typed, stepped, or otherwise changed on its own.
3. **Given** the block editor, **When** the teacher chooses 10 minutes, **Then** the selection stops at 10 and the maximum student count becomes 1.
4. **Given** the block editor, **When** the teacher chooses 120 minutes, **Then** the selection stops at 120 and the maximum student count becomes 12.
5. **Given** a 60-minute block that currently holds 5 students, **When** the teacher changes the length to 30 minutes, **Then** the system shows which 3 students would be kept and which 2 would be dropped, and asks the teacher to confirm before anything changes.
6. **Given** a 60-minute block holding 5 students, **When** the teacher shortens it to 30 minutes and confirms, **Then** the 3 students with the soonest next commitments (or, failing that, the first 3 alphabetically) stay and the other 2 are unassigned.
7. **Given** a 60-minute block holding 5 students, **When** the teacher shortens it to 30 minutes and declines, **Then** the block keeps its 60-minute length and all 5 students.
8. **Given** a block starting at 23:30, **When** the teacher chooses 90 minutes, **Then** the block is rejected with a clear message because it would run past midnight.
9. **Given** a block starting at 10:00 holding 3 students, **When** the teacher moves the start to 11:00 and one of those students has a commitment at 11:05, **Then** the system shows that this student would be dropped and asks the teacher to confirm.
10. **Given** a block starting at 10:00 holding 3 students, **When** the teacher moves the start to 11:00 and the teacher declines, **Then** the block keeps its 10:00 start and all 3 students.
11. **Given** a block holding 3 students, **When** the teacher increases its length, **Then** all 3 students keep exactly the slots they already had.

---

### User Story 2 - Students are sequenced by who has to leave first (Priority: P2)

A teacher adds several students to one lesson block. The students are not shown in arbitrary order: the student whose next commitment is soonest gets the earliest 10-minute slot, the next-soonest gets the slot after, and so on. A student whose commitment falls later inside the block is still accepted, as long as their own slot finishes before it. The teacher can see each student's resulting slot time range.

**Why this priority**: This is the user-visible payoff of the duration model — it turns a headcount into an actual lesson plan. It cannot exist without Story 1, but it delivers real value on its own once block lengths exist.

**Independent Test**: Build a block starting at 10:00 with three students whose next commitments are 10:30, 10:40 and 11:00, then confirm the slots resolve to 10:00–10:10, 10:10–10:20 and 10:20–10:30 respectively. Delivers value on its own: a correct, visible running order.

**Acceptance Scenarios**:

1. **Given** a block starting at 10:00, **When** three students with next commitments at 10:30, 10:40 and 11:00 are assigned, **Then** they are ordered A(10:00–10:10), B(10:10–10:20), C(10:20–10:30).
2. **Given** a block starting at 10:00, **When** a student whose next commitment is at 10:30 is assigned, **Then** the assignment is accepted because the student's slot ends at 10:10.
3. **Given** a block starting at 10:00 lasting 60 minutes, **When** a student whose next commitment is at 10:30 is assigned, **Then** the student is NOT rejected merely because a commitment falls later inside the block.
4. **Given** a block starting at 10:00, **When** three students all share a next commitment at 10:30, **Then** they are ordered alphabetically by name and each is validated against their own commitments.
5. **Given** a block starting at 10:00, **When** a student has no commitment that day, **Then** the student is placed after every student who does have one, ordered alphabetically by name among themselves.
6. **Given** a student's commitment running from 09:50 to 10:20 and a block starting at 10:00, **When** that student is assigned, **Then** the assignment is rejected because the commitment is still in progress when the block starts.
7. **Given** a student's commitment starting exactly at 10:10 and a slot of 10:00–10:10, **When** the assignment is validated, **Then** it is accepted because the slot ends exactly as the commitment begins.
8. **Given** a student's commitment starting exactly at 10:00 and a slot of 10:00–10:10, **When** the assignment is validated, **Then** it is rejected.
9. **Given** a block whose running order has settled, **When** the teacher adds an earlier commitment to one of its students, **Then** the running order is recalculated the next time the block is viewed and that student may move to an earlier slot.

---

### User Story 3 - Adding a student re-plans the whole block, or changes nothing (Priority: P3)

A teacher adds a fourth student to a block that already holds three. The system re-plans the entire block rather than appending the newcomer to the end, re-checks every student including the ones already there, and either accepts the addition and shows the new running order, or refuses it and leaves the block exactly as it was.

**Why this priority**: This is what keeps the running order trustworthy over time. It is the most intricate behaviour in the feature, so it sits after the core length and ordering rules are in place.

**Independent Test**: Take a 30-minute block already holding three students, then attempt to add a fourth whose commitment is earlier than all three existing ones; confirm the order is recalculated, then attempt to add a fifth to the same block and confirm it is refused as full with the block untouched.

**Acceptance Scenarios**:

1. **Given** a 30-minute block holding three students, **When** a fourth student is added, **Then** the system refuses the addition and the block still holds exactly the original three students in their original order.
2. **Given** a 120-minute block holding eleven students, **When** a twelfth student is added, **Then** the addition is accepted and the block holds twelve students.
3. **Given** a 120-minute block already holding twelve students, **When** a thirteenth student is added, **Then** the addition is refused as full.
4. **Given** a partly filled block, **When** a student with an earlier commitment than all existing students is added, **Then** the newcomer takes an earlier slot and the existing students shift forward.
5. **Given** a partly filled block, **When** the newcomer is accepted, **Then** every student — not only the newcomer — is validated against their own commitments before the change is committed.
6. **Given** a partly filled block where recalculation would push an existing student into a conflict, **When** a new student is added, **Then** the whole addition is refused and the block is unchanged.
7. **Given** a refusal for any reason, **When** the teacher looks at the block afterwards, **Then** the student list, the running order and the stored plan are exactly what they were before the attempt.

---

### User Story 4 - Existing plans and backups survive the change (Priority: P4)

A teacher who has been using the app before this change opens the app and finds every block they already created, with every student still assigned. The teacher also imports a backup file produced by the earlier version of the app and it loads successfully.

**Why this priority**: Without continuity the feature would cost existing users their data. It is separable from the new rules but must ship with them.

**Independent Test**: Create blocks and assignments on the previous version, upgrade, launch, and confirm all blocks and assignments are present with a valid length. Separately, import a file produced by the previous version and confirm it loads.

**Acceptance Scenarios**:

1. **Given** blocks and assignments saved before this change, **When** the teacher opens the app after the change, **Then** every block appears with a valid length and every previously assigned student is still assigned.
2. **Given** a previously saved block with a stored capacity of 6 students, **When** it is opened after the change, **Then** it is treated as a 60-minute block.
3. **Given** a backup file produced by the previous version of the app, **When** the teacher imports it, **Then** it imports successfully, every block receives a valid length, and the block field it carried is converted rather than reinterpreted.
4. **Given** a backup file whose version is newer than the app understands, **When** the teacher imports it, **Then** the import is refused with a clear message and nothing is written.
5. **Given** a backup entry that carries both a capacity field and a duration field, **When** the teacher imports it, **Then** that entry is skipped and counted in the import summary rather than guessed at.
6. **Given** a backup file exported by this version of the app, **When** the teacher exports and immediately re-imports it, **Then** every block keeps exactly the length it was saved with.

---

### Edge Cases

**Boundary conditions**

- The shortest selectable length is 10 minutes and holds exactly one student; the longest is 120 minutes and holds exactly twelve.
- Every selectable length is a multiple of 10, so the block's slots tile its extent exactly with no remainder and no gap.
- A commitment beginning exactly when a slot ends is not a conflict; one beginning exactly when a slot starts is.
- A commitment still running when the block starts blocks every slot from the first onward until it ends.
- A block whose start plus its length would pass midnight is rejected.
- Two blocks on the same day must not overlap, and overlap is judged on each block's own start and length.

**Ordering and re-planning**

- Two students whose next commitments start at the same minute are separated alphabetically, so the block's order never depends on which student was added first.
- Two students with the same name and the same next commitment are separated by a stable internal identifier, so the order is fully deterministic.
- Because the running order is derived rather than stored, adding, editing or deleting a commitment can change a student's slot. This is expected: the block's plan is refreshed every time it is viewed.
- Removing a student re-plans the remaining students so no gap appears in the running order.
- Increasing a block's length changes nobody's slot; only shortening it or moving its start can invalidate students, and both go through the same confirm-first re-plan.
- Moving a block's start time can invalidate *every* student at once, because all slots move together. The teacher is shown the full list of students who would be dropped before confirming.
- Moving a block's start time can also change which commitment counts as a student's "next" one — a commitment that was irrelevant at 10:00 may be the deciding one at 11:00 — so the ordering is recalculated, not merely shifted.

**Error scenarios**

- The block is already at its derived maximum: the addition is refused and the teacher is told the block is full.
- A student's recalculated slot collides with one of their commitments: the whole addition is refused and the teacher is told which conflict occurred.
- A recalculation would leave an already-assigned student in conflict: the whole addition is refused rather than evicting that student.
- A student's own commitments overlap each other: the earliest start drives their position in the order, and every overlapping commitment is considered when validating.
- Shortening a block, or moving its start time, never removes a student silently — the teacher always sees the keep/drop list and must confirm.
- If every student would be dropped by an edit, the confirm dialog says so plainly rather than offering an empty plan as if it were normal.
- A backup contains a block length that is outside the selectable range or not a multiple of 10: the entry is skipped and counted in the import summary rather than silently reinterpreted.

**Scale**

- A block holds at most twelve students, so a full recalculation touches at most twelve students' commitments.
- Adding a student to a full block is refused fast; the teacher should not wait to be told something they could see from the block's remaining count.
- Because the order is derived rather than cached, every visit to a day re-derives every block on it. That work is repeated by design and must stay off the main thread, or the schedule screen would stutter on open.
- A commitment edited while the schedule screen is open must not leave a stale order on screen — the next read reflects the change.
- Deriving an order never blocks the teacher from doing something else; a slow derivation degrades to a late update, never a frozen screen.

**Data integrity**

- Adding, removing or re-planning students must never leave a block in a state that was not fully validated.
- Creating, editing or deleting a commitment must re-check affected students against their own 10-minute slots rather than the whole block, so a commitment that no longer touches a student's slot must not evict that student.
- A student keeps at most one lesson per day; that existing rule is unchanged.

**Backwards compatibility**

- Existing stored capacities are converted to lengths.
- Existing backup files keep importing; files from newer, unknown versions are refused with a clear message.
- A backup entry that carries both a capacity field and a duration field is ambiguous and is skipped rather than guessed at — the teacher sees it counted in the import summary.
- A backup entry carrying neither field is likewise skipped, not defaulted to a length.
- A file whose declared version matches the current one but whose field is still the old capacity name is read as the previous format, so a hand-edited or partially-updated file still loads rather than being misread.
- A file whose declared version is older than the immediately preceding one is refused, matching today's behaviour for unrecognised versions.
- A file declaring more blocks than could physically fit in a week is truncated at the cap and reported; the remaining students and blocks still import.
- A file declaring millions of tiny records fails to import gracefully — the teacher sees a counted summary, never a crash or a silent partial import.
- A file that exceeds a record cap is NOT reported as a corrupt file, because the records that were skipped may be perfectly well-formed; the teacher is told how many were left out.

**Choosing the length**

- The slider cannot rest between steps, so there is no such thing as a block of 35 minutes.
- Dragging to the far end selects exactly 120 minutes, never 119 or 121.
- A switch-access or keyboard user moves the slider in the same 10-minute increments as a drag user, and hears both the minutes and the derived student count at each stop.
- Touching the slider track jumps to the nearest step rather than requiring a precise drag.
- The free-typed headcount field and its increase/decrease buttons are gone; there is no path that lets the teacher type a student count.
- The derived student count shown beside the slider always reflects the slider's current value mid-drag — it is never left showing the previous value until the dialog closes.
- A block whose stored length is somehow not a multiple of 10 cannot arise from the UI, but is treated as an out-of-range entry on import (FR-048) rather than being rounded.

#### Brainstorm Prompts

- **Boundary conditions**: Should a length shorter than 10 minutes be representable at all (e.g. via imported data), or is 10 the hard floor everywhere?
- **Error scenarios**: When an addition is refused because an *existing* student would be displaced into a conflict, should the teacher be offered any recourse (e.g. shorten the block, move a commitment) in this change, or is a plain message enough?
- **Scale**: Is twelve students per block ever expected to grow, or is 120 minutes a permanent ceiling?
- **User confusion**: Teachers currently think in headcounts. Does the block list need to keep showing "x / y students" as well as the length, or would the slot times alone be clearer?
- **Data integrity**: If a teacher's device is restored from a backup mid-edit, can a half-applied re-plan be observed?
- **Backwards compatibility**: Should the previous maximum-99-students ceiling disappear entirely, or be retained as an upper bound for legacy data that somehow exceeds it?

## Open Questions

| # | Question | Status | Resolution |
|---|----------|--------|------------|
| Q1 | When a block's length is reduced below what its current students need (e.g. 60 min / 5 students → 30 min / 3 slots), what happens to the students who no longer fit? | Resolved | **B** — shortening is allowed. The system keeps the students with the soonest next commitments up to the new maximum and shows the teacher the keep/drop list, requiring explicit confirmation before any student is unassigned. See FR-009 to FR-013. |
| Q2 | Should each student's slot position be stored with the assignment, or recomputed from stored data every time the block is read? | Resolved | **A** — nothing is stored. The running order is derived from students + commitments + block length every time the block is read or changed, which also means the displayed order never drifts from the rules. See FR-025 and FR-039. |
| Q3 | Among students who have no next commitment in a block, what determines their relative order? | Resolved | **A** — alphabetical by student name. The same alphabetical rule is reused as the tie-break for equal next commitment times. See FR-022, FR-023 and FR-024. |
| Q4 | If moving a block's start time would invalidate an already-assigned student, what happens? | Resolved | **A** — the same confirm-first re-plan as shortening: re-plan, show the resulting order plus the list of students who would be dropped, require confirmation, apply all-or-nothing. See FR-009 to FR-015. |
| Q5 | Which control does the teacher use to pick the length, given there are exactly 12 valid values? | Resolved | **C** — a single continuous slider from 10 to 120 in 10-minute steps, with the derived student count shown live beside it. Replaces the free-typed headcount field and its stepper buttons. See FR-051 to FR-055. |
| Q6 | How is the changed backup format distinguished from the previous one? | Resolved | **C** — both signals: the declared version is bumped AND the block field is renamed from a capacity name to a duration name. Import accepts exactly the previous version and the current one, and refuses anything else. See FR-042 to FR-047. |
| Q7 | What happens when a backup file is implausibly large or declares an implausible number of records? | Resolved | **A** — cap the record counts; entries past a cap are skipped and counted in the import summary, consistent with how other malformed entries are already reported. See FR-056 to FR-059. |
| Q8 | Is the derived running order recomputed on every read, or memoized? | Resolved | **A** — recompute on every read, off the main thread, with no cache. Correctness over speed, and it keeps the order consistent with the data by construction. See FR-060 to FR-062. |

## Requirements *(mandatory)*

### Functional Requirements

**Block length and derived capacity**

- **FR-001**: A lesson block MUST be configured by its length in minutes; the teacher MUST NOT be able to configure a maximum student count directly.
- **FR-002**: The selectable block lengths MUST be 10 to 120 minutes inclusive, offered only in 10-minute increments.
- **FR-003**: The system MUST derive a block's maximum student count from its length as `maximum students = length in minutes / 10`.
- **FR-004**: The derived maximum MUST be the only capacity value used for validation, display, totals, export and import; no second independently editable capacity value may exist anywhere in the system.
- **FR-005**: A newly created block MUST default to 60 minutes, i.e. a maximum of 6 students.
- **FR-006**: When the teacher changes a block's length, the displayed maximum student count and the remaining-slot count MUST update immediately.
- **FR-007**: A block whose start time plus its length would pass midnight MUST be rejected with a clear message.
- **FR-008**: Two blocks on the same day MUST NOT overlap; overlap MUST be evaluated using each block's own start and length.

**Re-planning a block when an edit changes its slots**

Two edits can invalidate students who are already assigned: shortening the block below its current occupancy, and moving the block's start time. Both are governed by the same rule — re-plan, show the consequences, require confirmation.

- **FR-009**: The system MUST allow a block's length to be reduced, and MUST allow a block's start time to be changed, even when the change would leave some currently assigned students without a valid slot.
- **FR-010**: When such an edit is made, the system MUST decide which students are retained by applying the full ordering rule (FR-019 to FR-024) to the recalculated plan and keeping the first *N*, where *N* is the block's derived maximum.
- **FR-011**: The system MUST present the resulting running order together with the list of students that would be dropped, and MUST require the teacher to confirm before applying anything.
- **FR-012**: If the teacher declines, the block MUST retain its previous length, its previous start time and its complete set of students, unchanged.
- **FR-013**: If the teacher confirms, the dropped students MUST be unassigned and the retained students MUST remain assigned in their recalculated order, applied and persisted as a single all-or-nothing change.
- **FR-014**: When the teacher *increases* a block's length, the system MUST NOT change any existing student's slot — a slot depends only on the student's position in the order and on the block's start time, never on the length.
- **FR-015**: When the teacher changes a block's start time, the system MUST re-evaluate every assigned student's next relevant commitment against the new start, because a commitment that previously fell outside the block may now fall inside a slot.

**Slot calculation**

- **FR-016**: Each assigned student MUST occupy exactly one contiguous 10-minute slot.
- **FR-017**: Slot *n* (1-based) MUST begin at the block's start plus (*n* − 1) × 10 minutes, so the final slot ends exactly at the block's end time.
- **FR-018**: The teacher MUST be able to see each assigned student's resulting slot start and end time.

**Ordering by next commitment**

- **FR-019**: For each assigned student the system MUST determine their *next relevant commitment* as the earliest of that student's commitments on the block's day whose start time is at or after the block's start time.
- **FR-020**: Students MUST be ordered by next relevant commitment start time, earliest first.
- **FR-021**: Students with no next relevant commitment MUST be ordered after every student who has one.
- **FR-022**: Students with identical next relevant commitment start times MUST be ordered alphabetically by student name.
- **FR-023**: Among students with no next relevant commitment, their relative order MUST be alphabetical by student name.
- **FR-024**: Where two students share both the same ordering key and the same name, the system MUST fall back to a stable internal identifier so the resulting order is fully deterministic.
- **FR-025**: A student's position MUST NOT be stored; the running order MUST be derived from the block's students, those students' commitments and the block's length every time the block is read or changed.

**Slot validation**

- **FR-026**: A student's slot MUST be rejected if it overlaps any of that student's commitments on that day, including commitments that began before the block started and are still running.
- **FR-027**: A student MUST NOT be rejected merely because they have a commitment later within the block's overall length, provided their own slot finishes before it.
- **FR-028**: Time ranges MUST be treated as half-open — a commitment beginning exactly when a slot ends MUST NOT block that slot, and a commitment beginning exactly when a slot starts MUST block it.
- **FR-029**: Where two of a student's commitments share the same start time, each student's slot MUST still be validated independently against that student's own commitments.
- **FR-030**: A student's earliest commitment start on the day MUST drive their position in the order, while every one of that student's commitments MUST be considered during validation.

**Adding to a partly filled block**

- **FR-031**: When a student is added, the system MUST include that student together with every already-assigned student in a single recalculation.
- **FR-032**: The system MUST NOT append the new student to the end of the order by default.
- **FR-033**: The system MUST validate EVERY assigned student's recalculated slot, not only the newly added student.
- **FR-034**: The addition MUST be refused if the block already holds its derived maximum number of students.
- **FR-035**: The addition MUST be refused if ANY assigned student — new or existing — would end up with a slot that conflicts with one of their commitments.
- **FR-036**: A refused addition MUST leave the block's membership, running order and stored plan exactly as they were; no partial update may be visible or stored.
- **FR-037**: Removing a student MUST re-plan the remaining students so their slots stay contiguous from the block's start.
- **FR-038**: When a commitment is created, edited or deleted, the system MUST re-validate affected students against their own 10-minute slots rather than the block's full extent, so a commitment that no longer touches a student's slot MUST NOT evict that student.

**Persistence, migration and backup files**

- **FR-039**: The running order shown to the teacher after any add, remove or re-plan MUST be the order produced by FR-019 through FR-025, and MUST be identical after closing and reopening the app because it is derived rather than stored.
- **FR-040**: Blocks and assignments stored before this change MUST continue to load without user action; a stored capacity MUST be converted to a length of `capacity × 10` minutes.
- **FR-041**: A converted length MUST be clamped into the selectable 10–120 minute range so that legacy data outside it still loads.
- **FR-042**: Backup exports MUST write each block's length in minutes under a field named for duration, and MUST NOT write a student-count field.
- **FR-043**: The change of shape MUST be signalled twice over — by bumping the backup's declared version AND by renaming the block field — so the two formats are distinguishable by either signal alone.
- **FR-044**: Backup imports MUST accept a file declaring the immediately preceding version, reading its capacity field and converting it to a length by multiplying by 10.
- **FR-045**: Backup imports MUST accept a file declaring the current version, reading the block length directly with no conversion.
- **FR-046**: A backup entry that carries both a capacity field and a duration field, or neither, MUST be treated as malformed — skipped and counted in the import summary rather than guessed at.
- **FR-047**: Backup imports MUST refuse files whose version is unknown or newer, with a clear message, and MUST write nothing.
- **FR-048**: A backup entry whose length falls outside 10–120 minutes or is not a multiple of 10 MUST be skipped and counted in the import summary, consistent with how the app already reports other malformed entries.
- **FR-049**: A student MUST continue to hold at most one lesson per day; this rule is unchanged by this feature.

**Feedback and accessibility**

- **FR-050**: Every refusal MUST tell the teacher why it was refused — the block is full, the student's lesson would collide with one of their commitments, the block would run past midnight, or an edit to the block would drop named students.
- **FR-051**: The length MUST be chosen with a single continuous slider spanning 10 to 120 minutes, replacing the free-typed headcount field and its stepper buttons.
- **FR-052**: The slider MUST move only in whole 10-minute steps; no intermediate value may be reachable, previewed, or stored.
- **FR-053**: The derived maximum student count MUST be displayed alongside the slider and MUST reflect the slider's current value as it is dragged, without requiring the dialog to be dismissed and reopened.
- **FR-054**: The slider MUST carry a spoken label that conveys both the selected length in minutes and the resulting maximum student count, and MUST be operable through switch access and keyboard with the same 10-minute granularity as dragging.
- **FR-055**: The block list and the add-student list MUST continue to show how many of the available student places are used.

**Import safety**

- **FR-056**: Backup import MUST enforce a documented maximum record count for students, lesson blocks, lesson assignments **and attendance dates per student**; entries beyond a maximum MUST be skipped and counted in the import summary rather than imported.
- **FR-057**: The lesson-block maximum MUST be 1,008 — the number of non-overlapping 10-minute blocks that can physically exist in a week — and the assignment maximum MUST be the largest number of students those blocks could hold.
- **FR-058**: The student maximum MUST be 10,000, far beyond any class set a single teacher manages, and MUST be stated in the import summary message when it is hit. The per-student attendance-date maximum MUST be 3,650 — ten years of daily attendance — because the unbounded date list, not the student count, is what drives import memory.
- **FR-059**: Exceeding a maximum MUST NOT abort the import of the remaining valid entries and MUST NOT be reported as a parse failure; the teacher is told what was skipped.

**Deriving the order**

- **FR-060**: A block's running order MUST be derived from current data on every read; the system MUST NOT serve it from a stored position or from a cached order.
- **FR-061**: Deriving a block's running order MUST NOT run on the main thread — the derivation MUST be performed off it and its result delivered back to the user-facing surface.
- **FR-062**: The student maximum of 10,000 is a judgement-based figure, not a derived one; it is the one cap in this feature chosen by opinion rather than arithmetic, and it MUST stay far enough above real class sizes that no legitimate backup can ever reach it.
- **FR-063**: Backup import MUST refuse a file larger than 32 MB **before** decoding it, reading no more than that many bytes, so that an implausibly large file is rejected without ever being materialised in memory. This is reported as its own error, distinct from a parse failure.
- **FR-064**: A single derivation MUST consider at most the block's twelve students and, across a full day's view, at most the day's blocks; the derivation MUST stay within the response budget defined in the success criteria.

### Key Entities

- **Lesson Block**: A teacher's availability slot on a given day. Attributes: day of week, start time, length in minutes, and the maximum student count derived from that length. Replaces the previously stored capacity value.
- **Lesson Assignment**: The link between a student and a lesson block. Carries no stored position — the student's place in the block's running order, and therefore their 10-minute slot, is derived from the block's students, their commitments and the block's length.
- **Busy Appointment**: A student's existing commitment on a given day. Attributes: day of week, start time, duration. Drives both ordering and slot validation. Unchanged by this feature.
- **Student**: The person being taught. One lesson per day, unchanged. The name is the tie-break key when ordering is otherwise equal.
- **Backup File**: The exported snapshot of students, their dates, and the weekly plan including each block's length.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A teacher can change a lesson block's length and see the correct maximum student count without re-entering anything, in under 30 seconds.
- **SC-002**: For all twelve selectable lengths, the maximum student count shown equals the length divided by 10 — verified as 10 min→1, 20 min→2, 30 min→3, 40 min→4, 50 min→5, 60 min→6, 70 min→7, 80 min→8, 90 min→9, 100 min→10, 110 min→11, 120 min→12.
- **SC-003**: In 100% of tested cases, no student is ever assigned a slot that overlaps one of their commitments, across every boundary case (commitment starting at the slot start, at the slot end, spanning the whole block, and starting before the block).
- **SC-004**: Deriving the running order from the same inputs produces an identical result in 100% of cases, including blocks where several students share the same next commitment and blocks where several students have no commitment at all.
- **SC-005**: Zero cases of partial state after a refused addition — the block's membership, running order and stored plan are unchanged in every refusal scenario.
- **SC-006**: 100% of blocks and assignments created before this change are still present and valid after the change, with no teacher action required.
- **SC-007**: 100% of backup files produced by the immediately preceding version of the app import successfully; 100% of files from newer unknown versions are refused with a message and nothing is written.
- **SC-008**: An export followed immediately by an import is a lossless round trip for every block length — 100% of blocks return with the same length they were saved with.
- **SC-009**: A backup entry carrying both a capacity field and a duration field is skipped and counted in 100% of cases, never silently interpreted as one or the other.
- **SC-010**: A backup file declaring an implausible number of records imports without crashing and without the main thread becoming unresponsive, in 100% of cases, and the teacher is always told how many entries were skipped.
- **SC-019**: Opening a day of the schedule shows every block on it, correctly ordered, within 1 second on a typical mid-range device — in 100% of measured runs, with no dropped frames attributable to deriving the order.
- **SC-011**: A teacher can read each assigned student's slot time range directly from the block without opening an editor.
- **SC-012**: No block whose length would run past midnight is ever accepted, in any code path.
- **SC-013**: Assigning a student — successfully or not — shows its outcome in under 1 second on a typical mid-range device, including when the block is already full.
- **SC-014**: In 100% of tested shortenings that would drop students, the teacher is shown the keep/drop list and no student is unassigned without an explicit confirmation.
- **SC-015**: In 100% of tested start-time moves that would drop students, the same keep/drop confirmation applies, and zero students are removed without it.
- **SC-016**: Increasing a block's length leaves every existing student's slot unchanged in 100% of cases.
- **SC-017**: A switch-access or keyboard user can set any of the 12 lengths without a pointer and hears both the minutes and the resulting student count at every stop.
- **SC-018**: Attendance, class filter, student detail and reporting screens behave exactly as they do today, with no user-visible change.

## Assumptions

- Backup import is bounded in two independent ways. Record caps (students, lesson blocks, assignments,
  attendance dates per student) bound what reaches the database. A 32 MB file-size cap, enforced
  before decoding, bounds what is parsed — without it a cap applied after decoding cannot prevent an
  out-of-memory error, because the memory is already spent by the time any cap is consulted.
- The student cap of 10,000 is the single judgement-based number in this feature; every other cap is
  derived arithmetically. It is set far above any real class size so that no legitimate backup reaches it.
- The 10-minute slot length is fixed for this feature and is not itself configurable.
- Only one actor is involved: the teacher using their own device. There are no roles or permissions to model.
- Time ranges are half-open throughout: a range covers its start but not its end, so back-to-back ranges never count as overlapping.
- A student's "next relevant commitment" ignores commitments that end before the block starts; only commitments still running at the block start, or starting later, are relevant.
- Start times keep their current exact-minute granularity; only the block's length is constrained to 10-minute increments.
- Busy appointment rules (30–360 minute lengths in 30-minute steps, no crossing midnight) are untouched by this feature.
- The existing "preview and confirm" behaviour for removing lessons that collide with a new commitment is preserved; only the precision of the check changes from the whole block to the student's own slot.
- The feature is offline-only; no network or server is involved.
- The app is a single-module Android application whose stored data lives in a versioned on-device database; any schema change ships with an explicit, non-destructive migration.
- Duration controls replace the previous free-typed headcount field entirely; the previous 99-student ceiling disappears because 120 minutes is the ceiling.
- A newly created block defaults to 60 minutes — the app's existing nominal lesson length — giving a default maximum of 6 students.
- Because the running order is derived rather than stored (Q2 = A), a student who gains an earlier commitment will move earlier in the block the next time it is viewed. This is accepted behaviour, not a defect.
- Alphabetical ordering (Q3 = A) is case-insensitive and locale-independent so that an Arabic and an English name never produce different orders for the same data.
- No stored slot position means the block's assignment records need no new ordering field; the schema change is limited to replacing the stored capacity with a length.
- The length is picked with a slider (Q5 = C). The slider replaces the free-typed headcount field and its stepper buttons outright; no legacy numeric-entry affordance is kept alongside it.
- The derived order is recomputed on every read rather than memoized (Q8 = A). Caching was considered and rejected: the order legitimately changes when commitments change, so a cache would need invalidation on every commitment write, and a missed invalidation would show a plan the rules would never produce.
- Backup record caps (Q7 = A) are derived where the domain allows it: the block cap of 1,008 follows from seven days of non-overlapping 10-minute blocks. Only the student cap of 10,000 is a judgement call, chosen well beyond any class set one teacher manages.
- Backup files are unencrypted JSON on the teacher's own device storage. That is pre-existing behaviour and is unchanged here; this feature adds no new place where student data is stored or transmitted.

## Brainstorm Log

<!--
  This section records insights from /speckit.superspec.brainstorm sessions.
  Each entry is dated and summarizes what was discovered and decided.
  Do not edit manually — this is maintained by the brainstorm command.
-->

### Session 1 — 2026-10-13

**Mode**: Built-in 5-category protocol. No `brainstorming` skill detected on the
filesystem (recorded in `.specify/superpowers.yml`), so the Superpowers questioning
path was skipped in favour of the fallback categories.

**Starting point**: `spec.md` from `/speckit.specify` with Q1–Q3 already resolved.
Categories 1 and 2 were largely covered; the gaps were elsewhere.

**Questions asked**: 5 (Q4–Q8). All five categories ended up touched.

| # | Category | Question | Answer |
|---|----------|----------|--------|
| Q4 | Error / boundary | Moving a block's start time can invalidate every student at once — what then? | A — same confirm-first re-plan as shortening |
| Q5 | UX / accessibility | Which control picks one of exactly 12 lengths? | C — a single continuous slider |
| Q6 | Error / backward compat | How is the reshaped backup format told apart from the old one? | C — version bump AND field rename |
| Q7 | Security / privacy | What about an implausibly large or malformed backup file? | A — capped record counts, skip-and-count |
| Q8 | Scale | Recompute the derived order every read, or memoize? | A — recompute every read, off the main thread |

**Key insights discovered**

1. **Start-time moves were an unstated hole.** The specify pass handled shortening
   the length but said nothing about moving the start, even though `startMinutes`
   is editable in the existing block editor and moving it shifts *every* slot.
   Generalized the confirm-first rule to cover both (FR-009–FR-015), and added the
   complementary guarantee that *growing* a block changes nobody (FR-014).
2. **"Accept the previous backup version" is new work, not a description of
   existing behaviour.** The import path rejects on `version != BACKUP_VERSION` —
   strict equality — so dual-version acceptance (FR-044, FR-045) has to be built.
3. **The capacity→duration rename makes old and new files genuinely ambiguous.**
   A capacity of `6` and a duration of `6` mean very different things, which is
   why Q6 landed on signalling the change twice over and on refusing to guess when
   a file carries both fields (FR-046).
4. **The import path has no resource limits at all.** The whole file is read into
   a string and every record materialised, with no cap anywhere. Q7 added bounded
   counts rather than a blanket refusal, so a legitimate large backup still works.
5. **Q2 = A has a recurring cost that no requirement covered.** Deriving the order
   on every read means every visit to a day re-derives every block on it. Q8
   settled this as recompute-always with no cache, and made the off-main-thread
   constraint explicit (FR-061) to satisfy the constitution's main-thread rule.

**Spec changes**: requirements grew from 48 to 62 and success criteria from 15 to
19. Sections `Re-planning a block when an edit changes its slots`, `Import safety`
and `Deriving the order` were added; `Feedback and accessibility` was rewritten
around the slider; the backup requirements were split into seven distinct rules.
US1 gained three scenarios (start moves, grow-is-a-no-op), US4 gained three
(dual-format import, ambiguous entries, export/import round trip), and seven
edge-case bullets were added across scale, error and backward-compatibility groups.

**Numbering note**: requirements were renumbered several times during this session
as rules were inserted mid-document. The final state was machine-verified —
FR-001…FR-064 and SC-001…SC-019, both contiguous with no duplicates, and every
inline cross-reference resolving to an existing requirement.

**Later amendment**: FR-063 (the pre-decode file-size guard) and FR-064 (the
per-derivation bound) were added after that note was first written, taking the
count from 62 to 64. The counts in this section and in `plan.md` were corrected
to match during Phase 8 (T093); FR-062 had also been listed out of numeric order
between FR-058 and FR-059 and was moved back into sequence. No requirement was
renumbered, added or removed by that correction — only its position in the
document and the stated totals.

**Open**: none. Q1–Q8 all resolved.