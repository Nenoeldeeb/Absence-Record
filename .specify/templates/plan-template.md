# Implementation Plan: [FEATURE]

**Branch**: `[###-feature-name]` | **Date**: [DATE] | **Spec**: [link]

**Input**: Feature specification from `/specs/[###-feature-name]/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command; its definition describes the execution workflow.

## Summary

[Extract from feature spec: primary requirement + technical approach from research]

## Technical Context

<!--
  ACTION REQUIRED: Replace the content in this section with the technical details
  for the project. Defaults below are pre-filled from the detected stack —
  adjust only where this feature genuinely differs.
-->

**Language/Version**: Kotlin 2.4.10, JVM target 25, AGP 9.3.0, compileSdk/targetSdk 37, minSdk 26

**Primary Dependencies**: Jetpack Compose (Material 3), Room 2.8.4, Coroutines 1.11.0, kotlinx-datetime 0.8.0, kotlinx-serialization-json 1.11.0, KSP 2.3.2, Accompanist Pager

**Storage**: Room (SQLite) via KSP. Offline-only — no backend, no network layer. Export/import via kotlinx.serialization JSON.

**Testing**: JUnit 6 (Jupiter) + MockK for JVM unit tests; JUnit4 + Compose `ui-test-junit4` + `room-testing` for instrumented tests

**Target Platform**: Android (minSdk 26+), single activity, single Gradle module `:app`

**Project Type**: mobile-app (offline, local-first)

**Performance Goals**: No blocking of the main thread — serialization, date parsing, and bulk collection processing MUST be offloaded via `withContext`. Compose UI at 60 fps.

**Constraints**: Offline-capable (no network). Bilingual en/ar strings via `strings.xml` + `values-ar/`. `java.time` prohibited — use `kotlinx-datetime`. Max 300 lines per file. Accessibility: 48dp targets, contentDescription, color never the sole state indicator.

**Scale/Scope**: ~14.4k LOC production Kotlin, ~18k LOC test Kotlin, 4 screens, 7 Room entities, 36 use cases

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle / Gate | Requirement | Status |
|------------------|-------------|--------|
| I. Clean Architecture & Layer Partitioning | Domain holds models/interfaces/use cases; Data holds Room + impls + mappers; Presentation holds Compose. Use cases are single-action `operator fun invoke` returning `Result<T>`. No `try-catch` escaping Data. | ⬜ |
| II. MVI & Stable State | One `@Stable` immutable state data class per screen; mutate via `_uiState.update { it.copy(...) }`; `sealed interface ScreenEvent`; collect via `collectAsStateWithLifecycle()`; async in `viewModelScope.launch`, no raw `Job`. | ⬜ |
| III. Androidx / Jetpack-First | Compose UI, Room storage, kotlinx.serialization, kotlinx-datetime. No `java.time`. No hardcoded strings — `strings.xml` + `values-ar/` + `UiText`. Manual DI via `AppContainer` / `AppViewModelProvider`. | ⬜ |
| IV. Verification & Test Discipline | Both `onSuccess` and `onFailure` paths tested for use cases and ViewModels; error state binds to UI. JUnit + MockK. `runTest` for virtual time. | ⬜ |
| V. Simplicity / Performance / Consistency | SOLID. CPU-heavy work off main thread. `ktlintFormat` + `./gradlew check` pass. **Max 300 lines per file** — decompose, or extract a delegate under `<feature>/delegates/`. | ⬜ |
| VI. Accessibility | `contentDescription` on interactive elements, 48dp touch targets, color not the sole indicator, 4.5:1 contrast, TalkBack-verified. | ⬜ |
| Dependency Direction | `presentation/` → `domain/` ← `data/`. Domain MUST NOT import data or presentation. Presentation MUST NOT import data directly. | ⬜ |

**Complexity justification**: If any cell above is ❌, record it in Complexity Tracking with the reason and the simpler alternative rejected.

## Project Structure

### Documentation (this feature)

```text
specs/[###-feature]/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)
<!--
  ACTION REQUIRED: Keep the Clean Architecture layout below. Mark only the
  layers this feature actually touches — do NOT re-describe the whole tree, and
  do NOT add layers. Show concrete paths under app/src/main/java/
  dev/nenoeldeeb/education/absencerecord/.
-->

```text
app/src/main/java/dev/nenoeldeeb/education/absencerecord/
├── app/                          # Application, MainActivity, AppContainer, AppViewModelProvider
│   └── navigation/               # AppDestination, AppNavigation (composition-based)
├── domain/
│   ├── models/                   # Pure domain models (no Android imports)
│   ├── repositories/             # Repository INTERFACES
│   ├── services/                 # Pure business rules (e.g. ScheduleRules)
│   └── usecases/
│       ├── attendance/  classes/  report/
│       └── schedule/     student/  transfer/
├── data/
│   ├── datasources/local/        # AppDatabase, DateConverters
│   │   ├── entities/             # Room @Entity
│   │   └── daos/                 # Room @Dao + query helpers
│   ├── repositories/             # Interface implementations
│   ├── mappers/                  # Entity <-> domain
│   ├── services/                 # Serialization / storage
│   └── utils/                    # e.g. CalendarImageGenerator
├── presentation/
│   ├── screens/
│   │   ├── components/           # Cross-feature shared composables
│   │   └── [feature]/            # Screen.kt, ViewModel.kt, ScreenState.kt, ScreenEvent.kt
│   │       ├── components/       # Feature-private composables
│   │       ├── dialogs/          # Feature-private dialogs
│   │       ├── delegates/        # ViewModel behaviour extraction
│   │       └── handlers/         # Event/intent helpers
│   ├── theme/
│   └── utils/                    # UiText, formatters, SoundPlayer
└── res/
    ├── values/strings.xml        # English strings
    └── values-ar/strings.xml     # Arabic translations

app/src/test/java/dev/nenoeldeeb/education/absencerecord/     # JUnit 6 + MockK unit tests
app/src/androidTest/java/dev/nenoeldeeb/education/absencerecord/  # Instrumented + Compose UI tests
app/schemas/                    # Room exported schemas (version-controlled)
```

**Layers touched by this feature**: [e.g. domain/models, domain/usecases/schedule, data/..., presentation/screens/schedule]

**Structure Decision**: New UI groups into `presentation/screens/[feature]/`; new use cases into the matching `domain/usecases/[group]/`; a ViewModel that outgrows the 300-line cap is decomposed into `<feature>/delegates/`.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| [e.g., 4th project] | [current need] | [why 3 projects insufficient] |
| [e.g., Repository pattern] | [specific problem] | [why direct DB access insufficient] |
