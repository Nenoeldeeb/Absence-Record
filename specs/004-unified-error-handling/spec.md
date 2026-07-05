# Feature Specification: Unified Error Handling

**Feature Branch**: `004-unified-error-handling`

**Created**: 2026-07-05

**Status**: Draft

**Input**: User description: "I want to unify error handling via StudentError unified interface Let's expand it if needed also apply it whereever it used"

## Clarifications

### Session 2026-07-05

- Q: How should transient errors (such as database read/write failures or file import/export errors) be presented to the user in the UI? → A: Snackbar: A brief message at the bottom of the screen that disappears automatically, with an optional "Retry" action.
- Q: How should internal system exceptions/failures (like database errors or file parsing failures) be logged or tracked? → A: Silent Catch: Silence errors at repository level (no logs generated) and rely only on UI error states.


## User Scenarios & Testing *(mandatory)*

### User Story 1 - Graceful Database Failure Handling (Priority: P1)

As a user, when a database operation fails (e.g. loading students, saving attendance, or modifying a class), I want to see a clear, localized error message instead of an app crash or technical database jargon, so I can understand what happened without confusion.

**Why this priority**: High priority because unhandled database errors degrade user trust and can cause app crashes.
**Independent Test**: Can be tested by mocking repository failures in the ViewModel unit tests and checking that the ViewModel updates the UI state with a localized `UiText.StringResource` matching the expected database error message.

**Acceptance Scenarios**:
1. **Given** the database is corrupt or encounters an IO exception, **When** the app attempts to load the students list, **Then** the UI displays the database error message "A database operation failed" (localized) and does not crash.
2. **Given** the database fails to write, **When** recording attendance for a student, **Then** the UI displays an error alert with "A database operation failed" and the app state remains consistent.

---

### User Story 2 - Duplicate Class Creation Prevention (Priority: P1)

As a user, when I try to create or rename a class to a name that already exists, I want to see a specific warning telling me the class name is a duplicate, so I don't accidentally create redundant classes.

**Why this priority**: Business logic check that prevents data pollution.
**Independent Test**: Can be verified by trying to add a class with an existing name and confirming that the returned result is a failure containing `StudentError.DuplicateClass`.

**Acceptance Scenarios**:
1. **Given** a class named "Class A" already exists, **When** I try to add a new class named "Class A", **Then** the app rejects the operation and displays "A class with this name already exists".
2. **Given** a class named "Class B" already exists, **When** I try to rename "Class C" to "Class B", **Then** the app rejects the operation and displays "A class with this name already exists".

---

### User Story 3 - Robust File Import/Export Error Handling (Priority: P2)

As a user, when importing or exporting student data, I want any file reading, formatting, or parsing errors to be handled cleanly and mapped to user-friendly messages, so I know if the file is invalid, corrupted, or if I cancelled the operation.

**Why this priority**: Essential for data portability and external file interaction.
**Independent Test**: Mocking file read exceptions or invalid JSON strings in use cases and verifying that they return `StudentError.FileRead` or `StudentError.ImportParse` failures.

**Acceptance Scenarios**:
1. **Given** I am importing a file with invalid JSON format, **When** the parsing use case runs, **Then** it returns a failure containing `StudentError.ImportParse` and the UI shows "Error parsing import file".
2. **Given** I open the file selector and press back, **When** the import operation is cancelled, **Then** it returns `StudentError.Cancelled` and the UI shows "Operation cancelled".

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: All repositories MUST catch data-source exceptions (e.g. SQLiteException, Room exceptions, IOException) and wrap them as `Result.failure(StudentError.Database)` or other appropriate `StudentError` subclasses.
- **FR-002**: No raw throwables (such as `java.io.IOException`, `kotlinx.serialization.SerializationException`, or generic `java.lang.Exception`) MUST escape the Data layer repositories or UseCases.
- **FR-003**: The `StudentError` sealed class MUST act as the unified error hierarchy for all domain, data, and presentation error handling.
- **FR-004**: All UseCases returning a `Result<T>` MUST guarantee that the failure exception is an instance of `StudentError`.
- **FR-005**: All ViewModels (Calendar, Report, Students) MUST observe UseCase outcomes and map `StudentError` to UI-friendly localized messages using a unified mapper (`StudentErrorUiMapper` / `toUiText()`).
- **FR-006**: The system MUST support localized Arabic versions for all mapped error messages via `values-ar/strings.xml`.
- **FR-007**: Validation logic in ViewModels or delegates (e.g. blank student names, empty class names) MUST return `StudentError.Validation` with the specific validation message.
- **FR-008**: Transient errors (such as database read/write failures or file import/export errors) MUST be presented to the user via a transient Snackbar with an optional Retry action where feasible, instead of blocking modal dialogs.
- **FR-009**: Caught system exceptions MUST NOT generate any telemetry, remote logs, or persistent local log files, relying solely on returning `StudentError` to populate UI error states.

### Key Entities *(include if feature involves data)*

- **StudentError**: A sealed class extending `Throwable` that defines all specific failure types:
  - `Database`: Database operations, queries, or writes failing.
  - `DuplicateClass`: Class name uniqueness constraint violation.
  - `FileRead`: File accessibility, permission, or opening failures.
  - `ImportParse`: JSON mapping, format, or parsing errors.
  - `Validation`: Input constraints violations (like empty/blank inputs).
  - `Cancelled`: System operations aborted by the user (like file picking).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of data-layer repository methods and use cases that return `Result` have their failure paths verified to return a `StudentError` subclass rather than a raw `Throwable`.
- **SC-002**: 0% of raw system, DB, or serialization exceptions leak to the UI layer without mapping, preventing technical stack traces from being shown to users.
- **SC-003**: All view models (Calendar, Report, Students) are updated to handle the mapped `StudentError` and display localized error notifications.

## Assumptions

- The app uses Room Database for local storage, which can throw standard SQLite/Room exceptions on write/read errors.
- The app uses Kotlin standard `Result<T>` to wrap success and failure values.
- `StudentError` inherits from `Throwable` to comply with Kotlin's `Result.failure()` requirement.
- The UI layer uses `UiText` wrapper to support localized resource string mappings or dynamic validation messages.
