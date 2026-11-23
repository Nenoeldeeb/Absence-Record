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
    private const val MIN_INTERVAL_MS = 150L

    @Volatile
    private var lastPlayedTimestampMs: Long = 0L

    fun playAddAttendanceSound(context: Context) {
        if (!shouldPlay(context)) return
        playTone(ToneGenerator.TONE_PROP_BEEP)
    }

    fun playRemoveAttendanceSound(context: Context) {
        if (!shouldPlay(context)) return
        playTone(ToneGenerator.TONE_PROP_NACK)
    }

    private fun shouldPlay(context: Context): Boolean {
        val now = System.currentTimeMillis()
        return now - lastPlayedTimestampMs >= MIN_INTERVAL_MS &&
            try {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                val isNormal = audioManager?.ringerMode == AudioManager.RINGER_MODE_NORMAL
                if (isNormal) {
                    lastPlayedTimestampMs = now
                }
                isNormal
            } catch (_: Exception) {
                lastPlayedTimestampMs = now
                true
            }
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