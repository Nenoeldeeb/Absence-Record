# Data Model: Refactor Students ViewModel

**Branch**: `001-refactor-students-viewmodel` | **Date**: 2026-05-29

## Domain Models (existing, unchanged)

### Student
| Field | Type | Notes |
|-------|------|-------|
| `id` | `Int` | Primary key, auto-generated |
| `name` | `String` | Non-empty, validated at ViewModel layer |
| `classId` | `Int?` | Nullable — unassigned students |

### StudentClass
| Field | Type | Notes |
|-------|------|-------|
| `id` | `Int` | Primary key, auto-generated |
| `name` | `String` | Unique (collation NOCASE), validated at ViewModel layer |

### StudentExportData (JSON serialization model)
| Field | Type | Notes |
|-------|------|-------|
| `name` | `String` | Student name |
| `className` | `String` | Class name (empty if unassigned) |
| `dates` | `List<String>` | Attendance date strings |
*Annotated with `@Serializable` for kotlinx.serialization JSON.*

### ParsedStudentImportData
| Field | Type | Notes |
|-------|------|-------|
| `originalData` | `StudentExportData` | Raw parsed data |
| `id` | `Int` | In-memory identity for selection tracking |

### ImportResult
| Field | Type | Notes |
|-------|------|-------|
| `newStudentsCount` | `Int` | Students created |
| `existingStudentsMergedCount` | `Int` | Existing students with merged attendance |
| `datesProcessedCount` | `Int` | Attendance records added |
| `datesSkippedCount` | `Int` | Duplicate attendance skipped |

### ClassFilter (presentation-level)
| Variant | Description |
|---------|-------------|
| `All` | No filter — show all students |
| `Unassigned` | Students with `classId == null` |
| `ByClass(studentClass)` | Students in a specific class |

## New Domain Model

### StudentError (sealed interface)
| Variant | Meaning | Trigger |
|---------|---------|---------|
| `Database` | General DB failure | Any repository call fails |
| `DuplicateClass` | Class name already exists | `AddClassUseCase` / `UpdateClassUseCase` unique constraint |
| `FileRead` | Cannot read import file | `StorageRepository.readTextFromUri` fails |
| `ImportParse` | JSON parse failure | `kotlinx.serialization` deserialization fails |
| `Validation` | Input validation failure | Blank name in add/update student or class name |
| `Cancelled` | User cancelled file picker | Null/invalid URI from activity result |

*Maps 1:1 to UiText resources in the ViewModel.*

## Screen State Model

### StudentsScreenState (existing, with modifications)
| Field | Type | Owner | Notes |
|-------|------|-------|-------|
| `allStudents` | `List<Student>` | ViewModel | From `GetAllStudentsUseCase` |
| `availableClasses` | `List<StudentClass>` | ViewModel | From `GetAllClassesUseCase` |
| `newStudentName` | `String` | ViewModel | Add-dialog input binding |
| `selectedClassFilter` | `ClassFilter` | ViewModel | Filter state |
| `classDropdownExpanded` | `Boolean` | ViewModel | UI toggle |
| `isClassFilterVisible` | `Boolean` | ViewModel | UI toggle |
| `showManageClassesDialog` | `Boolean` | ViewModel | Dialog toggle |
| `isMultiSelectionMode` | `Boolean` | ViewModel | Via SelectionStateDelegate |
| `selectedStudentIds` | `Set<Int>` | ViewModel | Via SelectionStateDelegate |
| `showImportSelectionDialog` | `Boolean` | ViewModel | Import phase 1 complete |
| `showBulkDeleteDialog` | `Boolean` | ViewModel | Dialog toggle |
| `showEditDialog` | `Student?` | ViewModel | Non-null = dialog open |
| `showAddStudentDialog` | `Boolean` | ViewModel | Dialog toggle |
| ~~`exportSelectionMap`~~ | ~~`Map<Int, Boolean>`~~ | ~~REMOVED~~ | Dead code — never written to |
| `parsedStudentsFromFile` | `List<ParsedStudentImportData>?` | ViewModel | Import phase 1 result |
| `importSelectionMap` | `Map<Int, Boolean>` | ViewModel | Via ImportExportDelegate |
| `toastMessage` | `UiText?` | ViewModel | Consumed via `ConsumeToastMessage` |
| `error` | `UiText?` | ViewModel | Error display |

## Delegate Contracts

### StudentActionDelegate
| Method | Input | Returns | Use Cases Called |
|--------|-------|---------|-----------------|
| `addStudent(name, classId)` | `String`, `Int?` | `Result<UiText?>` | `AddStudentUseCase` |
| `updateStudent(student, newName, newClassId)` | `Student`, `String`, `Int?` | `Result<UiText?>` | `UpdateStudentUseCase` |
| `deleteSelectedStudents(ids, allStudents)` | `Set<Int>`, `List<Student>` | `Result<UiText?>` | `DeleteStudentsUseCase` |
| `exportSelectedStudents(uri, ids, allStudents)` | `String`, `Set<Int>`, `List<Student>` | `Result<UiText?>` | `ExportStudentsUseCase` |
| `exportAndDeleteSelectedStudents(uri, ids, allStudents)` | `String`, `Set<Int>`, `List<Student>` | `Result<UiText?>` | `ExportStudentsUseCase` + `DeleteStudentsUseCase` |
| `prepareImportSelectionDialog(uri)` | `String` | `Result<List<ParsedStudentImportData>>` | `ImportStudentsUseCase.parseFile` |
| `performImport(parsed, selectionMap)` | `List<ParsedStudentImportData>`, `Map<Int, Boolean>` | `Result<UiText?>` | `ImportStudentsUseCase.performImport` |

### ClassActionDelegate
| Method | Input | Returns | Use Cases Called |
|--------|-------|---------|-----------------|
| `addClass(name)` | `String` | `Result<UiText?>` | `AddClassUseCase` |
| `renameClass(studentClass, newName)` | `StudentClass`, `String` | `Result<UiText?>` | `UpdateClassUseCase` |
| `deleteClass(studentClass)` | `StudentClass` | `Result<UiText?>` | `DeleteClassUseCase` |
| `onDeletedClassFilterFallback(currentFilter, deletedClass)` | `ClassFilter`, `StudentClass` | `ClassFilter` | Pure function — no use case |
