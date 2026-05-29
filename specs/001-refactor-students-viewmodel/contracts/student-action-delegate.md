# Contract: StudentActionDelegate

**File**: `presentation/screens/students/delegates/StudentActionDelegate.kt`

## Purpose
Encapsulate student-centric business operations (CRUD, bulk actions, import/export) so the ViewModel only orchestrates state transitions.

## Dependencies
- `StudentManagementUseCases` (DI via constructor)
- `StudentClassRepository` (for class name lookup in export)
- `ImportExportDelegate` (for `buildImportResultMessage` in `performImport`)

## Constructor
```kotlin
class StudentActionDelegate(
    private val studentManagementUseCases: StudentManagementUseCases,
    private val importExportDelegate: ImportExportDelegate
)
```

## Methods

### addStudent
```kotlin
suspend fun addStudent(name: String, classId: Int?): Result<UiText?>
```
- Validates: `name` is not blank — returns `Result.failure(StudentError.Validation)` if blank
- Calls `AddStudentUseCase`
- Returns success toast `UiText` or `StudentError`

### updateStudent
```kotlin
suspend fun updateStudent(student: Student, newName: String, newClassId: Int?): Result<UiText?>
```
- Validates: `newName` is not blank — returns `Result.failure(StudentError.Validation)` if blank
- Calls `UpdateStudentUseCase` with updated `Student` copy
- Returns success toast `UiText` or `StudentError`

### deleteSelectedStudents
```kotlin
suspend fun deleteSelectedStudents(selectedIds: Set<Int>, allStudents: List<Student>): Result<UiText?>
```
- Filters `allStudents` by `selectedIds`
- Calls `DeleteStudentsUseCase`
- Returns success toast `UiText` or `StudentError`

### exportSelectedStudents
```kotlin
suspend fun exportSelectedStudents(uriString: String, selectedIds: Set<Int>, allStudents: List<Student>): Result<UiText?>
```
- Filters students by selection
- Calls `ExportStudentsUseCase`
- Returns success toast `UiText` or `StudentError`

### exportAndDeleteSelectedStudents
```kotlin
suspend fun exportAndDeleteSelectedStudents(uriString: String, selectedIds: Set<Int>, allStudents: List<Student>): Result<UiText?>
```
- Exports first, then deletes only if export succeeds
- Two-phase: if export fails, do NOT delete (prevents data loss)
- Returns combined success toast or first failure

### prepareImportSelectionDialog
```kotlin
suspend fun prepareImportSelectionDialog(uriString: String): Result<List<ParsedStudentImportData>>
```
- Phase 1 of import: reads file via `StorageRepository`, parses JSON via `ImportStudentsUseCase.parseFile`
- Returns parsed data list for preview dialog
- Returns `StudentError.FileRead` if file unreadable, `StudentError.ImportParse` if JSON invalid

### performImport
```kotlin
suspend fun performImport(parsedStudents: List<ParsedStudentImportData>, selectionMap: Map<Int, Boolean>): Result<UiText?>
```
- Phase 2 of import: calls `ImportStudentsUseCase.performImport`
- Filters `parsedStudents` by `selectionMap`
- Returns `ImportResult` formatted as success toast via `ImportExportDelegate.buildImportResultMessage`

## Error Mapping
All failures from use cases are caught and mapped to `StudentError` variants before returning to ViewModel.

## Testability
- All use cases are constructor-injected (no internal instantiation)
- Each method is independently testable with mocked `StudentManagementUseCases`
- Validation-only methods (blank name checks) testable without mocks
