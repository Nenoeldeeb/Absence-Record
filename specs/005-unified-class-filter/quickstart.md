# Quickstart & Manual Validation Guide: Unified Class Filter System

## Overview

This guide provides step-by-step verification procedures to manually and programmatically validate the unified class filter system across Attendance dialog, Report screen, and Students screen.

## Prerequisites

- Android Studio / Gradle environment configured.
- Emulator or test device running Android API 26+.

## Validation Scenarios

### Scenario 1: Initial Cold Start & Unassigned Students Default

1. Launch the app fresh (cold start).
2. Open the **Students screen** or **Report screen**.
3. Open the class filter dropdown.
4. **Expected Result**:

- All class checkboxes are unchecked.
- The button label shows `No class selected` (`class_filter_none`).
- The list displays only unassigned students (students with no assigned class).

### Scenario 2: Synchronized Filtering Across Screens

1. On the **Students screen**, click the class filter dropdown and check **Class A** (ID 1).
2. Verify the Students screen list updates to show only students in Class A.
3. Navigate to the **Report screen**.
4. Open the class filter dropdown.
5. **Expected Result**:

- **Class A** checkbox is checked, and all other checkboxes remain unchecked.
- The Report screen reflects data for Class A.

6. Open the **Attendance dialog** (on Calendar screen).
7. **Expected Result**:

- **Class A** checkbox is checked in the dialog filter control as well.

### Scenario 3: Viewing All Enrolled Students

1. Open the class filter dropdown on any screen (Attendance dialog, Report, or Students).
2. Check **ALL** individual class checkboxes.
3. **Expected Result**:

- The dropdown list displays no "Select All" entry.
- The screen list updates to display all enrolled students across all checked classes.
- Unassigned students are not included when all class checkboxes are checked.

### Scenario 4: Automated Verification Commands

Run unit tests to verify ViewModel state synchronization and filter extension behavior:

```bash
# Run Unit Tests for Class Filter Repository & ViewModels
./gradlew testDebugUnitTest --tests "*ClassFilterRepositoryTest*"
./gradlew testDebugUnitTest --tests "*StudentsViewModelTest*"
./gradlew testDebugUnitTest --tests "*ReportViewModelTest*"
./gradlew testDebugUnitTest --tests "*CalendarViewModelTest*"

# Run ktlint formatting check & project validation
./gradlew check
```
