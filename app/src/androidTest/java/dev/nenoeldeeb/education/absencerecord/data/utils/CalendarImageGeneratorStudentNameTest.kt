package dev.nenoeldeeb.education.absencerecord.data.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CalendarImageGeneratorStudentNameTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    // region Student name tests

    @Test
    fun generateCalendarBitmap_handlesLongStudentName() {
        val month = LocalDate(2023, 10, 1)
        val longName = "A Very Long Student Name That Might Overflow"

        val bitmap =
            CalendarImageGenerator.generateCalendarBitmap(
                context = context,
                month = month,
                studentName = longName,
                markedDays = setOf(1)
            )

        assertNotNull("Should handle long names", bitmap)
    }

    @Test
    fun generateCalendarBitmap_handlesArabicName() {
        val month = LocalDate(2023, 10, 1)
        val arabicName = "محمد أحمد علي"

        val bitmap =
            CalendarImageGenerator.generateCalendarBitmap(
                context = context,
                month = month,
                studentName = arabicName,
                markedDays = setOf(1)
            )

        assertNotNull("Should handle Arabic names", bitmap)
    }

    @Test
    fun generateCalendarBitmap_handlesEmptyStudentName() {
        val month = LocalDate(2023, 10, 1)

        val bitmap =
            CalendarImageGenerator.generateCalendarBitmap(
                context = context,
                month = month,
                studentName = "",
                markedDays = setOf(1)
            )

        assertNotNull("Should handle empty name", bitmap)
    }

    @Test
    fun generateCalendarBitmap_handlesSpecialCharacters() {
        val month = LocalDate(2023, 10, 1)
        val specialName = "Student (Test) #123"

        val bitmap =
            CalendarImageGenerator.generateCalendarBitmap(
                context = context,
                month = month,
                studentName = specialName,
                markedDays = setOf(1)
            )

        assertNotNull("Should handle special characters", bitmap)
    }

    // endregion
}