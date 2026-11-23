# Bug Fix: Attendance Sound Plays Twice

- **Slug**: attendance-double-sound
- **Fixed**: 2026-11-23T14:22:00+02:00
- **Assessment**: ./assessment.md
- **Status**: applied

## Summary

Fixed attendance audio feedback occasionally playing twice (for both add and remove attendance actions) by replacing multi-burst supervisory tones (`TONE_SUP_PIP` for add, and `TONE_SUP_BUSY` for remove) with clean single-burst tones (`ToneGenerator.TONE_PROP_BEEP` and `ToneGenerator.TONE_PROP_NACK`), adding a minimum interval throttle in `SoundPlayer`, and adding an in-flight guard in `AttendanceBookingDelegate` to reject rapid duplicate taps for the same student and date.

## Changes

| File | Change | Notes |
|------|--------|-------|
| `app/src/main/java/dev/nenoeldeeb/education/absencerecord/presentation/utils/SoundPlayer.kt` | modified | Replaced `TONE_SUP_PIP` with `TONE_PROP_BEEP` for add attendance and `TONE_SUP_BUSY` with `TONE_PROP_NACK` for remove attendance; added 150ms playback throttle. |
| `app/src/main/java/dev/nenoeldeeb/education/absencerecord/presentation/screens/calendar/delegates/AttendanceBookingDelegate.kt` | modified | Added in-flight set tracking to ignore concurrent duplicate mark/delete requests for the same student and date. |
| `app/src/test/java/dev/nenoeldeeb/education/absencerecord/presentation/screens/calendar/CalendarViewModelMarkedDatesTest.kt` | modified | Added unit tests verifying rapid duplicate mark and delete actions while in-flight only invoke use cases once. |

## Diff Highlights

```kotlin
// SoundPlayer.kt
fun playAddAttendanceSound(context: Context) {
    if (!shouldPlay(context)) return
-   playTone(ToneGenerator.TONE_SUP_PIP)
+   playTone(ToneGenerator.TONE_PROP_BEEP)
}

fun playRemoveAttendanceSound(context: Context) {
    if (!shouldPlay(context)) return
-   playTone(ToneGenerator.TONE_SUP_BUSY)
+   playTone(ToneGenerator.TONE_PROP_NACK)
}

private fun shouldPlay(context: Context): Boolean {
    val now = System.currentTimeMillis()
    if (now - lastPlayedTimestampMs < MIN_INTERVAL_MS) {
        return false
    }
...
```

```kotlin
// AttendanceBookingDelegate.kt
private val inFlightStudentDates = mutableSetOf<Pair<Int, LocalDate>>()

fun markStudentAttendance(studentId: Int, date: LocalDate) {
    ...
    val key = studentId to date
    if (!inFlightStudentDates.add(key)) return
    scope.launch {
        try {
            attendanceUseCases.recordStudentAttendanceUseCase(...)
            ...
        } finally {
            inFlightStudentDates.remove(key)
        }
    }
}

fun deleteStudentAttendance(studentId: Int, date: LocalDate) {
    ...
    val key = studentId to date
    if (!inFlightStudentDates.add(key)) return
    scope.launch {
        try {
            attendanceUseCases.deleteStudentAttendanceUseCase(...)
            ...
        } finally {
            inFlightStudentDates.remove(key)
        }
    }
}
```

## Tests Added or Updated

- `CalendarViewModelMarkedDatesTest::rapid duplicate markStudentAttendance while in-flight calls use case only once` — verifies that rapid duplicate taps for the same student and date do not trigger multiple database operations or duplicated sound events.
- `CalendarViewModelMarkedDatesTest::rapid duplicate deleteStudentAttendance while in-flight calls use case only once` — verifies deletion calls while in-flight are also safely guarded.

## Local Verification

- Commands run:
  - `./gradlew testDebugUnitTest --tests "dev.nenoeldeeb.education.absencerecord.presentation.screens.calendar.CalendarViewModelMarkedDatesTest"` → Passed (BUILD SUCCESSFUL).
  - `./gradlew ktlintFormat` → Passed (BUILD SUCCESSFUL, code formatted and verified).
  - `./gradlew testDebugUnitTest` → Passed (BUILD SUCCESSFUL, all unit tests green).

## Deviations from Assessment

Extended tone remediation to the remove attendance event as requested, replacing the multi-burst supervisory tone `TONE_SUP_BUSY` with the single-burst negative acknowledgment tone `ToneGenerator.TONE_PROP_NACK`.

## Follow-ups

- None.
