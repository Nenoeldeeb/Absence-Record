# UI & Navigation Contracts: Student Detail Screen

**Feature Branch**: `006-student-detail-screen` | **Date**: 2026-08-06

## Navigation Contracts (Compose-only, NO navigation library)

### Destinations

1. **`AppDestination.MainPager`** (root):

- Displays `MainScreen` holding `HorizontalPager` with 2 pages:
  - Page 0: `StudentsScreen`
  - Page 1: `CalendarScreen`
- Represents the bottom of the back-stack; `BackHandler` disabled at this level.

2. **`AppDestination.StudentDetail(studentId: Int)`**:

- Pushed on top of `MainPager` when a student is tapped in normal mode.
- Displays `StudentDetailScreen(studentId = studentId, onNavigateBack = { backStack = backStack.dropLast(1) })`.

### Back-Stack Host (`app/navigation/AppNavigation.kt`)

- State: `var backStack by rememberSaveable { mutableStateOf(listOf<AppDestination>(AppDestination.MainPager)) }`.
- System back (button/gesture): `BackHandler(enabled = backStack.size > 1) { backStack = backStack.dropLast(1) }`.
- Pop also triggers UI effect `StudentDetailUiEffect.NavigateBack` path (delete-confirm) so the deletion screen pops itself.
- Transitions (`AnimatedContent`): push = `slideInHorizontally { it } + fadeIn()` vs `slideOutHorizontally { -it / 3 } + fadeOut()`; pop = reversed direction.
- Dialog precedence: modal dialogs intercept back before `BackHandler` at the host level.

### Top Bar Contract Updates

#### `StudentsScreen` Top App Bar Actions:

- **Add Student Icon**: Triggers add student dialog.
- **Filter Class Icon**: Toggles `ClassCheckboxFilter`.
- **Sort Icon** (`R.drawable.outline_sort_24`): Toggles `DropdownMenu` with options:
  - Name
  - Absence Count

#### `StudentDetailScreen` Top App Bar:

- **Back Navigation Icon**: Invokes `onNavigateBack`.
- **Title**: Student Name (tappable to trigger Edit Name dialog).
- **Change Class Icon**: Triggers Change Class dialog.
- **Delete Student Icon**: Triggers Delete Confirmation dialog.

---

## ViewModel Contracts

### `StudentDetailViewModel` Interface & Responsibilities

- **Inputs**:
  - `studentId: Int` passed directly to a custom factory (`AppViewModelProvider.studentDetailFactory(studentId)`), NOT `SavedStateHandle`/route parsing.
- **Observed Flows**:
  - Student details via `GetStudentByIdUseCase` / `StudentRepository`.
  - Assigned class via `StudentClassRepository`.
  - Attendance history via `GetStudentAttendanceDatesUseCase`.
  - Available classes via `GetAllClassesUseCase`.
- **Exposed Outputs**:
  - `uiState: StateFlow<StudentDetailScreenState>`
  - `uiEffect: SharedFlow<StudentDetailUiEffect>`
