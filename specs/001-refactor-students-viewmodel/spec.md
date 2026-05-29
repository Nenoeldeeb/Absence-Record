# Feature Specification: Refactor Students ViewModel

**Feature Branch**: `001-refactor-students-viewmodel`

**Created**: 2026-05-28

**Status**: Draft

**Input**: User description: "I have @[app/src/main/java/dev/nenoeldeeb/education/absencerecord/presentation/screens/students/StudentsViewModel.kt] contains code over 500 lines. I want to refactor It to be under 300 lines while following best practices."

## Clarifications

### Session 2026-05-28

- Q: What architectural strategy should we use to delegate the ViewModel's business and state logic? → A: Option A - Domain-focused Action Delegates (e.g., StudentActionDelegate, ClassActionDelegate) to handle student list actions (CRUD, multi-selection, bulk operations) and class actions (CRUD, filtering).

### Session 2026-05-29

- Q: What file format should the import/export delegates use? → A: JSON using existing kotlinx.serialization (@Serializable).
- Q: Which code boundaries are explicitly off-limits during this refactoring? → A: ViewModel + new delegate files + test files may be modified/created. UI composables and data layer (repositories, DAOs, Room entities) are out of scope.
- Q: How should delegates differentiate error types for UI presentation? → A: Typed domain errors via a sealed StudentError hierarchy mapped to UiText in the ViewModel.
- Q: How should the import flow (parse → preview → confirm → write) be structured in the delegates? → A: Two-phase approach — phase 1 reads/parses JSON and returns preview data, phase 2 persists the user-confirmed selections.
- Q: Who owns loading state transitions when delegates execute async operations? → A: ViewModel manages all loading states before/after delegate calls; delegates return pure Result<T> with no loading side effects.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Manage Student Roster (Priority: P1)

The user (teacher) can view, add, and edit students in the roster. Adding and editing students must validate that the student's name is not empty or blank, displaying appropriate user-facing messages.

**Why this priority**: Correctly managing the student list is the core functionality of the screen.

**Independent Test**: Can be tested by adding a student, editing their name/class, and verifying the changes are displayed on the screen.

**Acceptance Scenarios**:

1. **Given** the students screen, **When** the user adds a new student with a valid name, **Then** the student is added, a success toast is shown, and the input field is cleared.
2. **Given** the students screen, **When** the user attempts to add or edit a student with an empty/blank name, **Then** an error toast is shown and the operation is aborted.

---

### User Story 2 - Filter and Manage Classes (Priority: P1)

The user can view, add, rename, and delete classes, as well as filter the student list by the selected class.

**Why this priority**: Students are grouped by classes, and filtering is essential for roster navigation.

**Independent Test**: Can be tested by selecting a class filter and verifying only students in that class are listed, and by performing CRUD operations on classes.

**Acceptance Scenarios**:

1. **Given** multiple classes exist, **When** the user selects a class filter, **Then** only students belonging to that class are shown.
2. **Given** the class management dialog, **When** the user renames or deletes a class, **Then** the UI updates to reflect this change, and filtering is adjusted accordingly (e.g. if the currently filtered class is deleted, the filter falls back to "All").

---

### User Story 3 - Bulk Actions on Student Records (Priority: P2)

The user can select multiple students to perform bulk actions such as deletion and export.

**Why this priority**: High convenience for managing larger numbers of students.

**Independent Test**: Can be tested by entering multi-selection mode, selecting multiple students, and executing bulk delete or bulk export.

**Acceptance Scenarios**:

1. **Given** multi-selection mode is active, **When** the user selects "Select All", **Then** all currently visible students are selected.
2. **Given** multiple students are selected, **When** the user confirms bulk deletion, **Then** the selected students are deleted, selection mode is cleared, and a success toast is displayed.

---

### User Story 4 - Import & Export Student Records (Priority: P2)

The user can export selected students to a JSON file (kotlinx.serialization `@Serializable`), or import students from a JSON file, with options to select which students to import.

**Why this priority**: Data portability and easy backup/restore capability.

**Independent Test**: Can be tested by exporting selected records to a file, and importing from a valid file.

**Acceptance Scenarios**:

1. **Given** a valid student data file, **When** the user imports it, **Then** a two-phase delegate operation runs: phase 1 parses the JSON file and returns preview data for the dialog, phase 2 persists the user-confirmed selections.
2. **Given** selected students, **When** the user exports them, **Then** they are written to the selected destination and a success message is displayed.

---

### Edge Cases

- **Database Errors**: DB errors during student/class CRUD, import, or deletion should be gracefully caught and formatted into user-friendly error state messages.
- **Duplicate Class Names**: If the database throws a duplicate name exception, a specific error indicating that the class already exists is displayed instead of a generic failure.
- **Empty / Null Uri on File Actions**: Cancelled file picking or empty/null Uris should not crash the app; they must show a "File selection cancelled" message. The ViewModel MUST check for null/empty URIs in event handlers and return `StudentError.Cancelled` before calling any delegate.
- **Validation Errors**: Blank or whitespace-only student/class names MUST return a `StudentError.Validation` error variant rather than a generic database error or crash.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: StudentsViewModel MUST retain all existing features (student/class CRUD, import/export, class filtering, selection, and dialog state management).
- **FR-002**: Refactored StudentsViewModel codebase MUST be reduced to under 300 lines of code.
- **FR-003**: The refactored ViewModel must comply with MVI/Clean Architecture and coding standards specified in [AGENTS.md](file:///home/nenoeldeeb/Projects/AndroidStudioProjects/Absencerecord/AGENTS.md).
- **FR-004**: All existing unit tests in [StudentsViewModelTest.kt](file:///home/nenoeldeeb/Projects/AndroidStudioProjects/Absencerecord/app/src/test/java/dev/nenoeldeeb/education/absencerecord/presentation/screens/students/StudentsViewModelTest.kt) MUST pass.
- **FR-005**: New unit tests MUST be written for each action delegate, covering both success and failure paths per project testing standards (SC-004 tracks this outcome).
- **FR-006**: All UI-facing string formatting and resource-dependent UI messages MUST use the `UiText` wrapper and `strings.xml` resources.

### Out of Scope

- **UI Layer**: `StudentsScreen.kt` and any other UI composables in the students package MUST NOT be modified.
- **Data Layer**: Repositories, DAOs, Room entities, and data sources MUST NOT be modified.
- **Domain Layer**: Existing use cases and domain models MUST NOT be modified unless a thin adapter is needed for delegate compatibility.

### Key Entities

- **Student**: Represents a student with an ID, name, and optional class ID.
- **StudentClass**: Represents a class with an ID and name.
- **StudentsScreenState**: Immutable state class representing all screen visual components, loading states, errors, dialogs, and selections. Loading state transitions (before/after delegate calls) are managed solely by the ViewModel, not by delegates.
- **StudentsScreenEvent**: Sealed interface defining user interactions dispatched to the ViewModel.
- **StudentError** (sealed interface): Typed domain errors including `Database`, `DuplicateClass`, `FileRead`, `ImportParse`, `Validation`, and `Cancelled` variants. Defined in the domain layer and mapped to `UiText` in the ViewModel.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: The line count of `StudentsViewModel.kt` is strictly under 300 lines.
- **SC-002**: 100% of existing unit tests pass successfully.
- **SC-003**: Zero visual or functional changes observed in the UI components of the Students screen.
- **SC-004**: Each action delegate has corresponding unit tests covering both success and failure paths (verifies FR-005).

## Assumptions

- We can delegate business logic handling (e.g. database transactions, coroutine launches, delegate coordination) to new action delegate classes (`StudentActionDelegate` and `ClassActionDelegate`) to thin out the ViewModel.
- The UI layer (`StudentsScreen.kt`) does not need to change its public interface (all events and state mappings remain the same).
