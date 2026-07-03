# Feature Specification: Resolve Constitution Violations

**Feature Branch**: `003-resolve-constitution-violations`

**Created**: 2026-07-03

**Status**: Draft

**Input**: User description: "this project have some constitution rules violations, e.g source code file size. Let's resolve these violations to get a consistent project"

## Clarifications

### Session 2026-07-03

- Q: What is the exact remediation scope? -> A: Fix all detectable constitution violations, with file size as the first priority.
- Q: Are approved exceptions allowed for known violations? -> A: No exceptions; all directly detectable violations must be fixed.
- Q: Which files are subject to the 300-line source-file limit? -> A: Kotlin source and build scripts only.
- Q: What validation gate is required before this feature is complete? -> A: Full validation: format, file-size audit, and full project check.
- Q: What evidence is required for touched UI accessibility? -> A: Automated/static accessibility review is mandatory; manual TalkBack notes are required when performed.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Identify All Constitution Violations (Priority: P1)

As a maintainer, I want a complete compliance audit of the project against the constitution so that remediation work targets every directly detectable violation instead of fixing only one example.

**Why this priority**: A reliable inventory is required before any remediation can be considered complete.

**Independent Test**: Can be tested by running the project compliance audit and verifying it reports every Kotlin source or build script file over the 300-line limit and any other directly detectable constitution violations.

**Acceptance Scenarios**:

1. **Given** the current project, **When** maintainers audit Kotlin source and build script files against the constitution, **Then** every file over 300 lines is listed with its exact line count.
2. **Given** a file that is within the constitution limit, **When** maintainers review the audit output, **Then** the file is not reported as a file-size violation.
3. **Given** the constitution contains non-file-size quality rules, **When** maintainers audit the project, **Then** any directly detectable violation is recorded and remediated.

---

### User Story 2 - Restore Source File Size Compliance (Priority: P2)

As a maintainer, I want oversized source files split into focused units so that each file remains readable, reviewable, and consistent with the constitution.

**Why this priority**: The constitution states that files over 300 lines must be refactored before new feature work.

**Independent Test**: Can be tested by checking that every Kotlin source and build script file, including test files, has 300 or fewer lines after remediation while existing behavior remains unchanged.

**Acceptance Scenarios**:

1. **Given** a Kotlin source or build script file with more than 300 lines, **When** remediation is complete, **Then** the file is split or reduced so that no resulting source file exceeds 300 lines.
2. **Given** existing app behavior and tests before remediation, **When** the project is validated after remediation, **Then** no user-facing behavior is intentionally changed.
3. **Given** a split test file, **When** the related tests are run, **Then** all original success and failure coverage remains represented.

---

### User Story 3 - Verify Consistency Gates (Priority: P3)

As a maintainer, I want validation evidence after remediation so that the project can be treated as constitution-compliant before more feature work continues.

**Why this priority**: Compliance is only useful if it is verifiable and repeatable.

**Independent Test**: Can be tested by reviewing the final validation results for formatting, file-size compliance, the full project check, mandatory static accessibility review for touched UI, and manual TalkBack notes when available.

**Acceptance Scenarios**:

1. **Given** all remediation changes are complete, **When** maintainers run formatting, file-size audit, and the full project check, **Then** all gates pass.
2. **Given** UI files were touched, **When** accessibility is reviewed, **Then** static review confirms interactive controls retain meaningful labels, minimum touch targets, and non-color state indicators, with manual TalkBack notes included when performed.
3. **Given** project documentation must remain aligned with the constitution, **When** remediation is complete, **Then** project guidance documents reflect any changed structure or conventions.

### Edge Cases

- Some oversized files may be test files that share large fixture setup; remediation must preserve both success-path and failure-path coverage.
- TalkBack verification may require manual evidence; this feature requires manual TalkBack notes when such testing is performed.
- Reducing line count must not create unfocused helper files that comply numerically while weakening readability.
- Documentation updates are only required when remediation changes project structure, conventions, or developer workflow.
- Validation commands may expose pre-existing failures unrelated to this remediation; those must be documented separately from remediation-caused failures.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The project MUST provide an audit inventory of all directly detectable constitution violations before remediation begins.
- **FR-002**: The audit inventory MUST include every Kotlin source and build script file above 300 lines with its path and measured line count.
- **FR-003**: The remediation MUST reduce every Kotlin source and build script file, including test files, to 300 lines or fewer as the first remediation priority.
- **FR-004**: The remediation MUST fix every directly detectable constitution violation without exceptions.
- **FR-005**: The remediation MUST preserve existing user-visible behavior unless a behavior change is explicitly documented as a bug fix.
- **FR-006**: The remediation MUST preserve existing test intent, including both success and failure-path coverage where applicable.
- **FR-007**: The remediation MUST keep source files focused by responsibility rather than moving unrelated content together only to satisfy the line limit.
- **FR-008**: The project MUST complete formatting, file-size audit, and the full project check after remediation.
- **FR-009**: Any validation gate that cannot be completed or does not pass MUST block feature completion until resolved.
- **FR-010**: Any touched user-facing UI MUST pass static accessibility review for labels, touch targets, contrast, keyboard or switch access, and non-color state indicators; manual TalkBack notes MUST be recorded when performed.
- **FR-011**: Project guidance documents MUST remain aligned with the constitution when remediation changes structure, conventions, or workflow.

### Key Entities *(include if feature involves data)*

- **Constitution Violation**: A detected mismatch between the project and an active constitution rule; includes rule name, affected artifact, measured evidence, status, and resolution notes.
- **Source File Audit Entry**: A measured source file record containing path, line count, limit, compliance status, and remediation status.
- **Validation Result**: Evidence from a project quality gate; includes gate name, outcome, timestamp, and unresolved issues if any.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of Kotlin source and build script files, including test files, contain 300 lines or fewer after remediation.
- **SC-002**: 100% of currently known directly detectable constitution violations are remediated with zero exceptions.
- **SC-003**: All existing critical attendance, student management, reporting, import, and export workflows remain functionally unchanged after remediation.
- **SC-004**: Formatting, file-size audit, and the full project check all pass before the work is considered ready.
- **SC-005**: 100% of touched UI surfaces have static accessibility review results, and any performed TalkBack checks have notes recorded.
- **SC-006**: Maintainers can review the final compliance status in under 5 minutes using the audit inventory and validation evidence.

## Assumptions

- "Source file" for the 300-line limit means Kotlin source files and build scripts, including production and test files. XML resources are not part of this feature's line-count gate.
- The initial audit identified these files over 300 lines: `ImportStudentsUseCaseTest.kt` (353), `StudentsViewModelTest.kt` (607), `ReportViewModelTest.kt` (313), `StudentsViewModel.kt` (320), `ComposeCalendar.kt` (315), `StudentDaoTest.kt` (320), `AttendanceDaoTest.kt` (356), `CalendarImageGeneratorTest.kt` (383), `CalendarScreenTest.kt` (456), and `ReportScreenTest.kt` (330).
- The primary remediation goal is consistency and maintainability, not user-facing feature expansion.
- Existing architecture and dependency rules remain unchanged.
- Static accessibility review is required for touched UI surfaces; manual TalkBack notes are required when TalkBack testing is performed.
