# Cross-requirement audit — T084

Audited 2026-10-16 against `spec.md`: **FR-001 … FR-064 (64 requirements)** and
**SC-001 … SC-019 (19 criteria)**.

**Result: every FR and SC is implemented. Nothing deferred.**

Two requirements are numbers rather than code, and are recorded with the
measurement that satisfies them:

| Req | Claim | Evidence |
|---|---|---|
| SC-019 | Full day renders correctly ordered in < 1 s | Derivation **3.3 ms** median (JVM, 1,008 blocks); render **33 ms** (device, 144 blocks). See `DayPlanningPerformanceTest`, `ScheduleDayRenderMeasurementTest`. |
| FR-058 | Student cap of 10,000 stated in the summary when hit | `ImportExportDelegate` emits `import_complete_student_cap_hit` naming the cap. |

## Deviations from the plan, all deliberate

| Planned | Actual | Why |
|---|---|---|
| `LessonSlotPlannerTest` (one class) | split into `…OrderingTest` + `…SlotTest` | 300-line cap |
| One instrumented run per class | full `connectedDebugAndroidTest` on two devices | per-class runs silently overwrote each other's results |
| `ImportMergeEngine` holds the caps | caps extracted to `ImportCapEnforcement.kt` | the engine crossed 300 lines (T066 anticipated this) |
| `HourSlotList.kt` extraction | slot rendering lives in `HourAssignedStudents.kt` | keeps `HourRow.kt` untouched, as the plan preferred |
| Record caps alone bound import memory | **added a 32 MB pre-decode file guard (FR-063)** | a cap applied after decoding cannot prevent an OOM — memory is already spent |
| `MAX_IMPORT_STUDENTS` accepted at face value | kept, but **attendance dates also capped** | the unbounded date list, not the student count, dominates memory |

## Known environment limitations

- **No `connectedReleaseAndroidTest`** — AGP 9 does not create one here, so
  SC-019 was measured on a debug build. Cold-start figures (1.2–1.7 s) are
  reported separately from warm figures (33 ms) precisely because they are not
  comparable.
- **API 26 and API 35 are emulators**, not a physical mid-range device. SC-019
  asks for "a typical mid-range device"; that remains unverified on hardware.
