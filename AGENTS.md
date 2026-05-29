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

## Kotlin & Project Specifics

- **Dates**: Use `kotlinx.datetime` exclusively (NO `java.time`).
- **Strings**: Use `strings.xml` (with `values-ar/` support) and the `UiText` wrapper state. NO hardcoded strings.
- **Serialization**: Use `kotlinx.serialization` (`@Serializable`).
- **DI**: Manual via `AppContainer` and `AppViewModelProvider`.

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
shell commands, and other important information, read the current plan
<!-- SPECKIT END -->

## Active Technologies
- Kotlin 2.4.0+ / Kotlin JVM 25 + Jetpack Compose, Room, Kotlinx Datetime, Kotlinx Serialization (001-refactor-students-viewmodel)
- Room SQLite database (001-refactor-students-viewmodel)

## Recent Changes
- 001-refactor-students-viewmodel: Added Kotlin 2.4.0+ / Kotlin JVM 25 + Jetpack Compose, Room, Kotlinx Datetime, Kotlinx Serialization
