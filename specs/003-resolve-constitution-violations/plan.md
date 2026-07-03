# Implementation Plan: Resolve Constitution Violations

**Branch**: `003-resolve-constitution-violations` | **Date**: 2026-07-03 | **Spec**: `specs/003-resolve-constitution-violations/spec.md`

**Input**: Feature specification from `specs/003-resolve-constitution-violations/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

## Summary

Audit and remediate all directly detectable constitution violations across the project. Primary focus: reduce every Kotlin source and build script file to ≤300 lines. Secondary: fix architecture violations (try-catch in wrong layers, domain→presentation imports, non-single-action use case), accessibility issues (empty contentDescription), and style issues (@Stable on ViewModels). **10 files over limit** (range 313–607 lines) → **~18 new files created, 3 deleted**. Validation gates: `ktlintFormat`, file-size audit, `./gradlew check`, and static accessibility review for touched UI.

## Technical Context

**Language/Version**: Kotlin 2.4.0+ / Kotlin JVM 25

**Primary Dependencies**: Jetpack Compose, Room, Kotlinx Datetime, Kotlinx Serialization

**Storage**: Room SQLite database

**Testing**: JUnit, MockK, Compose rules, runTest

**Target Platform**: Android (API 26+)

**Project Type**: Android App Module (Clean + MVI)

**Performance Goals**: Responsive UI at 60+ fps, no Main-thread blocking for CPU-intensive operations (parsing/db/serialization).

**Constraints**: Main safe backgrounding via dispatchers, manual DI, Arabic localization support.

**Scale/Scope**: Local offline-first architecture.

## Constitution Check

_GATE: Must pass before Phase 0 research. Re-check after Phase 1 design._

- [ ] Clean Architecture Check: Are Domain, Data, and Presentation layers separated?
- [ ] MVI / UDF Check: Is there a single immutable ScreenState, a ScreenEvent sealed interface, and lifecycle-aware collection?
- [ ] Technology Check: Are we using `kotlinx-datetime`, `@Serializable`, Room, and `strings.xml` / `UiText`?
- [ ] Test Check: Do we have test cases for both onSuccess and onFailure paths?
- [ ] Main-Safety Check: Are intensive tasks offloaded to appropriate background dispatchers?
- [ ] **FILE SIZE CHECK: FAIL — 10 files exceed the 300-line limit (see Complexity Tracking). This is the primary remediation target.**
- [ ] Accessibility Check: Are all interactive composables accessible (content descriptions, 48dp touch targets, contrast)?

**Gate Result**: FILE SIZE CHECK fails. All other checks require validation during research phase. Violations are the explicit scope of this feature — see Complexity Tracking for justification.

### Re-Check (Post-Design)

- [x] Clean Architecture Check: Largely clean. **P1 fix needed**: `domain/models/StudentError.kt` imports `presentation.utils.UiText`. Also `ImportStudentsUseCase` has 6 try-catch blocks in domain layer. Both addressed in remediation.
- [x] MVI / UDF Check: ✅ All 3 screens compliant (sealed ScreenEvent, immutable ScreenState, `collectAsStateWithLifecycle()`).
- [x] Technology Check: ✅ `kotlinx-datetime`, `@Serializable`, Room, `strings.xml`/`UiText` all used correctly. No `java.time` usage detected.
- [x] Test Check: ✅ Tests cover both onSuccess and onFailure paths across ViewModel and use case tests.
- [x] Main-Safety Check: ✅ `withContext(Dispatchers.IO)` used in data layer repository implementations.
- [x] **FILE SIZE CHECK: FAIL → ALL 10 FILES REMEDIATED per research.md proposals.**
- [x] Accessibility Check: Mostly clean. **P2 fix needed**: `ComposeCalendar.kt:246` has empty contentDescription for non-today/non-marked cells. Addressed in remediation.

## Project Structure

### Documentation (this feature)

```text
specs/003-resolve-constitution-violations/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
app/src/main/java/dev/nenoeldeeb/education/absencerecord/
├── app/                  # Application container & manual DI providers
├── domain/
│   ├── models/           # Domain models
│   ├── repositories/     # Repository interfaces
│   ├── usecases/         # Single-action use cases
│   └── services/         # Service interfaces
├── data/
│   ├── datasources/local/# Room Entities, DAOs, Database
│   ├── repositories/     # Repository implementations
│   └── mappers/          # Entity <-> Domain mappers
└── presentation/
    ├── screens/
    │   ├── components/   # Shared UI components
    │   └── [feature]/    # Feature specific files
    │       ├── Screen.kt
    │       ├── ViewModel.kt
    │       ├── ScreenState.kt
    │       └── ScreenEvent.kt
    ├── theme/            # Theme settings
    └── utils/            # UI wrappers/helpers (e.g. UiText)
```

**Structure Decision**: Standard Clean + MVI architecture is preserved. File splitting targets existing files; no new directories or structural changes beyond decomposing oversized files into focused sub-units. Any new files created by splitting follow the existing naming conventions (e.g. `ViewModel.kt`, `ScreenState.kt`, etc.) and live alongside their parent entities.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
| --------- | ---------- | ------------------------------------ |
| File size >300 lines (10 files) | Files accumulated functionality over time; this feature explicitly resolves them | Inaction violates constitution's hard 300-line limit |
| `try-catch` in domain/presentation (8 sites) | Offloaded from Data layer to orchestrate Result chaining | Moving to repository impls keeps domain pure |
| Domain imports presentation (StudentError.toUiText) | Convenience extension for error mapping | Move to presentation-layer mapper preserves layer isolation |
| `ImportStudentsUseCase` (2 methods) | Combines parse + import for convenience | Split into two single-action use cases for consistent pattern |
| Empty contentDescription in DayCell | Missing accessibility label for calendar cells | Adding descriptive string ensures TalkBack coverage |
| `@Stable` on ViewModels (3 VMs) | Copied from ScreenState annotation | Remove; only ScreenState needs @Stable |

