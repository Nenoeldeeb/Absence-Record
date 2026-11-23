# Bug Assessment: Attendance Sound Plays Twice

- **Slug**: attendance-double-sound
- **Created**: 2026-11-23T11:46:00+02:00
- **Source**: pasted text
- **Verdict**: valid
- **Severity**: low

## Report (verbatim or summarized)

> "when marking attendance the sound play twice sometimes."

## Symptom

When a user marks a student as present in the attendance dialog on the Calendar screen, the audio feedback tone occasionally plays twice instead of once. The expected behavior is a single, clean audio confirmation click/tone per action.

## Reproduction

1. Open the application and navigate to the **Calendar** tab (pager page 1).
2. Tap on any past or current date cell to open the `AttendanceDialog`.
3. Tap on an absent student's row (or tap rapidly / double-tap).
4. Observe that the audio confirmation tone (`ToneGenerator.TONE_SUP_PIP`) plays twice ("pip-pip" or two successive playback triggers).

## Suspected Code Paths

- `app/src/main/java/dev/nenoeldeeb/education/absencerecord/presentation/utils/SoundPlayer.kt:19-22,37-65` — `playAddAttendanceSound()` uses `ToneGenerator.TONE_SUP_PIP`. Under Android's `ToneGenerator`, `TONE_SUP_PIP` is an IS-95 supervisory "pip pip" tone pattern (100ms on, 100ms off, 100ms on). With `TONE_DURATION_MS = 150` and `RELEASE_DELAY_MS = 250L`, the system plays the first pip and begins playing the second pip before releasing, creating an audible double-beep.
- `app/src/main/java/dev/nenoeldeeb/education/absencerecord/presentation/screens/calendar/CalendarScreenContent.kt:77-86` — Sound playback is triggered via `LaunchedEffect(uiState.pendingSoundIsAdd)` and relies on an asynchronous event `CalendarScreenEvent.ConsumeSound` to reset the flag in `CalendarViewModel`. Rapid state emissions or concurrent Flow updates can cause race conditions.
- `app/src/main/java/dev/nenoeldeeb/education/absencerecord/presentation/screens/calendar/delegates/AttendanceBookingDelegate.kt:28-68` — `markStudentAttendance` does not debounce or track in-flight requests per student. If a user quickly taps twice before the Room write and flow emission finish, two records and two sound triggers are queued.
- `app/src/main/java/dev/nenoeldeeb/education/absencerecord/presentation/screens/calendar/dialogs/AttendanceDialog.kt:252-282` & `StudentListItem.kt:111-116` — `StudentListItem` does not guard against rapid consecutive taps while attendance modification is in progress.

## Root Cause Hypothesis

There are two primary contributing factors causing this behavior (Confidence: High):
1. **Tone Selection**: `ToneGenerator.TONE_SUP_PIP` naturally contains multiple burst pulses (pip-pip) in Android's tone generator implementation. Because `TONE_DURATION_MS` is set to 150ms and release happens at 250ms, the second pulse is audible on many devices. A single-pulse tone like `TONE_PROP_BEEP` or `TONE_PROP_ACK` produces a single acoustic feedback cue.
2. **Missing Tap Debounce / In-Flight Guard**: In `AttendanceDialog`, `toggleEnabled` is statically true during normal operation. If a user double-taps or taps rapidly on a student row, both taps invoke `onToggleAttendance` before the first database insert and `StateFlow` update have completed. This dispatches two `MarkStudentAttendance` actions, each succeeding and independently setting `pendingSoundIsAdd = true`.

## Proposed Remediation

**Preferred**:
1. In `SoundPlayer.kt`, replace `ToneGenerator.TONE_SUP_PIP` with a single-pulse tone (e.g. `ToneGenerator.TONE_PROP_BEEP` or `ToneGenerator.TONE_PROP_ACK`), or configure the duration so only a single beep plays.
2. In `SoundPlayer.kt`, add a throttling / debounce mechanism (e.g., minimum interval between played tones like 150–200ms) to prevent consecutive overlapping playback if multiple events arrive within milliseconds.
3. In `AttendanceBookingDelegate.kt` or `AttendanceDialog.kt`, prevent duplicate submissions for the same student while a mark/delete operation is already in-flight for that student and date.

**Alternatives**:
- Convert one-shot sound effects from ViewModel StateFlow (`pendingSoundIsAdd`) to a shared UI effect channel (`SharedFlow` or `Channel`), though keeping it in State complies with the project's current MVI pattern if properly throttled and debounced.

**Files likely to change**:
- `app/src/main/java/dev/nenoeldeeb/education/absencerecord/presentation/utils/SoundPlayer.kt`
- `app/src/main/java/dev/nenoeldeeb/education/absencerecord/presentation/screens/calendar/delegates/AttendanceBookingDelegate.kt`
- `app/src/main/java/dev/nenoeldeeb/education/absencerecord/presentation/screens/calendar/dialogs/AttendanceDialog.kt`
- `app/src/test/java/dev/nenoeldeeb/education/absencerecord/presentation/screens/calendar/CalendarViewModelMarkedDatesTest.kt`

**Tests to add or update**:
- Unit tests verifying rapid successive `markStudentAttendance` calls for the same student do not double-fire.
- Instrumentation / unit tests for `SoundPlayer` confirming single tone playback and debouncing behavior.

## Risks & Considerations

- Tone audible feedback should remain distinct between adding attendance and removing attendance (`TONE_SUP_BUSY` vs add tone).
- Must adhere to the 300-line file limit per `AGENTS.md`.
- No architectural changes to Room schema or migrations needed.

## Open Questions

- None.
