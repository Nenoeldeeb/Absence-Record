# Tasks: Student Detail Screen & Composition-Based Navigation

**Input**: Design documents from `/specs/006-student-detail-screen/`

**Prerequisites**: spec.md (user stories), plan.md (technical architecture), research.md, data-model.md, contracts/

**Tests**: Unit tests are MANDATORY for all ViewModels and UseCases (testing BOTH `onSuccess` and `onFailure` paths), per constitution Principle IV.

**Organization**: Tasks are grouped by user story phases so each story can be implemented and tested independently.

## Format: `- [ ] [ID] [P?] [Story?] Description with exact file path`

## Conventions Used In This Plan

- **Navigation**: NO `androidx.navigation`. Plain Compose state stack (`AppDestination` sealed interface + `AppNavigation` composable with `rememberSaveable` back-stack, `BackHandler`, `AnimatedContent`). Do not create nav graphs or route strings.
- **Sorting**: Reuse the existing 2-value `SortType` enum at `app/src/main/java/dev/nenoeldeeb/education/absencerecord/domain/models/SortType.kt` (`ByName`, `ByAttendance`) — do NOT expand it to the 4-value enum sketched in `data-model.md` (plan: "Student, SortType unchanged").
- **Month types**: Keep `kotlinx.datetime.LocalDate` (first-of-month) for months, matching existing `ReportScreenState`, `GetAvailableMonthsUseCase`, and `ComposeCalendar(initialMonth: LocalDate?)`. Do NOT introduce `YearMonth`.
- **Source files MUST stay ≤ 300 lines** (Constitution V). Split Screen + dialogs into separate files.
- **Strings**: All user-facing copy must be added to BOTH `app/src/main/res/values/strings.xml` and `app/src/main/res/values-ar/strings.xml` and wrapped in `UiText`.
- **Package root for all main-source paths below**: `app/src/main/java/dev/nenoeldeeb/education/absencerecord/`
- **Test root**: `app/src/test/java/dev/nenoeldeeb/education/absencerecord/`

---

## Phase 1: Setup (Shared Infrastructure & Navigation Types)

**Purpose**: Feature-independent building blocks: navigation destination types, Student Detail state/event/effect contracts, strings, and DI factory.

- [X] T001 Create `app/navigation/AppDestination.kt` defining `sealed interface AppDestination { data object MainPager : AppDestination; data class StudentDetail(val studentId: Int) : AppDestination }` in `app/src/main/java/dev/nenoeldeeb/education/absencerecord/app/navigation/AppDestination.kt` (interface + objects only, no bodies).
- [X] T002 Create `StudentDetailScreenState.kt` in `app/src/main/java/dev/nenoeldeeb/education/absencerecord/presentation/screens/studentdetail/StudentDetailScreenState.kt` as an `@Stable` data class with fields: `student: Student? = null`, `assignedClassName: String? = null`, `availableClasses: List<StudentClass> = emptyList()`, `allAttendanceDates: List<LocalDate> = emptyList()`, `availableMonths: List<LocalDate> = emptyList()`, `selectedMonth: LocalDate? = null`, `isMonthDropdownExpanded: Boolean = false`, `isEditNameDialogOpen: Boolean = false`, `isChangeClassDialogOpen: Boolean = false`, `isDeleteConfirmationDialogOpen: Boolean = false`, `isLoading: Boolean = true`, `error: UiText? = null`, `toastMessage: UiText? = null`, `shareFileUri: Uri? = null`.
- [X] T003 Create `StudentDetailScreenEvent.kt` in `app/src/main/java/dev/nenoeldeeb/education/absencerecord/presentation/screens/studentdetail/StudentDetailScreenEvent.kt` as a `sealed interface` with events: `ToggleEditNameDialog`, `UpdateStudentName(newName: String)`, `ToggleChangeClassDialog`, `UpdateStudentClass(classId: Int?)`, `ToggleDeleteConfirmationDialog`, `ConfirmDeleteStudent`, `SelectMonth(month: LocalDate?)`, `ToggleMonthDropdown(expanded: Boolean)`, `ShareAttendanceReport`, `ShareFileResult(uri: Uri, error: UiText?)`, `ConsumeError`, `ConsumeToastMessage`.
- [X] T004 Create `StudentDetailUiEffect.kt` in `app/src/main/java/dev/nenoeldeeb/education/absencerecord/presentation/screens/studentdetail/StudentDetailUiEffect.kt` as `sealed interface StudentDetailUiEffect { data object NavigateBack : StudentDetailUiEffect }`.
- [X] T005 [P] Add Student Detail screen strings (back navigation description, student detail title placeholder, edit name dialog title/field label/save/cancel, change class dialog title + unassigned option, delete confirmation title/message/confirm, month filter label, all months, no attendance records message, share button text, change class icon description, delete icon description, sharing failed toast) to `app/src/main/res/values/strings.xml`.
- [X] T006 [P] Add the SAME strings from T005 translated to Arabic in `app/src/main/res/values-ar/strings.xml`.
- [X] T007 Add `studentDetailFactory(studentId: Int): ViewModelProvider.Factory` in `app/src/main/java/dev/nenoeldeeb/education/absencerecord/app/AppViewModelProvider.kt` returning a `viewModelFactory { initializer { ... } }` that constructs `StudentDetailViewModel(studentId, studentManagementUseCases, attendanceUseCases, reportUseCases, classManagementUseCases)`; do NOT remove existing `initializer` blocks.

**Checkpoint**: Destination types + state/event/effect contracts compile; strings exist in both locales. No screen behavior yet.

---

## Phase 2: Foundational (Domain / Data — Get Student By ID)

**Goal**: Add the per-student read path the detail screen needs. All layers return `Result<T>`; NO try-catch escapes.

- [X] T008 Add `fun getStudentById(studentId: Int): Flow<Result<Student?>>` to `app/src/main/java/dev/nenoeldeeb/education/absencerecord/domain/repositories/StudentRepository.kt`.
- [X] T009 Add Room query in `app/src/main/java/dev/nenoeldeeb/education/absencerecord/data/datasources/local/daos/StudentDao.kt`: `@Query("SELECT * FROM students WHERE id = :studentId") fun getStudentById(studentId: Int): Flow<StudentEntity?>`.
- [X] T010 Implement `getStudentById` in `app/src/main/java/dev/nenoeldeeb/education/absencerecord/data/repositories/StudentRepositoryImpl.kt` mapping the entity via `toStudent()` (only when present) and catching failures into `Result.failure(StudentError.Database)` with `.catch`/`runCatching`, matching the existing method style.
- [X] T011 [P] Write unit tests for repository `getStudentById`: (a) existing student → `onSuccess` with mapped `Student`, (b) unknown id → `onSuccess` with null, (c) DAO throwing → `onFailure(StudentError.Database)` in `app/src/test/java/dev/nenoeldeeb/education/absencerecord/data/repositories/StudentRepositoryImplTest.kt` (or a new `StudentRepositoryGetByIdTest.kt`; keep each file ≤ 300 lines).
- [X] T012 Implement `GetStudentByIdUseCase` in `app/src/main/java/dev/nenoeldeeb/education/absencerecord/domain/usecases/student/GetStudentByIdUseCase.kt` as a single-action class overriding `operator fun invoke(studentId: Int): Flow<Result<Student?>>` delegating 1:1 to the repository.
- [X] T013 [P] Write `GetStudentByIdUseCaseTest` in `app/src/test/java/dev/nenoeldeeb/education/absencerecord/domain/usecases/student/GetStudentByIdUseCaseTest.kt` verifying (a) repository success → useCase returns the student, (b) repository failure → `onFailure`, (c) the passed `studentId` is delegated. Use `mockk` + `runTest`.
- [X] T014 Wire `getStudentByIdUseCase` into the `StudentManagementUseCases` aggregator in `app/src/main/java/dev/nenoeldeeb/education/absencerecord/domain/usecases/StudentManagementUseCases.kt` by adding `val getStudentByIdUseCase: GetStudentByIdUseCase = GetStudentByIdUseCase(studentRepository)`.

**Checkpoint**: Single-student lookup works end-to-end. All later user stories build on this.

---

## Phase 3: User Story 1 - Navigate to Student Detail (Priority: P1) 🎯 MVP

**Goal**: Single-tapping a student in normal mode navigates to the Student Detail screen; back returns to the Students screen.

**Independent Test**: Tap any student name on the Students screen → Student Detail opens showing that student's name; pressing back returns to the list. Works even before other detail sections are built.

### Implementation

- [X] T015 [US1] Add `onStudentClick: (Int) -> Unit` callback parameter to `MainScreen` in `app/src/main/java/dev/nenoeldeeb/education/absencerecord/app/MainActivity.kt` and pass it down to `StudentsScreen`. The `HorizontalPager` keeps 3 pages until Phase 8 (T045). Keep `initialPage = 1`.
- [X] T016 [US1] Add `onStudentClick: (Int) -> Unit = {}` parameter to `StudentsScreen` in `app/src/main/java/dev/nenoeldeeb/education/absencerecord/presentation/screens/students/StudentsScreen.kt`; in the `StudentList.onStudentClick` lambda: when `!uiState.isMultiSelectionMode` call `onStudentClick(student.id)` (navigate) INSTEAD of `ShowStudentDialog`; when in multi-selection keep `ToggleStudentSelection`. Long-press flow unchanged.
- [X] T017 [US1] Create `AppNavigation` in `app/src/main/java/dev/nenoeldeeb/education/absencerecord/app/navigation/AppNavigation.kt` implementing state-driven navigation: `rememberSaveable` (via `listSaver`) back-stack seeded with `listOf(AppDestination.MainPager)`; `BackHandler(enabled = backStack.size > 1) { backStack = backStack.dropLast(1) }`; `AnimatedContent(targetState = backStack.last())` with push (slide-in from right + fade) / pop (reverse) transitions; render `MainScreen(contentPadding, onStudentClick)` for `MainPager` and `StudentDetailScreen(studentId, onNavigateBack)` for `StudentDetail`. NO navigation library.
- [X] T018 [US1] Update `MainActivity.setContent` in `app/src/main/java/dev/nenoeldeeb/education/absencerecord/app/MainActivity.kt` to host `AppNavigation(contentPadding)` inside the Scaffold (instead of direct `MainScreen`) and pass `onStudentClick = { id -> backStack = backStack + AppDestination.StudentDetail(id) }`.
- [X] T019 [US1] Create `StudentDetailViewModel` in `app/src/main/java/dev/nenoeldeeb/education/absencerecord/presentation/screens/studentdetail/StudentDetailViewModel.kt` with constructor `(private val studentId: Int, studentManagementUseCases, attendanceUseCases, reportUseCases, classManagementUseCases)`. In `init`: collect `getStudentByIdUseCase(studentId)`, `getAllClassesUseCase`, `getAvailableMonthsUseCase`, `getStudentAttendanceDatesUseCase(studentId)` and combine into `_uiState`; derive `assignedClassName` from `availableClasses` + `student.classId`; set `isLoading = false` once the student arrives; map failures to `error = e.toUiText()`. Expose `uiState: StateFlow`, `uiEffect: SharedFlow<StudentDetailUiEffect>`.
- [X] T020 [US1] Create `StudentDetailScreen` in `app/src/main/java/dev/nenoeldeeb/education/absencerecord/presentation/screens/studentdetail/StudentDetailScreen.kt`: `Scaffold` + `TopAppBar` with back button (contentDescription) + title = student name; observe `uiState` with `collectAsStateWithLifecycle()`; `CircularProgressIndicator` while `isLoading`; empty state if `student == null` after load; `LaunchedEffect` on `uiEffect` → call `onNavigateBack` when `NavigateBack`; snackbar host for toast/error; calendar + dialogs rendered in later phases.

### Tests for US1

- [X] T021 [P] [US1] Add `StudentDetailViewModelLoadTest.kt` (+ `StudentDetailViewModelTestBase.kt` mirroring the `ReportViewModelTest` base) in `app/src/test/java/dev/nenoeldeeb/education/absencerecord/presentation/screens/studentdetail/` verifying (a) load `onSuccess`: `student` set, `allAttendanceDates` populated, `isLoading = false`; (b) student not found → handled without crash; (c) use-case `onFailure` → `error` set. `runTest` + `mockk`.
- [X] T022 [P] [US1] Add `StudentsViewModelNavigationTest.kt` in `app/src/test/java/dev/nenoeldeeb/education/absencerecord/presentation/screens/students/` verifying single-tap in normal mode triggers navigation callback (and NOT the edit dialog), and taps in multi-selection mode toggle selection without navigating. Reuse the existing `StudentsViewModelTestBase` fixture style.

**Checkpoint**: Tap a student → Detail shell shows with the real name; back returns. MVP interaction works.

---

## Phase 4: User Story 2 - Long Press Starts Selection Mode (Priority: P2)

**Goal**: Long-pressing an item still enters multi-selection; single taps inside it toggle without navigating.

**Independent Test**: Long-press any student → selection mode activates with that student selected; tapping others toggles them; back exits selection.

- [X] T023 [US2] In `app/src/main/java/dev/nenoeldeeb/education/absencerecord/presentation/screens/components/StudentList.kt` confirm the `combinedClickable` long-press path (`onLongClick = { onStudentLongClick(student.id) }`) remains intact alongside the new click behavior; adjust only if the tap/navigate path conflicts with selection-mode entry.
- [X] T024 [P] [US2] Add `StudentsViewModelLongPressTest.kt` in `app/src/test/java/dev/nenoeldeeb/education/absencerecord/presentation/screens/students/` verifying (a) long-press turns on selection mode and selects that student, (b) another tap toggles selection without navigating, (c) long-press does not trigger `onStudentClick`.

**Checkpoint**: Bulk delete/export flows still usable after navigation change.

---

## Phase 5: User Story 3 & 3b - Edit / Change Class / Delete from Detail (Priority: P2)

**Goal**: From the Detail screen the teacher can edit the student's name (modal), change the class (dedicated dialog), and delete the student (confirmation → pop back).

### Dialogs

- [X] T025 [P] [US3] Create `EditStudentNameDialog.kt` in `app/src/main/java/dev/nenoeldeeb/education/absencerecord/presentation/screens/studentdetail/dialogs/EditStudentNameDialog.kt` (AlertDialog + `OutlinedTextField` pre-filled; Save/Cancel; Save disabled for empty/whitespace name; 48dp targets; content descriptions).
- [X] T026 [P] [US3] Create `ChangeClassDialog.kt` in `app/src/main/java/dev/nenoeldeeb/education/absencerecord/presentation/screens/studentdetail/dialogs/ChangeClassDialog.kt` (list of available classes + "Unassigned" option; Save/Cancel; content descriptions).
- [X] T027 [P] [US3b] Create `DeleteConfirmationDialog.kt` in `app/src/main/java/dev/nenoeldeeb/education/absencerecord/presentation/screens/studentdetail/dialogs/DeleteConfirmationDialog.kt` (warning icon, error-colored confirm, Cancel; content descriptions).

### ViewModel + Screen wiring

- [X] T028 [US3] Add handlers in `StudentDetailViewModel` for `ToggleEditNameDialog`, `ToggleChangeClassDialog`, `ToggleDeleteConfirmationDialog` flipping the matching dialog flags.
- [X] T029 [US3] Implement `UpdateStudentName(newName)` in `StudentDetailViewModel` (trim; `updateStudent` on `onSuccess` updates `student` in state; `onFailure` sets `error`); implement `UpdateStudentClass(classId)` (copy + recompute `assignedClassName`).
- [X] T030 [US3b] Implement `ConfirmDeleteStudent` in `StudentDetailViewModel` calling `deleteStudentsUseCase(listOf(student))`; on `onSuccess` emit `StudentDetailUiEffect.NavigateBack`; on `onFailure` set `error` (no navigation).
- [X] T031 [US3] Render the three dialogs from `StudentDetailScreen` bound to state flags and dispatching the corresponding events.
- [X] T032 [US3] Add the Detail top-bar action icons: back (exists), edit-name (title is tappable too), change-class, delete — with content descriptions and 48dp target sizes.
- [X] T033 [P] [US3] Remove now-unused list-edit wiring on the Students screen: delete `showEditDialog`/`UpdateStudent(event)` usage that was previously reachable via single-tap (add-flow stays); update `StudentsScreen`/`StudentsViewModel`/`StudentsScreenEvent` if they still reference edit mode.

### Tests

- [X] T034 [P] [US3] Add `StudentDetailViewModelEditTest.kt` in `app/src/test/java/dev/nenoeldeeb/education/absencerecord/presentation/screens/studentdetail/` verifying name-edit `onSuccess`/`onFailure` and class-change `onSuccess`/`onFailure`.
- [X] T035 [P] [US3b] Add `StudentDetailViewModelDeleteTest.kt` in the same package verifying delete `onSuccess` → `NavigateBack` effect emitted; delete `onFailure` → `error` set and no effect.

**Checkpoint**: Editing, class change, and deletion operate entirely from the Detail screen.

---

## Phase 6: User Story 4 - View Student Attendance Report (Priority: P2)

**Goal**: On the Detail screen, render the interactive view-only calendar grid, filter by month, and share the month's calendar image.

- [X] T036 [US4] In `StudentDetailViewModel` implement `SelectMonth(month)` to set `selectedMonth` and set `isMonthDropdownExpanded = false`; implement `ToggleMonthDropdown(expanded)` to set `isMonthDropdownExpanded`; implement `ShareAttendanceReport` calling `getAttendanceHistoryForDateRangeUseCase(studentId, monthStart, monthEnd)` then `reportUseCases.shareReportUseCase(studentName, month, dates)`; on success store the returned file `Uri` in `shareFileUri`; on failure set `toastMessage`; handle `ShareFileResult` and `ConsumeToastMessage`/`ConsumeError`.
- [X] T037 [US4] Render view-only `ComposeCalendar(interactive = false, onDateSelected = {})` inside `StudentDetailScreen` with `initialMonth = uiState.selectedMonth`, `markedDates = allAttendanceDates` filtered to the selected month, wrapped such that changing the month resets the calendar (e.g., `key(selectedMonth)`); show the "no attendance records" empty-state message (string from T005) when the student has no records for the selected month; add a month-filter `DropdownMenu` (from `availableMonths`, formatted with the existing `DateFormatter.toMonthYearUiText`) and a Share `IconButton`.
- [X] T038 [US4] Add `StudentDetailViewModelAttendanceTest.kt` in `app/src/test/java/dev/nenoeldeeb/education/absencerecord/presentation/screens/studentdetail/` covering (a) month select filters state, (b) share `onSuccess` → `shareFileUri` set, (c) share `onFailure` → `toastMessage`, (d) attendance-dates reload `onSuccess`/`onFailure`.
- [X] T039 [US4] In `StudentDetailScreen`, add the share-sheet harness mirroring `ReportScreen.kt` (LaunchedEffect on `uiState.shareFileUri` → chooser intent with `FLAG_GRANT_READ_URI_PERMISSION` → dispatch `ShareFileResult(uri, error)`); handle `ShareFileResult` in the ViewModel (clear `shareFileUri`; set `toastMessage` on error). This must be in place before Phase 8 deletes `ReportScreen.kt` — the only existing implementation of this harness.

**Checkpoint**: Detail shows the month-scoped attendance calendar and shares it.

---

## Phase 7: User Story 5 - Sort Students from Students Screen (Priority: P3)

**Goal**: Add a sort icon opening a `DropdownMenu` (Name / Attendance) on the Students top bar; selection reorders the list and is preserved within the session (ViewModel-held).

**Independent Test**: Tap sort icon → menu opens; pick "Attendance" → list reorders; navigate away/back → choice retained.

- [X] T040 [US5] Add `sortType: SortType = SortType.ByName` and `isSortMenuExpanded: Boolean = false` to `StudentsScreenState` in `app/src/main/java/dev/nenoeldeeb/education/absencerecord/presentation/screens/students/StudentsScreenState.kt`.
- [X] T041 [US5] Add `UpdateSortType(sortType: SortType)` and `ToggleSortMenuExpanded(expanded: Boolean)` events to `StudentsScreenEvent` in `app/src/main/java/dev/nenoeldeeb/education/absencerecord/presentation/screens/students/StudentsScreenEvent.kt`.
- [X] T042 [US5] In `StudentsViewModel`, thread `sortType` through `getAllStudentsUseCase(sortType, month = null)` (mirror `ReportViewModel`'s `_uiState.map { it.sortType }` observation so the flow re-emits on change) while preserving the `applyMultiClassFilter` class-filter step; handle `UpdateSortType` (update state → reload) and `ToggleSortMenuExpanded`.
- [X] T043 [P] [US5] Add the sort icon (`outline_sort_24` exists in `app/src/main/res/drawable/`) and a `DropdownMenu` (options from `sort_by_name`, `sort_by_attendance` strings) into the Students `TopAppBar` actions (order: Add, Filter, Sort); wire the events.
- [X] T044 [P] [US5] Add `StudentsViewModelSortTest.kt` in `app/src/test/java/dev/nenoeldeeb/education/absencerecord/presentation/screens/students/` verifying (a) selecting a sort option reorders students, (b) failure path sets `error`, (c) `sortType` persists in state after simulated re-entry.

**Checkpoint**: Sort icon shown; list reorders; choice persists.

---

## Phase 8: User Story 6 - Reports Screen Retired (Priority: P3)

**Goal**: Remove the Reports page so the pager is exactly 2 pages (Students ↔ Calendar); delete the `report` presentation + test packages.

**Independent Test**: App launches; swiping shows only Students and Calendar; no orphaned references; report share remains available from Detail.

- [X] T045 [US6] Update the `HorizontalPager` in `MainActivity.kt` to exactly 2 pages (0 = Students, 1 = Calendar) and remove the `2 -> ReportScreen` branch + its import.
- [X] T046 [US6] Delete `app/src/main/java/dev/nenoeldeeb/education/absencerecord/presentation/screens/report/` files: `ReportScreen.kt`, `ReportViewModel.kt`, `ReportScreenState.kt`, `ReportScreenEvent.kt`, `components/ReportControls.kt`, `dialogs/CalendarPreviewDialog.kt`, `dialogs/StudentHistoryDialog.kt`. Keep `domain/usecases/report/ShareReportUseCase` and the `data/` report generator (still needed by the Detail share flow).
- [X] T047 [US6] Remove the `ReportViewModel` provider wiring (initializer) from `AppViewModelProvider.kt`; drop unused imports; ensure `reportUseCases` remains wired for the Detail screen.
- [X] T048 [US6] Delete report tests under `app/src/test/java/dev/nenoeldeeb/education/absencerecord/presentation/screens/report/` (`ReportViewModelTest.kt`, `ReportViewModelSharingTest.kt`, `ReportViewModelTestBase.kt`); grep for remaining `ReportScreen*`/`ReportViewModel` references and clean them.
- [X] T049 [US6] Re-run `./gradlew compileDebugKotlin` and `./gradlew testDebugUnitTest` to confirm zero broken references after deletion.

**Checkpoint**: Build clean; pager = Students ↔ Calendar only; all report capability reachable from Detail.

---

## Phase 9: Polish & Cross-Cutting Concerns

- [X] T050 [P] Run `./gradlew ktlintFormat` and manually fix any remaining ktlint violations.
- [X] T051 Ensure accessibility compliance: meaningful `contentDescription` on new icons/dialogs/dropdowns, touch targets ≥ 48dp, no color-only state indication, across `studentdetail/*` and updated `students/*` files.
- [X] T052 [P] Verify no source file (src or test) exceeds 300 lines; split `StudentDetailScreen.kt` / `StudentDetailViewModel.kt` / dialogs / test base if needed.
- [X] T053 Update `AGENTS.md` and `README.md` documenting: Student Detail screen, composition-based navigation (`app/navigation/`), retired Reports screen, sort control on Students (constitution governance), and DI changes.
- [X] T054 Run `./gradlew check` (ktlint + lint + unit tests) and fix all failures; also run `./gradlew testDebugUnitTest --tests "dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.*"`.
- [ ] T055 Verify the Student Detail and updated Students screens with TalkBack (plus keyboard and switch access) on a device/emulator, confirming every new interactive element is announced, focusable, and reachable; fix any accessibility regressions before shipping (Constitution Principle VI). (manual — device)

**Checkpoint**: `./gradlew check` green; feature documented.

**Manual verification** (SC-001 / SC-004, on a mid-range device): detail screen loads within 1 second of tapping a student; long-press enters multi-selection within 500ms.

---

## Dependencies & Execution Order

```
Phase 1 (Setup) ─► Phase 2 (GetStudentById) ─► Phase 3 (US1: Navigate + Detail shell - MVP)
                                                  │
                                                  ├─► Phase 4 (US2: Long press)
                                                  ├─► Phase 5 (US3/3b: Edit/Delete)
                                                  ├─► Phase 6 (US4: Attendance calendar)
                                                  └─► Phase 7 (US5: Sort)
                                                           └─► Phase 8 (US6: Retire Reports)
                                                                    └─► Phase 9 (Polish)
```

- **Phase 1 → 2 → 3** strictly sequential (the `GetStudentByIdUseCase` must exist before the ViewModel initializers can collect it).
- **US4 (Phase 6)** depends only on Phase 3 (Detail ViewModel already exists); it does not wait for US2/US3/US5.
- **US5 (sort)** only touches the Students screen and its ViewModel — independent of US3/US4.
- **US6 (retire Reports)** must run last, only after US4's share flow is live (it deletes the old report code).

### Parallel opportunities

- T005 + T006 (EN/AR strings) and T001 run in parallel, early.
- T011 and T013 (repository + use-case tests) after T008–T010.
- After Phase 3, dialogs T025/T026/T027 can be done in parallel.
- T038 (attendance tests) and T039 (share harness) run after T036–T037; T039 must land before the T046 deletion of `ReportScreen.kt`.
- T040–T044 (sort state/VM/UI) run independently of Phase 5 and 6.

## Suggested MVP Scope

Only **Phase 1 + Phase 2 + Phase 3 (US1)**: tap any student → land on a Detail shell showing the name; back returns reliably. Foundation for all later work.

## Notes

- No `YearMonth`, no new `SortType` values, no navigation library, no new DB tables (data-model.md confirms `Student` unchanged).
- All DAO/repo functions return `Result<T>`; errors map to `toUiText()` at the Presentation boundary.
- Keep each file ≤ 300 lines; split into focused sub-composables/helpers when approaching the cap (constitution rule).
- Validate against `quickstart.md` scenarios 1–8; final gate `./gradlew check`.