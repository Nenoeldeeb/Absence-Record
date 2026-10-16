# Specification Quality Checklist: Duration-Based Lesson Scheduling

**Purpose**: Validate specification completeness and quality before proceeding to planning
**Created**: 2026-10-13
**Feature**: [spec.md](../spec.md)

**Note**: This is the built-in lifecycle checklist maintained by `/speckit.specify` and `/speckit.clarify`. It is not the reviewer-owned `/speckit.checklist` artifact.

## Content Quality

- [x] CHK001 No implementation details (languages, frameworks, APIs)
- [x] CHK002 Focused on user value and business needs
- [x] CHK003 Written for non-technical stakeholders
- [x] CHK004 All mandatory sections completed

## Requirement Completeness

- [x] CHK005 No [NEEDS CLARIFICATION] markers remain
- [x] CHK006 Requirements are testable and unambiguous
- [x] CHK007 Success criteria are measurable
- [x] CHK008 Success criteria are technology-agnostic (no implementation details)
- [x] CHK009 All acceptance scenarios are defined
- [x] CHK010 Edge cases are identified
- [x] CHK011 Scope is clearly bounded
- [x] CHK012 Dependencies and assumptions identified

## Feature Readiness

- [x] CHK013 All functional requirements have clear acceptance criteria
- [x] CHK014 User scenarios cover primary flows
- [x] CHK015 Feature meets measurable outcomes defined in Success Criteria
- [x] CHK016 No implementation details leak into specification

## Notes

**Iteration 1 (2026-10-13) — failures found and corrected:**

- CHK001/CHK016 — first draft referenced the project's rule constant, storage
  technology, serialization format and screen class names. Rewritten to describe
  behaviour only ("lesson block", "stored plan", "backup file", "block list").
- CHK005 — three markers remained: FR-009 (Q1), FR-018 (Q3) and FR-032 (Q2).
  Each mapped to a question with materially different outcomes, so guessing
  would have changed scope or caused silent data loss.
- CHK011 — scope was originally unbounded around commitment edits; a
  requirement now states explicitly that the existing conflict check narrows
  from whole-block to per-slot, and US4/FR-044 pin the untouched rules.

**Iteration 2 (2026-10-13) — clarifications answered.**

The three open questions were put to the user and answered: **Q1 = B, Q2 = A,
Q3 = A**. Consequences encoded into `spec.md`:

- **Q1 = B** — shortening below occupancy is allowed. New requirements FR-009 to
  FR-013 define the keep/drop computation (retain the first *N* of the full
  ordering), the mandatory confirmation step, and the all-or-nothing apply.
  US1 scenarios 5–7 and SC-011 cover the flow.
- **Q2 = A** — no stored slot position. FR-023 and FR-037 make the derived
  ordering the single source of truth; the Key Entities entry for the
  assignment record was corrected to drop "position in the running order".
  The accepted consequence — a student who gains an earlier commitment moves
  earlier — is stated in US2 scenario 9, the Edge Cases section, and
  Assumptions so it cannot be re-opened as a defect during planning.
- **Q3 = A** — alphabetical fallback, promoted to FR-021. FR-020 reuses the same
  alphabetical rule as the tie-break for equal commitment times (the original
  description allowed any deterministic rule), and FR-022 adds a final stable
  identifier so duplicate names cannot make the order non-deterministic.

Requirements were renumbered sequentially (FR-001 to FR-048, none reused) to
keep the numbering contiguous after the five inserted requirements.

**Iteration 3 (2026-10-13) — final validation.**

All 16 items pass. No `[NEEDS CLARIFICATION]` markers remain; every open
question carries a recorded resolution.

**Iteration 4 (2026-10-13) — post-brainstorm revalidation.**

A `/speckit.superspec.brainstorm` session added Q4–Q8 and grew the spec from 48
to 62 requirements. Re-checked the items most at risk from that growth:

- CHK005 — still no `[NEEDS CLARIFICATION]` markers. Q4–Q8 were resolved by the
  user rather than assumed, and each is recorded in the Open Questions table.
- CHK006 — re-checked every newly added requirement for testability. The two
  that were initially ambiguous were tightened: the import caps now carry
  concrete, domain-derived numbers (FR-057, FR-058) instead of "a documented
  maximum", and the ordering-rule references were verified to point at the
  rules they claim (FR-010 → FR-019–FR-024; FR-039 → FR-019–FR-025).
- CHK010 — edge cases extended with slider granularity, ambiguous backup
  entries, record-cap behaviour, and the repeated-derivation cost.
- CHK011 — scope stayed bounded. Q7's caps and Q8's no-cache rule were both
  framed as constraints on existing behaviour, not as new subsystems.
- CHK012 — dependencies and assumptions extended with the derived-order cost,
  the rejection of caching, the provenance of each cap number, and a note that
  backup files remain unencrypted (pre-existing, unchanged).

**Numbering**: FR-001…FR-062 and SC-001…SC-019, both machine-verified
contiguous with no duplicates and no dangling cross-references.

All 16 items pass. Spec is ready for `/speckit.plan`.