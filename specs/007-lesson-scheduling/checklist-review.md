# Review — 007 Lesson Scheduling (2026-08-10)

Scope: latest feature (default). Superpowers `requesting-code-review` not detected → built-in protocol.
Confidence threshold: >= 80. Findings grouped by severity.

## Critical
- None found.

## Important

| # | Finding | Conf | Location | Fix | Status |
|---|---------|------|----------|-----|--------|
| I1 | File exceeds 300-line constitution limit (345 lines) | 100 | `domain/usecases/transfer/PerformImportUseCase.kt` | Split merge helpers into a separate file | ✅ Fixed — `PerformImportUseCase.kt` (149) + new `PerformImportMergeEngine.kt` (227) |
| I1b | Test file exceeds 300-line limit (435 lines) | 100 | `app/src/test/kotlin/.../transfer/PerformImportScheduleMergeTest.kt` | Split merge-skip tests into a second file | ✅ Fixed — shared `PerformImportUseCaseTestBase.kt` + `PerformImportHoursMergeTest.kt` (282) + `PerformImportScheduleMergeTest.kt` (245) |
| I2 | FR-004 rejection message is raw English `StudentError.Validation(...)` mapped to `UiText.DynamicString` — i18n violation | 95 | `data/.../daos/ScheduleDao.kt:147-149` + `presentation/utils/StudentErrorUiMapper.kt:20` | Add typed `StudentError.MaxBelowAssigned` and map to a `strings.xml` resource | ✅ Fixed — `StudentError.MaxBelowAssigned`, `error_max_below_assigned` in `values/` + `values-ar/` |
| I3 | Unassign resolves hour id from stale `hoursForWeekday` (not cleared in `selectWeekday`) — can no-op silently or unassign wrong-weekday lesson | 85 | `studentdetail/StudentScheduleTabsDelegate.kt:92-97` | Add `it.hour.weekday == lesson.weekday` to predicate + reset cache in `selectWeekday` | ✅ Fixed |
| I4 | FR-029 capacity-raise path untested | 95 | `PerformImportUseCase.kt:254-269` | Add test: pre-existing hour + imported lesson needing raised capacity | ✅ Fixed — `raises existing hour capacity to fit imported lessons` in `PerformImportHoursMergeTest.kt` |
| I5 | FR-028 busy-wins import removal untested with positive count | 95 | `PerformImportUseCase.kt:203-205` | Add test asserting `lessonsRemovedBusyWinsCount > 0` | ✅ Fixed — `imported busy removes conflicting lessons and reports wins` in `PerformImportScheduleMergeTest.kt` |
| I6 | Duplicate student names in one backup are not merged (spec edge case) | 85 | `PerformImportUseCase.kt:66-80` | Consult `studentIdByName` when resolving existing students | ✅ Fixed — `studentIdByName` cache consulted before `existingMap` lookup |

## Suggestion

| # | Finding | Conf | Location | Fix | Status |
|---|---------|------|----------|-----|--------|
| S1 | Hardcoded "–" time-range separator | 90 | `HourRow.kt:53`, `AddLessonDialog.kt:105`, `StudentScheduleLessonsTab.kt:67` | Use a `%1$s – %2$s` string resource | ✅ Fixed — `time_range` resource; all 3 call sites updated |
| S2 | Dead code: `existingAssignments` fetched, never used | 100 | `PerformImportUseCase.kt:46-47` | Remove the extra DB query | ✅ Fixed — removed during merge-engine extraction |
| S3 | Re-import duplicates busy appointments for existing students | 80 | `PerformImportUseCase.kt:186-208` | Skip busy entries identical to an existing one | ✅ Fixed — `existingKeys` dedup in `PerformImportMergeEngine.mergeBusyAppointments` |
| S4 | M3 default buttons below 48dp touch target | 85 | `AppointmentsTab.kt:46,75`, `HourDialog.kt:121,126`, `ScheduleConfirmDialog.kt:32,43` | `Modifier.defaultMinSize(minHeight = 48.dp)` | ✅ Fixed — all 5 buttons |
| S5 | Decorative page dots announce "selected" to TalkBack | 85 | `StudentDetailPageDots.kt:43` | `clearAndSetSemantics {}` (pager semantics already announce position) | ✅ Fixed — dots now `clearAndSetSemantics { testTag }`; pager test asserts display only |
| S6 | AddLessonDialog Save stays enabled if selection becomes ineligible | 85 | `AddLessonDialog.kt:82` | Re-check eligibility of the selected hour before enabling confirm | ✅ Fixed — Save enabled only when selected hour is currently eligible |

## Verified compliant (selected)
- US1/FR-001..005/013: hour CRUD, occupancy, overlap reject, capacity-drop reject, cascade, empty state, confirm dialog — `ScheduleDaoTest`, `ScheduleScreenAppointmentsTest`.
- US2/FR-006..008/018/019: AddLessonDialog eligibility + reasons; unassign; one-per-day guard.
- US3/FR-009..012: busy presets 15..240, boundary correctness, preview-and-confirm.
- FR-017/020: edit auto-removal toast; tap-to-profile state restore (`ScheduleNavigationTest`).
- FR-021: 2-page pager, header pinned, dots, reset (`StudentDetailBodyPagerTest`).
- FR-022..033: versioned format, legacy fallback, version reject, additive merge, skip+count, full-plan export.
- Constitution: MVI, `Result<T>` onSuccess/onFailure, kotlinx-datetime only, localized strings, accessibility, all schedule use cases test both paths.

## Verification (2026-08-10)
- `./gradlew ktlintFormat` clean.
- `./gradlew testDebugUnitTest` — all pass (transfer package incl. new I4/I5 tests).
- `./gradlew compileDebugAndroidTestKotlin` — passes (pager dots + `StudentsScreenTest` updated to `parsedImportData` API).
