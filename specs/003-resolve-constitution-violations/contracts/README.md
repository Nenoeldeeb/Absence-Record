# Contracts: Resolve Constitution Violations

## Interface Contracts

### Production Layer Contracts

| Contract | Before (violation) | After (compliant) |
|----------|-------------------|-------------------|
| `StudentsViewModel` | 321 lines, `onEvent` handles 30 events inline | ~210 lines, delegates ImportExport + BulkAction to handler classes |
| `ComposeCalendar` | 316 lines, 4 private composables | ~254 lines, `DayCell` extracted to public `internal` composable |

**New classes**:
- `handlers/ImportExportHandler` — handles `PrepareImportSelectionDialog`, `CloseImportSelectionDialog`, `ToggleImportSelection`, `PerformImport`
- `handlers/BulkActionHandler` — handles `DeleteSelectedStudents`, `ExportSelectedStudents`, `ExportAndDeleteSelectedStudents`
- `DayCell.kt` — extracted `DayCell` composable (changed from `private` to `internal`)

### Test Layer Contracts

| Test File | Lines | Split Strategy | New Files |
|-----------|-------|---------------|-----------|
| `StudentsViewModelTest.kt` | 607 | Base class + 3 subclasses | `TestBase`, `Crud`, `Import`, `Export` |
| `ReportViewModelTest.kt` | 313 | Base class + 2 subclasses | `TestBase`, `Sharing` |
| `CalendarScreenTest.kt` | 456 | By thematic group | `DisplayTest` |
| `ReportScreenTest.kt` | 330 | By component | `ReportControls`, `StudentHistoryDialog`, `CalendarPreviewDialog` |
| `AttendanceDaoTest.kt` | 356 | CRUD vs Query | `QueryTest` |
| `StudentDaoTest.kt` | 320 | CRUD vs Sorting | `SortingTest` |
| `ImportStudentsUseCaseTest.kt` | 353 | By @Nested class | `ParseFile`, `PerformImport` |
| `CalendarImageGeneratorTest.kt` | 383 | By test theme | `MonthHandling`, `StudentName` |

### Architecture Contracts

| Violation | Fix | Contract Change |
|-----------|-----|-----------------|
| `StudentError.toUiText()` in domain | Move to `StudentErrorUiMapper.kt` in presentation | Domain no longer imports `presentation.utils.UiText` |
| `try-catch` in `ImportStudentsUseCase` | Move to repository implementations | UseCases rely solely on `Result<T>.onSuccess/onFailure` |
| `try-catch` in `ExportStudentsUseCase` | Move to `StorageRepositoryImpl` | UseCase becomes pure orchestration |
| `try-catch` in `ReportViewModel` | Remove outer try-catch | VM delegates all error handling to use case results |
| `try-catch` in `ReportScreen` | Migrate error to ViewModel event | Composable fires event, VM handles |
| `ImportStudentsUseCase` (2 methods) | Split into `ParseImportFileUseCase` + `PerformImportUseCase` | Each class has single `operator fun invoke` |

### Verification Contract

| Gate | Command | Expected Outcome |
|------|---------|-----------------|
| Formatting | `./gradlew ktlintFormat` | No changes (all files already formatted) |
| File size audit | `find ... -name "*.kt" -exec wc -l {} + | awk '$1>300'` | Empty output |
| Full validation | `./gradlew check` | BUILD SUCCESSFUL |
| Accessibility | Static review of touched UI | No violations |
