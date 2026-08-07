# Phase 0 Research: Student Detail Screen & Navigation Architecture

**Feature Branch**: `006-student-detail-screen` | **Date**: 2026-08-05

## Technical Decisions & Rationale

### 1. Navigation Architecture (Custom Compose State Stack + BackHandler + AnimatedContent)

- **Decision**: Completely avoid third-party or Jetpack Navigation libraries (`navigation-compose`). Implement state-driven navigation directly in Compose using a sealed interface `AppDestination` back-stack (`listOf<AppDestination>`), `androidx.activity.compose.BackHandler`, and `androidx.compose.animation.AnimatedContent` with slide/fade transitions.
- **Rationale**:
  - Eliminates external navigation library dependencies and extra boilerplate/route string parsing.
  - Type-safe navigation passing primitive `studentId: Int` directly in `AppDestination.StudentDetail(val studentId: Int)`.
  - Retains the exact 2-page swipeable `HorizontalPager` experience for Students ↔ Calendar as specified in Q4 clarification when `AppDestination.MainPager` is active.
  - Native back gesture & back button integration via `BackHandler(enabled = backStack.size > 1) { backStack = backStack.dropLast(1) }`.
  - Rich, customizable UI transitions using built-in Jetpack Compose animation APIs (`AnimatedContent`, `slideInHorizontally`, `slideOutHorizontally`, `fadeIn`, `fadeOut`, `togetherWith`).
- **Transition contract** (`AppNavigation.kt`): `AnimatedContent(targetState = backStack.last())` with a direction-aware `transitionSpec`.
  - Push (`initial = MainPager`, `target = StudentDetail`): detail `slideInHorizontally { it } + fadeIn()`, pager `slideOutHorizontally { -it / 3 } + fadeOut()`, combined via `togetherWith`.
  - Pop (`initial = StudentDetail`, `target = MainPager`): same spec animates in reverse (detail slides out right, pager slides back from left) — `AnimatedContent` auto-swaps enter/exit roles.
  - Process death: back-stack persisted via `rememberSaveable` (`listSaver`); `studentId` survives rotation/recreation.
- **BackHandler precedence**: `BackHandler(enabled = backStack.size > 1)` lives in `AppNavigation`; modal dialogs (name edit, change class, delete) own the back callback while open, so host back-stack pop is suppressed until dialogs close.
- **Alternatives Considered**:
  - *Jetpack Navigation (`navigation-compose`)*: Rejected per explicit requirement to remove any navigation library.
  - *Static screen toggle without animations*: Rejected because smooth push/pop motion enhances UX when entering detail screens.

### 2. Manual ViewModel Dependency Injection for `StudentDetailViewModel`

- **Decision**: Pass `studentId: Int` directly to a custom ViewModel factory method in `AppViewModelProvider` (e.g., `AppViewModelProvider.studentDetailFactory(studentId)`).
- **Rationale**:
  - Strictly follows Constitution Principle III (Manual DI via `AppContainer` and `AppViewModelProvider`).
  - Completely decouples ViewModels from navigation libraries or `SavedStateHandle` route string parsing.
  - `viewModel(key = "student_detail_$studentId", factory = ...)` ensures ViewModels are scoped correctly per student ID.
- **Alternatives Considered**:
  - *Global singleton / shared ViewModel*: Rejected because detail screen state must be scoped to the active student being viewed.

### 3. Reuse & Adaptation of Report Components (`ComposeCalendar` & Use Cases)

- **Decision**:
  - Reuse existing `ComposeCalendar.kt` (interactive grid composable) on `StudentDetailScreen` in view-only / read-only mode (onDayClick ignored or disabled).
  - Move/reuse `GetStudentAttendanceDatesUseCase`, `GetAvailableMonthsUseCase`, and `ShareReportUseCase` directly into `StudentDetailViewModel`.
  - Delete `ReportScreen.kt`, `ReportViewModel.kt`, `ReportScreenState.kt`, `ReportScreenEvent.kt`, `ReportControls.kt`, `CalendarPreviewDialog.kt`, and `StudentHistoryDialog.kt`.
- **Rationale**:
  - `ComposeCalendar` is already built and tested.
  - Per Q3 clarification, the detail screen reuses `ComposeCalendar`. Per Q7, it is view-only.
  - Eliminates all code duplication once the Reports screen is retired.

### 4. Moving Sorting to `StudentsScreen`

- **Decision**:
  - Add `sortType: SortType` (default `SortType.ByName`) and `isSortMenuExpanded: Boolean` to `StudentsScreenState`.
  - Add `ToggleSortMenuExpanded(expanded: Boolean)` and `UpdateSortType(sortType: SortType)` to `StudentsScreenEvent`.
  - Render a sort icon in `StudentsScreen` top bar next to add and filter icons.
  - Tapping sort icon opens `DropdownMenu` with `SortType` entries.
  - Update `StudentsViewModel` (or `StudentActionDelegate`) to sort the student list based on active `SortType`.
- **Rationale**:
  - Per Q8 clarification, sort options are presented via a top-bar `DropdownMenu`.
  - Sorting was previously available on the Reports screen; moving it to `StudentsScreen` preserves feature parity.

### 5. Detail Screen Editing & Deletion Flows

- **Decision**:
  - Name editing: Triggered via tap on name or edit icon; opens `EditStudentNameDialog` (modal dialog with text field, Save, Cancel). Save triggers `StudentDetailScreenEvent.UpdateStudentName`.
  - Class assignment: Triggered via "Change Class" action; opens `ChangeClassDialog` (modal with class choices). Triggers `StudentDetailScreenEvent.UpdateStudentClass`.
  - Deletion: Triggered via top-bar delete icon; opens `DeleteConfirmationDialog`. Confirm triggers `StudentDetailScreenEvent.ConfirmDeleteStudent`, which invokes `DeleteStudentsUseCase` and emits a UI effect `NavigateBack`.
- **Rationale**:
  - Aligns exactly with Q1 (modal edit name), Q5 (delete icon + dialog -> pop back), and Q6 (dedicated Change Class button).

## Architecture & Code Quality Constraints Checklist

- **Clean Architecture**: Domain layer use cases called by ViewModel. ViewModel maps to ScreenState.
- **Max 300 Lines per File**: Keep `StudentDetailScreen.kt`, `StudentDetailViewModel.kt`, and dialog composables in separate focused files under `presentation/screens/studentdetail/`.
- **Strings & A11y**: All new text in `strings.xml` & `values-ar/strings.xml`. Touch targets >= 48dp. Content descriptions on all icons.
- **No Navigation Libraries**: Uses state-driven stack (`listOf<AppDestination>`), `BackHandler`, and `AnimatedContent`.
