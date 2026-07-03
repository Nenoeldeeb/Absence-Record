# Data Model: Resolve Constitution Violations

## Entities

### ConstitutionViolation
- **Rule**: String (the constitution rule that was violated)
- **Category**: ViolationCategory (FILE_SIZE, ARCHITECTURE, ACCESSIBILITY, STYLE)
- **Severity**: ViolationSeverity (P1, P2, P3)
- **AffectedArtifact**: String (file path or component name)
- **MeasuredEvidence**: String (e.g. "321 lines", "try-catch at line 26")
- **Status**: ViolationStatus (IDENTIFIED, IN_PROGRESS, RESOLVED)
- **Resolution**: String? (how it was fixed)

### SourceFileAuditEntry
- **Path**: String (relative to project root)
- **LineCount**: Int
- **Limit**: Int = 300
- **Status**: ComplianceStatus (COMPLIANT, OVER_LIMIT)
- **Remediation**: FileRemediation (IN_PLACE, SPLIT, EXTRACTED)

### ValidationResult
- **GateName**: String (e.g. "ktlintFormat", "file-size-audit", "check", "accessibility-review")
- **Outcome**: GateOutcome (PASS, FAIL)
- **Timestamp**: String (ISO datetime)
- **UnresolvedIssues**: List<String>

## Enums

### ViolationCategory
- FILE_SIZE
- ARCHITECTURE
- ACCESSIBILITY
- STYLE

### ViolationSeverity
- P1 (Critical — blocks build/functionality)
- P2 (Major — should fix)
- P3 (Minor — nice to fix)

### ViolationStatus
- IDENTIFIED
- IN_PROGRESS
- RESOLVED

### ComplianceStatus
- COMPLIANT
- OVER_LIMIT

### FileRemediation
- IN_PLACE (trimmed, not split)
- SPLIT (divided into multiple files)
- EXTRACTED (portions moved to new files)

### GateOutcome
- PASS
- FAIL

## Validation Rules

1. No source file (`.kt` or `.kts`) may exceed 300 lines.
2. No `try-catch` may appear in `domain/` or `presentation/` layers.
3. Domain layer must not import from `presentation/` package.
4. All use cases must be single-action classes with `operator fun invoke`.
5. Interactive composables must have non-empty `contentDescription`.
6. `@Stable` annotation must only appear on `ScreenState` data classes, not ViewModels.
7. All files from FR-001 audit must be resolved with zero exceptions per FR-004.

## State Transitions

```
IDENTIFIED → IN_PROGRESS → RESOLVED
                        ↘ FAILED (if remediation introduces new violations)
```

## Relationship Map

```
SourceFileAuditEntry (N) ──detects──> ConstitutionViolation (N)
ValidationResult (1) ──proves──> ConstitutionViolation (N) compliance
```
