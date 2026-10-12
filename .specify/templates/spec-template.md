# Feature Specification: [FEATURE NAME]

**Feature Branch**: `[###-feature-name]`

**Created**: [DATE]

**Status**: Draft

**Input**: User description: "$ARGUMENTS"

<!--
  PROJECT: Absence Record — offline Android attendance tracker.
  Tailor this spec to: Jetpack Compose UI, Room local persistence, MVI
  presentation, Kotlinx Datetime, bilingual (en/ar) strings. Features are
  grouped by presentation feature under presentation/screens/<feature>/.
-->

## User Scenarios & Testing *(mandatory)*

<!--
  IMPORTANT: User stories should be PRIORITIZED as user journeys ordered by importance.
  Each user story/journey must be INDEPENDENTLY TESTABLE - meaning if you implement just ONE of them,
  you should still have a viable MVP (Minimum Viable Product) that delivers value.

  Assign priorities (P1, P2, P3, etc.) to each story, where P1 is the most critical.
  Think of each story as a standalone slice of functionality that can be:
  - Developed independently
  - Tested independently
  - Demonstrated to users independently

  For this project, "independently testable" means covered by a JVM unit test
  (JUnit 6 + MockK) and, for user-facing UI, a Compose test (instrumented).
-->

### User Story 1 - [Brief Title] (Priority: P1)

[Describe this user journey in plain language]

**Why this priority**: [Explain the value and why it has this priority level]

**Independent Test**: [Describe how this can be tested independently - e.g., "Can be fully tested by [specific action] and delivers [specific value]"]

**Acceptance Scenarios**:

1. **Given** [initial state], **When** [action], **Then** [expected outcome]
2. **Given** [initial state], **When** [action], **Then** [expected outcome]

---

### User Story 2 - [Brief Title] (Priority: P2)

[Describe this user journey in plain language]

**Why this priority**: [Explain the value and why it has this priority level]

**Independent Test**: [Describe how this can be tested independently]

**Acceptance Scenarios**:

1. **Given** [initial state], **When** [action], **Then** [expected outcome]

---

### User Story 3 - [Brief Title] (Priority: P3)

[Describe this user journey in plain language]

**Why this priority**: [Explain the value and why it has this priority level]

**Independent Test**: [Describe how this can be tested independently]

**Acceptance Scenarios**:

1. **Given** [initial state], **When** [action], **Then** [expected outcome]

---

[Add more user stories as needed, each with an assigned priority]

### Edge Cases

<!--
  ACTION REQUIRED: The content in this section represents placeholders.
  Fill them out with the right edge cases.
-->

- What happens when [boundary condition]?
- How does system handle [error scenario]?

## Requirements *(mandatory)*

<!--
  ACTION REQUIRED: The content in this section represents placeholders.
  Fill them out with the right functional requirements.
-->

### Functional Requirements

<!--
  Conventions for this project:
  - Use MUST for binding behaviour, SHOULD for recommended, MAY for optional.
  - Numbering is FR-NNN, sequential across the whole spec, never reused.
  - If a requirement involves a rule boundary (capacity, overlap, time range),
    state the exact threshold — e.g. "MUST reject lessons beyond capacity".
  - If it involves persistence, state whether it lands in Room or in the
    exported JSON backup format.
  - Mark anything genuinely undecided with [NEEDS CLARIFICATION: ...].
-->

- **FR-001**: System MUST [specific capability, e.g., "record attendance for a student on a given date"]
- **FR-002**: System MUST [specific capability, e.g., "reject a lesson that would overlap a busy appointment"]
- **FR-003**: User MUST be able to [key interaction, e.g., "undo a student deletion"]
- **FR-004**: System MUST [data requirement, e.g., "persist lesson assignments in Room"]
- **FR-005**: System MUST [behavior, e.g., "exclude students in unselected classes from the count"]

*Example of marking unclear requirements:*

- **FR-006**: System MUST [NEEDS CLARIFICATION: undo window not specified]
- **FR-007**: System MUST [NEEDS CLARIFICATION: whether partial lesson durations are in scope]

### Accessibility Requirements *(mandatory)*

<!--
  Constitution Principle VI — accessibility regressions are treated as bugs.
  Every user-facing FR above SHOULD have a matching accessible affordance here.
-->

- **AC-001**: All interactive elements MUST expose a meaningful `contentDescription`, or be explicitly marked decorative
- **AC-002**: All touch targets MUST be at least 48dp
- **AC-003**: State MUST NOT be conveyed by color alone — pair it with an icon or text label
- **AC-004**: Text and interactive elements MUST meet 4.5:1 contrast (3:1 for large text)

### Localization Requirements *(mandatory)*

- **LZ-001**: All user-facing strings MUST live in `strings.xml` with an Arabic translation in `values-ar/`; NO hardcoded strings
- **LZ-002**: Strings MUST be surfaced through the `UiText` wrapper so they resolve at composition time

### Data & Migration Requirements *(include if the feature touches persistence)*

<!--
  Include this section when Room entities/DAOs or the export/import JSON change.
-->

- **DM-001**: A schema change MUST ship an explicit Room `Migration` with a version bump — destructive migration is NOT acceptable for user data
- **DM-002**: Exported schema JSON under `app/schemas/` MUST be committed alongside the change
- **DM-003**: A schema change MUST include a migration test in `androidTest/`
- **DM-004**: If the backup format changes: state the new `version` field value and how legacy files are handled (accepted / partially restored / rejected)

### Key Entities *(include if feature involves data)*

<!--
  Use real domain model names from domain/models/ — e.g. Student,
  StudentAttendance, AvailableLessonHour, LessonAssignment, BusyAppointment.
  Describe attributes and relationships, not Room annotations.
-->

- **[Entity 1]**: [What it represents, key attributes without implementation]
- **[Entity 2]**: [What it represents, relationships to other entities]

## Success Criteria *(mandatory)*

<!--
  ACTION REQUIRED: Define measurable success criteria.
  These must be technology-agnostic and measurable.
-->

### Measurable Outcomes

<!--
  Prefer criteria a test can assert (e.g. "export writes a version 2 file
  containing N records") over subjective ones. The app is OFFLINE-ONLY —
  avoid criteria about server latency, throughput, or concurrent users.
-->

- **SC-001**: [Measurable metric, e.g., "a single student can be marked absent for a date in under 3 taps"]
- **SC-002**: [Measurable metric, e.g., "exporting 500 students completes without blocking the main thread"]
- **SC-003**: [Metric, e.g., "100% of new strings have an Arabic translation"]
- **SC-004**: [Business metric, e.g., "a teacher can confirm a day's attendance in a single pass"]

## Assumptions

<!--
  Context that holds for this project: the app is a fully offline, local-only
  Android app. There is no server, no account system, and no network
  dependency. Data persists locally; export/import JSON is the only transfer
  mechanism.
-->

- [Assumption about target users, e.g., "a single teacher manages their own classes"]
- [Assumption about scope boundaries, e.g., "cloud sync is out of scope"]
- [Assumption about data/environment, e.g., "the existing Room schema and versioned backup format are reused"]
- [Dependency on existing system/service, e.g., "depends on the shared class-filter state already existing"]
