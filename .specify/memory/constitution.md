<!--
SYNC IMPACT REPORT
==================

--- Amendment 2026-10-12 (brownfield bootstrap) ---
- Version change: 1.1.0 → 1.2.0
- List of modified principles: None. Principles I–VI are left byte-identical;
  this amendment only adds sections and quality gates derived from the
  existing codebase.
- Added sections:
  * Code Boundaries (layer dependency rule + source tree map)
  * ViewModel Delegate Decomposition
  * Naming, Branching & Commit Conventions
- Added to Development Quality Gates:
  * Instrumented test command form (AGP 9 rejects `--tests`)
  * On-device UI test harness constraint
  * Room migration & schema export
- Added to Core Technologies & Constraints: Room migrations + exported schemas
- Removed sections: None
- Deliberately NOT added: any CI/CD rule. No CI exists in this repository, so
  such a rule would be invented rather than detected.
- Templates requiring updates:
  * .specify/templates/spec-template.md (✅ updated)
  * .specify/templates/plan-template.md (✅ updated)
  * .specify/templates/tasks-template.md (✅ updated)
  * .specify/templates/checklist-template.md (— no change required)
- Governance-mandated docs updated to match this amendment:
  * AGENTS.md (✅)
  * README.md (✅)
- Follow-up TODOs:
  * `app/src/androidTest/.../screens/students/StudentsScreenTest.kt` is 302
    lines and violates Principle V's 300-line cap. Reported, not fixed here.

--- Amendment 2026-06-20 (previous) ---
- Version change: 1.0.0 → 1.1.0
- List of modified principles:
  * Principle V: Simplicity, Performance, & Code Consistency (expanded — no title change)
  * Principle VI: Accessibility (NEW)
- Added sections:
  * Principle VI: Accessibility
  * Max line count per file (300) to Development Quality Gates
  * AGENTS.md/README.md forced update rule in Governance
- Removed sections: None
- Templates requiring updates:
  * .specify/templates/plan-template.md (✅ updated)
  * .specify/templates/spec-template.md (⚠ pending — no changes required)
  * .specify/templates/tasks-template.md (✅ updated)
  * .specify/templates/checklist-template.md (⚠ pending — no changes required)
- Follow-up TODOs: None.
-->

# Absence Record Constitution

## Core Principles

### I. Clean Architecture & Layer Partitioning

The codebase MUST strictly partition into three clean layers: Domain, Data, and
Presentation. Domain holds business logic (models, repository interfaces,
usecases, services). Data handles local storage via Room Entities/DAOs,
repository implementations, and mappers. Presentation handles the UI using
Jetpack Compose. All use cases MUST be single-action classes implementing
`operator fun invoke`. Standardize return types to Kotlin's built-in
`Result<T>`. NO `try-catch` blocks may escape to the Domain or Presentation
layers; they must be caught and handled at the Data layer or boundaries using
`onSuccess`/`onFailure`.

### II. MVI Pattern & Stable State Management

The UI state must be a single immutable data class annotated with `@Stable`.
Mutation must occur strictly through `_uiState.update { it.copy(...) }`.
Derived state properties MUST use Jetpack Compose's `derivedStateOf` to prevent
redundant recompositions. User intents must be modeled via a
`sealed interface ScreenEvent`. Collect UI state in composables ONLY via
`collectAsStateWithLifecycle()` to respect the lifecycle. All async operations
must run in `viewModelScope.launch`, and developers must never manually create
raw `Job` instances.

### III. Androidx & Jetpack-First Architecture

Prioritize Jetpack Compose for UI, Room Database for persistent storage,
Kotlinx Serialization (`@Serializable`) for data serialization (e.g.,
import/export functionality), and `kotlinx-datetime` for all date/time
operations. We strictly prohibit the use of `java.time`. We prohibit hardcoding
user-facing strings; all strings must reside in `strings.xml` (with Arabic
translations supported in `values-ar/`) and be wrapped via the `UiText`
utility. Dependency injection is manual, utilizing `AppContainer` and
`AppViewModelProvider`.

### IV. Verification & Test Discipline

Testability is a first-class citizen. Both unit and UI tests must be written to
verify functional code. We MUST explicitly test both `onSuccess` and `onFailure`
paths for UseCases and ViewModels, ensuring error states bind correctly to the
UI. Unit tests must use JUnit and MockK (e.g. `mockk(relaxed = true)` for
ViewModels in Compose tests). Advance virtual time with `runTest` for
asynchronous testing.

### V. Simplicity, Performance, & Code Consistency

Follow the SOLID principles and prioritize simple, maintainable approaches.
Optimize for performance: CPU-intensive operations (such as serialization,
collection processing, and date parsing) MUST NOT run on the Main thread. They
must be offloaded to background threads using `withContext` with a dedicated
default or IO dispatcher. Maintain code consistency by enforcing formatting via
`./gradlew ktlintFormat` and full validation (ktlint + lint + tests) via
`./gradlew check` before committing any code.

Every source file, including test files, MUST NOT exceed 300 lines. Large files
MUST be decomposed into smaller, focused units. This keeps each file's
responsibility narrow and reviewable. Refactoring a file that exceeds this
limit takes priority over new feature work.

### VI. Accessibility

All user-facing composables MUST be accessible. Interactive elements MUST have
meaningful `contentDescription` values (or be marked as decorative where
appropriate). Touch targets MUST be at least 48dp in size as recommended by
Material Design guidelines. Color MUST NOT be the sole means of conveying
information or state. Text and interactive elements MUST maintain sufficient
color contrast ratios (4.5:1 for normal text, 3:1 for large text). The UI MUST
remain navigable via keyboard, switch access, and TalkBack. Test each screen
with TalkBack before shipping. Accessibility regressions are treated as bugs.

## Code Boundaries

### Layer Dependency Rule

```text
presentation/  ──►  domain/  ◄──  data/
                               (data implements the interfaces declared in domain)
```

Dependencies point **inward only**. `domain/` MUST NOT import from `data/` or
`presentation/`. `presentation/` MUST NOT import from `data/` directly; it
reaches persistence only through use cases.

### Source Tree Map

Single Gradle module (`:app`). All Kotlin lives under
`app/src/main/java/dev/nenoeldeeb/education/absencerecord/`:

| Path | Contents |
|------|----------|
| `app/` | `Application`, `MainActivity`, `AppContainer`, `AppViewModelProvider` |
| `app/navigation/` | `AppDestination`, `AppNavigation` — composition-based navigation (no navigation library) |
| `domain/models/` | Pure domain models. No Android imports. |
| `domain/repositories/` | Repository **interfaces** |
| `domain/services/` | Pure business rules (e.g. `ScheduleRules`) |
| `domain/usecases/<group>/` | Single-action use cases, grouped by feature |
| `data/datasources/local/` | `AppDatabase`, `DateConverters` |
| `data/datasources/local/entities/` | Room `@Entity` types |
| `data/datasources/local/daos/` | Room `@Dao` interfaces + query helpers |
| `data/repositories/` | Interface implementations |
| `data/mappers/` | Entity ↔ domain mapping |
| `data/services/` | Serialization / storage services |
| `data/utils/` | Pure-Kotlin utilities (e.g. `CalendarImageGenerator`) |
| `presentation/screens/<feature>/` | `Screen.kt`, `ViewModel.kt`, `ScreenState.kt`, `ScreenEvent.kt` |
| `presentation/screens/<feature>/components/` | Feature-private composables |
| `presentation/screens/<feature>/dialogs/` | Feature-private dialogs |
| `presentation/screens/<feature>/delegates/` | ViewModel behaviour extraction (see below) |
| `presentation/screens/<feature>/handlers/` | Event/intent handling helpers |
| `presentation/screens/components/` | Cross-feature shared composables (e.g. `ClassCheckboxFilter`) |
| `presentation/theme/`, `presentation/utils/` | Theme, `UiText`, formatters |

New features group code by **presentation feature** (a new
`presentation/screens/<feature>/` directory), not by adding across every layer
blindly. Use-case directories mirror the feature grouping already in use:
`attendance`, `classes`, `report`, `schedule`, `student`, `transfer`.

### ViewModel Delegate Decomposition

When a ViewModel's behaviour exceeds what the 300-line cap allows to keep
cohesive, extract cohesive behaviour into a delegate class under
`<feature>/delegates/` and have the ViewModel own/compose the delegate. The
established pattern is a delegate holding an injected action/operation handler,
with the ViewModel retaining state ownership. Three of the four ViewModels
(`ScheduleViewModel`, `StudentDetailViewModel`, `StudentsViewModel`) use this
pattern; it is the expected remedy for an oversized ViewModel, and MUST be
preferred over letting a ViewModel grow.

## Core Technologies & Constraints

- **Jetpack Compose**: All screens and UI components must be built natively
  using Jetpack Compose, respecting the Unidirectional Data Flow pattern.
- **Room Database**: Local persistence layer using Room DAOs and Entities. All
  DAO methods returning results must be suspend functions or return flows.
- **Room Migrations & Schema Export**: `room.schemaLocation` is set to
  `app/schemas`, and those exported JSON schemas are version-controlled. Any
  change to the database MUST add an explicit `Migration` with a version bump —
  destructive migration (`fallbackToDestructiveMigration`) is NOT acceptable for
  user data. Every schema-affecting change MUST come with a migration test in
  `androidTest/` (see `ScheduleDaoMigrationTest` for the established pattern).
- **Kotlinx Serialization**: JSON import/export flows must use
  kotlinx.serialization to serialize and deserialize data.
- **Kotlinx Datetime**: Date representation must exclusively use
  kotlinx-datetime types.
- **Manual Dependency Injection**: Manage dependencies via `AppContainer` and
  provide ViewModels through `AppViewModelProvider` to keep the DI model simple
  and transparent.
- **Main Safety**: Offload complex computations and database operations to
  background threads using Kotlin Coroutine dispatchers.

## Development Quality Gates

- **Code Style Compliance**: Run `./gradlew ktlintFormat` to keep formatting
  aligned with the style guide. This also runs automatically as a `preBuild`
  dependency.
- **Max Line Count**: Every source file (including test files) MUST NOT exceed
  300 lines. Break large files into smaller, focused units before submitting
  any pull request.
- **Verification Gates**: Run `./gradlew check` to run formatting checks,
  Android lints, and all unit tests.
- **Targeted Unit Tests**: `./gradlew testDebugUnitTest --tests "PackageOrClass"`.
- **Instrumented Test Command Form**: Android Gradle Plugin 9 REJECTS the
  `--tests` flag on `connectedDebugAndroidTest`. Filter by class with
  `-Pandroid.testInstrumentationRunnerArguments.class="PackageOrClass"`. Using
  `--tests` here is a known failure mode.
- **On-Device UI Test Harness**: `mockk-android` cannot mock final ViewModel
  classes on-device, so instrumented UI tests MUST NOT rely on mocking a
  ViewModel. Extract a stateless content composable and drive it through a
  hand-rolled harness backed by `MutableStateFlow` plus an event recorder. See
  `ScheduleScreenTestHarness` / `ScheduleScreenContent` for the established
  pattern. MockK remains required for unit tests (JVM).
- **Naming Suffixes**:
  - ViewModel: `ViewModel` (e.g. `StudentsViewModel`)
  - ScreenState: `ScreenState` (e.g. `StudentsScreenState`)
  - ScreenEvent: `ScreenEvent` (e.g. `StudentsScreenEvent`)
  - Screen file: `Screen.kt` (e.g. `StudentsScreen.kt`)
  - Layout Folder: `presentation/screens/[feature]/`

## Naming, Branching & Commit Conventions

- **File naming**: `PascalCase.kt` for types; lowercase for directories.
  `ktlint_standard_filename` is enabled.
- **Branch naming**: feature branches use the spec-kit numeric prefix —
  `NNN-kebab-case-feature-name` (e.g. `007-lesson-scheduling`). Ad-hoc
  branches use Conventional Commit prefixes: `feat/`, `fix/`, `chore/`,
  `refactor/`. Long-lived integration branches are `develop` and `release`.
- **Commit messages**: Conventional Commits **with a scope**, where the scope
  is the presentation feature or affected area — e.g.
  `feat(schedule): add weekly availability plan`,
  `fix(student-detail): give busy conflict priority over full`,
  `refactor(calendar): extract attendance dialog`. Commit bodies are optional.

## Governance

- This constitution is the ultimate reference for code structure, architectural
  rules, and libraries in the Absence Record project.
- Any change to the architectural pattern, such as introducing new framework
  libraries or altering the MVI state flow, must be discussed, approved, and
  updated in this constitution first.
- The `AGENTS.md` and `README.md` files must remain aligned with these
  principles at all times.
- **AGENTS.md & README.md Update Requirement**: Whenever a new feature,
  technology, or project structure change is introduced via a spec, both
  `AGENTS.md` and `README.md` MUST be updated to reflect the change before the
  feature is considered complete.
- Development tools, including Spec-Kit commands, must consult these guidelines
  to ensure consistency.

**Version**: 1.2.0 | **Ratified**: 2026-05-22 | **Last Amended**: 2026-10-12
