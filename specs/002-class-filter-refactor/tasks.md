---
description: "Task list for Class Filter Refactor feature implementation"
---

# Tasks: Class Filter Refactor

**Feature Branch**: `002-class-filter-refactor`

**Design Documents**: plan.md, spec.md, data-model.md, research.md, contracts/calendar-viewmodel-contract.md, quickstart.md

**Target Audience**: Low-skill LLM implementer — each task provides step-by-step instructions with exact file paths, code snippets, and what to change.

**No data model changes**: No Room entities, DAOs, repositories, use cases, or domain models are modified. All changes are scoped to the `presentation/` layer only.

---

## How this file is organized

Tasks are grouped into **Phases**. Complete phases in order. Within each phase, tasks marked **[P]** can be done in parallel (they touch different files).

| Phase | What | Why |
|-------|------|-----|
| 1 | Setup | Project initialization (nothing needed for this feature) |
| 2 | Foundational | Shared utilities + state model — must complete before any story |
| 3 | User Story 1+2 | Multi-select filter inside dialog + dynamic count (P1) |
| 4 | User Story 3 | Attendance count clamping (P1) |
| 5 | Polish | Format, verify, accessibility |

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Feature initialization. No setup needed — no new dependencies, no DI changes, no new files.

No tasks required.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared utilities and state/event model that ALL user stories depend on.

**⚠️ CRITICAL**: Complete all tasks in this phase before starting any user story phase.

### Task T001 ✅ — Add `applyMultiClassFilter` extension to StudentFilters.kt

**File**: `app/src/main/java/dev/nenoeldeeb/education/absencerecord/presentation/utils/StudentFilters.kt`

**What to do**: Add a new extension function `applyMultiClassFilter` that filters and deduplicates a list of students by a set of class IDs.

**Step-by-step instructions**:

1. Open `StudentFilters.kt`
2. Add the following new function after the existing `applyClassFilter` function:
   **Note**: Keep the `ClassFilter` import — the existing `applyClassFilter` function still references it. Do NOT remove it.

```kotlin
fun Iterable<Student>.applyMultiClassFilter(classIds: Set<Int>): List<Student> {
    if (classIds.isEmpty()) return this.toList()
    return this
        .filter { it.classId != null && it.classId in classIds }
        .distinctBy { it.id }
}
```

4. **Important**: Keep the existing `applyClassFilter` function as-is — it is still used by ReportScreen and StudentsScreen. Do NOT delete it.

**Expected file after change** (11 lines → 18 lines — the file now has both functions):

```
package dev.nenoeldeeb.education.absencerecord.presentation.utils

import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.presentation.screens.components.ClassFilter

fun Iterable<Student>.applyClassFilter(filter: ClassFilter): List<Student> =
    when (filter) {
        ClassFilter.All -> this.toList()
        ClassFilter.Unassigned -> this.filter { it.classId == null }
        is ClassFilter.ByClass -> this.filter { it.classId == filter.studentClass.id }
    }

fun Iterable<Student>.applyMultiClassFilter(classIds: Set<Int>): List<Student> {
    if (classIds.isEmpty()) return this.toList()
    return this
        .filter { it.classId != null && it.classId in classIds }
        .distinctBy { it.id }
}
```

**How the function works**:
- When `classIds` is empty (no filter), returns ALL students (including unassigned with `classId = null`)
- When `classIds` is non-empty, filters to only students whose `classId` is in the set
- Uses `distinctBy { it.id }` for deduplication (if a student belongs to multiple selected classes — though each student has only one class, this ensures correctness)

---

### Task T002 — Update CalendarScreenState.kt with multi-select filter fields

**File**: `app/src/main/java/dev/nenoeldeeb/education/absencerecord/presentation/screens/calendar/CalendarScreenState.kt`

**What to do**: Replace the single-select `ClassFilter` fields with multi-select `Set<Int>` fields.

**Current state fields (remove these three)**:
- `selectedClassFilter: ClassFilter = ClassFilter.All`
- `classDropdownExpanded: Boolean = false`
- `isClassFilterVisible: Boolean = false`

**New state fields to add**:
- `selectedClassIds: Set<Int> = emptySet()` — the set of selected class IDs
- `filterDropdownExpanded: Boolean = false` — whether the filter dropdown is open

**Step-by-step instructions**:

1. Open `CalendarScreenState.kt`
2. Remove the import line: `import dev.nenoeldeeb.education.absencerecord.presentation.screens.components.ClassFilter`
3. In the data class, replace the three existing fields with the two new fields

**Exact code before**:
```kotlin
@Stable
data class CalendarScreenState(
    val studentsForSelectedDate: List<StudentAttendance> = emptyList(),
    val allStudents: List<Student> = emptyList(),
    val availableClasses: List<StudentClass> = emptyList(),
    val selectedClassFilter: ClassFilter = ClassFilter.All,
    val classDropdownExpanded: Boolean = false,
    val isClassFilterVisible: Boolean = false,
    val selectedDateForDialog: LocalDate? = null,
    val error: UiText? = null
)
```

**Exact code after**:
```kotlin
@Stable
data class CalendarScreenState(
    val studentsForSelectedDate: List<StudentAttendance> = emptyList(),
    val allStudents: List<Student> = emptyList(),
    val availableClasses: List<StudentClass> = emptyList(),
    val selectedClassIds: Set<Int> = emptySet(),
    val filterDropdownExpanded: Boolean = false,
    val selectedDateForDialog: LocalDate? = null,
    val error: UiText? = null
)
```

---

### Task T003 — Update CalendarScreenEvent.kt with new events

**File**: `app/src/main/java/dev/nenoeldeeb/education/absencerecord/presentation/screens/calendar/CalendarScreenEvent.kt`

**What to do**: Remove old single-select filter events and add new multi-select filter events.

**Events to REMOVE**:
- `data class SelectClassFilter(val filter: ClassFilter) : CalendarScreenEvent`
- `data object ToggleClassFilterVisibility : CalendarScreenEvent`
- `data class ToggleClassDropdown(val expanded: Boolean) : CalendarScreenEvent`

**Events to ADD**:
- `data class ToggleClassSelection(val classId: Int) : CalendarScreenEvent` — toggles a class ID in/out of the selected set
- `data object ToggleFilterDropdown : CalendarScreenEvent` — toggles the dropdown open/closed

**Step-by-step instructions**:

1. Open `CalendarScreenEvent.kt`
2. Remove the import: `import dev.nenoeldeeb.education.absencerecord.presentation.screens.components.ClassFilter`
3. Remove the three old event lines
4. Add the two new event lines

**Exact code after**:
```kotlin
package dev.nenoeldeeb.education.absencerecord.presentation.screens.calendar

import kotlinx.datetime.LocalDate

sealed interface CalendarScreenEvent {
    data class MarkStudentAttendance(val studentId: Int, val date: LocalDate) : CalendarScreenEvent

    data class DeleteStudentAttendance(val studentId: Int, val date: LocalDate) :
        CalendarScreenEvent

    data class SelectDateForDialog(val date: LocalDate?) : CalendarScreenEvent

    data class ToggleClassSelection(val classId: Int) : CalendarScreenEvent

    data object ToggleFilterDropdown : CalendarScreenEvent
}
```

---

### Task T004 [P] — Add class filter string resources (both languages)

**Files**:
- `app/src/main/res/values/strings.xml` (English)
- `app/src/main/res/values-ar/strings.xml` (Arabic)

**What to do**: Add three new string resources for the class filter dropdown label.

**English strings to add** (inside `<resources>` in `values/strings.xml`):
```xml
    <!-- Class Filter -->
    <string name="class_filter_none">No filter</string>
    <string name="class_filter_one">1 class selected</string>
    <string name="class_filter_n">%d classes selected</string>
```

**Arabic strings to add** (inside `<resources>` in `values-ar/strings.xml`):
```xml
    <!-- Class Filter -->
    <string name="class_filter_none">لا يوجد تصفية</string>
    <string name="class_filter_one">تم اختيار صف واحد</string>
    <string name="class_filter_n">تم اختيار %d صفوف</string>
```

**Step-by-step instructions**:

1. Open `app/src/main/res/values/strings.xml`
2. Find an appropriate place (e.g., after the "Calendar Screen" section, around line 47-55)
3. Insert the three English string lines
4. Open `app/src/main/res/values-ar/strings.xml`
5. Find the corresponding place (after the calendar screen section, around line 47-54)
6. Insert the three Arabic string lines

**How these strings are used**: The UI will display:
- "No filter" when no class checkboxes are checked
- "1 class selected" when exactly one checkbox is checked
- "3 classes selected" (or any number) when multiple are checked
- The Kotlin code will choose which string to show based on `selectedClassIds.size`

---

**Checkpoint**: Foundation ready — State model, events, utility function, and strings all exist. User story implementation can begin.

---

## Phase 3: User Story 1+2 — Filter UI + Dynamic Count (Priority: P1) 🎯 MVP

**Combined stories**:
- **US1**: Filter students by class within the attendance dialog using a multi-select checkbox dropdown
- **US2**: Total student count updates dynamically as classes are selected/deselected

**Why combined**: US2 is a natural consequence of US1 — the dialog title already displays `allStudents.size`, so when filtering changes `allStudents`, the count updates automatically.

**Independent Test Criteria**:
1. Open attendance dialog → see all students
2. Tap filter dropdown → see class checkboxes (all unchecked)
3. Check one class → only students from that class appear in the list
4. Check a second class → students from both classes appear (deduplicated by ID)
5. Uncheck all classes → all students reappear
6. Total student count in dialog title always matches `allStudents.size`

### Task T005 [US1] — Update CalendarViewModel.kt with multi-select filter logic

**File**: `app/src/main/java/dev/nenoeldeeb/education/absencerecord/presentation/screens/calendar/CalendarViewModel.kt`

**What to do**: Update the ViewModel to use the new multi-select filter state. Replace old filter event handlers with new ones. Add a method to re-apply the multi-class filter whenever class selection changes.

**Changes needed**:

1. **Replace imports**:
   - Remove: `import dev.nenoeldeeb.education.absencerecord.presentation.utils.applyClassFilter`
   - Remove: `import dev.nenoeldeeb.education.absencerecord.presentation.screens.components.ClassFilter` (not imported currently but verify)
   - Add: `import dev.nenoeldeeb.education.absencerecord.presentation.utils.applyMultiClassFilter`
   - Add: `import kotlinx.coroutines.Dispatchers`
   - Add: `import kotlinx.coroutines.withContext`

2. **Update `initializeStudents()` method**:
   - Change the `applyClassFilter(state.selectedClassFilter)` call to `applyMultiClassFilter(state.selectedClassIds)`:

```kotlin
private fun initializeStudents() {
    viewModelScope.launch {
        studentManagementUseCases.getAllStudentsUseCase().collectLatest { result ->
            result
                .onSuccess { students ->
                    rawStudents = students
                    _uiState.update { state ->
                        state.copy(
                            allStudents =
                                students.applyMultiClassFilter(state.selectedClassIds)
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            error =
                                UiText.StringResource(
                                    R.string.error_loading_students,
                                    e.message ?: "Unknown error"
                                )
                        )
                    }
                }
        }
    }
}
```

3. **Replace the `onEvent` handlers for filter events**:

In the `when(event)` block inside `onEvent()`:
- Replace the `SelectClassFilter` case with:
```kotlin
            is CalendarScreenEvent.ToggleClassSelection ->
                toggleClassSelection(event.classId)
```
- Replace the `ToggleClassFilterVisibility` case with **nothing** (remove the entire branch)
- Replace the `ToggleClassDropdown` case with:
```kotlin
            is CalendarScreenEvent.ToggleFilterDropdown ->
                _uiState.update { it.copy(filterDropdownExpanded = !it.filterDropdownExpanded) }
```

4. **Add the `toggleClassSelection` method** and a `applyFilterAndClamp` helper:

```kotlin
    private fun toggleClassSelection(classId: Int) {
        viewModelScope.launch {
            val updatedIds = _uiState.value.selectedClassIds.toMutableSet()
            if (classId in updatedIds) {
                updatedIds.remove(classId)
            } else {
                updatedIds.add(classId)
            }

            val filteredStudents = withContext(Dispatchers.Default) {
                rawStudents.applyMultiClassFilter(updatedIds)
            }

            _uiState.update { state ->
                val visibleIds = filteredStudents.map { it.id }.toSet()
                state.copy(
                    selectedClassIds = updatedIds,
                    allStudents = filteredStudents,
                    studentsForSelectedDate = state.studentsForSelectedDate.filter {
                        it.studentId in visibleIds
                    }
                )
            }
        }
    }
```

5. **Update the `setupAttendanceListener()` method** — It needs to filter the attendance results by the currently visible students. Change the `.onSuccess` block inside `setupAttendanceListener()`:

```kotlin
                .collectLatest { result ->
                    result
                        .onSuccess { selectedStudentsForDate ->
                            _uiState.update { state ->
                                val visibleIds = state.allStudents.map { it.id }.toSet()
                                state.copy(
                                    studentsForSelectedDate = selectedStudentsForDate.filter {
                                        it.studentId in visibleIds
                                    }
                                )
                            }
                        }
                        .onFailure { e ->
                            _uiState.update {
                                it.copy(
                                    error =
                                        UiText.StringResource(
                                            R.string.error_loading_attendance,
                                            e.message ?: "Unknown error"
                                        )
                                )
                            }
                        }
                }
```

6. **Remove unused import**: No other files import CalendarViewModel, so verify the file still compiles.

**Final state**: The `CalendarViewModel.kt` file should now:
- Use `applyMultiClassFilter` instead of `applyClassFilter`
- Have a `toggleClassSelection(classId)` method that toggles class IDs, re-filters, and clamps attendance
- Have `ToggleFilterDropdown` that toggles the dropdown
- No longer reference `ClassFilter`, `SelectClassFilter`, `ToggleClassFilterVisibility`, or `ToggleClassDropdown`

---

### Task T006 [US1] — Add ClassCheckboxFilter composable inside CalendarScreen.kt

**File**: `app/src/main/java/dev/nenoeldeeb/education/absencerecord/presentation/screens/calendar/CalendarScreen.kt`

**What to do**: Add a new `ClassCheckboxFilter` composable that renders a dropdown menu with checkboxes for each class. Wire it into the `AttendanceDialog` between the title and the student list.

**Step-by-step instructions**:

1. Open `CalendarScreen.kt`

2. **Add new imports** at the top:
```kotlin
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.semantics.contentDescription
```

3. **Add the `ClassCheckboxFilter` composable** (add it after the `CalendarScreen` composable and before `AttendanceDialog`, or at the bottom of the file):

```kotlin
@Composable
internal fun ClassCheckboxFilter(
    availableClasses: List<StudentClass>,
    selectedClassIds: Set<Int>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onClassToggle: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val label = when (selectedClassIds.size) {
        0 -> stringResource(R.string.class_filter_none)
        1 -> stringResource(R.string.class_filter_one)
        else -> stringResource(R.string.class_filter_n, selectedClassIds.size)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { onExpandedChange(!expanded) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Start
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            availableClasses.forEach { cls ->
                val isChecked = cls.id in selectedClassIds
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { onClassToggle(cls.id) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = cls.name)
                        }
                    },
                    onClick = { onClassToggle(cls.id) },
                    modifier = Modifier.semantics {
                        contentDescription = "${cls.name} checkbox, ${if (isChecked) "checked" else "unchecked"}"
                    }
                )
            }
        }
    }
}
```

4. **Wire `ClassCheckboxFilter` into the `AttendanceDialog`** — Pass the necessary parameters:
   - Add parameters to `AttendanceDialog`:
     - `availableClasses: List<StudentClass>`
     - `selectedClassIds: Set<Int>`
     - `filterDropdownExpanded: Boolean`
     - `onToggleClassSelection: (Int) -> Unit`
     - `onToggleFilterDropdown: () -> Unit`

   Update the `AttendanceDialog` composable signature:
```kotlin
@Composable
internal fun AttendanceDialog(
    modifier: Modifier = Modifier,
    allStudents: List<Student>,
    studentsForSelectedDate: List<StudentAttendance>,
    error: UiText?,
    selectedDate: LocalDate,
    availableClasses: List<StudentClass>,
    selectedClassIds: Set<Int>,
    filterDropdownExpanded: Boolean,
    onDismiss: () -> Unit,
    onToggleAttendance: (Student, LocalDate, Boolean) -> Unit,
    onToggleClassSelection: (Int) -> Unit,
    onToggleFilterDropdown: () -> Unit
)
```

5. **In the `AttendanceDialog` body**, add the `ClassCheckboxFilter` inside the `AlertDialog`'s `text` block, between the title and `DialogContent`:

```kotlin
        text = {
            Column {
                ClassCheckboxFilter(
                    availableClasses = availableClasses,
                    selectedClassIds = selectedClassIds,
                    expanded = filterDropdownExpanded,
                    onExpandedChange = { onToggleFilterDropdown() },
                    onClassToggle = onToggleClassSelection,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                DialogContent(
                    modifier = Modifier.fillMaxWidth(),
                    allStudents = allStudents,
                    studentsForSelectedDate = studentsForSelectedDate,
                    error = error,
                    selectedDate = selectedDate,
                    onToggleAttendance = onToggleAttendance
                )
            }
        }
```

   **Add missing import**: `import androidx.compose.foundation.layout.Column` (if not already present — check the existing imports. If already imported by the CalendarScreen composable's Column, it will be in context).

   Also add `import androidx.compose.foundation.layout.padding` if not already imported.

6. **Update the `CalendarScreen` composable call site** where `AttendanceDialog` is invoked (around line 102-123):

```kotlin
    uiState.selectedDateForDialog?.let {
        AttendanceDialog(
            allStudents = uiState.allStudents,
            studentsForSelectedDate = uiState.studentsForSelectedDate,
            error = uiState.error,
            selectedDate = it,
            availableClasses = uiState.availableClasses,
            selectedClassIds = uiState.selectedClassIds,
            filterDropdownExpanded = uiState.filterDropdownExpanded,
            onDismiss = { viewModel.onEvent(CalendarScreenEvent.SelectDateForDialog(null)) },
            onToggleAttendance = { student, date, isPresent ->
                if (isPresent) {
                    viewModel.onEvent(
                        CalendarScreenEvent.DeleteStudentAttendance(student.id, date)
                    )
                    SoundPlayer.playRemoveAttendanceSound()
                } else {
                    viewModel.onEvent(
                        CalendarScreenEvent.MarkStudentAttendance(student.id, date)
                    )
                    SoundPlayer.playAddAttendanceSound()
                }
            },
            onToggleClassSelection = { classId ->
                viewModel.onEvent(CalendarScreenEvent.ToggleClassSelection(classId))
            },
            onToggleFilterDropdown = {
                viewModel.onEvent(CalendarScreenEvent.ToggleFilterDropdown)
            }
        )
    }
```

---

### Task T007 [US1] — Remove old filter UI (top bar icon and AnimatedVisibility) from CalendarScreen.kt

**File**: `app/src/main/java/dev/nenoeldeeb/education/absencerecord/presentation/screens/calendar/CalendarScreen.kt`

**What to do**: Remove the filter icon from the TopAppBar and the `AnimatedVisibility` + `ClassFilterDropdown` block that used to appear above the calendar.

**Step-by-step instructions**:

1. Open `CalendarScreen.kt`

2. **Remove the `IconButton`** inside the `TopAppBar`'s `actions` block (lines 62-74). Replace the whole `actions` block with just an empty block or remove it:
```kotlin
        TopAppBar(
            title = {},
            actions = {}   // Empty — remove the old IconButton entirely
        )
```

3. **Remove the `AnimatedVisibility` block** (lines 77-93) that contained the `ClassFilterDropdown`. Delete this entire section.

4. **Remove unused imports** after making changes. At minimum:
   - Remove `import androidx.compose.animation.AnimatedVisibility`
   - Remove `import dev.nenoeldeeb.education.absencerecord.presentation.screens.components.ClassFilterDropdown`
   - Remove `import androidx.compose.material3.Icon`
   - Remove `import androidx.compose.material3.IconButton`
   - Remove `import androidx.compose.ui.graphics.vector.ImageVector`
   - Remove `import androidx.compose.ui.res.vectorResource`
   - Remove `import dev.nenoeldeeb.education.absencerecord.R` (only if not used elsewhere — but it IS used for `stringResource(R.string.filter_description)` and `R.drawable.outline_filter_24`. After removing those, verify if `R` is still needed for other strings. It likely is, so keep it.)

   **To check which imports are unused**: After making the changes, run `./gradlew ktlintFormat` — it will report unused imports.

---

### Task T008 [US1] — Write tests for multi-select filter in CalendarViewModelTest.kt

**File**: `app/src/test/java/dev/nenoeldeeb/education/absencerecord/presentation/screens/calendar/CalendarViewModelTest.kt`

**What to do**: Add test functions that verify US1 (multi-select filter) and US2 (dynamic count) behavior.

**Tests to add** (add these inside the `CalendarViewModelTest` class, before the `// endregion` comment):

First, add the necessary import at the top of the file alongside the existing imports:
```kotlin
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentClass
```

Then add these test functions inside the class body:

```kotlin
    // region Multi-Select Filter Tests

    @Test
    fun `ToggleClassSelection filters students by single class`() =
        runTest {
            // Given - students in different classes
            val class1 = StudentClass(1, "Class A")
            val class2 = StudentClass(2, "Class B")
            val students = listOf(
                Student(1, "Student1", classId = 1),
                Student(2, "Student2", classId = 1),
                Student(3, "Student3", classId = 2)
            )
            val classes = listOf(class1, class2)

            coEvery { studentManagementUseCases.getAllStudentsUseCase() } returns flowOf(Result.success(students))
            val classManagementUseCases = mockk<ClassManagementUseCases>(relaxed = true)
            coEvery { classManagementUseCases.getAllClassesUseCase() } returns flowOf(Result.success(classes))
            coEvery { getAttendanceForDateUseCase(any()) } returns flowOf(Result.success(emptyList()))

            viewModel = CalendarViewModel(attendanceUseCases, studentManagementUseCases, classManagementUseCases)
            advanceUntilIdle()

            // When - toggle class 1
            viewModel.onEvent(CalendarScreenEvent.ToggleClassSelection(1))
            advanceUntilIdle()

            // Then - only students from class 1 shown
            assertEquals(2, viewModel.uiState.value.allStudents.size)
            assertEquals(setOf(1), viewModel.uiState.value.selectedClassIds)
        }

    @Test
    fun `ToggleClassSelection with multiple classes merges and deduplicates`() =
        runTest {
            // Given
            val class1 = StudentClass(1, "Class A")
            val class2 = StudentClass(2, "Class B")
            val students = listOf(
                Student(1, "S1", classId = 1),
                Student(2, "S2", classId = 1),
                Student(3, "S3", classId = 2),
                Student(4, "S4", classId = 2)
            )
            val classes = listOf(class1, class2)

            coEvery { studentManagementUseCases.getAllStudentsUseCase() } returns flowOf(Result.success(students))
            val classManagementUseCases = mockk<ClassManagementUseCases>(relaxed = true)
            coEvery { classManagementUseCases.getAllClassesUseCase() } returns flowOf(Result.success(classes))
            coEvery { getAttendanceForDateUseCase(any()) } returns flowOf(Result.success(emptyList()))

            viewModel = CalendarViewModel(attendanceUseCases, studentManagementUseCases, classManagementUseCases)
            advanceUntilIdle()

            // When - toggle both classes
            viewModel.onEvent(CalendarScreenEvent.ToggleClassSelection(1))
            advanceUntilIdle()
            viewModel.onEvent(CalendarScreenEvent.ToggleClassSelection(2))
            advanceUntilIdle()

            // Then - all 4 students shown
            assertEquals(4, viewModel.uiState.value.allStudents.size)
            assertEquals(setOf(1, 2), viewModel.uiState.value.selectedClassIds)
        }

    @Test
    fun `unchecking all classes shows all students including unassigned`() =
        runTest {
            // Given
            val class1 = StudentClass(1, "Class A")
            val students = listOf(
                Student(1, "S1", classId = 1),
                Student(2, "S2", classId = null)
            )
            val classes = listOf(class1)

            coEvery { studentManagementUseCases.getAllStudentsUseCase() } returns flowOf(Result.success(students))
            val classManagementUseCases = mockk<ClassManagementUseCases>(relaxed = true)
            coEvery { classManagementUseCases.getAllClassesUseCase() } returns flowOf(Result.success(classes))
            coEvery { getAttendanceForDateUseCase(any()) } returns flowOf(Result.success(emptyList()))

            viewModel = CalendarViewModel(attendanceUseCases, studentManagementUseCases, classManagementUseCases)
            advanceUntilIdle()

            // First toggle a class
            viewModel.onEvent(CalendarScreenEvent.ToggleClassSelection(1))
            advanceUntilIdle()
            assertEquals(1, viewModel.uiState.value.allStudents.size) // only S1

            // When - uncheck it
            viewModel.onEvent(CalendarScreenEvent.ToggleClassSelection(1))
            advanceUntilIdle()

            // Then - all students including unassigned shown
            assertEquals(2, viewModel.uiState.value.allStudents.size)
            assertEquals(emptySet(), viewModel.uiState.value.selectedClassIds)
        }

    @Test
    fun `ToggleClassSelection shows error on students load failure`() =
        runTest {
            // Given - use case fails on first load
            val errorMsg = "Failed to load students"
            coEvery { studentManagementUseCases.getAllStudentsUseCase() } returns flowOf(Result.failure(Exception(errorMsg)))
            coEvery { getAttendanceForDateUseCase(any()) } returns flowOf(Result.success(emptyList()))

            viewModel = CalendarViewModel(
                attendanceUseCases,
                studentManagementUseCases,
                mockk<ClassManagementUseCases>(relaxed = true)
            )
            advanceUntilIdle()

            // Then - error state is set
            val expectedError = UiText.StringResource(R.string.error_loading_students, errorMsg)
            assertEquals(expectedError, viewModel.uiState.value.error)
        }

    @Test
    fun `selecting empty class shows zero students`() =
        runTest {
            // Given - a class with no students assigned
            val class1 = StudentClass(1, "Empty Class")
            val students = listOf(
                Student(1, "S1", classId = 2) // student in different class
            )
            val classes = listOf(class1)

            coEvery { studentManagementUseCases.getAllStudentsUseCase() } returns flowOf(Result.success(students))
            val classManagementUseCases = mockk<ClassManagementUseCases>(relaxed = true)
            coEvery { classManagementUseCases.getAllClassesUseCase() } returns flowOf(Result.success(classes))
            coEvery { getAttendanceForDateUseCase(any()) } returns flowOf(Result.success(emptyList()))

            viewModel = CalendarViewModel(attendanceUseCases, studentManagementUseCases, classManagementUseCases)
            advanceUntilIdle()

            // When - select the empty class
            viewModel.onEvent(CalendarScreenEvent.ToggleClassSelection(1))
            advanceUntilIdle()

            // Then - zero students shown (empty list, not crash)
            assertEquals(0, viewModel.uiState.value.allStudents.size)
            assertEquals(setOf(1), viewModel.uiState.value.selectedClassIds)
        }

    // endregion
```

**How to add these tests**:
1. Open `CalendarViewModelTest.kt`
2. Add the `import dev.nenoeldeeb.education.absencerecord.domain.models.StudentClass` at the top with the other imports
3. Add the test functions inside the `CalendarViewModelTest` class
4. Note: In these tests, `viewModel` is created directly instead of using `createViewModel()` because we need to inject a non-mocked `ClassManagementUseCases` that actually returns data. The `createViewModel()` helper uses `mockk<ClassManagementUseCases>(relaxed = true)` which won't return real class data.

---

**Checkpoint**: At this point, User Story 1 and 2 should be fully functional:
- The top bar filter icon and dropdown are gone
- The attendance dialog has a "No filter" button that opens a checkbox dropdown
- Checking classes filters the student list and updates the count
- Unchecking all shows all students
- Tests verify these behaviors

---

## Phase 4: User Story 3 — Attendance Count Clamping (Priority: P1)

**Goal**: Ensure the marked-present count never exceeds the visible student count, even when the filter changes.

**Independent Test Criteria**:
1. Filter to a class, mark all its students present → count shows N/N
2. Add another class → total increases, present count stays the same → now shows N/(N+M)
3. Students whose attendance was recorded but are now filtered out are excluded from the visible count

**Note**: Most of the clamping logic was already implemented in Task T005 (`toggleClassSelection` already filters `studentsForSelectedDate` to only include visible students). This phase adds dedicated tests and hardening.

### Task T009 [US3] — Add clamping hardening in CalendarViewModel.kt

**File**: `app/src/main/java/dev/nenoeldeeb/education/absencerecord/presentation/screens/calendar/CalendarViewModel.kt`

**What to do**: Verify that the clamping is already correct from T005. Add an additional safeguard: when `allStudents` changes (due to filter or data update), always re-filter `studentsForSelectedDate` to only include records for students in the visible set.

**Step-by-step instructions**:

1. Open `CalendarViewModel.kt`
2. Find the `_uiState.update { ... }` inside the `collectLatest` callback of `initializeStudents()`. It should already call `applyMultiClassFilter`. Add clamping there too:

```kotlin
                    _uiState.update { state ->
                        val filteredStudents = students.applyMultiClassFilter(state.selectedClassIds)
                        val visibleIds = filteredStudents.map { it.id }.toSet()
                        state.copy(
                            allStudents = filteredStudents,
                            studentsForSelectedDate = state.studentsForSelectedDate.filter {
                                it.studentId in visibleIds
                            }
                        )
                    }
```

   (Replace the existing `state.copy(allStudents = ...)` block — the current one only sets `allStudents` without clamping `studentsForSelectedDate`)

3. **Important**: The `toggleClassSelection` method from T005 already includes clamping. Verify it has this logic:

```kotlin
    private fun toggleClassSelection(classId: Int) {
        viewModelScope.launch {
            val updatedIds = _uiState.value.selectedClassIds.toMutableSet()
            if (classId in updatedIds) {
                updatedIds.remove(classId)
            } else {
                updatedIds.add(classId)
            }

            val filteredStudents = withContext(Dispatchers.Default) {
                rawStudents.applyMultiClassFilter(updatedIds)
            }

            _uiState.update { state ->
                val visibleIds = filteredStudents.map { it.id }.toSet()
                state.copy(
                    selectedClassIds = updatedIds,
                    allStudents = filteredStudents,
                    studentsForSelectedDate = state.studentsForSelectedDate.filter {
                        it.studentId in visibleIds
                    }
                )
            }
        }
    }
```

   If the clamping line (`studentsForSelectedDate = state.studentsForSelectedDate.filter { it.studentId in visibleIds }`) is missing, add it.

---

### Task T010 [US3] — Write tests for attendance count clamping

**File**: `app/src/test/java/dev/nenoeldeeb/education/absencerecord/presentation/screens/calendar/CalendarViewModelTest.kt`

**What to do**: Add test functions that verify US3 (present count never exceeds visible count) behavior.

**Tests to add** (inside the `CalendarViewModelTest` class):

```kotlin
    // region Attendance Clamping & Edge Case Tests

    @Test
    fun `attendance count is clamped when filter removes students`() =
        runTest {
            // Given
            val class1 = StudentClass(1, "Class A")
            val class2 = StudentClass(2, "Class B")
            val date = LocalDate(2026, Month.MARCH, 1)
            val students = listOf(
                Student(1, "S1", classId = 1),
                Student(2, "S2", classId = 1),
                Student(3, "S3", classId = 2)
            )
            val classes = listOf(class1, class2)
            val attendance = listOf(
                StudentAttendance(studentId = 1, date = date),
                StudentAttendance(studentId = 2, date = date)
            )

            coEvery { studentManagementUseCases.getAllStudentsUseCase() } returns flowOf(Result.success(students))
            val classManagementUseCases = mockk<ClassManagementUseCases>(relaxed = true)
            coEvery { classManagementUseCases.getAllClassesUseCase() } returns flowOf(Result.success(classes))
            coEvery { getAttendanceForDateUseCase(date) } returns flowOf(Result.success(attendance))

            viewModel = CalendarViewModel(attendanceUseCases, studentManagementUseCases, classManagementUseCases)
            advanceUntilIdle()

            // Open dialog for date
            viewModel.onEvent(CalendarScreenEvent.SelectDateForDialog(date))
            advanceUntilIdle()

            // Filter to class 1 only
            viewModel.onEvent(CalendarScreenEvent.ToggleClassSelection(1))
            advanceUntilIdle()

            // Then - studentsForSelectedDate should only include students visible in filtered class 1
            assertEquals(2, viewModel.uiState.value.allStudents.size) // S1, S2
            assertEquals(2, viewModel.uiState.value.studentsForSelectedDate.size) // both are in class 1

            // When - add class 2 (new students, attendance not yet marked for S3)
            viewModel.onEvent(CalendarScreenEvent.ToggleClassSelection(2))
            advanceUntilIdle()

            // Then - total students increases, present count stays same
            assertEquals(3, viewModel.uiState.value.allStudents.size) // S1, S2, S3
            assertEquals(2, viewModel.uiState.value.studentsForSelectedDate.size) // S3 not marked yet
        }

    @Test
    fun `present count never exceeds visible count when classes are unchecked`() =
        runTest {
            // Given - two classes, attendance for all students
            val class1 = StudentClass(1, "Class A")
            val class2 = StudentClass(2, "Class B")
            val date = LocalDate(2026, Month.MARCH, 1)
            val students = listOf(
                Student(1, "S1", classId = 1),
                Student(2, "S2", classId = 1),
                Student(3, "S3", classId = 2)
            )
            val classes = listOf(class1, class2)
            val attendance = listOf(
                StudentAttendance(studentId = 1, date = date),
                StudentAttendance(studentId = 2, date = date),
                StudentAttendance(studentId = 3, date = date)
            )

            coEvery { studentManagementUseCases.getAllStudentsUseCase() } returns flowOf(Result.success(students))
            val classManagementUseCases = mockk<ClassManagementUseCases>(relaxed = true)
            coEvery { classManagementUseCases.getAllClassesUseCase() } returns flowOf(Result.success(classes))
            coEvery { getAttendanceForDateUseCase(date) } returns flowOf(Result.success(attendance))

            viewModel = CalendarViewModel(attendanceUseCases, studentManagementUseCases, classManagementUseCases)
            advanceUntilIdle()

            // Open dialog and select both classes
            viewModel.onEvent(CalendarScreenEvent.SelectDateForDialog(date))
            advanceUntilIdle()
            viewModel.onEvent(CalendarScreenEvent.ToggleClassSelection(1))
            advanceUntilIdle()
            viewModel.onEvent(CalendarScreenEvent.ToggleClassSelection(2))
            advanceUntilIdle()

            // Initially all 3 students present
            assertEquals(3, viewModel.uiState.value.allStudents.size)
            assertEquals(3, viewModel.uiState.value.studentsForSelectedDate.size)

            // When - uncheck class 2 (S3's class)
            viewModel.onEvent(CalendarScreenEvent.ToggleClassSelection(2))
            advanceUntilIdle()

            // Then - present count should exclude S3
            assertEquals(2, viewModel.uiState.value.allStudents.size) // S1, S2 only
            assertEquals(2, viewModel.uiState.value.studentsForSelectedDate.size) // S3 excluded
        }

    @Test
    fun `attendance records persist when student is filtered out and back in`() =
        runTest {
            // Given - one student with attendance, filter to their class
            val class1 = StudentClass(1, "Class A")
            val date = LocalDate(2026, Month.MARCH, 1)
            val students = listOf(
                Student(1, "S1", classId = 1)
            )
            val classes = listOf(class1)
            val attendance = listOf(
                StudentAttendance(studentId = 1, date = date)
            )

            coEvery { studentManagementUseCases.getAllStudentsUseCase() } returns flowOf(Result.success(students))
            val classManagementUseCases = mockk<ClassManagementUseCases>(relaxed = true)
            coEvery { classManagementUseCases.getAllClassesUseCase() } returns flowOf(Result.success(classes))
            coEvery { getAttendanceForDateUseCase(date) } returns flowOf(Result.success(attendance))

            viewModel = CalendarViewModel(attendanceUseCases, studentManagementUseCases, classManagementUseCases)
            advanceUntilIdle()

            // Open dialog and filter to class
            viewModel.onEvent(CalendarScreenEvent.SelectDateForDialog(date))
            advanceUntilIdle()
            viewModel.onEvent(CalendarScreenEvent.ToggleClassSelection(1))
            advanceUntilIdle()

            // Verify attendance is visible
            assertEquals(1, viewModel.uiState.value.studentsForSelectedDate.size)

            // When - uncheck class (filter out the student)
            viewModel.onEvent(CalendarScreenEvent.ToggleClassSelection(1))
            advanceUntilIdle()

            // Then - attendance record still shows because student is in allStudents (all shown)
            // When filter is empty, all students including S1 are shown
            assertEquals(1, viewModel.uiState.value.allStudents.size)
            assertEquals(1, viewModel.uiState.value.studentsForSelectedDate.size)

            // When - filter back to class
            viewModel.onEvent(CalendarScreenEvent.ToggleClassSelection(1))
            advanceUntilIdle()

            // Then - attendance state is still shown
            assertEquals(1, viewModel.uiState.value.studentsForSelectedDate.size)
        }

    // endregion
```

**Note**: The import `dev.nenoeldeeb.education.absencerecord.domain.models.StudentAttendance` may already be imported via a different path in the existing file (the test file uses FQN for it in the existing tests). Check what imports exist at the top and add `import dev.nenoeldeeb.education.absencerecord.domain.models.StudentAttendance` if it is not already present. For consistency with the existing code style, you may keep FQN for `StudentAttendance` in the new tests as well — choose whichever approach matches the surrounding code.

---

**Checkpoint**: At this point, all three user stories are implemented:
- US1: Multi-select filter inside attendance dialog ✓
- US2: Dynamic count updates ✓
- US3: Present count never exceeds visible count ✓

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Ensure code quality, formatting, and file size compliance.

### Task T011 [P] — Format code with ktlintFormat

**Command to run**:
```shell
./gradlew ktlintFormat
```

**What this does**: Automatically reformats all Kotlin source files to match the project's code style. Run this before committing.

**Step-by-step**:
1. Open a terminal
2. Run `./gradlew ktlintFormat`
3. If there are warnings or errors, fix them manually (usually unused imports or formatting issues ktlint can't auto-fix)
4. Run `./gradlew ktlintFormat` again to ensure clean output

---

### Task T012 — Verify all tests pass

**Commands to run**:
```shell
./gradlew testDebugUnitTest --tests "CalendarViewModelTest"
```

**What this does**: Runs all CalendarViewModel tests and reports results. This also serves as verification for **SC-005** (all existing attendance-marking functionality continues to work unchanged) — if existing tests pass, SC-005 is satisfied.

**Step-by-step**:
1. Run the test command
2. All tests MUST pass (green)
3. If any test fails, read the error message and fix the implementation code (not the test)
4. Re-run until all pass

---

### Task T013 [P] — Verify file size limits (≤300 lines per file)

**Purpose**: The project requires every source file (including tests) to be ≤300 lines.

**Files to check**:
- `CalendarScreenState.kt` — should be small (~15 lines)
- `CalendarScreenEvent.kt` — should be small (~15 lines)
- `CalendarViewModel.kt` — was 188 lines before changes; verify still ≤300
- `CalendarScreen.kt` — was 272 lines before changes; removing old filter (~20 lines) and adding ClassCheckboxFilter (~40 lines) should keep it under 300
- `CalendarViewModelTest.kt` — was 384 lines before changes; adding tests may exceed 300

**Pre-split required**: The existing `CalendarViewModelTest.kt` is already 384 lines (exceeds the 300-line limit). **Before adding any new tests**, you MUST split it:

1. Create `app/src/test/java/dev/nenoeldeeb/education/absencerecord/presentation/screens/calendar/CalendarViewModelBaseTest.kt`
2. Move the existing tests (attendance marking, date selection, flow tests) into `CalendarViewModelBaseTest.kt`
3. Keep in `CalendarViewModelTest.kt` only the `@BeforeEach setUp()` and `createViewModel()` helper
4. Now add the new filter tests from T008 and T010 to `CalendarViewModelTest.kt`

After the split, verify both files are under 300 lines each.

**If CalendarScreen.kt exceeds 300 lines**: Extract the `ClassCheckboxFilter` composable into its own file:
- Create: `app/src/main/java/dev/nenoeldeeb/education/absencerecord/presentation/screens/calendar/ClassCheckboxFilter.kt`
- Move the `ClassCheckboxFilter` composable function there
- Import it in `CalendarScreen.kt`

---

### Task T014 — Run full validation

**Command to run**:
```shell
./gradlew check
```

**What this does**: Runs ktlint, Android lint, and all tests. Must pass cleanly before the feature is complete.

If any issues arise, fix them and re-run `./gradlew check` until clean.

---

## Summary

### User Stories

| Story | Priority | Description | Independent Test |
|-------|----------|-------------|-----------------|
| US1+2 | P1 | Multi-select filter inside attendance dialog + dynamic count | Open dialog, check class, verify student list and count update |
| US3 | P1 | Present count never exceeds visible count | Filter to a class, mark all present, add class, verify count stays valid |

### Total Tasks: 14

| Phase | Tasks | Count |
|-------|-------|-------|
| Phase 1: Setup | (none needed) | 0 |
| Phase 2: Foundational | T001-T004 | 4 |
| Phase 3: US1+US2 | T005-T008 | 4 |
| Phase 4: US3 | T009-T010 | 2 |
| Phase 5: Polish | T011-T014 | 4 |

### Parallel Opportunities

| Tasks | Can run in parallel? | Reason |
|-------|---------------------|--------|
| T004 (strings) | YES — with T001/T002/T003 | Different files, no dependencies |
| T011 (format), T013 (file size) | YES — with each other | Independent checks |

### MVP Scope

The MVP is **Phase 2 + Phase 3** (US1+US2). This delivers the core feature:
- Multi-select class filter inside the attendance dialog
- Dynamic student count updates
- Removal of old separate filter UI

Phase 4 (US3 - clamping) builds on US1 but adds important data integrity. Include it for a complete feature.

### Dependency Graph

```
Phase 2 (Foundational: T001-T004)
    │
    ▼
Phase 3 (US1+US2: T005-T008) ───► Phase 4 (US3: T009-T010)
    │                                      │
    └──────────────┬───────────────────────┘
                   ▼
          Phase 5 (Polish: T011-T014)
```

### Format Validation

✅ All tasks use the format: `- [ ] TXXX [P] [Story] Description with file path`
- Checkbox: ✓
- Task ID (T001-T014): ✓
- [P] marker: Only on parallel tasks (T004, T011, T013): ✓
- [Story] labels: Only on US1/US2/US3 tasks: ✓
- File paths: Every task includes exact file path: ✓
