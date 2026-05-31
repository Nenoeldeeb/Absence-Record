# Contract: ClassActionDelegate

**File**: `presentation/screens/students/delegates/ClassActionDelegate.kt`

## Purpose
Encapsulate class management operations (CRUD, filter fallback) so the ViewModel only orchestrates state transitions.

## Dependencies
- `ClassManagementUseCases` (DI via constructor)

## Constructor
```kotlin
class ClassActionDelegate(
    private val classManagementUseCases: ClassManagementUseCases
)
```

## Methods

### addClass
```kotlin
suspend fun addClass(name: String): Result<UiText?>
```
- Validates: `name` is not blank — returns `Result.failure(StudentError.Validation)` if blank
- Calls `AddClassUseCase`
- DB unique constraint violation → mapped to `StudentError.DuplicateClass`
- Returns success toast `UiText` or `StudentError`

### renameClass
```kotlin
suspend fun renameClass(studentClass: StudentClass, newName: String): Result<UiText?>
```
- Validates: `newName` is not blank — returns `Result.failure(StudentError.Validation)` if blank
- Calls `UpdateClassUseCase` with updated `StudentClass` copy
- DB unique constraint violation → mapped to `StudentError.DuplicateClass`
- Returns success toast `UiText` or `StudentError`

### deleteClass
```kotlin
suspend fun deleteClass(studentClass: StudentClass): Result<UiText?>
```
- Calls `DeleteClassUseCase`
- Returns success toast `UiText` or `StudentError`

### onDeletedClassFilterFallback
```kotlin
fun onDeletedClassFilterFallback(currentFilter: ClassFilter, deletedClass: StudentClass): ClassFilter
```
- Pure function (no suspend, no IO)
- If `currentFilter` is `ByClass(deletedClass)` → returns `ClassFilter.All`
- Otherwise returns `currentFilter` unchanged

## Error Mapping
All failures from use cases are caught and mapped to `StudentError` variants before returning to ViewModel.

## Testability
- `ClassManagementUseCases` is constructor-injected
- `onDeletedClassFilterFallback` is a pure function — trivially testable without mocks
- Each method independently testable with mocked use cases
