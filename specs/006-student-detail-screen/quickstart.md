# Quickstart Validation Guide: Student Detail Screen

**Feature Branch**: `006-student-detail-screen` | **Date**: 2026-08-05

## Overview

This guide outlines end-to-end verification scenarios to validate the Student Detail screen, navigation, sorting, and retirement of the Reports screen.

---

## Prerequisites

1. Android Studio or Gradle CLI configured (`./gradlew`).
2. An Android Emulator or connected physical device (API 26+).
3. The project compiled cleanly via `./gradlew assembleDebug`.

---

## Validation Scenarios

### Scenario 1: Navigate to Student Detail Screen & Back

1. Launch app. Ensure you are on the `Students` screen.
2. Tap on any student name in the list.
3. **Expected Result**: App navigates to `StudentDetailScreen` showing student's name, assigned class name, and view-only interactive `ComposeCalendar` grid.
4. Tap the back button in the top app bar (or use back gesture).
5. **Expected Result**: App pops back to `StudentsScreen` with list state preserved.

### Scenario 2: Multi-Selection Mode Entry via Long Press

1. On `StudentsScreen`, perform a long-press on a student name.
2. **Expected Result**: Selection mode activates; student item is visually selected; multi-selection header appears.
3. Tap another student name.
4. **Expected Result**: Student toggles selection (does NOT navigate to detail screen).
5. Press back button. Selection mode exits.

### Scenario 3: Edit Student Name from Detail Screen

1. Navigate to a student's `StudentDetailScreen`.
2. Tap the student's name or edit icon in the top bar.
3. **Expected Result**: Modal dialog opens with pre-filled name text field and Save/Cancel buttons.
4. Clear text and attempt to tap Save.
5. **Expected Result**: Save button is disabled.
6. Enter a new name (e.g. "Jane Doe Updated") and tap Save.
7. **Expected Result**: Dialog dismisses; top bar title updates immediately; popping back to `StudentsScreen` reflects the new name.

### Scenario 4: Change Student Class from Detail Screen

1. On `StudentDetailScreen`, tap the "Change Class" icon in the top bar.
2. **Expected Result**: Class selection modal dialog opens.
3. Select a different class or clear class assignment, and confirm.
4. **Expected Result**: Detail screen updates assigned class tag immediately.

### Scenario 5: View & Share Attendance Report

1. On `StudentDetailScreen`, view the `ComposeCalendar` grid. It opens on the **current month** by default.
2. Tap the month dropdown filter and select a specific month.
3. **Expected Result**: Grid updates to display attendance dates for that month only.
4. Tap the Share button.
5. **Expected Result**: Android system share sheet launches to share generated attendance report image.

### Scenario 6: Delete Student from Detail Screen

1. On `StudentDetailScreen`, tap the Delete icon in the top bar.
2. **Expected Result**: Confirmation dialog opens warning of permanent removal.
3. Tap Confirm.
4. **Expected Result**: Student and all associated attendance records are deleted; app navigates back to `StudentsScreen`; deleted student is no longer present in list.

### Scenario 7: Sort Students from Students Screen

1. On `StudentsScreen`, tap the Sort icon in top app bar.
2. **Expected Result**: `DropdownMenu` opens with options: Name, Absence Count.
3. Select "Absence Count".
4. **Expected Result**: Student list reorders with students having highest absence counts at top.

### Scenario 8: Reports Screen Retired

1. Swipe left/right on main screen.
2. **Expected Result**: Only 2 pages exist in `HorizontalPager`: Students and Calendar. Reports page is completely gone.

---

## Automated Testing Validation Commands

```bash
# Run Unit Tests for ViewModels & UseCases
./gradlew testDebugUnitTest --tests "dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.*"

# Run Ktlint and Lint checks
./gradlew ktlintCheck
./gradlew lintDebug

# Run Full Verification Check
./gradlew check
```
