# AGENTS.md - Guidelines for Agentic Coding

This document provides guidelines for AI agents working on this codebase.

## Project Overview

Absence Record is an Android application for student attendance tracking. Built with Kotlin, Jetpack Compose, Room database, and Clean Architecture.

## Build Commands

### Building

```bash
./gradlew assembleDebug        # Debug build
./gradlew assembleRelease     # Release build
./gradlew build               # Full build with tests
```

### Running Tests

```bash
./gradlew test                         # All unit tests
./gradlew connectedDebugAndroidTest   # All Android instrumented tests
./gradlew testDebugUnitTest            # Unit tests for debug variant
```

### Running a Single Test

```bash
# Unit test - specify fully qualified class name
./gradlew testDebugUnitTest --tests "dev.nenoeldeeb.education.absencerecord.presentation.utils.DateFormatterTest"

# Android instrumented test
./gradlew connectedDebugAndroidTest --tests "dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenTest"
```

### Linting & Code Quality

```bash
./gradlew ktlintCheck       # Check code style
./gradlew ktlintFormat     # Auto-format code
./gradlew lint             # Android lint
./gradlew check            # All checks (ktlint + lint + tests)
```

**Note:** `ktlintFormat` runs automatically before every build (`preBuild` task).

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
- Don't use Java libraries if Kotlin have an alternative

2. **Visibility Modifiers**: Use explicit modifiers. Prefer `private` for class members.

3. **Data Classes**: Use for immutable state objects and DTOs. Prefer `val` over `var`.

4. **Coroutines**: Use `viewModelScope.launch` for async operations. Use `Result<T>` for operations that can fail.

5. **Flow**: Use `StateFlow` for UI state. Collect using `collectAsStateWithLifecycle()` in Compose.

6. **Error Handling**: Use `Result.onSuccess { }.onFailure { }` pattern instead of try-catch for repository/use case results.

7. **Compose**:

- Use `@Composable` annotation for all UI functions
- Add `@Stable` annotation to ViewModels State and Event wrappers
- Use `remember`, `retain` and `rememberSaveable` for UI state prefer `retain`
- Use `derivedStateOf` for computed state
- Group related imports

### Code Organization

1. **Use Cases**: One class per file, named `[Action]UseCase`. Constructor injection of dependencies. Invoke via `operator fun invoke()`.

2. **Repository**: Interface in `domain/repositories`, implementation in `data/repositories`.

3. **ViewModels**: Single `uiState: StateFlow<ScreenState>`, single `onEvent(event: ScreenEvent)` function. Use ` MutableStateFlow` internally, expose as `StateFlow`.

4. **Screen State**: Immutable data class with all UI state. No functions, only data.

5. **Screen Events**: Sealed interface/class hierarchy for all user interactions.

### Testing

- **Unit Tests**: Located in `src/test/`. Use JUnit 6 with MockK.
- **Instrumented Tests**: Located in `src/androidTest/`. Use Compose Testing with MockK.
- **Test Naming**: `[Method]_[Scenario]_[ExpectedResult]`
- **Arrange-Act-Assert**: Structure test code clearly
- **Use `@get:Rule` for Compose test rule**
- **Use `mockk()` for ViewModel mocks in Compose tests**

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
9. Add strings to `res/values/strings.xml`
10. Write tests for new functionality

### Adding Tests

1. Unit tests: Create in `src/test/java/...` mirroring source structure
2. Instrumented tests: Create in `src/androidTest/java/...`
3. Run tests: `./gradlew testDebugUnitTest --tests "TestClassName"`

---

## Important Notes

- **Always run `./gradlew ktlintFormat` before committing**
- **All strings must be in strings.xml** - never hardcode user-facing text
- **Use UiText wrapper for localized strings in ViewModel state**
- **Prefer immutable data classes for state**
- **Use `kotlinx.datetime` for date handling, not java.time**
- **Room schema location**: `app/schemas/` (auto-generated)
