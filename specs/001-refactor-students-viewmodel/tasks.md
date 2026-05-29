---
description: "Tasks for refactoring StudentsViewModel (574 lines → <300 lines) by extracting logic into action delegates"
---

# Tasks: Refactor Students ViewModel

**Input**: Design documents from `specs/001-refactor-students-viewmodel/`

**Prerequisites**: [plan.md](file:///home/nenoeldeeb/Projects/AndroidStudioProjects/Absencerecord/specs/001-refactor-students-viewmodel/plan.md), [spec.md](file:///home/nenoeldeeb/Projects/AndroidStudioProjects/Absencerecord/specs/001-refactor-students-viewmodel/spec.md), [data-model.md](file:///home/nenoeldeeb/Projects/AndroidStudioProjects/Absencerecord/specs/001-refactor-students-viewmodel/data-model.md), [contracts/](file:///home/nenoeldeeb/Projects/AndroidStudioProjects/Absencerecord/specs/001-refactor-students-viewmodel/contracts/)

**Organization**: Tasks are grouped by user story. Each user story phase is independently testable.

## Format: `[ID] [P?] [Story] Description with file path`

- **[P]**: Can run in parallel (different files, no dependency on other tasks in same phase)
- **[Story]**: Which user story this task belongs to (US1, US2, US3, US4)
- Include exact file paths in descriptions

## Path Conventions

- **Source**: `app/src/main/java/dev/nenoeldeeb/education/absencerecord/presentation/screens/students/`
- **Tests**: `app/src/test/java/dev/nenoeldeeb/education/absencerecord/presentation/screens/students/`

---

## Phase 1: Foundational (Shared Infrastructure)

**Purpose**: Create shared types and delegate skeletons that all user stories depend on. These are pure additive tasks — no existing code is modified.

**⚠️ CRITICAL**: Must complete before any user story phase begins

- [x] T001 Create sealed interface `StudentError` with variants `Database`, `DuplicateClass`, `FileRead`, `ImportParse`, `Validation`, and `Cancelled` in `domain/models/StudentError.kt`. `Validation` is a data class with a `message: String` field to support reusable validation messages. All other variants are data objects or data classes (none need payloads initially). Add a `.toUiText()` extension function that maps each variant to a `UiText.StringResource` using existing strings.xml resources (or add new ones if needed). Follow the existing `UiText` pattern from `presentation/utils/UiText.kt`.
- [x] T002 [P] Create `StudentActionDelegate` class skeleton in `presentation/screens/students/delegates/StudentActionDelegate.kt`. Constructor takes `StudentManagementUseCases` and `ImportExportDelegate`. Add empty method stubs with correct signatures per the contract: `addStudent(name, classId)`, `updateStudent(student, newName, newClassId)`, `deleteSelectedStudents(ids, allStudents)`, `exportSelectedStudents(uri, ids, allStudents)`, `exportAndDeleteSelectedStudents(uri, ids, allStudents)`, `prepareImportSelectionDialog(uri)`, `performImport(parsed, selectionMap)`. Each method body should be `TODO("implement in US1/US3/US4")` for now. Follow the same class style as existing `ImportExportDelegate.kt` (no `@Stable`, no `open`).
- [x] T003 [P] Create `ClassActionDelegate` class skeleton in `presentation/screens/students/delegates/ClassActionDelegate.kt`. Constructor takes `ClassManagementUseCases`. Add empty method stubs: `addClass(name)`, `renameClass(studentClass, newName)`, `deleteClass(studentClass)`, `onDeletedClassFilterFallback(currentFilter, deletedClass)`. Each async method body is `TODO("implement in US2")`. `onDeletedClassFilterFallback` is a pure function (non-suspend) — return `currentFilter` as a placeholder.

**Checkpoint**: `StudentError.kt`, `StudentActionDelegate.kt`, and `ClassActionDelegate.kt` exist and compile (stubs return TODO or placeholder values). Proceed to user stories in priority order.

---

## Phase 2: User Story 1 - Manage Student Roster (P1) 🎯 MVP

**Goal**: Extract student add/edit logic from ViewModel into `StudentActionDelegate`. Students can be added and edited with name validation.

**Independent Test**: Open students screen, add "Test Student", verify it appears in the list. Edit the name, verify change persists. Try adding blank name, verify error toast.

### Implementation for User Story 1

- [x] T004 [US1] Implement `StudentActionDelegate.addStudent()` body. In the current ViewModel at `StudentsViewModel.kt` lines 209-241, the private `addStudent(name, classId)` method calls `studentManagementUseCases.addStudentUseCase(student)` with validation. Copy the logic: check `name.isNotBlank()`. If blank, return `Result.failure(StudentError.Validation("Name must not be blank"))`. Otherwise create a `Student(name=name, classId=classId)` domain model, call `addStudentUseCase(student)`. On success return `Result.success(UiText.StringResource(R.string.student_added_success))`. On failure return `Result.failure(StudentError.Database)`. Remove the original private method from the ViewModel after confirming the delegate handles it.
- [x] T005 [US1] Implement `StudentActionDelegate.updateStudent()` body. In the current ViewModel at lines 243-275, the private `updateStudent(student, newName, newClassId)` method validates `newName.isNotBlank()`, creates a `student.copy(name=newName, classId=newClassId)`, and calls `updateStudentUseCase`. If `newName` is blank, return `Result.failure(StudentError.Validation("Name must not be blank"))`. Copy this logic into the delegate. Remove the original private method from the ViewModel.
- [x] T006 [US1] Wire `AddStudent` and `UpdateStudent` events in `StudentsViewModel.onEvent()`. In the `onEvent` method (currently ~101 lines, at lines 105-205), locate the `StudentsScreenEvent.AddStudent` and `StudentsScreenEvent.UpdateStudent` cases. Replace the direct calls to `addStudent(...)` and `updateStudent(...)` with delegate calls: `studentActionDelegate.addStudent(name, classId)`. Use `onSuccess { toast -> _uiState.update { it.copy(toastMessage = toast, newStudentName = "", showAddStudentDialog = false) } }` and `onFailure { error -> _uiState.update { it.copy(error = error.toUiText()) } }`. The loading state is already managed in the existing `viewModelScope.launch` block (set `isLoading = true` before, `isLoading = false` after).
- [x] T007 [US1] Write unit tests for `StudentActionDelegate.addStudent()` and `updateStudent()` in `delegates/StudentActionDelegateTest.kt`. Use `mockk` for `StudentManagementUseCases` (relaxed=true). Test: (1) addStudent with valid name returns success toast, (2) addStudent with blank name returns `StudentError.Validation`, (3) addStudent with DB error returns `StudentError.Database`, (4) updateStudent with valid data returns success toast, (5) updateStudent with blank name returns `StudentError.Validation`. Follow the same MockK + JUnit pattern as existing `StudentsViewModelTest.kt`.

**Checkpoint**: Students can be added and edited through the delegate. Existing UI composables show no visual changes. Run `StudentsViewModelTest` — all existing tests must still pass. Proceed to US2.

---

## Phase 3: User Story 2 - Filter and Manage Classes (P1)

**Goal**: Extract class CRUD logic from ViewModel into `ClassActionDelegate`. Classes can be added, renamed, deleted with duplicate name detection. Filter falls back to "All" when the filtered class is deleted.

**Independent Test**: Add "Class A" and "Class B". Assign a student to Class A. Filter by Class A, verify only that student shows. Rename Class A to "Class A1". Delete Class A, verify filter resets to "All".

### Implementation for User Story 2

- [x] T008 [US2] Implement `ClassActionDelegate.addClass()` body. In the current ViewModel at lines 458-486, the private `addClass(name)` method calls `classManagementUseCases.addClassUseCase(StudentClass(name=name))`. Copy the logic: first check `name.isNotBlank()`. If blank, return `Result.failure(StudentError.Validation("Class name must not be blank"))`. Otherwise create a `StudentClass(name=name)`, call `addClassUseCase`. Map the failure: if the use case returns a failure related to unique constraint (check exception type or message), return `StudentError.DuplicateClass`; otherwise return `StudentError.Database`. On success return a success toast. Remove the original private method from the ViewModel.
- [x] T009 [US2] Implement `ClassActionDelegate.renameClass()` body. In the current ViewModel at lines 488-540, the private `renameClass(studentClass, newName)` creates `studentClass.copy(name=newName)` and calls `updateClassUseCase`. First check `newName.isNotBlank()`. If blank, return `Result.failure(StudentError.Validation("Class name must not be blank"))`. Otherwise copy the logic. Map duplicate name errors to `StudentError.DuplicateClass`. Remove the original private method from the ViewModel.
- [x] T010 [US2] Implement `ClassActionDelegate.deleteClass()` body. In the current ViewModel at lines 542-573, the private `deleteClass(studentClass)` calls `deleteClassUseCase(studentClass)`. Copy the logic. On success, if the deleted class matches the current filter, the ViewModel must reset the filter — but that's a ViewModel concern (handled in T012). The delegate returns a success toast on success. Remove the original private method from the ViewModel.
- [x] T011 [US2] Implement `ClassActionDelegate.onDeletedClassFilterFallback()` body. This is a pure function (no `suspend`, no use case calls). Check if `currentFilter` is `ClassFilter.ByClass(deletedClass)`; if so, return `ClassFilter.All`. Otherwise return `currentFilter` unchanged. No try-catch needed. No TODO stub left.
- [x] T012 [US2] Wire class events in `StudentsViewModel.onEvent()`. Locate `AddClass`, `RenameClass`, and `DeleteClass` event cases. Replace direct calls to private methods with `classActionDelegate.addClass(name)`, `.renameClass(...)`, `.deleteClass(...)`. For the delete handler, after the delegate returns success, compute the filter fallback: `val newFilter = classActionDelegate.onDeletedClassFilterFallback(uiState.value.selectedClassFilter, studentClass)`, then update state with both `toastMessage` and `selectedClassFilter = newFilter`. Also wire `SelectClassFilter` and class filter visibility events — these are simple state toggles that don't need the delegate (remain in ViewModel).
- [x] T013 [US2] Write unit tests for `ClassActionDelegate` in `delegates/ClassActionDelegateTest.kt`. Use `mockk` for `ClassManagementUseCases`. Test: (1) addClass returns success toast, (2) addClass with blank name returns `StudentError.Validation`, (3) addClass with duplicate name returns `StudentError.DuplicateClass`, (4) renameClass succeeds, (5) renameClass with blank name returns `StudentError.Validation`, (6) renameClass with duplicate name returns `DuplicateClass`, (7) deleteClass returns success toast, (8) `onDeletedClassFilterFallback` with matching filter returns `ClassFilter.All`, (9) `onDeletedClassFilterFallback` with non-matching filter returns filter unchanged.

**Checkpoint**: Classes can be created, renamed, and deleted. Filter fallback works. Run `StudentsViewModelTest` — all existing tests pass. Proceed to US3.

---

## Phase 4: User Story 3 - Bulk Actions on Student Records (P2)

**Goal**: Extract bulk delete logic into `StudentActionDelegate`. Multi-selection mode uses the existing `SelectionStateDelegate` (no changes needed there). This phase requires US1 completion (StudentActionDelegate skeleton must have addStudent/updateStudent already implemented).

**Independent Test**: Enter multi-selection mode. Select multiple students. Tap delete. Verify selected students are removed and selection mode clears.

### Implementation for User Story 3

- [x] T014 [US3] Implement `StudentActionDelegate.deleteSelectedStudents()` body. In the current ViewModel at lines 352-381, the private `deleteSelectedStudents()` method filters `rawStudents` by `selectedStudentIds` and calls `deleteStudentsUseCase(students)`. Copy the logic: filter the `allStudents` list by `selectedIds`, call `deleteStudentsUseCase`. On success return a success toast. On failure return `StudentError.Database`. Remove the original private method from the ViewModel. (Note: the ViewModel's `rawStudents` cache stays in the ViewModel — the delegate receives `allStudents` as a parameter from the ViewModel.)
- [x] T015 [US3] Wire `DeleteSelectedStudents`, `ToggleSelectionMode`, `ToggleStudentSelection`, `ToggleStudentsSelection`, `ShowBulkDeleteDialog`, and `DismissBulkDeleteDialog` events. The selection events (`ToggleStudentSelection`, `ToggleSelectionMode`, `ToggleStudentsSelection`) already use `SelectionStateDelegate` — verify they still do and don't change them. For `DeleteSelectedStudents`: replace the private method call with `studentActionDelegate.deleteSelectedStudents(uiState.value.selectedStudentIds, uiState.value.allStudents)`. Use `onSuccess` to clear selection state (`isMultiSelectionMode = false`, `selectedStudentIds = emptySet()`, `showBulkDeleteDialog = false`). `ShowBulkDeleteDialog`/`DismissBulkDeleteDialog` are simple state toggles — leave them in the ViewModel.
- [x] T016 [US3] Write unit tests for `StudentActionDelegate.deleteSelectedStudents()` in `delegates/StudentActionDelegateTest.kt` (append to existing test file). Test: (1) deleteSelectedStudents with valid IDs returns success toast, (2) deleteSelectedStudents with empty IDs returns success (nothing to delete), (3) deleteSelectedStudents with DB error returns `StudentError.Database`.

**Checkpoint**: Multi-selection mode works, bulk delete works. Run `StudentsViewModelTest` — all existing tests pass. Proceed to US4.

---

## Phase 5: User Story 4 - Import & Export Student Records (P2)

**Goal**: Extract export, export-and-delete, import-phase-1, and import-phase-2 logic into `StudentActionDelegate`. Import uses two-phase flow: parse JSON → preview dialog → confirm → persist. The existing `ImportExportDelegate` is reused for dialog state transformations.

**Independent Test**: Export selected students to a JSON file. Import the same JSON file, verify students appear. Try importing an invalid JSON file, verify error message.

### Implementation for User Story 4

- [x] T017 [US4] Implement `StudentActionDelegate.exportSelectedStudents()` body. In the current ViewModel at lines 383-410, the private `exportSelectedStudents(uri)` filters `rawStudents` by `selectedStudentIds` and calls `exportStudentsUseCase(uriString, selectedStudentIds, allStudents)`. Copy the logic: filter `allStudents` by `selectedIds`, call `exportStudentsUseCase(uriString, selectedIds, filteredStudents)`. On success return a success toast. On failure return `StudentError.FileRead` (wrapping the actual error). Remove the original private method from the ViewModel.
- [x] T018 [US4] Implement `StudentActionDelegate.exportAndDeleteSelectedStudents()` body. In the current ViewModel at lines 412-454, the private `exportAndDeleteSelectedStudents(uri)` first exports, then deletes only if export succeeds. Copy the logic: call `exportStudentsUseCase` first. If it fails, return the failure immediately (do NOT delete). If it succeeds, call `deleteStudentsUseCase` with the filtered students. Return a combined success toast (or the first failure). The original method returns a toast like "Exported and deleted N students" — preserve the same wording. Remove the original private method from the ViewModel.
- [x] T019 [US4] Implement `StudentActionDelegate.prepareImportSelectionDialog()` body. In the current ViewModel at lines 279-319, the private `prepareImportSelectionDialog(uri)` calls `importStudentsUseCase.parseFile(uriString)`. Copy the logic: call `importStudentsUseCase.parseFile(uriString)`. Map `StorageRepository` read failures to `StudentError.FileRead`. Map JSON parse failures to `StudentError.ImportParse`. Return the parsed list on success. Remove the original private method from the ViewModel.
- [x] T020 [US4] Implement `StudentActionDelegate.performImport()` body. In the current ViewModel at lines 321-348, the private `performImport()` calls `importStudentsUseCase.performImport(parsedStudents, selectionMap)`. Copy the logic: call `importStudentsUseCase.performImport(parsedStudents, selectionMap)`. On success, use `importExportDelegate.buildImportResultMessage(result)` (available via the delegate's constructor-injected `ImportExportDelegate`) to format the success toast. On failure return `StudentError.Database`. Remove the original private method from the ViewModel.
- [x] T021 [US4] Wire export/import events in `StudentsViewModel.onEvent()`. The `onEvent` method handles: `PrepareImportSelectionDialog(uri)`, `CloseImportSelectionDialog`, `ToggleImportSelection`, `PerformImport`, `ExportSelectedStudents(uri)`, `ExportAndDeleteSelectedStudents(uri)`. Replace direct calls with delegate calls. For `PrepareImportSelectionDialog`: first check if `uri` is null — if so, set `error = StudentError.Cancelled.toUiText()` and return early. Otherwise call `studentActionDelegate.prepareImportSelectionDialog(uri)`, on success set `parsedStudentsFromFile` and `showImportSelectionDialog = true` via the existing `ImportExportDelegate.prepareImportDialog(parsed)`. For `PerformImport`: call `studentActionDelegate.performImport(...)`, on success clear the import dialog state and show toast. For `ExportSelectedStudents`/`ExportAndDeleteSelectedStudents`: first check if `uri` is null — if so, set `error = StudentError.Cancelled.toUiText()` and return early. Otherwise call corresponding delegate methods. Leave `CloseImportSelectionDialog` and `ToggleImportSelection` events as simple state toggles in the ViewModel (they don't need the delegate).
- [x] T022 [US4] Write unit tests for export/import methods in `delegates/StudentActionDelegateTest.kt` (append to existing). Test: (1) `exportSelectedStudents` with valid data returns success, (2) `exportSelectedStudents` with export failure returns `StudentError.FileRead`, (3) `exportAndDeleteSelectedStudents` — export succeeds, delete succeeds — returns combined success, (4) `exportAndDeleteSelectedStudents` — export fails — returns file error (verify delete was NOT called), (5) `prepareImportSelectionDialog` with valid file returns parsed data, (6) `prepareImportSelectionDialog` with unreadable file returns `StudentError.FileRead`, (7) `performImport` with valid selections returns success toast, (8) `performImport` with DB failure returns `StudentError.Database`.

**Checkpoint**: Import and export work through delegates. Run `StudentsViewModelTest` — all existing tests pass. Proceed to Polish phase.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Clean up, remove dead code, verify line count target, and run full validation.

- [x] T023 Remove the unused `exportSelectionMap: Map<Int, Boolean>` field from `StudentsScreenState` data class in `presentation/screens/students/StudentsScreenState.kt`. Search the entire codebase for references to `exportSelectionMap` to confirm it's truly unused (it was identified as dead code during research). If removing it causes compilation errors in tests or UI composables, keep the field but add a `@Suppress("UNUSED")` annotation with a comment.
- [x] T024 Verify `StudentsViewModel.kt` line count is strictly under 300 (SC-001). Count all non-blank, non-comment lines in the file using `wc -l` (or equivalent). If still over 300, identify remaining inline logic that can be moved to delegates — common candidates: leftover state toggle helpers, dialog visibility flags, or the `loadData()` init block (which may call delegates instead of use cases directly).
- [x] T025 Re-add any `UiText` string resources needed by the delegates to `res/values/strings.xml` and `res/values-ar/strings.xml`. Check that all toast messages returned by delegates (success toasts, error messages) use `UiText.StringResource(R.string.xxx)` referencing real string resources. Verify `StudentError.toUiText()` maps every variant to a real string resource.
- [x] T026 [P] Run `./gradlew ktlintFormat` to auto-format all modified files.
- [x] T027 Run `./gradlew check` to run ktlint, Android lint, and all unit tests (FR-004). Fix any lint warnings or test failures. Existing tests must pass at 100% (SC-002). New delegate tests must pass at 100% (SC-004).
- [x] T028 Perform final review: open the app in an emulator, navigate to the Students screen. Verify visually that all UI elements look identical to pre-refactoring (SC-003). Test adding a student, editing, filtering, bulk selection, import, and export.

---

## Implementation Strategy

### MVP Scope (Phase 2 only)
Minimum viable deliverable is **User Story 1 (Manage Student Roster)**. If time or risk is constrained, stop after T007 — student add/edit works through the delegate, ViewModel is thinner, tests pass.

### Incremental Delivery Order
```
Phase 1 (Foundational) → Phase 2 (US1, P1) → Phase 3 (US2, P1) → Phase 4 (US3, P2) → Phase 5 (US4, P2) → Phase 6 (Polish)
```
Within each phase, tasks with `[P]` can run in parallel.

### Parallel Opportunities
| Task Pair | Rationale |
|-----------|-----------|
| T002 + T003 | `StudentActionDelegate` and `ClassActionDelegate` are independent files |
| T004 + T005 | `addStudent` and `updateStudent` bodies are independent methods in the same file |
| T008–T011 | Class delegate methods are independent of student delegate work |
| T026 + T027 | Format + check can run last while you review |

### Per-Story Independent Test Commands
| Story | Test Command |
|-------|-------------|
| US1 | `./gradlew testDebugUnitTest --tests "*.delegates.StudentActionDelegateTest"` |
| US2 | `./gradlew testDebugUnitTest --tests "*.delegates.ClassActionDelegateTest"` |
| US3 | `./gradlew testDebugUnitTest --tests "*.delegates.StudentActionDelegateTest"` (bulk tests added to same file) |
| US4 | `./gradlew testDebugUnitTest --tests "*.delegates.StudentActionDelegateTest"` (export/import tests added to same file) |
| All | `./gradlew testDebugUnitTest` |
| Full gate | `./gradlew check` |

---

## Dependencies & Execution Order

### Phase Dependencies
```
Phase 1 (Foundational) ─────────────────────────────────────────────────────┐
    ↓                                                                        │
Phase 2 (US1 - P1 MVP) ──── depends on: StudentError + StudentActionDelegate│
    ↓                                                                        │
Phase 3 (US2 - P1) ──────── depends on: ClassActionDelegate                  │
    ↓                                                                        │
Phase 4 (US3 - P2) ──────── depends on: StudentActionDelegate (US1)         │
    ↓                                                                        │
Phase 5 (US4 - P2) ──────── depends on: StudentActionDelegate (US1)         │
    ↓                                                                        │
Phase 6 (Polish) ────────── depends on: all prior phases                     │
```

### Critical Path
```
T001 → T002 → T004 → T005 → T006 → T007  (US1 MVP ship)
                                    ↓
                              T008 → T009 → T010 → T011 → T012 → T013  (US2)
                                                                    ↓
                                                              T014 → T015 → T016  (US3)
                                                                              ↓
                                                                        T017 → T018 → T019 → T020 → T021 → T022  (US4)
                                                                                                                ↓
                                                                                                          T023 → T024 → T025 → T026 → T027 → T028  (Polish)
```

---

## Notes

- **DO NOT modify** any files in the `Out of Scope` list: UI composables (`StudentsScreen.kt`, dialogs, components), data layer (entities, DAOs, repository impls, mappers), domain layer (use cases, repository interfaces, domain models).
- **DO NOT modify** existing delegates `SelectionStateDelegate.kt` or `ImportExportDelegate.kt` unless adding a new method that is explicitly required.
- The `ViewModel` class must remain `open` for testability (existing tests depend on it).
- All delegate methods must return `Result<T>` — never throw exceptions.
- Run `./gradlew ktlintFormat` before each commit to keep formatting clean.
- If a test fails after refactoring, check whether the test directly calls a private method that was moved. Update the test to call through the ViewModel's `onEvent` instead (since private method visibility changed).
