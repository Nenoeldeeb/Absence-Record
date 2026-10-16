# Code Review — 008 Duration-Based Scheduling

**Reviewed**: 2026-10-16 · **Scope**: Phase 8 (Convergence) changes, uncommitted
working tree · **Reviewer**: `speckit.superspec.review`

`requesting-code-review` was not present (`.specify/superpowers.yml`,
`detected: false`), so the built-in protocol ran: spec compliance, edge-case
coverage, constitution compliance, code quality, test coverage. Findings below 80
confidence are suppressed; four were raised, all above threshold.

---

## Critical

### R1 — The confirm gate could still unassign a student it never showed · confidence 97

**Where** `domain/services/LessonSlotPlanner.kt` (new `closedLosses`),
`domain/models/LessonPlanning.kt` (`PlanResult.studentsLost`),
`data/datasources/local/daos/ScheduleDaoHelpers.kt` (`closedDropSet`),
`ScheduleDao.kt:updateHour`

Phase 8's T088 fix computed the drop list in a single pass over the plan. But the
running order is *derived*, so deleting a student re-plans every survivor's slot.
A survivor can therefore slide backwards into a slot that lands on their own
commitment — and be unassigned without ever appearing in the list the teacher
confirmed. FR-013 requires the retained students to remain assigned; SC-015
requires that zero students are removed without confirmation.

**Reproduced on a reachable state**, not a synthetic one — a 09:00–10:00 block
holding Tara (busy 09:10–09:40) and Sam (busy 08:40–09:10, which starts before the
block so it sorts last):

```
proposal showed   [1]        (Tara only)
confirming removed [1, 2]   (Tara AND Sam)
```

An exhaustive search over every legal block length, start and commitment window
found **5,903** rosters with this shape, and **0** survivors that ever move
later — so iterating to a fixed point is both necessary and sufficient.

The reviewer's own reproduction was arithmetically wrong (its numbers produce two
conflicts, not one); the finding survived only because it was verified against the
real planner rather than accepted or dismissed on the prose.

**Fix** — `PlanResult.studentsLost()` moved into `domain/models/` (it is a pure
rule over a domain model, and had been parked in the DAO-helper file purely to
dodge the line cap), plus a new pure `LessonSlotPlanner.closedLosses()` that
re-plans the remaining roster until nobody new collides. Every continuing pass
removes at least one student, so it terminates in at most 12 passes.
`ScheduleDao.closedDropSet()` feeds it the roster. Covered by
`ScheduleDaoReplanFixedPointTest` (3 tests: both names shown, removals equal the
confirmed set, declining removes nobody).

## Important

### R2 — The backup contract contradicted itself · confidence 92

**Where** `specs/008-duration-based-scheduling/contracts/backup-file-format.md`

T091 added `MAX_IMPORT_FILE_BYTES` as the first row of the cap table but left the
pre-existing blanket sentence beneath it unqualified. Applied to that row it
states the opposite of the code: the size guard returns
`StudentError.ImportFileTooLarge` and aborts the whole import, where the sentence
promises "skips and counts the surplus" and "never aborts the remaining entries".
The same document's new "Why there are two layers" section then asserted the
correct behaviour eight lines later.

FR-059 reads literally as non-conformant for this cap unless it is scoped to
record counts. **Fix**: the paragraph now separates the two layers — record caps
skip and count, the file guard refuses the whole file — and the header's spec
range was widened from `FR-042…FR-047, FR-040, FR-059` to include FR-048,
FR-056…FR-058, FR-062 and FR-063.

### R3 — A pure business rule was parked in the Room DAO-helper file · confidence 90

**Where** `data/datasources/local/daos/ScheduleDaoHelpers.kt`

`PlanResult.studentsLost()` was added there as a line-count workaround, and
declared `internal` so only the data layer could reach it. The constitution's
source-tree map assigns *pure business rules* to `domain/services/` and
*Room `@Dao` interfaces + query helpers* to `data/datasources/local/daos/`. The
placement was acknowledged in T088 as a cap workaround, but Principle V requires a
cap breach to be fixed by decomposing into focused units — not by putting a rule
in the wrong layer.

**Fix**: resolved by R1's relocation to `domain/models/LessonPlanning.kt`, where it
is reachable and correctly layered. `ScheduleDaoHelpers` retains only the
I/O-shaped `closedDropSet` that supplies the roster.

### R4 — The constitution's open TODO now contradicted the code · confidence 88

**Where** `.specify/memory/constitution.md` (SYNC IMPACT REPORT, follow-up TODOs)

The 2026-10-12 amendment recorded `StudentsScreenTest.kt` as a 302-line Principle V
breach, "reported, not fixed here". T087 fixed it, and tasks.md then claimed the
TODO was closed — while the constitution, a governance document, still asserted
the violation as open.

**Fix**: the TODO now records that it was closed on 2026-10-16, how, and with what
verification.

---

## Verified sound (no findings)

- **Overlap SQL** (R/T089): correct half-open semantics using the existing row's
  own length; `excludeId` still excludes the edited block and `excludeId = 0` is
  safe for inserts.
- **`distinctBy { it.studentId }`** in `studentsLost()` is sound — `dropped` and
  `conflicts` are drawn from disjoint lists (`conflicts` covers only the kept set).
- **Import ordering**: `raiseExistingHourCapacities` still runs before
  `executePlannedLessons`; `postBusyAssignments` remains the correct post-busy
  snapshot for both `existingLessonWeekdays` and the capacity maths.
- **Import resource bounds**: the 32 MB guard is enforced *before* decode
  (`StorageRepositoryImpl.kt:37-39`); removing the busy pre-filter does not worsen
  the worst case, since a conflict-free file already reached `assignStudent` in bulk.
- **No silent data loss on import**: refusals land in the same `lessonsSkipped`
  counter as before and are surfaced in the summary.
- **Constitution gates**: 0 files above 300 lines, 0 `java.time`, 0
  `domain/` → data/presentation imports, 0 bare `collectAsState()`, string
  parity intact, no secrets or build artifacts introduced.
- **`ScheduleNavigationTest`**: failed on API 35 with
  `RootViewWithoutFocusException` but passes on a freshly restarted emulator and
  touches no changed code path (it seeds the real database via
  `insertAvailableHour`, bypassing the overlap check). Emulator degradation, not a
  regression.