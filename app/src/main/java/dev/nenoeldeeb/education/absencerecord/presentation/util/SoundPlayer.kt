package dev.nenoeldeeb.education.absencerecord.presentation.util

import android.media.AudioManager
import android.media.ToneGenerator

/**
 * Utility object for playing audio feedback sounds for attendance actions.
 * Uses Android's ToneGenerator for lightweight system tones without requiring
 * external audio files.
 */
object SoundPlayer {
    private const val VOLUME_PERCENTAGE = 80
    private const val TONE_DURATION_MS = 150

    /**
     * Plays a confirmation beep when adding attendance.
     * Uses a positive acknowledgment tone (TONE_PROP_ACK).
     */
    fun playAddAttendanceSound() {
        playTone(ToneGenerator.TONE_SUP_PIP)
    }

    /**
     * Plays a distinct beep when removing attendance.
     * Uses a negative acknowledgment tone (TONE_PROP_NACK).
     */
    fun playRemoveAttendanceSound() {
        playTone(ToneGenerator.TONE_SUP_BUSY)
    }

    private fun playTone(toneType: Int) {
        try {
            ToneGenerator(AudioManager.STREAM_SYSTEM, VOLUME_PERCENTAGE)
                .startTone(toneType, TONE_DURATION_MS)
        } catch (_: Exception) {
            // Silently fail if audio is unavailable - don't disrupt main functionality
        }
    }
}