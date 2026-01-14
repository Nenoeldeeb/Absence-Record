package dev.nenoeldeeb.education.absencerecord.presentation.util

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation tests for [SoundPlayer].
 *
 * These tests verify that SoundPlayer methods execute without throwing exceptions. Since audio
 * output cannot be easily asserted, we verify graceful execution and error handling on actual
 * Android devices.
 */
@RunWith(AndroidJUnit4::class)
class SoundPlayerTest {
    @Test
    fun playAddAttendanceSound_doesNotThrow() {
        // Should execute without throwing any exception
        SoundPlayer.playAddAttendanceSound()
    }

    @Test
    fun playRemoveAttendanceSound_doesNotThrow() {
        // Should execute without throwing any exception
        SoundPlayer.playRemoveAttendanceSound()
    }

    @Test
    fun playAddAttendanceSound_multipleCallsInSuccession_doesNotThrow() {
        // Rapid succession shouldn't cause issues
        repeat(5) { SoundPlayer.playAddAttendanceSound() }
    }

    @Test
    fun playRemoveAttendanceSound_multipleCallsInSuccession_doesNotThrow() {
        // Rapid succession shouldn't cause issues
        repeat(5) { SoundPlayer.playRemoveAttendanceSound() }
    }

    @Test
    fun alternatingPlayCalls_doesNotThrow() {
        // Alternating between add and remove sounds
        repeat(3) {
            SoundPlayer.playAddAttendanceSound()
            SoundPlayer.playRemoveAttendanceSound()
        }
    }
}