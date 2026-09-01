# AGENTS.md - LLM Code Guidelines

## Core Architecture (Clean + MVI)

- **Domain (`domain/`)**: `models/`, `repositories/` (interfaces), `usecases/` (single action, `operator fun invoke`), `services/`.
- **Data (`data/`)**: `repositories/` (impls), `datasources/local/` (Room Entities/DAOs), `mappers/`.
  - **Rule**: Standardize return types to `Result<T>`. NO `try-catch` escaping to the domain/presentation layers; handle with `onSuccess`/`onFailure`.
- **Presentation (`presentation/`)**:
  - `screens/components/` (Shared UI composables).
  - `screens/[feature]/` (Feature-grouped files). Includes `Screen.kt`, `ViewModel.kt` (Suffix: `ViewModel`), `ScreenState.kt` (Suffix: `ScreenState`), `ScreenEvent.kt` (Suffix: `ScreenEvent`).
  - `theme/` and `utils/`.

## State & ViewModels

- **State**: Single immutable data class marked `@Stable`. Mutate strictly via `_uiState.update { it.copy(...) }`. Use `derivedStateOf` for derived properties.
- **Events**: Defined as `sealed interface ScreenEvent`.
- **Composable Observation**: Collect state ONLY via `collectAsStateWithLifecycle()`.
- **Async Execution**: Use `viewModelScope.launch`. NEVER create raw `Job` instances.

## Accessibility

- All interactive composables MUST have meaningful `contentDescription` values.
- Touch targets MUST be at least 48dp.
- Color MUST NOT be the sole indicator of state; use icons or text labels too.
- Test each screen with TalkBack before shipping.

## Code Quality

- Every source file (including test files) MUST NOT exceed 300 lines. Break
  large files into focused units.

## Kotlin & Project Specifics

- **Dates**: Use `kotlinx.datetime` exclusively (NO `java.time`).
- **Strings**: Use `strings.xml` (with `values-ar/` support) and the `UiText` wrapper state. NO hardcoded strings.
- **Serialization**: Use `kotlinx.serialization` (`@Serializable`).
- **DI**: Manual via `AppContainer` and `AppViewModelProvider`.
- **Class Filter Pattern**: Unified multi-select class filter shared across Calendar (attendance dialog) and Students screens.
  - `ClassFilterRepository` (domain/repositories/) + `ClassFilterRepositoryImpl` (data/repositories/) share an in-memory `selectedClassIds: StateFlow<Set<Int>>` (default `emptySet()`) observed by both ViewModels.
  - `ClassCheckboxFilter` (presentation/screens/components/) is the shared multi-select checkbox dropdown used by both screens.
  - Filter semantics: empty set = unassigned students only; checked classes = enrolled students in those classes; no "Select All" entry.

## Testing (JUnit 6 + MockK)

- **Philosophy**: MUST explicitly test BOTH `onSuccess` and `onFailure` paths. Verify ViewModel error state bindings.
- **UI Tests**: Use Compose rules (`@get:Rule`) and `mockk(relaxed = true)` for ViewModels.
- **Async Tests**: Advance virtual time with `runTest`.

## Build & Lint Commands

- **Unit Test**: `./gradlew testDebugUnitTest --tests "PackageOrClass"`
- **UI Test**: `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class="PackageOrClass"` (AGP 9 does NOT support `--tests` on this task)
- **Format**: `./gradlew ktlintFormat` (ALWAYS run before commit)
- **Validate**: `./gradlew check` (ktlint + lint + tests)

<!-- SPECKIT START -->
For additional context about technologies to be used, project structure,
shell commands, and other important information, read the current plan:
specs/007-lesson-scheduling/plan.md
<!-- SPECKIT END -->

## Active Technologies

- Kotlin 2.4.0+ / Kotlin JVM 25 + Jetpack Compose, Room, Kotlinx Datetime, Kotlinx Serialization (001-refactor-students-viewmodel)
- Room SQLite database (001-refactor-students-viewmodel)

## Recent Changes

- 007-lesson-scheduling: Added weekly lesson scheduling. The Schedule page (pager page 2, next to Calendar) is the weekly availability plan ONLY: Saturday-first `WeekdaySelector` + list of hours with occupancy "x / y" (add/edit/delete via `HourDialog` — start time from a Material3 `TimePickerDialog` at exact-minute granularity — + delete confirm); each `HourRow` is expandable (`expandedHourIds` in `ScheduleViewModel` state, surviving tap-to-profile return) to show assigned student names READ-ONLY; tapping a name pushes `AppDestination.StudentDetail`. Assignment and busy management moved to the Student Detail screen (2026-08-09 spec revision): a Saturday-first `WeekdaySelector` (reused from schedule/components) + `SecondaryTabRow` "Lessons" | "Busy". Lessons tab: per-weekday lessons with unassign (hour id resolved from `observeHoursForWeekdayUseCase` by matching `startMinutes` + `assignedStudentIds`), "Add lesson" unavailable when the student already has a lesson that day (FR-019), `AddLessonDialog` offers only eligible hours (capacity FR-007, no busy overlap FR-009, "no hours for this day" hint FR-018). Busy tab: per-weekday busy CRUD via `BusyAppointmentDialog` (Material3 time picker start at exact-minute granularity + 0.5h–6h duration slider in 30-min steps with a live "Ends at" preview; a midnight-crossing block is invalid and blocks confirm) with PREVIEW-AND-CONFIRM — conflicting lessons are listed and removed only after the teacher confirms (FR-011); delete restores assignability (FR-012). Room schema: `available_lesson_hours`, `lesson_assignments`, `busy_appointments` tables with cascading deletes and overlap guards in `ScheduleDao` (busy insert/update auto-removal report retained at the data layer). Domain: `AvailableLessonHour`/`HourWithOccupancy`/`BusyAppointment`, `ScheduleRepository`, `ScheduleRules` (domain/services), use cases. Delegates: `ScheduleViewModel` → `HourManagementDelegate`; `StudentDetailViewModel` → `StudentScheduleTabsDelegate` + `BusyManagementDelegate` + `StudentScheduleDelegate` + `StudentDetailReportDelegate`. Instrumented UI tests use a mockk-free `ScheduleScreenTestHarness` (MutableStateFlow + event recorder + `ScheduleScreenContent` stateless extraction) because mockk-android cannot mock final ViewModels on-device; `connectedDebugAndroidTest` must use `-Pandroid.testInstrumentationRunnerArguments.class=...` (no `--tests` in AGP 9). Backup/restore (rev 3, 2026-08-09): the Students-screen import/export now carries schedule data via a single versioned JSON object `{version: 2, students, availableHours}` — export always writes the FULL availability plan regardless of selected students plus each selected student's `lessonAssignments`/`busyAppointments` (natural-key `weekday`+`startMinutes`, ids rebuilt on import); import merges additively (hours overlapping an existing hour skipped, imported busy wins over existing lessons, existing-hour capacity raised to fit imported lessons, orphan/duplicate/malformed entries skipped + counted), legacy flat-array files still restore students + dates only, unknown/higher versions rejected with `StudentError.UnsupportedBackupVersion`, and the post-import message reports schedule outcomes (FR-022…FR-033). Transfer use cases (data: `ScheduleBackupData`/`ParsedImportData` models): `ExportStudentsUseCase`, `ParseImportFileUseCase`, `PerformImportUseCase`.
- 006-student-detail-screen: Added Student Detail screen (view/edit name, change class, delete, month-filtered attendance calendar with per-month share image) reachable by tapping a student. Composition-based navigation in `app/navigation/` (`AppDestination` sealed interface + `AppNavigation` with `rememberSaveable` back-stack, `BackHandler`, `AnimatedContent`) — NO navigation library. Retired the Report screen: pager is now exactly 2 pages (0: Students, 1: Calendar) and `presentation/screens/report/**` + its tests were deleted; share/attendance reporting moved into Student Detail (ReportUseCases kept for Detail share). Added a sort control on the Students top bar: `StudentsTopBar` with a `SortType` `DropdownMenu` (ByName/ByAttendance, existing 2-value enum — NOT expanded), threaded through `StudentsViewModel.loadData()` via `_uiState.map { it.sortType }.distinctUntilChanged()` + `flatMapLatest`. DI: `AppViewModelProvider.studentDetailFactory(studentId)` added; ReportViewModel
  initializer removed.
- 005-unified-class-filter: Unified class filter via `ClassFilterRepository`/`ClassFilterRepositoryImpl` (in-memory `selectedClassIds: StateFlow<Set<Int>>`), shared `ClassCheckboxFilter` across Calendar/Report/Students, and removed `ClassFilterDropdown`/`ClassFilter`.
- 002-class-filter-refactor: Moved class filter from top bar into attendance dialog as multi-select checkbox dropdown. Added `applyMultiClassFilter` extension, `ClassCheckboxFilter` composable, `Set<Int>` filter state, dynamic count, and attendance count clamping. Removed old filter icon and `ClassFilterDropdown` from calendar screen.
- 001-refactor-students-viewmodel: Added Kotlin 2.4.0+ / Kotlin JVM 25 + Jetpack Compose, Room, Kotlinx Datetime, Kotlinx Serialization
- 003-resolve-constitution-violations: Fixed 10 file-size violations + 6 architecture/accessibility violations. Added handler classes (`handlers/`), DayCell extraction, test base class pattern, and split use cases.
