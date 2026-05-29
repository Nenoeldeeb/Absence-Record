# Refactoring Safety Checklist: Refactor Students ViewModel

**Purpose**: PR-review gate checklist validating that the spec adequately guards against behavioral regressions during ViewModel-to-delegate extraction
**Created**: 2026-05-29
**Feature**: [spec.md](file:///home/nenoeldeeb/Projects/AndroidStudioProjects/Absencerecord/specs/001-refactor-students-viewmodel/spec.md)

## Feature Inventory Completeness

- [ ] CHK001 - Is every existing feature/behavior that must survive the refactoring explicitly enumerated in the spec? [Gap, Spec §FR-001]
- [ ] CHK002 - Does the spec define what "all existing features" (FR-001) means concretely — is there a feature manifest or is the reader expected to reverse-engineer it from the 500-line ViewModel? [Clarity, Spec §FR-001]
- [ ] CHK003 - Are the implicit behaviors (e.g., undo support, optimistic UI, auto-refresh, pull-to-refresh) that are NOT listed in User Stories 1-4 but may exist in the current ViewModel addressed? [Coverage, Gap]
- [ ] CHK004 - Is there a documented inventory of public ViewModel APIs (events, state fields, side effects) that external callers depend on and must remain stable? [Gap, Spec §Assumptions]
- [ ] CHK005 - Does the spec specify a verification process (e.g., diff review, test coverage report) to confirm FR-001 compliance beyond test pass rate? [Completeness, Spec §Success Criteria]

## Behavioral Preservation Safety

- [ ] CHK006 - Is "zero visual or functional changes" (SC-003) defined with measurable criteria — what constitutes a "functional change" that would fail the gate? [Clarity, Spec §SC-003]
- [ ] CHK007 - Are the exact dialog states, toast messages, and error strings that exist today catalogued as the behavioral baseline? [Gap, Spec §User Stories 1-4]
- [ ] CHK008 - Does the spec define what happens when a delegate refactoring accidentally alters an edge-case behavior that lacks test coverage? [Gap, Spec §Edge Cases]
- [ ] CHK009 - Are the state transition sequences (e.g., loading → data → error → retry) that the current ViewModel produces documented as invariant constraints? [Completeness, Key Entities]

## Extraction Boundary Clarity

- [ ] CHK010 - Are the responsibility boundaries between StudentActionDelegate and ClassActionDelegate explicitly defined (what each owns vs. what the ViewModel retains)? [Clarity, Spec §Clarifications 2026-05-29]
- [ ] CHK011 - Is the contract for how delegates return results (Result<T> typed errors) documented as a requirement, not just an assumption? [Completeness, Spec §Key Entities — StudentError]
- [ ] CHK012 - Is it specified which coroutine context delegates run on (e.g., Dispatchers.IO vs. the ViewModel's scope) and whether main-safety is delegated or retained? [Gap, Spec §Assumptions]
- [ ] CHK013 - Are the import two-phase boundary (read/parse vs. persist) contracts specified with input/output types, not just described narratively? [Clarity, Spec §User Story 4, Acceptance 1]

## Edge Case Behavioral Mapping

- [ ] CHK014 - Are all scenarios in the Edge Cases section mapped to both the pre-refactoring and post-refactoring code paths to prove equivalent handling? [Gap, Spec §Edge Cases]
- [ ] CHK015 - Does the spec define how delegate-level StudentError variants (Database, FileRead, ImportParse, Cancelled) map one-to-one to existing error behaviors — ensuring no error path disappears or changes message text? [Coverage, Spec §Key Entities — StudentError]
- [ ] CHK016 - Are cancellation flows (file picker cancellation, user aborting multi-step import) specified as distinct require-preserve behaviors? [Gap, Spec §Edge Cases, Line 88]
- [ ] CHK017 - Does the spec define what happens to mid-flight operations when the user rapidly toggles screens or the composable leaves composition? [Gap, Spec §Edge Cases]

## Preservation Test Coverage

- [ ] CHK018 - Is there a requirement to validate that the existing test suite (FR-004) has explicit coverage parity for every delegate-extracted operation? [Completeness, Spec §FR-004, FR-005]
- [ ] CHK019 - Does the spec define what constitutes sufficient delegate unit test coverage (FR-005) — e.g., must each StudentError variant have a dedicated test case? [Clarity, Spec §FR-005]
- [ ] CHK020 - Are integration-level tests (ViewModel + real delegates) required in addition to unit tests to catch extraction regressions? [Gap, Spec §FR-005]
- [ ] CHK021 - Does FR-004 specify whether existing tests must continue to pass *without modification* (i.e., no test changes allowed to fit new delegate structure) or may tests be adapted? [Clarity, Spec §FR-004]
- [ ] CHK022 - Is the same behavior assertion coverage required for delegate tests as exists in the current ViewModel tests? [Gap, Spec §SC-004]

## Regression Detection & Gate Criteria

- [ ] CHK023 - Are the line-count targets (SC-001, <300) and test-pass targets (SC-002, 100%) sufficient to gate the refactoring, or should a behavioral-diff gate (e.g., screenshot comparison, API trace comparison) be required? [Completeness, Spec §Success Criteria]
- [ ] CHK024 - Is the process for handling a regression (e.g., a test that passes but behavior changed, or a behavior that has no test) specified? [Gap, Spec §Edge Cases, Success Criteria]
- [ ] CHK025 - Does SC-003 ("zero visual or functional changes") conflict with the structural necessity of changing how state flows through delegates vs. directly in the ViewModel? [Consistency, Spec §SC-003 vs. FR-003]
- [ ] CHK026 - Are review gates specified for the delegate public API surface area to prevent scope creep beyond the documented boundaries? [Gap, Spec §Out of Scope]

## Dependency & Assumption Stability

- [ ] CHK027 - Is the assumption that "repositories/DAOs/Room entities are out of scope and will not change" validated against the actual delegate interaction patterns? [Assumption, Spec §Out of Scope]
- [ ] CHK028 - Does the spec define fallback behavior if a delegate extraction reveals an undocumented dependency between ViewModel logic and UI composables (contradicting the "UI unaffected" assumption)? [Gap, Spec §Assumptions]
- [ ] CHK029 - Are all clarifications from Session 2026-05-29 traceable to specific spec sections, or do they exist only in the Clarifications appendix? [Traceability, Spec §Clarifications]

## Notes

- [ ] CHK030 - Check items off as completed: `[x]`
- [ ] CHK031 - Add comments or findings inline with the item number
- [ ] CHK032 - Link to relevant spec sections or external references
- [ ] CHK033 - Items are numbered sequentially for easy reference across reviews
