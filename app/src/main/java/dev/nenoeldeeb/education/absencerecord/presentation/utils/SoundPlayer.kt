package dev.nenoeldeeb.education.absencerecord.presentation.utils

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper

/**
 * Utility object for playing audio feedback sounds for attendance actions.
 * Uses Android's ToneGenerator for lightweight system tones without requiring
 * external audio files.
 */
object SoundPlayer {
    private const val VOLUME_PERCENTAGE = 80
    private const val TONE_DURATION_MS = 150
    private const val RELEASE_DELAY_MS = 250L

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

    fun playAddAttendanceSound(context: Context) {
        if (!shouldPlay(context)) return
        playTone(ToneGenerator.TONE_SUP_PIP)
    }

    fun playRemoveAttendanceSound(context: Context) {
        if (!shouldPlay(context)) return
        playTone(ToneGenerator.TONE_SUP_BUSY)
    }

    private fun shouldPlay(context: Context): Boolean =
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.ringerMode == AudioManager.RINGER_MODE_NORMAL
        } catch (_: Exception) {
            true
        }

    private fun playTone(toneType: Int) {
        try {
            val toneGenerator = ToneGenerator(AudioManager.STREAM_SYSTEM, VOLUME_PERCENTAGE)
            try {
                toneGenerator.startTone(toneType, TONE_DURATION_MS)
            } catch (_: Exception) {
                // Tone failed to start; still release below.
            }
            try {
                Handler(Looper.getMainLooper()).postDelayed(
                    {
                        try {
                            toneGenerator.release()
                        } catch (_: Exception) {
                            // Best effort release.
                        }
                    },
                    RELEASE_DELAY_MS
                )
            } catch (_: Exception) {
                try {
                    toneGenerator.release()
                } catch (_: Exception) {
                    // Best effort release.
                }
            }
        } catch (_: Exception) {
            // Silently fail if audio is unavailable - don't disrupt main functionality
        }
    }
}