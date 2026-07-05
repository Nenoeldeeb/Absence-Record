# Quickstart Validation Guide: Unified Error Handling

This document provides step-by-step verification flows to confirm that unified error handling is implemented correctly and data layer exceptions do not leak.

## Verification Prerequisites

- Android Studio / SDK installed.
- Emulator running or physical device connected.
- All code formatted using `./gradlew ktlintFormat`.
- Verification gates passing using `./gradlew check`.

---

## Runnable Verification Scenarios

### Scenario 1: Verify Unit Test Coverage for Repository Errors
Confirm that all repository implementations catch exceptions and correctly return `StudentError.Database`.
- **Command**:
  ```bash
  ./gradlew testDebugUnitTest --tests "dev.nenoeldeeb.education.absencerecord.data.repositories.*"
  ```
- **Expected Outcome**: All repository tests must pass. Specifically, tests for failure paths must verify that:
  - SQLite/Room exceptions are mapped to `StudentError.Database`.
  - Duplicate name checks return `StudentError.DuplicateClass`.

---

### Scenario 2: Verify Unit Test Coverage for Use Case Boundaries
Confirm that all UseCases return failure results carrying `StudentError` instances.
- **Command**:
  ```bash
  ./gradlew testDebugUnitTest --tests "dev.nenoeldeeb.education.absencerecord.domain.usecases.*"
  ```
- **Expected Outcome**: All use case unit tests pass, confirming that no raw database or JSON parser exceptions escape to ViewModels.

---

### Scenario 3: Verify Snackbar Error UI Display (Manual/UI Test)
Confirm that transient database/IO errors trigger a Snackbar instead of a blocking dialog.
- **Setup**: Inject a mock failure into `StudentRepository` for `getAllStudents()`.
- **Action**: Launch the app and observe the main student listing screen.
- **Expected Outcome**: A transient Snackbar appears at the bottom of the screen with the text "Database operation failed." (or translation), preserving the UI layout, and the app does not crash.
