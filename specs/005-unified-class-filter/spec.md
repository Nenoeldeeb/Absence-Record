# Feature Specification: Unified Class Filter System

**Feature Branch**: `005-unified-class-filter`

**Created**: 2026-07-31

**Status**: Draft

**Input**: User description: "i have in this project two different class filter systems, one in attendance dialog another in report and students screens. i want to replace the second with first one. also i want to link all three where if i change the filter in one it should apply to other two. Note that the default filter state is non checked that shows unassigned students. If i want to show all students i should check all filters. no need to create extra select all filter entry."

## Clarifications

### Session 2026-07-31

- Q: Scope of "All Students" when All Classes Checked → A: Display strictly students enrolled in checked classes; unassigned students shown only when 0 checkboxes are checked.
- Q: Filter Persistence on Cold Start → A: Always reset to default state (all checkboxes unchecked) on app cold start; runtime synchronization applies within the session.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Multi-Select Class Filtering on Report and Students Screens (Priority: P1)

As a user viewing the Report screen or Students screen, I want to use the multi-select checkbox class filter (the same component used in the Attendance dialog) so that I have a consistent filtering experience across the entire application.

**Why this priority**: Unifies the UI design and interaction model across all app screens, eliminating confusion caused by inconsistent dropdown controls.

**Independent Test**: Open the Report screen or Students screen, click the class filter dropdown, and verify that it displays multi-select checkboxes matching the Attendance dialog format instead of the old single-select dropdown.

**Acceptance Scenarios**:

1. **Given** I am on the Report or Students screen, **When** I open the class filter dropdown, **Then** I see a list of classes with individual checkboxes for each class.
2. **Given** I am selecting class filters, **When** I check one or multiple class checkboxes, **Then** the screen updates to display records for only the selected classes.

---

### User Story 2 - Synchronized Class Filter Across All Views (Priority: P1)

As a user navigating between the Attendance dialog, Report screen, and Students screen, I want any change made to the class filter in one view to automatically apply to the other two views so that my filter context remains consistent throughout my session.

**Why this priority**: Prevents disjointed views where different screens display mismatched sets of student data when switching tasks.

**Independent Test**: Select specific class checkboxes in the Attendance dialog, navigate to the Report screen and Students screen, and verify that the exact same class checkboxes are selected and applied.

**Acceptance Scenarios**:

1. **Given** I change the class filter selection in the Attendance dialog, **When** I navigate to the Report screen or Students screen, **Then** the class filter control shows the updated selection and filters the data accordingly.
2. **Given** I change the class filter selection in the Students screen, **When** I open the Attendance dialog in the Calendar screen or view the Report screen, **Then** the filter state reflects the changes made in the Students screen.

---

### User Story 3 - Default Unchecked State and Unassigned Student View (Priority: P2)

As a user opening the application or clearing class filter selections, I want all class checkboxes to be unchecked by default—showing only unassigned students—and to be able to see all students by explicitly checking all class checkboxes without needing an explicit "Select All" entry.

**Why this priority**: Establishes predictable default filtering rules and clean UI without redundant control entries.

**Independent Test**: Uncheck all class filter options and verify that only unassigned students (students without a class) are displayed. Check all class options and verify that all students are displayed.

**Acceptance Scenarios**:

1. **Given** the app is launched or all class filters are cleared, **When** I view any of the three screens, **Then** all class filter checkboxes are unchecked and only unassigned students are displayed.
2. **Given** all class filter options are unchecked, **When** I check every individual class checkbox, **Then** all students across all classes are displayed.
3. **Given** I view the class filter options list, **When** I inspect the list entries, **Then** there is no "Select All" entry in the dropdown list.

---

### Edge Cases

- **Class Addition/Deletion**: When a new class is added or an existing class is deleted in the system, the global filter state gracefully updates without selecting non-existent classes.
- **No Unassigned Students Exist**: When all class checkboxes are unchecked and no unassigned students exist in the system, screens display an appropriate empty state indicating no unassigned students found.
- **All Classes Unchecked vs Partial Check**: Checking 2 out of 5 classes shows only students in those 2 classes; checking 0 classes shows only students without any class assignment.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The Report screen and Students screen MUST replace their existing class filter control with the multi-select checkbox class filter dropdown component used in the Attendance dialog.
- **FR-002**: The class filter selection state MUST be shared globally across the Attendance dialog (Calendar screen), Report screen, and Students screen.
- **FR-003**: Updating the class filter selection in any one of the three views MUST immediately update the filter state and data view in the other two screens/dialogs.
- **FR-004**: The default initial state of the class filter MUST have all class options unchecked.
- **FR-005**: When all class filter checkboxes are unchecked (empty selection), the application MUST display only unassigned students (students not assigned to any class).
- **FR-006**: To view all enrolled students across all classes, users MUST check all individual class filter options.
- **FR-007**: The class filter options list MUST NOT contain a dedicated "Select All" item or toggle entry.
- **FR-008**: When a subset of class checkboxes is selected, the application MUST display only the students assigned to those selected classes.
- **FR-009**: The synchronized class filter state MUST reset to the default state (all class checkboxes unchecked) upon application cold restart.

### Key Entities

- **Class Filter State**: Represents the active collection of selected class identifiers shared globally across the application. An empty collection indicates filtering for unassigned students.
- **Student Assignment**: The relationship linking a student to a specific class or marking them as unassigned (no class).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% UI consistency achieved by using the multi-select checkbox class filter component across Attendance dialog, Report screen, and Students screen.
- **SC-002**: 100% filter synchronization accuracy across all three views when switching between screens during a session.
- **SC-003**: Correct data filtering validation: 0 checked options displays unassigned students only, N checked options displays students for those N classes only, and all checked options displays all students.
- **SC-004**: Zero extra "Select All" entries present in the class filter dropdown list across all screens.

## Assumptions

- The list of available classes is centrally managed and uniform across all screens.
- Filter selections persist in runtime state across screen navigation within the app session, but reset to default (unchecked) on cold start.
- Unassigned students are identified as students who do not belong to any active class ID.
