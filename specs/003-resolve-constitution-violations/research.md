# Research: Resolve Constitution Violations

**Date**: 2026-07-03 | **Branch**: `003-resolve-constitution-violations` | **Spec**: `specs/003-resolve-constitution-violations/spec.md`

## Overview

Comprehensive audit of all directly detectable constitution violations across the codebase. 10 files exceed the 300-line limit (range 313-607 lines). 5 additional non-file-size violations found (architecture, accessibility, style).

---

## Violation Type 1: Production Source File Size

### File: `StudentsViewModel.kt` (321 lines)

**Decision**: Extract import/export event handling to `handlers/ImportExportHandler.kt` and bulk-action event handling to `handlers/BulkActionHandler.kt`.

**Rationale**:
- Already follows delegate pattern with `delegates/` subdirectory — extending with `handlers/` is consistent
- Import flow events (PrepareImportSelectionDialog, PerformImport, etc.) are tightly coupled
- Bulk action events (Delete/Export selected students) are another cohesive group
- Reduces VM to ~210 lines, well under 300

**Alternatives Considered**:
- Extract only one group (not enough, would remain ~256 lines with only 20-line buffer)
- Extract all event handling (over-engineered, remaining 17 events stay lean)

### File: `ComposeCalendar.kt` (316 lines)

**Decision**: Extract `DayCell` composable to `DayCell.kt` (top-level composable, change from `private` to `internal`).

**Rationale**:
- DayCell is a leaf-level composable (single calendar day button) — natural extraction boundary
- After extraction, original file is ~254 lines (under 300)
- Enables isolated testing of DayCell

**Alternatives Considered**:
- Also extract `CalendarGrid` (optional, not needed for line count compliance)
- Extract `calculateDaysInMonth` to utility file (adds flexibility but not required)

---

## Violation Type 2: Test ViewModel File Size

### File: `StudentsViewModelTest.kt` (607 lines)

**Decision**: Split into base class + 3 focused subclasses, following existing `CalendarViewModelTestBase` pattern.

- `StudentsViewModelTestBase.kt` (~75 lines): shared setup, mock wiring, `createViewModel()`
- `StudentsViewModelCrudTest.kt` (~220 lines): init, add, update, delete, state updates
- `StudentsViewModelImportTest.kt` (~140 lines): prepare import, perform import
- `StudentsViewModelExportTest.kt` (~150 lines): export, export-and-delete

**Rationale**: Mirror the proven base-class pattern from calendar screen tests. Each file covers a distinct domain capability with minimal boilerplate duplication.

### File: `ReportViewModelTest.kt` (313 lines)

**Decision**: Extract sharing tests to `ReportViewModelSharingTest.kt`.

- `ReportViewModelTestBase.kt` (~75 lines): shared setup with `mockkStatic(Uri::class)`
- `ReportViewModelTest.kt` (~150 lines): SelectStudentForHistory, state updates, init failures
- `ReportViewModelSharingTest.kt` (~175 lines): all 4 PrepareCalendarImageForSharing tests

**Rationale**: Only 14 lines over the limit; a modest split suffices. Sharing tests have unique setup (`mockkStatic`) that benefits from isolation.

---

## Violation Type 3: Test Screen File Size

### File: `CalendarScreenTest.kt` (456 lines)

**Decision**: Split by thematic group — display/template tests vs. interaction/edge-case tests.

- `AttendanceDialogDisplayTest.kt` (~219 lines): formatted date, attendance count, student list, close button, content descriptions
- `CalendarScreenTest.kt` (~251 lines): toggle callbacks, error state, empty state, multiple students, count updates

**Rationale**: Natural grouping (passive display verification vs. active behavior testing). Both files sit comfortably under 300 lines.

### File: `ReportScreenTest.kt` (330 lines)

**Decision**: Split by component — one test file per production composable.

- `ReportControlsTest.kt` (~176 lines): ReportControls tests
- `StudentHistoryDialogTest.kt` (~126 lines): StudentHistoryDialog tests
- `CalendarPreviewDialogTest.kt` (~87 lines): CalendarPreviewDialog tests

**Rationale**: Mirrors production code structure where each composable is already a separate file. Maximum discoverability.

---

## Violation Type 4: Test DAO File Size

### File: `AttendanceDaoTest.kt` (356 lines)

**Decision**: Extract query-only test regions to `AttendanceDaoQueryTest.kt`.

- `AttendanceDaoTest.kt` (~175 lines): CRUD tests (getAttendanceForDate, insertAttendance, deleteAttendance)
- `AttendanceDaoQueryTest.kt` (~195 lines): Query tests (getStudentAttendanceDates, getAttendanceHistoryForDateRange, getDistinctDatesWithAttendance)

**Rationale**: Natural boundary between write-focused (CRUD) and read-focused (query) tests.

### File: `StudentDaoTest.kt` (320 lines)

**Decision**: Extract sorting/analytics query tests to `StudentDaoSortingTest.kt`.

- `StudentDaoTest.kt` (~150 lines): CRUD tests (getAllStudents, insertStudent, updateStudent, deleteStudents)
- `StudentDaoSortingTest.kt` (~195 lines): Sorting tests (getAllStudentsSortedByAttendance, getStudentsActiveInMonthSortedByName, getAllStudentsSortedByAttendanceForMonth)

**Rationale**: Sorting tests involve JOIN-based analytics distinctly different from basic CRUD.

---

## Violation Type 5: Test UseCase File Size

### File: `ImportStudentsUseCaseTest.kt` (353 lines)

**Decision**: Split each `@Nested` inner class into its own top-level test file.

- `ImportStudentsUseCaseParseFileTest.kt` (~115 lines): ParseFileTests (4 tests)
- `ImportStudentsUseCasePerformImportTest.kt` (~250 lines): PerformImportTests (8 tests)

**Rationale**: Each inner class already forms a natural test boundary. Duplicating setup (~10-16 lines per file) is simpler than creating a shared base class for only 2 files. Under 300 lines with room to grow.

---

## Violation Type 6: Test Utility File Size

### File: `CalendarImageGeneratorTest.kt` (383 lines)

**Decision**: Extract month handling and student name tests into separate focused files.

- `CalendarImageGeneratorTest.kt` (~170 lines): bitmap creation (3), marked days (4), content validation (1)
- `CalendarImageGeneratorMonthHandlingTest.kt` (~134 lines): month handling tests (6)
- `CalendarImageGeneratorStudentNameTest.kt` (~104 lines): student name tests (4)

**Rationale**: Thematic cohesion — each new file serves a single, clearly named concern. All three files stay under 300 lines with headroom.

---

## Violation Type 7: Non-File-Size Violations

### P1 (Must fix)

| Violation | File | Fix |
|-----------|------|-----|
| Domain imports Presentation | `domain/models/StudentError.kt:4` imports `presentation.utils.UiText` | Move `toUiText()` extensions to `presentation/utils/StudentErrorUiMapper.kt` |
| try-catch in Domain | `domain/usecases/transfer/ImportStudentsUseCase.kt` (6 sites) | Move error handling to repository implementations |
| try-catch in Domain | `domain/usecases/transfer/ExportStudentsUseCase.kt` (2 sites) | Move error handling to `StorageRepositoryImpl.writeTextToUri()` |
| try-catch in Presentation | `presentation/screens/report/ReportViewModel.kt:248,287` | Remove outer try-catch; inner `Result.onSuccess`/`onFailure` already covers errors |
| try-catch in Presentation | `presentation/screens/report/ReportScreen.kt:52,59` | Move error handling to ViewModel via event |
| Non-single-action UseCase | `domain/usecases/transfer/ImportStudentsUseCase.kt` (has `parseFile` + `performImport`) | Split into `ParseImportFileUseCase` and `PerformImportUseCase` |

### P2 (Should fix)

| Violation | File | Fix |
|-----------|------|-----|
| Empty content description | `presentation/screens/components/ComposeCalendar.kt:246` | Provide descriptive string for all calendar day cells |

### P3 (Nice to fix)

| Violation | File | Fix |
|-----------|------|-----|
| `@Stable` on ViewModels | `CalendarViewModel.kt:26`, `StudentsViewModel.kt:50`, `ReportViewModel.kt:33` | Remove `@Stable` from VM classes (belongs on ScreenState only) |

### Confirmed Compliant

- ViewModel/ScreenState/ScreenEvent naming suffixes ✅
- `collectAsStateWithLifecycle()` ✅
- `_uiState.update { it.copy(...) }` ✅
- `viewModelScope.launch` (no raw `Job`) ✅
- `kotlinx-datetime` (no `java.time`) ✅
- `strings.xml` + `UiText` (no hardcoded strings) ✅
- `@Serializable` on export models ✅
- `AppContainer` + `AppViewModelProvider` DI ✅
- Repository interfaces in `domain/`, impls in `data/` ✅
- DAO methods as `suspend` or `Flow` ✅
- `Result<T>` return type standardization ✅
- `withContext` for main-safety in data layer ✅
- Tests test both `onSuccess` and `onFailure` ✅
- `mockk(relaxed = true)` + `runTest` ✅

---

## Summary of Changes

| Category | Files Over Limit | New Files Needed | Files Removed |
|----------|-----------------|------------------|---------------|
| Production source | 2 | 3 | 0 |
| Test ViewModel | 2 | 5 | 2 (original split files) |
| Test Screen | 2 | 3 | 0 |
| Test DAO | 2 | 2 | 0 |
| Test UseCase | 1 | 2 | 1 |
| Test Utility | 1 | 2 | 0 |
| Non-file-size | — | 2 (UseCase split) + 1 (UiMapper) | 1 (original ImportStudentsUseCase) |
| **Total** | **10** | **18 new files** | **3 deleted** |
