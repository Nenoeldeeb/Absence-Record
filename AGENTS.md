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
- **Class Filter Pattern**: Unified multi-select class filter shared across Calendar (attendance dialog), Report, and Students screens.
  - `ClassFilterRepository` (domain/repositories/) + `ClassFilterRepositoryImpl` (data/repositories/) share an in-memory `selectedClassIds: StateFlow<Set<Int>>` (default `emptySet()`) observed by all three ViewModels.
  - `ClassCheckboxFilter` (presentation/screens/components/) is the shared multi-select checkbox dropdown used by all three screens.
  - Filter semantics: empty set = unassigned students only; checked classes = enrolled students in those classes; no "Select All" entry.

## Testing (JUnit 6 + MockK)

- **Philosophy**: MUST explicitly test BOTH `onSuccess` and `onFailure` paths. Verify ViewModel error state bindings.
- **UI Tests**: Use Compose rules (`@get:Rule`) and `mockk(relaxed = true)` for ViewModels.
- **Async Tests**: Advance virtual time with `runTest`.

## Build & Lint Commands

- **Unit Test**: `./gradlew testDebugUnitTest --tests "PackageOrClass"`
- **UI Test**: `./gradlew connectedDebugAndroidTest --tests "Class"`
- **Format**: `./gradlew ktlintFormat` (ALWAYS run before commit)
- **Validate**: `./gradlew check` (ktlint + lint + tests)

<!-- SPECKIT START -->
For additional context about technologies to be used, project structure,
shell commands, and other important information, read the current plan:
specs/004-unified-error-handling/plan.md
<!-- SPECKIT END -->

## Active Technologies

- Kotlin 2.4.0+ / Kotlin JVM 25 + Jetpack Compose, Room, Kotlinx Datetime, Kotlinx Serialization (001-refactor-students-viewmodel)
- Room SQLite database (001-refactor-students-viewmodel)

## Recent Changes

- 005-unified-class-filter: Unified class filter via `ClassFilterRepository`/`ClassFilterRepositoryImpl` (in-memory `selectedClassIds: StateFlow<Set<Int>>`), shared `ClassCheckboxFilter` across Calendar/Report/Students, and removed `ClassFilterDropdown`/`ClassFilter`.
- 002-class-filter-refactor: Moved class filter from top bar into attendance dialog as multi-select checkbox dropdown. Added `applyMultiClassFilter` extension, `ClassCheckboxFilter` composable, `Set<Int>` filter state, dynamic count, and attendance count clamping. Removed old filter icon and `ClassFilterDropdown` from calendar screen.
- 001-refactor-students-viewmodel: Added Kotlin 2.4.0+ / Kotlin JVM 25 + Jetpack Compose, Room, Kotlinx Datetime, Kotlinx Serialization
- 003-resolve-constitution-violations: Fixed 10 file-size violations + 6 architecture/accessibility violations. Added handler classes (`handlers/`), DayCell extraction, test base class pattern, and split use cases.
