# Feature Specification: Student Detail Screen

**Feature Branch**: `006-student-detail-screen`

**Created**: 2026-08-05

**Status**: Draft

## Clarifications

### Session 2026-08-05

- Q: When a teacher edits a student's name on the Student Detail screen, should the edit happen inline (directly in place, no dialog) or through a modal/bottom sheet dialog? → A: Modal / bottom sheet dialog with explicit Save/Cancel buttons (consistent with existing StudentDialog pattern).
- Q: Where should the profile picture be stored — should the app copy the selected image into its own private app storage, or should it only keep a reference (URI) to the image in the device's photo library? → A: Profile picture feature is removed from scope entirely — no photo picking, no storage, no placeholder avatar.
- Q: When the Reports screen is removed, should the attendance view on the Student Detail screen show the same interactive calendar grid (the existing ComposeCalendar component) or a simpler flat list of absence dates? → A: Reuse the existing interactive calendar grid (ComposeCalendar component) — same visual as the Reports screen today.
- Q: When the HorizontalPager is replaced to support push-navigation to the Student Detail screen, should the two remaining top-level screens (Students and Calendar) still be swipeable side-by-side as a pager, or switch to bottom navigation / tab row? → A: Keep the swipeable horizontal pager for Students ↔ Calendar; simply remove the Reports page from it.
- Q: Should a student be deletable directly from their Student Detail screen, or should deletion remain exclusively in the multi-selection flow on the Students screen? → A: Allow deletion from the Student Detail screen — a delete icon in the top bar triggers a confirmation dialog, then navigates back on confirm.
- Q: Can a teacher change or assign a student's class from the Student Detail screen's edit dialog, or is the dialog limited to editing the student's name only? → A: Dedicated "Change Class" button/action on the detail screen separate from the name edit dialog.
- Q: On the Student Detail screen, should tapping dates in the attendance calendar grid allow teachers to record or modify attendance for that student directly, or should the calendar remain view-only/read-only? → A: View-only / read-only — tapping calendar dates does not alter attendance records.
- Q: How should tapping the sort icon in the Students screen top bar present the available sorting options to the teacher? → A: Dropdown menu — tapping the sort icon opens a popup DropdownMenu with sort options.

**Input**: User description: "I want to make a major change to this application by adding a dedicated screen for each student. This screen, accessible by simply tapping the student's name, will contain all student-related information, including name changes, profile picture changes, and attendance reports. After completing this change, a single tap on a student's name on the student page will take you to that student's individual page. A long press on a student's name will also begin the selection process. Once this change is complete, the reports screen will become unnecessary. We will also move the sorting feature to the student screen, adding a small icon next to the add and filter icons."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Navigate to Student Detail (Priority: P1)

A teacher taps a student's name on the Students screen and is taken to that student's dedicated detail screen, where they can see all information about that specific student.

**Why this priority**: This is the foundational interaction that all other functionality on the Student Detail screen depends on. Without navigation to the screen, nothing else can be accessed.

**Independent Test**: Tapping any student name on the Students screen opens a new screen scoped to that student, showing their name and basic information. This delivers immediate navigational value even before other detail sections are built.

**Acceptance Scenarios**:

1. **Given** the Students screen is displayed with a list of students, **When** the user taps a student's name, **Then** the app navigates to a Student Detail screen scoped to that student.
2. **Given** the Student Detail screen is open, **When** the user presses the back button or uses a back navigation gesture, **Then** the app returns to the Students screen.
3. **Given** the Students screen is in selection mode, **When** the user taps a student's name (not a long press), **Then** the student is toggled (selected/deselected) rather than navigating to the detail screen.

---

### User Story 2 - Long Press Starts Selection Mode (Priority: P2)

A teacher long-presses a student's name on the Students screen to begin the multi-selection process, mirroring the existing behavior but now that tapping navigates away.

**Why this priority**: The long press is the entry point to the bulk-action flow (delete, export). Preserving this pattern ensures teachers who rely on bulk operations are not blocked.

**Independent Test**: Long-pressing any student item enters selection mode, visually highlights the student as selected, and shows the multi-selection header. This can be tested without any detail screen implementation.

**Acceptance Scenarios**:

1. **Given** the Students screen is in normal mode, **When** the user long-presses a student item, **Then** the app enters multi-selection mode with that student already selected.
2. **Given** multi-selection mode is active, **When** the user taps additional student items, **Then** those students are toggled in/out of the selection set.
3. **Given** multi-selection mode is active, **When** the user presses the back button, **Then** selection mode is cancelled and the screen returns to normal mode.

---

### User Story 3 - Edit Student Name (Priority: P2)

On the Student Detail screen, a teacher can update the student's name via a dialog directly on that screen.

**Why this priority**: The student detail screen consolidates all per-student editing. Centralizing name edits here removes the need for modals on the Students list screen and makes the experience more discoverable.

**Independent Test**: From the Student Detail screen, the teacher can tap the student's name or an edit icon to open a dialog, change the name, and confirm. The updated name reflects immediately on the detail screen and back in the student list.

**Acceptance Scenarios**:

1. **Given** the Student Detail screen is open, **When** the user taps the student's name or an edit icon, **Then** a modal/bottom-sheet dialog opens pre-filled with the current name, with explicit Save and Cancel buttons.
2. **Given** the edit dialog is visible, **When** the user enters a new name and taps Save, **Then** the student's name is updated and the detail screen reflects the new name immediately.
3. **Given** the edit dialog is visible, **When** the user taps Cancel, **Then** the dialog is dismissed and no change is made.

---

### User Story 3b - Delete Student from Detail Screen (Priority: P2)

From the Student Detail screen, a teacher can delete the student by tapping a delete icon in the top bar. A confirmation dialog prevents accidental deletions. On confirmation, the student and all their attendance records are permanently removed and the app navigates back to the Students screen.

**Why this priority**: Providing a delete action on the detail screen removes the need to navigate back to the Students screen and use multi-selection just to delete a single student. It is grouped at P2 alongside other detail-screen editing capabilities.

**Independent Test**: From the Student Detail screen, the teacher taps the delete icon, a confirmation dialog appears, and on confirming, the student is gone from the Students list and the app returns to it.

**Acceptance Scenarios**:

1. **Given** the Student Detail screen is open, **When** the user taps the delete icon in the top bar, **Then** a confirmation dialog appears asking the teacher to confirm permanent deletion.
2. **Given** the confirmation dialog is visible, **When** the user confirms deletion, **Then** the student and all their attendance records are permanently deleted and the app navigates back to the Students screen.
3. **Given** the confirmation dialog is visible, **When** the user cancels, **Then** the dialog is dismissed and the Student Detail screen remains open with no changes.

---

### User Story 4 - View Student Attendance Report (Priority: P2)

On the Student Detail screen, a teacher can view a full attendance history and report for that specific student, replacing the need to visit the dedicated Reports screen.

**Why this priority**: Moving the per-student attendance report to the detail screen means the Reports screen (which shows all students) becomes redundant. The per-student view is more directly useful during one-on-one reviews.

**Independent Test**: The Student Detail screen displays the interactive calendar grid for the student, filterable by month, without requiring any other screen. The calendar grid is the same component used on the Reports screen today.

**Acceptance Scenarios**:

1. **Given** a student has recorded attendance entries, **When** the teacher views that student's detail screen, **Then** the attendance days are displayed on the interactive calendar grid, scoped to the selected month.
2. **Given** the attendance section is visible, **When** the teacher selects a month filter, **Then** only attendance records for that month are shown.
3. **Given** a student has no attendance records, **When** viewing their detail screen, **Then** a clear empty-state message is shown in the attendance section.
4. **Given** the attendance report is visible, **When** the teacher taps a share or export option, **Then** the student's attendance calendar image can be shared via the system share sheet.

---

### User Story 5 - Sort Students from Students Screen (Priority: P3)

A sort icon is added to the Students screen's top bar (alongside the existing add and filter icons), allowing teachers to sort the student list without leaving the screen.

**Why this priority**: Sorting is a quality-of-life feature that was previously only accessible from the Reports screen. Moving it to the Students screen makes list management more efficient. It is lower priority because the list is still usable without sorting.

**Independent Test**: The sort icon appears in the Students screen top bar. Tapping it opens a DropdownMenu with sort options (e.g., Name, Absence Count), and selecting an option reorders the student list accordingly.

**Acceptance Scenarios**:

1. **Given** the Students screen is displayed, **When** the user views the top bar, **Then** a sort icon is visible alongside the existing add and filter icons.
2. **Given** the sort icon is tapped, **When** the dropdown menu opens and a sort option is selected, **Then** the student list reorders immediately according to the selected sort criterion.
3. **Given** a sort order is active, **When** the user navigates away and returns to the Students screen, **Then** the previously selected sort order is preserved.

---

### User Story 6 - Reports Screen Retired (Priority: P3)

The dedicated Reports screen (previously the third pager page) is removed from the navigation. Students and Calendar remain as a two-page swipeable horizontal pager — the same interaction pattern as today, just with one fewer page. The Student Detail screen is pushed on top of the pager via a back-stack host.

**Why this priority**: Removing the Reports screen simplifies navigation and eliminates a now-redundant surface. Keeping the pager preserves the familiar swipe-to-navigate feel teachers already know. It is addressed last after the Student Detail screen fully covers all report functionality.

**Independent Test**: The Reports screen no longer appears as a swipeable page. Swiping navigates only between Students and Calendar. All report functionality (per-student attendance, sharing) is accessible exclusively from the Student Detail screen.

**Acceptance Scenarios**:

1. **Given** the app is launched, **When** the user swipes between screens, **Then** only Students and Calendar screens are present (no Reports page).
2. **Given** any report functionality previously on the Reports screen, **When** the teacher needs it, **Then** it is accessible from the Student Detail screen.

---

### Edge Cases

- What happens when a student's name is cleared during editing? The Save button in the dialog should be disabled until the name field contains at least one non-whitespace character.
- What happens if navigation to the Student Detail screen occurs while multi-selection mode is active? Tapping during selection mode should toggle selection, not navigate.
- What happens if the attendance history spans many months? The interactive calendar grid shows one month at a time, so the teacher navigates months via the filter; the grid must stay performant without UI jank.
- What happens when a student is deleted via the detail screen's delete action? A confirmation dialog must be shown first; on confirmation, the student is permanently removed and the app navigates back to the Students screen.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST navigate to a Student Detail screen when a student item is tapped in normal (non-selection) mode on the Students screen.
- **FR-002**: The system MUST initiate multi-selection mode when a student item is long-pressed on the Students screen, selecting that student as the first item.
- **FR-003**: The Student Detail screen MUST display the student's name and assigned class.
- **FR-004**: The Student Detail screen MUST allow the teacher to edit the student's name via a modal/bottom-sheet dialog containing a pre-filled text field and explicit Save and Cancel buttons; the change MUST be persisted only when Save is confirmed.
- **FR-005**: The Student Detail screen MUST display the student's full attendance history using a view-only (read-only) interactive calendar grid component, with the ability to filter by month; the grid defaults to the current month when the screen is opened. The calendar grid is the same visual component currently used on the Reports screen.
- **FR-006**: The Student Detail screen MUST provide the ability to share or export the student's attendance calendar image.
- **FR-007**: The Students screen MUST display a sort icon in the top bar alongside the existing add and filter icons.
- **FR-008**: Tapping the sort icon on the Students screen MUST open a DropdownMenu allowing the teacher to change the student list sort order (e.g., by name, by absence count).
- **FR-009**: The selected sort order on the Students screen MUST persist across navigation (leaving and returning to the screen within the same session).
- **FR-010**: The Reports screen MUST be removed from the main navigation once the Student Detail screen covers all report functionality.
- **FR-011**: The system MUST prevent saving an empty or whitespace-only student name during editing on the Student Detail screen.
- **FR-012**: The Student Detail screen MUST provide a clear back navigation control — a top-bar back button, the system back button, or a back gesture — that returns to the Students screen in its previous state (including active sort/filter and pager position).
- **FR-013**: The Student Detail screen MUST provide a delete icon in the top bar that, when tapped, presents a confirmation dialog before permanently deleting the student and all their attendance records, then navigates back to the Students screen.
- **FR-014**: The Student Detail screen MUST provide a dedicated "Change Class" action that allows the teacher to assign, change, or clear the student's assigned class.

### Key Entities *(include if feature involves data)*

- **Student**: A person enrolled in classes. Attributes: unique ID, name, optional class association. The detail screen is scoped to one student at a time. No profile picture attribute.
- **AttendanceRecord**: A record of a student's attendance on a specific date. Attributes: student ID, date, status. Used to populate the attendance history on the Student Detail screen.
- **SortPreference**: The currently active sort criterion applied to the student list on the Students screen. Attributes: sort type (e.g., name, absence count), sort direction (ascending/descending).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A teacher can navigate from the Students screen to a specific student's detail screen in a single tap, with the detail screen loading within 1 second on a mid-range device.
- **SC-002**: A teacher can update a student's name from the Student Detail screen in under 30 seconds, including opening the dialog, editing, and confirming, without returning to any other screen.
- **SC-003**: The full attendance history for a student is reachable on the Student Detail screen by navigating months, with the month filter showing only the selected month's attendance days.
- **SC-004**: Long-pressing a student item enters multi-selection mode within 500ms, and the selected item is visually distinguished from non-selected items.
- **SC-005**: The sort icon on the Students screen is discoverable without instruction, placed in the top bar with an icon consistent with the app's visual language.
- **SC-006**: Removing the Reports screen does not leave any orphaned navigation paths or broken links within the app.
- **SC-007**: The Students screen and Student Detail screen remain fully accessible via TalkBack, with all interactive elements having meaningful content descriptions.

## Assumptions

- The `Student` domain model requires no structural changes — the existing `id`, `name`, and `classId` fields are sufficient; no new fields are added for this feature.
- The attendance data displayed on the Student Detail screen will reuse the existing attendance repository and use cases; no new data source is required.
- The share/export functionality on the Student Detail screen will reuse the existing calendar image generation and share use case logic from the Reports screen.
- The sorting options on the Students screen will mirror the sort types already available on the Reports screen (e.g., by name, by absence count); no new sort criteria will be introduced.
- The existing multi-selection flow (bulk delete, export) is preserved exactly as-is; only the entry point changes from a single tap to a long press.
- The HorizontalPager for Students ↔ Calendar is retained as-is (two pages, same swipe feel). A back-stack navigation host is layered around the pager so the Student Detail screen can be pushed on top and popped back to the Students pager page. This is a prerequisite architectural change.
- The Reports screen and all its associated files (ViewModel, State, Event, components, dialogs) will be deleted as part of this feature; any unique functionality not covered by the Student Detail screen must be identified and migrated before deletion.
- Arabic string localizations must be provided for all new user-facing strings introduced by this feature.
