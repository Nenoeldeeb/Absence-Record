# Interface Contracts: Unified Error Mapping

This document specifies the contracts and API signatures for error handling.

## 1. Extension Function Contract: `toUiText`

Every `StudentError` instance must be mappable to a `UiText` representation for localized UI presentation.

```kotlin
package dev.nenoeldeeb.education.absencerecord.presentation.utils

import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError

fun StudentError.toUiText(): UiText
```

### Signature Mapping

- `StudentError.Database` -> `UiText.StringResource(R.string.error_database_operation_failed)`
- `StudentError.DuplicateClass` -> `UiText.StringResource(R.string.error_class_name_already_exists)`
- `StudentError.FileRead` -> `UiText.StringResource(R.string.error_reading_file)`
- `StudentError.FileWrite` -> `UiText.StringResource(R.string.error_writing_file)`
- `StudentError.ImportParse` -> `UiText.StringResource(R.string.error_parsing_import_file)`
- `StudentError.ReportGeneration` -> `UiText.StringResource(R.string.error_generating_report)`
- `StudentError.Validation(msg)` -> `UiText.DynamicString(msg)`
- `StudentError.Cancelled` -> `UiText.StringResource(R.string.file_selection_cancelled)`

---

## 2. Repository Interface Exception Guarantees

All repository functions that return `Result<T>` or `Flow<Result<T>>` must guarantee that any failure is wrapped in a subclass of `StudentError`.

### Example Contract for flow-based repository calls:
```kotlin
override fun getAllStudents(): Flow<Result<List<Student>>> {
    return studentDao.getAllStudents()
        .map { entities -> Result.success(entities.map { it.toStudent() }) }
        .catch { emit(Result.failure(StudentError.Database)) }
}
```

### Example Contract for suspend repository calls:
```kotlin
override suspend fun insertStudent(student: Student): Result<Long> {
    return runCatching {
        studentDao.insertStudent(student.toStudentEntity())
    }.fold(
        onSuccess = { Result.success(it) },
        onFailure = { Result.failure(StudentError.Database) }
    )
}
```
