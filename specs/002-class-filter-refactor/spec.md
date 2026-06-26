# Feature Specification: Class Filter Refactor

**Feature Branch**: `002-class-filter-refactor`

**Created**: 2026-06-20

**Status**: Approved

**Input**: User description: "change class filter implementation inside @app/src/main/java/dev/nenoeldeeb/education/absencerecord/presentation/screens/calendar/CalendarScreen.kt by implementing it directly inside students list pop-up dialog as a dropdown menu with checkboxes, for student list merge multiple classes if they are checked, for total student count increment/decrement them as classes checked, marked students attendance count never go above the total count"

## Clarifications

### Session 2026-06-20

- Q: Should student-to-class relationship remain single-class-per-student (union filter) or become many-to-many? → A: Single class per student, filter by union. No data model change. Each student has exactly one class (or is unassigned). Multi-select shows a deduplicated union of students from selected classes.
- Q: What happens to the existing separate filter UI (top bar icon + dropdown above calendar)? → A: Remove entirely. Filter lives only inside the attendance dialog.
- Q: How should "Unassigned" students be handled in multi-select mode? → A: No "Unassigned" checkbox. Unassigned students appear only in the default "all" state (when no class checkboxes are checked). When any class is checked, only students from selected classes are shown.
- Q: Where should the filter dropdown appear within the dialog? → A: Between the dialog title and the student list, as a collapsible dropdown.
- Q: What should the dropdown button label show when multiple classes are selected? → A: Count format — "3 classes selected" with proper pluralization ("1 class selected"), and "No filter" when none are checked.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Filter students by class within the attendance dialog (Priority: P1)

A teacher opens the attendance dialog for a specific date to mark which students are present. Instead of using a separate class filter above the calendar, the teacher now selects one or more classes directly inside the dialog via a dropdown with checkboxes. The student list updates dynamically to show only students from the selected classes. The total student count in the dialog title updates to reflect the merged student list size.

**Why this priority**: This is the core change — moving and enhancing the filter from a separate UI element into the dialog itself with multi-select support.

**Independent Test**: Can be fully tested by opening the attendance dialog, checking one class, verifying the student list contains only students from that class, then checking a second class and verifying students from both classes appear. Delivers the primary filtering UX improvement.

**Acceptance Scenarios**:

1. **Given** the attendance dialog is open, **When** the teacher taps the filter dropdown, **Then** a list of all available classes appears with unchecked checkboxes
2. **Given** the class dropdown is open, **When** the teacher checks one class, **Then** the student list updates to show only students belonging to that class
3. **Given** one class is already checked, **When** the teacher checks a second class, **Then** the student list merges to show students from both classes (deduplicated)
4. **Given** the attendance dialog is open, **When** no class is selected, **Then** all students are shown (current default behavior)

---

### User Story 2 - Dynamic total student count updates with class selection (Priority: P1)

As the teacher selects or deselects classes in the filter dropdown, the total student count shown in the dialog title updates in real time to reflect the number of students currently displayed.

**Why this priority**: The total count must always match the visible student list for accurate attendance tracking.

**Independent Test**: Can be tested by checking a class with 10 students and verifying the total shows 10, then checking another class with 5 unique students and verifying the total shows 15.

**Acceptance Scenarios**:

1. **Given** the attendance dialog is open, **When** the teacher selects a class, **Then** the total student count increments to match the number of students from the selected class (or merged with other selected classes)
2. **Given** a class checkbox is unchecked, **When** the teacher unchecks it, **Then** the total student count decrements accordingly
3. **Given** multiple classes are selected, **When** the teacher unchecks one class, **Then** only students exclusive to that class are removed from the count (students also in other selected classes remain)

---

### User Story 3 - Attendance count cannot exceed visible students (Priority: P1)

The teacher marks students as present by tapping on them. The count of marked students can never exceed the total number of visible (filtered) students. This prevents invalid states where more students are marked present than are shown in the filtered list.

**Why this priority**: This is a data integrity constraint that prevents inconsistent attendance records.

**Independent Test**: Can be tested by filtering to one class, marking all its students present, then adding another class — the present count stays at its valid value and never exceeds the new total.

**Acceptance Scenarios**:

1. **Given** the teacher has selected a class with 20 students, **When** the teacher marks 20 students as present, **Then** the present count shows 20/20
2. **Given** 20 students are marked present in a class of 20, **When** the teacher adds another class with 5 new students, **Then** the present count stays at 20 and total becomes 25 (20/25)
3. **Given** 20 students are marked present out of 20 total, **When** the teacher unchecks the class, **Then** the total decreases and the present count adjusts (present students that belonged to filtered-out class are no longer counted)

### Edge Cases

- What happens when a student belongs to multiple selected classes? The student should appear only once in the merged list (deduplication by student ID).
- What happens to unassigned students when at least one class checkbox is checked? Unassigned students are hidden — only students assigned to the selected classes are shown.
- What happens when the teacher unchecks all classes? The dialog should display all students (default state).
- What happens when a class has no students? The class still appears in the dropdown but selecting it results in zero students shown.
- What happens when attendance was already marked for a student who is then filtered out? The attendance record persists but the student is not visible; if the filter is changed back to include them, their attendance state should still be shown.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The attendance dialog MUST contain a collapsible dropdown menu with checkboxes listing all available classes, positioned between the dialog title and the student list
- **FR-002**: Multiple classes MUST be selectable simultaneously via checkboxes
- **FR-003**: The student list inside the dialog MUST display only students belonging to the selected class(es)
- **FR-004**: Students appearing in multiple selected classes MUST be shown only once (deduplication)
- **FR-005**: When no class is selected, all students (including unassigned) MUST be displayed
- **FR-006**: The total student count in the dialog title MUST update dynamically as classes are checked or unchecked
- **FR-007**: The number of students marked as present MUST never exceed the total number of visible (filtered) students
- **FR-008**: The existing filter icon in the top app bar and the separate ClassFilterDropdown composable above the calendar MUST be removed. The only class filter is inside the attendance dialog.

### Key Entities *(include if feature involves data)*

- **Student**: Represents a learner assigned to exactly one class (or unassigned); has a unique identifier
- **Class**: A group or section that students are assigned to; used as the filtering dimension
- **Attendance Record**: A pairing of a student, a date, and a presence status (present/absent); persists independently of the current filter selection

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Teachers can filter the student list by one or more classes in 2 taps or fewer from within the attendance dialog
- **SC-002**: The student list updates to reflect the selected class filter within 200ms of checking/unchecking a class
- **SC-003**: The total student count always matches the number of visible student items in the list
- **SC-004**: The marked-present count never exceeds the visible student count under any sequence of filter changes
- **SC-005**: All existing attendance-marking functionality continues to work unchanged after the refactor

## Assumptions

- Each student has exactly one class assignment; students may also be unassigned (classId = null)
- The existing class data model already supports listing available classes and filtering students by class
- The deduplication strategy uses the student's unique identifier to determine if they should appear more than once
- The separate ClassFilterDropdown component above the calendar AND the filter icon in the top app bar are both removed
