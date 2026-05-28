<!--
SYNC IMPACT REPORT
==================
- Version change: Template Placeholders -> 1.0.0
- List of modified principles:
  * Principle I: Clean Architecture & Unidirectional Data Flow (Ratified)
  * Principle II: MVI Pattern & Stable State Management (Ratified)
  * Principle III: Androidx & Jetpack-First Architecture (Ratified)
  * Principle IV: Verification & Test Discipline (Ratified)
  * Principle V: Simplicity, Performance, & Code Consistency (Ratified)
- Added sections:
  * Core Technologies & Constraints (Section 2)
  * Development Quality Gates (Section 3)
- Templates requiring updates:
  * .specify/templates/plan-template.md (✅ updated)
  * .specify/templates/spec-template.md (✅ updated)
  * .specify/templates/tasks-template.md (✅ updated)
- Follow-up TODOs: None.
-->

# Absence Record Constitution

## Core Principles

### I. Clean Architecture & Layer Partitioning

The codebase MUST strictly partition into three clean layers: Domain, Data, and Presentation. Domain holds business logic (models, repository interfaces, usecases, services). Data handles local storage via Room Entities/DAOs, repository implementations, and mappers. Presentation handles the UI using Jetpack Compose.
All use cases MUST be single-action classes implementing `operator fun invoke`. Standardize return types to Kotlin's built-in `Result<T>`. NO `try-catch` blocks may escape to the Domain or Presentation layers; they must be caught and handled at the Data layer or boundaries using `onSuccess`/`onFailure`.

### II. MVI Pattern & Stable State Management

The UI state must be a single immutable data class annotated with `@Stable`. Mutation must occur strictly through `_uiState.update { it.copy(...) }`. Derived state properties MUST use Jetpack Compose's `derivedStateOf` to prevent redundant recompositions. User intents must be modeled via a `sealed interface ScreenEvent`. Collect UI state in composables ONLY via `collectAsStateWithLifecycle()` to respect the lifecycle. All async operations must run in `viewModelScope.launch`, and developers must never manually create raw `Job` instances.

### III. Androidx & Jetpack-First Architecture

Prioritize Jetpack Compose for UI, Room Database for persistent storage, Kotlinx Serialization (`@Serializable`) for data serialization (e.g., import/export functionality), and `kotlinx-datetime` for all date/time operations. We strictly prohibit the use of `java.time`. We prohibit hardcoding user-facing strings; all strings must reside in `strings.xml` (with Arabic translations supported in `values-ar/`) and be wrapped via the `UiText` utility. Dependency injection is manual, utilizing `AppContainer` and `AppViewModelProvider`.

### IV. Verification & Test Discipline

Testability is a first-class citizen. Both unit and UI tests must be written to verify functional code. We MUST explicitly test both `onSuccess` and `onFailure` paths for UseCases and ViewModels, ensuring error states bind correctly to the UI. Unit tests must use JUnit and MockK (e.g. `mockk(relaxed = true)` for ViewModels in Compose tests). Advance virtual time with `runTest` for asynchronous testing.

### V. Simplicity, Performance, & Code Consistency

Follow the SOLID principles and prioritize simple, maintainable approaches. Optimize for performance: CPU-intensive operations (such as serialization, collection processing, and date parsing) MUST NOT run on the Main thread. They must be offloaded to background threads using `withContext` with a dedicated default or IO dispatcher. Maintain code consistency by enforcing formatting via `./gradlew ktlintFormat` and full validation (ktlint + lint + tests) via `./gradlew check` before committing any code.

## Core Technologies & Constraints

- **Jetpack Compose**: All screens and UI components must be built natively using Jetpack Compose, respecting the Unidirectional Data Flow pattern.
- **Room Database**: Local persistence layer using Room DAOs and Entities. All DAO methods returning results must be suspend functions or return flows.
- **Kotlinx Serialization**: JSON import/export flows must use kotlinx.serialization to serialize and deserialize data.
- **Kotlinx Datetime**: Date representation must exclusively use kotlinx-datetime types.
- **Manual Dependency Injection**: Manage dependencies via `AppContainer` and provide ViewModels through `AppViewModelProvider` to keep the DI model simple and transparent.
- **Main Safety**: Offload complex computations and database operations to background threads using Kotlin Coroutine dispatchers.

## Development Quality Gates

- **Code Style Compliance**: Run `./gradlew ktlintFormat` to keep formatting aligned with the style guide.
- **Verification Gates**: Run `./gradlew check` to run formatting checks, Android lints, and all unit tests.
- **Naming Suffixes**:
  - ViewModel: `ViewModel` (e.g. `StudentsViewModel`)
  - ScreenState: `ScreenState` (e.g. `StudentsScreenState`)
  - ScreenEvent: `ScreenEvent` (e.g. `StudentsScreenEvent`)
  - Screen file: `Screen.kt` (e.g. `StudentsScreen.kt`)
  - Layout Folder: `presentation/screens/[feature]/`

## Governance

- This constitution is the ultimate reference for code structure, architectural rules, and libraries in the Absence Record project.
- Any change to the architectural pattern, such as introducing new framework libraries or altering the MVI state flow, must be discussed, approved, and updated in this constitution first.
- The `AGENTS.md` and `README.md` files must remain aligned with these principles at all times.
- Development tools, including Spec-Kit commands, must consult these guidelines to ensure consistency.

**Version**: 1.0.0 | **Ratified**: 2026-05-22 | **Last Amended**: 2026-05-22
