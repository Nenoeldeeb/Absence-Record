# Quickstart: Refactor Students ViewModel

## Goal
Reduce `StudentsViewModel.kt` from 574 lines to <300 lines by extracting business logic into action delegates.

## Files to Create

| File | Location | Lines (est.) |
|------|----------|-------------|
| `StudentActionDelegate.kt` | `presentation/screens/students/delegates/` | ~120 |
| `ClassActionDelegate.kt` | `presentation/screens/students/delegates/` | ~70 |

## Files to Modify

| File | Change | Lines after |
|------|--------|-------------|
| `StudentsViewModel.kt` | Extract 11 private methods into delegates; keep only orchestration + state | <300 |
| `StudentsScreenState.kt` | Remove dead `exportSelectionMap` field | ~35 |

## Files Unchanged (out of scope)
- All UI composables (`StudentsScreen.kt`, dialogs, components)
- All domain models, use cases, repository interfaces
- All data layer (entities, DAOs, repository impls, mappers)
- Existing delegates (`SelectionStateDelegate.kt`, `ImportExportDelegate.kt`)

## Architecture

```
User Event → StudentsViewModel.onEvent()
                ↓
         ┌─ StudentActionDelegate ──→ StudentManagementUseCases ──→ Repositories
         │   (add, update, delete,
         │    export, import phases)
         │
         └─ ClassActionDelegate ────→ ClassManagementUseCases ────→ Repositories
             (add, rename, delete,
              filter fallback)
                ↓
         _uiState.update { copy(...) }
                ↓
         collectAsStateWithLifecycle()
```

## Implementation Order
1. Create `StudentActionDelegate` — move student CRUD + bulk + export methods
2. Create `ClassActionDelegate` — move class CRUD + filter fallback
3. Update `StudentError` sealed interface in `StudentsScreenState` file (or new file)
4. Thin `StudentsViewModel` — import delegates, redirect `onEvent` cases
5. Remove dead `exportSelectionMap` from state
6. Write delegate unit tests (FR-005 / SC-004)
7. Run existing tests (FR-004) to confirm no regressions

## Sanity Checks
- After each step: `./gradlew ktlintFormat`
- Before commit: `./gradlew check`
- Verify: existing UI screens look/behave identically (SC-003)
