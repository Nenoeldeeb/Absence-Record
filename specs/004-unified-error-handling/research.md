# Phase 0 Research: Unified Error Handling

## 1. UI Presentation of Transient Errors
- **Decision**: Transient errors (database read/write failures, file IO errors) will be displayed to the user using Android's standard **Snackbar** component.
- **Rationale**: Snackbars provide a non-blocking, transient feedback mechanism aligned with Material Design principles. It notifies the user of the error without disrupting their current task flow.
- **Alternatives Considered**: 
  - *Modal Dialog*: Rejected because modal dialogs are blocking and require explicit user action (dismissal), causing high user friction for non-critical, transient failures.
  - *Inline Error Cards*: Rejected because they alter the screen layout dynamically and can cause jarring visual layout shifts.

## 2. Telemetry and Logging Strategy for Caught Exceptions
- **Decision**: **Silent Catch** pattern will be implemented at the repository level. Caught system exceptions (such as SQLite exceptions, IO exceptions, and serialization exceptions) will not generate any remote/telemetry log files or persistent local log entries.
- **Rationale**: This respects the explicit privacy/architecture constraint of avoiding unnecessary network telemetry or local trace logs, focusing strictly on returning typed `StudentError` instances to update the UI error states.
- **Alternatives Considered**:
  - *Firebase Crashlytics / Remote Logging*: Rejected based on user instruction to avoid tracking/telemetry.
  - *Logcat Logging*: Useful for development, but in release builds, errors are caught silently and propagated solely through the UI state.

## 3. Mapping of Data-Source Exceptions to StudentError Subclasses
- **Decision**: Map Room SQLite/Database exceptions to `StudentError.Database`. Map duplicate class constraint checks to `StudentError.DuplicateClass`. Map file reading and import/export issues to `StudentError.FileRead` and a new `StudentError.FileWrite`. Map report generation failures to a new `StudentError.ReportGeneration`.
- **Rationale**: Keeps domain layers and presentation layers fully decoupled from Android, Room, or standard Java exception classes. Ensures 100% main safety and strict type safety.
- **Alternatives Considered**:
  - *Failing with generic Exception*: Rejected because it leaks technical details and violates the Clean Architecture and MVI core principles.
