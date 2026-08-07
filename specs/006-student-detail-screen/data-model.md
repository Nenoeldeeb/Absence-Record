# Phase 1 Data Model & State Contracts: Student Detail Screen

**Feature Branch**: `006-student-detail-screen` | **Date**: 2026-08-05

## Domain & Data Entities

### 1. Student (`dev.nenoeldeeb.education.absencerecord.domain.models.Student`)

No database schema or model changes required.

```kotlin
data class Student(
    val id: Int = 0,
    val name: String,
    val classId: Int? = null
)
```

### 2. SortType (`dev.nenoeldeeb.education.absencerecord.domain.models.SortType`)

Existing 2-value enum reused for sorting students (NOT expanded to a 4-value enum — see `tasks.md` conventions):

```kotlin
enum class SortType {
    ByName,
    ByAttendance
}
```

---

## Presentation State Models

### 1. StudentDetailScreenState (`dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.StudentDetailScreenState`)

```kotlin
@Stable
data class StudentDetailScreenState(
    val student: Student? = null,
    val assignedClassName: String? = null,
    val availableClasses: List<StudentClass> = emptyList(),
    val allAttendanceDates: List<LocalDate> = emptyList(),
    val availableMonths: List<LocalDate> = emptyList(),
    // null -> ComposeCalendar defaults to the current month on open (FR-005)
    val selectedMonth: LocalDate? = null,
    val isMonthDropdownExpanded: Boolean = false,
    val isEditNameDialogOpen: Boolean = false,
    val isChangeClassDialogOpen: Boolean = false,
    val isDeleteConfirmationDialogOpen: Boolean = false,
    val isLoading: Boolean = true,
    val error: UiText? = null,
    val toastMessage: UiText? = null,
    val shareFileUri: Uri? = null
)
```

### 2. StudentDetailScreenEvent (`dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.StudentDetailScreenEvent`)

```kotlin
sealed interface StudentDetailScreenEvent {
    data object ToggleEditNameDialog : StudentDetailScreenEvent
    data class UpdateStudentName(val newName: String) : StudentDetailScreenEvent
    data object ToggleChangeClassDialog : StudentDetailScreenEvent
    data class UpdateStudentClass(val classId: Int?) : StudentDetailScreenEvent
    data object ToggleDeleteConfirmationDialog : StudentDetailScreenEvent
    data object ConfirmDeleteStudent : StudentDetailScreenEvent
    data class SelectMonth(val month: LocalDate?) : StudentDetailScreenEvent
    data class ToggleMonthDropdown(val expanded: Boolean) : StudentDetailScreenEvent
    data object ShareAttendanceReport : StudentDetailScreenEvent
    data class ShareFileResult(val uri: Uri, val error: UiText?) : StudentDetailScreenEvent
    data object ConsumeError : StudentDetailScreenEvent
    data object ConsumeToastMessage : StudentDetailScreenEvent
}
```

### 3. StudentDetailUiEffect (`dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.StudentDetailUiEffect`)

```kotlin
sealed interface StudentDetailUiEffect {
    data object NavigateBack : StudentDetailUiEffect
}
```

---

## Updated StudentsScreenState & Events

### StudentsScreenState Additions

```kotlin
// Additional fields in StudentsScreenState:
val sortType: SortType = SortType.ByName,
val isSortMenuExpanded: Boolean = false
```

### StudentsScreenEvent Additions

```kotlin
// Additional events in StudentsScreenEvent:
data class UpdateSortType(val sortType: SortType) : StudentsScreenEvent
data class ToggleSortMenuExpanded(val expanded: Boolean) : StudentsScreenEvent
// NOTE: Navigation to Student Detail is NOT a StudentsScreenEvent; it is a
// composable callback `onStudentClick: (Int) -> Unit` threaded from AppNavigation.
```

---

## State Transition Diagrams

```
[Normal Mode - StudentsScreen]
       │
       ├─ (Single Tap Student) ─────────► [Navigate to StudentDetailScreen]
       │                                            │
       │                                            ├─ (Tap Name/Edit) ──► EditNameDialog
       │                                            ├─ (Tap Change Class) ► ChangeClassDialog
       │                                            ├─ (Tap Delete) ─────► DeleteConfirmationDialog ──► (Confirm) ──► [Delete & Pop Back]
       │                                            └─ (Tap Back) ───────► [Pop Back to StudentsScreen]
       │
       └─ (Long Press Student) ─────────► [Enter Multi-Selection Mode]
```
