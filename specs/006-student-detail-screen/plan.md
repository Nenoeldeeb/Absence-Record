# Implementation Plan: Student Detail Screen

**Branch**: `006-student-detail-screen` | **Date**: 2026-08-06 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/006-student-detail-screen/spec.md`

## Summary

Add a dedicated `StudentDetailScreen` accessible by tapping a student's name on the `StudentsScreen`. The screen displays the student's name, assigned class, and interactive view-only `ComposeCalendar` grid with month filtering and report sharing capabilities. It provides modal dialogs for editing student name, changing class assignment, and deleting the student (with confirmation). Single-tap on `StudentsScreen` navigates to `StudentDetailScreen` (slide-in push), while long-press enters multi-selection mode. Sorting is moved to `StudentsScreen`'s top bar as a `DropdownMenu`. The `Reports` screen is retired, reducing the main `HorizontalPager` to 2 pages (Students ↔ Calendar).

**Navigation**: NO navigation library. Plain Compose composition-based navigation: a sealed `AppDestination` back-stack held in state, `androidx.activity.compose.BackHandler` for system back (button + gesture), and `AnimatedContent` with slide/fade transitions for push/pop. Exactly one navigation host composable (`AppNavigation`) in `MainActivity`.

## Technical Context

**Language/Version**: Kotlin 2.4.0+ / Kotlin JVM 25

**Primary Dependencies**: Jetpack Compose (incl. `androidx.compose.animation`), Room, Kotlinx Datetime, Kotlinx Serialization, `androidx.activity:activity-compose` (for `BackHandler`). Explicitly NO `androidx.navigation:*`.

**Storage**: Room SQLite database

**Testing**: JUnit, MockK, Compose rules, runTest

**Target Platform**: Android (API 26+)

**Project Type**: Android App Module (Clean + MVI)

**Performance Goals**: Responsive UI at 60+ fps (`AnimatedContent` transitions must not block Main thread); no Main-thread blocking for CPU-intensive operations (parsing/db/serialization).

**Constraints**: Main safe backgrounding via dispatchers, manual DI, Arabic localization support, max 300 lines per source/test file, no navigation library.

**Scale/Scope**: Local offline-first architecture.

## Constitution Check

_GATE: Must pass before Phase 0 research. Re-check after Phase 1 design._

- [x] Clean Architecture Check: Are Domain, Data, and Presentation layers separated?
- [x] MVI / UDF Check: Is there a single immutable ScreenState, a ScreenEvent sealed interface, and lifecycle-aware collection?
- [x] Technology Check: Are we using `kotlinx-datetime`, `@Serializable`, Room, and `strings.xml` / `UiText`? (Navigation stays inside `androidx.compose.*`/`androidx.activity.*` — no new library.)
- [x] Test Check: Do we have test cases for both onSuccess and onFailure paths?
- [x] Main-Safety Check: Are intensive tasks offloaded to appropriate background dispatchers?
- [x] File Size Check: Does every source file (including tests) stay within 300 lines?
- [x] Accessibility Check: Are all interactive composables accessible (content descriptions, 48dp touch targets, contrast)?

## Project Structure

### Documentation (this feature)

```text
specs/006-student-detail-screen/
├── spec.md              # Feature specification
├── plan.md              # Implementation plan (this file)
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 state & data models
├── quickstart.md        # Phase 1 validation scenarios
├── contracts/           # Phase 1 UI & Navigation contracts
│   └── ui-navigation-contracts.md
└── tasks.md             # Phase 2 output (/speckit-tasks - regenerated to drop NavHost references)
```

### Source Code (repository root)

```text
app/src/main/java/dev/nenoeldeeb/education/absencerecord/
├── app/
│   ├── MainActivity.kt                  # setContent { AppNavigation(contentPadding) }; MainScreen = 2-page HorizontalPager (Students, Calendar)
│   ├── navigation/
│   │   ├── AppDestination.kt            # sealed interface AppDestination { MainPager; StudentDetail(studentId: Int) }
│   │   └── AppNavigation.kt             # Back-stack state + BackHandler + AnimatedContent (push/pop slide+fade)
│   ├── AppContainer.kt                  # DI container (adds StudentDetailViewModel deps)
│   └── AppViewModelProvider.kt          # studentDetailFactory(studentId)
├── domain/
│   ├── models/                          # Student, SortType (unchanged)
│   ├── repositories/                   # StudentRepository (+ getStudentById)
│   └── usecases/
│       ├── student/                    # AddStudent, DeleteStudents, GetAllStudents, UpdateStudent, GetStudentById (NEW)
│       ├── attendance/                 # GetStudentAttendanceDates, GetAvailableMonths, etc. (reused)
│       └── report/                     # ShareReportUseCase (reused)
├── presentation/
│   ├── screens/
│   │   ├── components/
│   │   │   ├── ComposeCalendar.kt      # Shared calendar grid (view-only mode on detail)
│   │   │   ├── ClassCheckboxFilter.kt  # Shared class filter (unchanged)
│   │   │   └── StudentList.kt          # + single-tap nav callback & long-press selection
│   │   ├── students/                   # + Sort DropdownMenu icon, single-tap nav, sort persistence
│   │   └── studentdetail/              # NEW feature package
│   │       ├── StudentDetailScreen.kt          # Main detail composable
│   │       ├── StudentDetailViewModel.kt      # Detail ViewModel
│   │       ├── StudentDetailScreenState.kt    # Single immutable ScreenState
│   │       ├── StudentDetailScreenEvent.kt    # Sealed interface ScreenEvent
│   │       ├── StudentDetailUiEffect.kt       # One-time side effects (NavigateBack)
│   │       └── dialogs/
│   │           ├── EditStudentNameDialog.kt
│   │           ├── ChangeClassDialog.kt
│   │           └── DeleteConfirmationDialog.kt
│   └── theme/ & utils/
```

### Navigation Host (replaces NavHost)

```kotlin
// app/navigation/AppDestination.kt
sealed interface AppDestination {
    data object MainPager : AppDestination
    data class StudentDetail(val studentId: Int) : AppDestination
}

// app/navigation/AppNavigation.kt
@Composable
fun AppNavigation(contentPadding: PaddingValues) {
    var backStack by rememberSaveable(
        // listSaver keeps the two-level stack across process death
    ) { mutableStateOf(listOf<AppDestination>(AppDestination.MainPager)) }
    val current = backStack.last()

    BackHandler(enabled = backStack.size > 1) { backStack = backStack.dropLast(1) }

    AnimatedContent(
        targetState = current,
        transitionSpec = {
            if (targetState is AppDestination.StudentDetail) {
                (slideInHorizontally { it } + fadeIn()) togetherWith
                    (slideOutHorizontally { -it / 3 } + fadeOut())
            } else {
                (slideInHorizontally { -it / 3 } + fadeIn()) togetherWith
                    (slideOutHorizontally { it } + fadeOut())
            }
        },
        label = "app_navigation"
    ) { destination ->
        when (destination) {
            AppDestination.MainPager -> MainScreen(
                contentPadding = contentPadding,
                onStudentClick = { id -> backStack = backStack + AppDestination.StudentDetail(id) }
            )
            is AppDestination.StudentDetail -> StudentDetailScreen(
                studentId = destination.studentId,
                onNavigateBack = { backStack = backStack.dropLast(1) }
            )
        }
    }
}
```

Push = new screen slides in from the right; pop = it slides out to the right while the pager fades/slides back in. `BackHandler` (already available via `androidx.activity:activity-compose`, used in `MainActivity` today) covers system back button and predictive back gesture.

**Structure Decision**: Clean Architecture + MVI. Navigation is plain Compose state (`listOf<AppDestination>`) + `BackHandler` + `AnimatedContent` — NO `navigation-compose`/`NavHost`/route strings. New feature is packaged under `presentation/screens/studentdetail/` with modular dialog composables to enforce the 300-line/file rule. `AppNavigation` keeps its own `BackHandler` so dialogs can still intercept back first.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|--------------------------------------|
| None      | N/A        | N/A                                  |