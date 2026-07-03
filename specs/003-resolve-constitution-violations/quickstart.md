# Quickstart: Resolve Constitution Violations

## Prerequisites

- Android SDK (API 26+)
- Emulator or device for instrumentation tests
- Project synced and built: `./gradlew build`

## Validation Scenarios

### Scenario 1: File Size Audit

```bash
# List all Kotlin source and build script files exceeding 300 lines
find app/src -name "*.kt" | xargs wc -l | sort -rn | awk '$1 > 300'

# Expected: empty output after remediation (was 10 files)
```

### Scenario 2: Format Check

```bash
./gradlew ktlintFormat
# Expected: BUILD SUCCESSFUL, no unstaged formatting changes
```

### Scenario 3: Full Project Validation

```bash
./gradlew check
# Expected: BUILD SUCCESSFUL (ktlint + lint + tests all pass)
```

### Scenario 4: Run Specific Tests

```bash
# Students ViewModel tests (after split)
./gradlew testDebugUnitTest --tests "dev.nenoeldeeb.education.absencerecord.presentation.screens.students.*"

# Report ViewModel tests (after split)  
./gradlew testDebugUnitTest --tests "dev.nenoeldeeb.education.absencerecord.presentation.screens.report.*"

# CalendarImageGenerator tests (after split)
./gradlew connectedDebugAndroidTest --tests "*CalendarImageGenerator*"

# All use case transfer tests
./gradlew testDebugUnitTest --tests "*ImportStudentsUseCase*"

# All DAO tests
./gradlew connectedDebugAndroidTest --tests "*Dao*"
```

### Scenario 5: Architecture Violation Check

```bash
# Check no try-catch in domain layer
rg "try\s*\{" app/src/main/java/dev/nenoeldeeb/education/absencerecord/domain/

# Check no try-catch in presentation layer
rg "try\s*\{" app/src/main/java/dev/nenoeldeeb/education/absencerecord/presentation/

# Check no domain imports of presentation
rg "import.*presentation" app/src/main/java/dev/nenoeldeeb/education/absencerecord/domain/

# Expected: all empty after remediation
```

### Scenario 6: Accessibility Check for Touched UI

```bash
# Run lint with accessibility checks
./gradlew lint
# Manual: review lint output for accessibility warnings on modified files
```

## Expected Outcomes

1. All 10 files over 300 lines are reduced to ≤300 lines
2. All P1 architecture violations resolved (try-catch, domain imports, non-single-action use case)
3. `./gradlew check` passes with no new failures
4. Touched UI files pass static accessibility review
5. Existing functionality unchanged (all tests pass)
6. AGENTS.md updated to reflect new structure

## Verification Summary

| Check | Command | Pass Condition |
|-------|---------|---------------|
| File sizes | `find ... \| xargs wc -l \| awk '$1>300'` | Empty output |
| Format | `./gradlew ktlintFormat` | No changes |
| Full validation | `./gradlew check` | BUILD SUCCESSFUL |
| Tests | Various test commands | All pass |
| Architecture | `rg "try\s*\{" domain/ presentation/` | Empty |
| Accessibility | `./gradlew lint` | No accessibility warnings on touched files |
