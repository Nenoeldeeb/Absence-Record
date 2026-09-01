package dev.nenoeldeeb.education.absencerecord.presentation.utils

import android.content.Context
import android.text.format.DateFormat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Calendar

@RunWith(AndroidJUnit4::class)
class TimeFormatterTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun expectedTime(minutes: Int): String {
        val calendar =
            Calendar.getInstance().apply {
                clear()
                set(Calendar.HOUR_OF_DAY, minutes / 60)
                set(Calendar.MINUTE, minutes % 60)
            }
        return DateFormat.getTimeFormat(context).format(calendar.time)
    }

    @Test
    fun format_540_rendersSystemLocaleHourForNineAM() {
        assertEquals(expectedTime(540), TimeFormatter.format(540, context))
    }

    @Test
    fun format_540_containsTheNineOClockDigit() {
        val formatted = TimeFormatter.format(540, context)
        assertTrue(formatted.contains("9") || formatted.contains("٩"))
    }

    @Test
    fun format_1380_rendersSystemLocaleHourForElevenPM() {
        assertEquals(expectedTime(1380), TimeFormatter.format(1380, context))
    }

    @Test
    fun format_minutesOfDayBoundaries_doNotThrow() {
        assertTrue(TimeFormatter.format(0, context).isNotBlank())
        assertTrue(TimeFormatter.format(1410, context).isNotBlank())
        assertTrue(TimeFormatter.format(1440, context).isNotBlank())
    }
}