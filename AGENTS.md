# AGENTS.md - Guidelines for Agentic Coding

This document provides guidelines for AI agents working on this codebase. Follow these conventions strictly to maintain consistency and code quality.

## Project Overview

Absence Record is an Android application for student attendance tracking. Built with Kotlin, Jetpack Compose, Room database, and Clean Architecture. The app uses MVI (Model-View-Intent) for state management and enforces strict separation of concerns across three layers.

## Build Commands

### Building

```bash
./gradlew assembleDebug        # Debug build
./gradlew assembleRelease      # Release build
./gradlew build                # Full build with tests
```

### Running Tests

```bash
./gradlew test                           # All unit tests
./gradlew testDebugUnitTest              # Unit tests only (no instrumented tests)
./gradlew connectedDebugAndroidTest      # Instrumented tests (requires connected device/emulator)
./gradlew check                          # All checks (ktlint + lint + unit tests)
```

### Running a Single Test

```bash
# Unit test - fully qualified class name
./gradlew testDebugUnitTest --tests "dev.nenoeldeeb.education.absencerecord.presentation.utils.DateFormatterTest"

# Instrumented test - requires emulator/device
./gradlew connectedDebugAndroidTest --tests "dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenTest"

# Run multiple test classes
./gradlew testDebugUnitTest --tests "dev.nenoeldeeb.education.absencerecord.presentation.**"
```

### Linting & Code Quality

```bash
./gradlew ktlintCheck       # Check code style without fixing
./gradlew ktlintFormat      # Auto-format all Kotlin sources
./gradlew lint              # Android lint checks
./gradlew check             # All checks (ktlint + lint + tests)
```

**Important:** `ktlintFormat` runs automatically before every build (`preBuild` task). Always run lint checks before committing.

---

## Code Style Guidelines

### Architecture

This project follows **Clean Architecture** with three distinct layers:

- **Domain Layer** (`domain/`): Business logic, use cases, repository interfaces, domain models
- **Data Layer** (`data/`): Repository implementations, Room database, data sources
- **Presentation Layer** (`presentation/`): Compose UI, ViewModels, screens, components

**MVI Pattern**: UI state is managed using single immutable `State` data class per screen. Events are handled via sealed `Event` interface. ViewModels expose `StateFlow<State>` for UI observation.

### Package Structure

```
app/src/main/java/dev/nenoeldeeb/education/absencerecord/
├── app/                  # Application class, DI container, ViewModel provider
├── domain/
│   ├── models/           # Domain models (Student, Attendance, etc.)
│   ├── repositories/    # Repository interfaces
│   └── usecases/        # Use case classes
├── data/
│   ├── datasources/     # Room DAOs, local data sources
│   ├── repositories/    # Repository implementations
│   └── utils/           # Data utilities
└── presentation/
    ├── screens/         # Screen composables, ViewModels, state, events
    │   └── [feature]/
    │       ├── Screen.kt
    │       ├── ViewModel.kt
    │       ├── ScreenState.kt
    │       └── ScreenEvent.kt
    ├── theme/           # Material3 theme (Color, Type, Theme)
    └── utils/           # UI utilities (UiText, DateFormatter, etc.)
```

### Naming Conventions

- **Classes**: PascalCase (`StudentsViewModel`, `StudentRepository`)
- **Functions**: camelCase (`onEvent`, `collectAsStateWithLifecycle`)
- **Files**: PascalCase matching class name (`StudentsScreen.kt`)
- **Packages**: lowercase (`dev.nenoeldeeb.education.absencerecord.domain.models`)
- **ViewModel**: `[Feature]ViewModel` suffix (`StudentsViewModel`, `CalendarViewModel`)
- **Screen State**: `[Feature]ScreenState` suffix
- **Screen Event**: `[Feature]ScreenEvent` sealed interface

### Kotlin Conventions

1. **Imports**: Grouped and ordered:

- Kotlin/Java standard library
- Android framework (`androidx.*`)
- Third-party libraries
- Project imports
- Use explicit imports (avoid wildcard `*`)
- Don't use Java libraries if Kotlin has an alternative

2. **Visibility Modifiers**: Use explicit modifiers. Prefer `private` for class members.

3. **Data Classes**: Use for immutable state objects and DTOs. Prefer `val` over `var`.

4. **Coroutines**: Use `viewModelScope.launch` for async operations. Use `Result<T>` for operations that can fail.

5. **Flow**: Use `StateFlow` for UI state. Collect using `collectAsStateWithLifecycle()` in Compose.

6. **Error Handling**: Use `Result.onSuccess { }.onFailure { }` pattern instead of try-catch for repository/use case results.

7. **Compose**:

- Use `@Composable` annotation for all UI functions
- Add `@Stable` annotation to ViewModels State and Event wrappers
- Use `remember` and `rememberSaveable` for UI state (prefer `rememberSaveable` for important state)
- Use `derivedStateOf` for computed state that should not recompose parent
- Never mutate state directly—always use immutable updates
- Group related imports

### Code Organization

1. **Use Cases**: One class per file, named `[Action]UseCase`. Constructor injection of dependencies. Invoke via `operator fun invoke()`.

2. **Repository**: Interface in `domain/repositories`, implementation in `data/repositories`.

3. **ViewModels**: Single `uiState: StateFlow<ScreenState>`, single `onEvent(event: ScreenEvent)` function. Use `MutableStateFlow` internally, expose as `StateFlow`. Always wrap state in `@Stable` annotation.

4. **Screen State**: Immutable data class with all UI state. No functions, only data. Mark with `@Stable` annotation.

5. **Screen Events**: Sealed interface/class hierarchy for all user interactions.

### Sealed Classes vs Sealed Interfaces

- Use **sealed interface** for events (allowing multiple implementation): `sealed interface ScreenEvent { data class MyEvent(...) : ScreenEvent }`
- Use **sealed class** when you need a base constructor or method: `sealed class Result<T>`

### Testing

- **Unit Tests**: Located in `src/test/`. Use JUnit 6 with MockK.
- **Instrumented Tests**: Located in `src/androidTest/`. Use Compose Testing with MockK.
- **Test Naming**: `[Method]_[Scenario]_[ExpectedResult]`
- **Arrange-Act-Assert**: Structure test code clearly
- **Use `@get:Rule` for Compose test rule**
- **Use `mockk()` for ViewModel mocks in Compose tests** (e.g., `mockk(relaxed = true)` for flexible mocking)

#### Testing Patterns

**Test failure paths explicitly:**

```kotlin
result.onFailure { e ->
    // Assert error state is set correctly
    assert(uiState.value.error != null)
}
```

**Do NOT rely only on happy path tests.** Test Repository error handling, UseCase validation, and ViewModel error state updates.

### Threading & Async

- **Suspend functions**: Use `suspend` in repository/use case methods for database/IO operations
- **ViewModels**: Launch coroutines with `viewModelScope.launch` (never create raw Job instances)
- **Testing async code**: Use `runTest` from `kotlinx-coroutines-test` to advance virtual time
- **Result<T> pattern**: Domain/Data layers return `Result<T>`, Presentation layer handles `onSuccess`/`onFailure`

### Android-Specific Guidelines

1. **Resources**: All strings in `res/values/strings.xml` (with Arabic translations in `values-ar/`). Use `stringResource()`, `pluralStringResource()`.

2. **Theme**: Define colors in `Color.kt`, typography in `Type.kt`, theme in `Theme.kt`.

3. **DI**: Manual dependency injection via `AppContainer` and `AppViewModelProvider`.

4. **Database**: Room with KSP. Entity classes in `data/datasources/local/entities/`. DAOs in `data/datasources/local/daos/`.

5. **Serialization**: Use Kotlinx Serialization for JSON. Add `@Serializable` annotation to serializable classes.

---

## Key Dependencies

- **Compose BOM**: 2026.01.01
- **Room**: 2.8.4
- **Kotlin**: 2.3.0
- **KSP**: 2.3.2
- **Ktlint**: 14.0.1 (configured in build.gradle.kts)
- **MockK**: 1.14.7
- **JUnit 6**: 6.0.2
- **Espresso**: 3.7.0

---

## Common Tasks

### Adding a New Feature

1. Create domain model in `domain/models/`
2. Create repository interface in `domain/repositories/`
3. Create use case in `domain/usecases/`
4. Create repository implementation in `data/repositories/`
5. Create Room entities/DAOs if needed in `data/datasources/`
6. Create ViewModel, ScreenState, ScreenEvent in `presentation/screens/[feature]/`
7. Create Composable screen in `presentation/screens/[feature]/`
8. Add DI in `AppContainer.kt` and `AppViewModelProvider.kt`
9. Add strings to `res/values/strings.xml` (with Arabic translations in `values-ar/`)
10. Write tests for new functionality

### Adding Tests

1. **Unit tests**: Create in `src/test/java/...` mirroring source structure
2. **Instrumented tests**: Create in `src/androidTest/java/...`
3. **Run single test**: `./gradlew testDebugUnitTest --tests "TestClassName"`
4. **Run multiple tests**: `./gradlew testDebugUnitTest --tests "dev.nenoeldeeb.education.absencerecord.presentation.**"`
5. **Always test both success and failure paths** - verify error state is set correctly

### Anti-Patterns to Avoid

- **Don't mutate state directly**: Always use `_uiState.update { it.copy(...) }`
- **Don't use `try-catch` in repositories**: Use `Result<T>` and `onSuccess`/`onFailure` patterns
- **Don't hardcode strings in UI**: All user-facing text must be in `strings.xml`
- **Don't call `collect()` without proper lifecycle management**: Use `collectAsStateWithLifecycle()` in Compose
- **Don't bypass ViewModelScope**: Always use `viewModelScope.launch`, never create raw Jobs
- **Don't store UI state in domain/data layers**: State belongs in presentation layer only

---

## Important Notes

- **Always run `./gradlew ktlintFormat` before committing**
- **All strings must be in strings.xml** - never hardcode user-facing text
- **Use UiText wrapper for localized strings in ViewModel state**
- **Prefer immutable data classes for state**
- **Use `kotlinx.datetime` for date handling, not java.time**
- **Room schema location**: `app/schemas/` (auto-generated)