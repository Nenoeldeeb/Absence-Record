package dev.nenoeldeeb.education.absencerecord.data.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CalendarImageGeneratorMonthHandlingTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    // region Month handling tests

    @Test
    fun generateCalendarBitmap_handlesJanuary31Days() {
        val month = LocalDate(2023, 1, 1)
        val markedDays = setOf(1, 31)

        val bitmap =
            CalendarImageGenerator.generateCalendarBitmap(
                context = context,
                month = month,
                studentName = "January Test",
                markedDays = markedDays
            )

        assertNotNull(bitmap)
        assertEquals(1080, bitmap.width)
        assertEquals(1920, bitmap.height)
    }

    @Test
    fun generateCalendarBitmap_handlesFebruary28Days() {
        val month = LocalDate(2023, 2, 1)
        val markedDays = setOf(1, 28)

        val bitmap =
            CalendarImageGenerator.generateCalendarBitmap(
                context = context,
                month = month,
                studentName = "February Test",
                markedDays = markedDays
            )

        assertNotNull(bitmap)
    }

    @Test
    fun generateCalendarBitmap_handlesLeapYearFebruary29Days() {
        val month = LocalDate(2024, 2, 1)
        val markedDays = setOf(29)

        val bitmap =
            CalendarImageGenerator.generateCalendarBitmap(
                context = context,
                month = month,
                studentName = "Leap Year Test",
                markedDays = markedDays
            )

        assertNotNull(bitmap)
    }

    @Test
    fun generateCalendarBitmap_handlesApril30Days() {
        val month = LocalDate(2023, 4, 1)
        val markedDays = setOf(30)

        val bitmap =
            CalendarImageGenerator.generateCalendarBitmap(
                context = context,
                month = month,
                studentName = "April Test",
                markedDays = markedDays
            )

        assertNotNull(bitmap)
    }

    @Test
    fun generateCalendarBitmap_handlesYearBoundary() {
        val december = LocalDate(2023, 12, 1)
        val january = LocalDate(2024, 1, 1)

        val decBitmap =
            CalendarImageGenerator.generateCalendarBitmap(
                context = context,
                month = december,
                studentName = "Year Boundary",
                markedDays = setOf(31)
            )
        val janBitmap =
            CalendarImageGenerator.generateCalendarBitmap(
                context = context,
                month = january,
                studentName = "Year Boundary",
                markedDays = setOf(1)
            )

        assertNotNull(decBitmap)
        assertNotNull(janBitmap)
    }

    // endregion
}
