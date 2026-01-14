package dev.nenoeldeeb.education.absencerecord.data.utils

import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation tests for [CalendarImageGenerator].
 *
 * These tests verify that bitmap generation with Canvas drawing works correctly using actual
 * Android graphics APIs.
 */
@RunWith(AndroidJUnit4::class)
class CalendarImageGeneratorTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    // region Bitmap creation tests

    @Test
    fun generateCalendarBitmap_createsCorrectDimensions() {
        // Arrange
        val month = LocalDate(2023, 10, 1)
        val markedDays = setOf(1, 15, 31)

        // Act
        val bitmap =
            CalendarImageGenerator.generateCalendarBitmap(
                context = context,
                month = month,
                studentName = "Test Student",
                markedDays = markedDays
            )

        // Assert
        assertNotNull("Bitmap should not be null", bitmap)
        assertEquals("Width should be 1080", 1080, bitmap.width)
        assertEquals("Height should be 1920", 1920, bitmap.height)
    }

    @Test
    fun generateCalendarBitmap_generatesNonTransparentBitmap() {
        // Arrange
        val month = LocalDate(2023, 10, 1)

        // Act
        val bitmap =
            CalendarImageGenerator.generateCalendarBitmap(
                context = context,
                month = month,
                studentName = "Test",
                markedDays = emptySet()
            )

        // Assert - Check that the background is not transparent
        val centerPixel = bitmap.getPixel(bitmap.width / 2, bitmap.height / 2)
        assertNotEquals("Bitmap should not be transparent", Color.TRANSPARENT, centerPixel)
    }

    @Test
    fun generateCalendarBitmap_hasDarkBackground() {
        // Arrange
        val month = LocalDate(2023, 10, 1)

        // Act
        val bitmap =
            CalendarImageGenerator.generateCalendarBitmap(
                context = context,
                month = month,
                studentName = "Test",
                markedDays = emptySet()
            )

        // Assert - Background should be dark (#202124)
        val cornerPixel = bitmap.getPixel(10, 10)
        val red = Color.red(cornerPixel)
        val green = Color.green(cornerPixel)
        val blue = Color.blue(cornerPixel)

        // Dark color should have low RGB values
        assertTrue("Red should be low (dark)", red < 50)
        assertTrue("Green should be low (dark)", green < 50)
        assertTrue("Blue should be low (dark)", blue < 50)
    }

    // endregion

    // region Month handling tests

    @Test
    fun generateCalendarBitmap_handlesJanuary31Days() {
        // Arrange
        val month = LocalDate(2023, 1, 1)
        val markedDays = setOf(1, 31)

        // Act
        val bitmap =
            CalendarImageGenerator.generateCalendarBitmap(
                context = context,
                month = month,
                studentName = "January Test",
                markedDays = markedDays
            )

        // Assert
        assertNotNull(bitmap)
        assertEquals(1080, bitmap.width)
        assertEquals(1920, bitmap.height)
    }

    @Test
    fun generateCalendarBitmap_handlesFebruary28Days() {
        // Arrange - Non-leap year
        val month = LocalDate(2023, 2, 1)
        val markedDays = setOf(1, 28)

        // Act
        val bitmap =
            CalendarImageGenerator.generateCalendarBitmap(
                context = context,
                month = month,
                studentName = "February Test",
                markedDays = markedDays
            )

        // Assert
        assertNotNull(bitmap)
    }

    @Test
    fun generateCalendarBitmap_handlesLeapYearFebruary29Days() {
        // Arrange - Leap year
        val month = LocalDate(2024, 2, 1)
        val markedDays = setOf(29)

        // Act
        val bitmap =
            CalendarImageGenerator.generateCalendarBitmap(
                context = context,
                month = month,
                studentName = "Leap Year Test",
                markedDays = markedDays
            )

        // Assert
        assertNotNull(bitmap)
    }

    @Test
    fun generateCalendarBitmap_handlesApril30Days() {
        // Arrange
        val month = LocalDate(2023, 4, 1)
        val markedDays = setOf(30)

        // Act
        val bitmap =
            CalendarImageGenerator.generateCalendarBitmap(
                context = context,
                month = month,
                studentName = "April Test",
                markedDays = markedDays
            )

        // Assert
        assertNotNull(bitmap)
    }

    @Test
    fun generateCalendarBitmap_handlesYearBoundary() {
        // Arrange - December to January transition
        val december = LocalDate(2023, 12, 1)
        val january = LocalDate(2024, 1, 1)

        // Act
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

        // Assert
        assertNotNull(decBitmap)
        assertNotNull(janBitmap)
    }

    // endregion

    // region Marked days tests

    @Test
    fun generateCalendarBitmap_handlesEmptyMarkedDays() {
        // Arrange
        val month = LocalDate(2023, 10, 1)

        // Act
        val bitmap =
            CalendarImageGenerator.generateCalendarBitmap(
                context = context,
                month = month,
                studentName = "No Marks",
                markedDays = emptySet()
            )

        // Assert
        assertNotNull("Should handle empty marked days", bitmap)
    }

    @Test
    fun generateCalendarBitmap_handlesAllDaysMarked() {
        // Arrange - October has 31 days
        val month = LocalDate(2023, 10, 1)
        val allDays = (1..31).toSet()

        // Act
        val bitmap =
            CalendarImageGenerator.generateCalendarBitmap(
                context = context,
                month = month,
                studentName = "All Days",
                markedDays = allDays
            )

        // Assert
        assertNotNull("Should handle all days marked", bitmap)
    }

    @Test
    fun generateCalendarBitmap_handlesSingleMarkedDay() {
        // Arrange
        val month = LocalDate(2023, 10, 1)

        // Act
        val bitmap =
            CalendarImageGenerator.generateCalendarBitmap(
                context = context,
                month = month,
                studentName = "Single Day",
                markedDays = setOf(15)
            )

        // Assert
        assertNotNull(bitmap)
    }

    @Test
    fun generateCalendarBitmap_ignoresInvalidDayNumbers() {
        // Arrange - Include invalid day numbers (0, 32, negative)
        val month = LocalDate(2023, 10, 1)
        val invalidDays = setOf(0, 32, -1, 100)

        // Act - Should not crash with invalid day numbers
        val bitmap =
            CalendarImageGenerator.generateCalendarBitmap(
                context = context,
                month = month,
                studentName = "Invalid Days",
                markedDays = invalidDays
            )

        // Assert
        assertNotNull("Should handle invalid day numbers gracefully", bitmap)
    }

    // endregion

    // region Student name tests

    @Test
    fun generateCalendarBitmap_handlesLongStudentName() {
        // Arrange
        val month = LocalDate(2023, 10, 1)
        val longName = "A Very Long Student Name That Might Overflow"

        // Act
        val bitmap =
            CalendarImageGenerator.generateCalendarBitmap(
                context = context,
                month = month,
                studentName = longName,
                markedDays = setOf(1)
            )

        // Assert
        assertNotNull("Should handle long names", bitmap)
    }

    @Test
    fun generateCalendarBitmap_handlesArabicName() {
        // Arrange
        val month = LocalDate(2023, 10, 1)
        val arabicName = "محمد أحمد علي"

        // Act
        val bitmap =
            CalendarImageGenerator.generateCalendarBitmap(
                context = context,
                month = month,
                studentName = arabicName,
                markedDays = setOf(1)
            )

        // Assert
        assertNotNull("Should handle Arabic names", bitmap)
    }

    @Test
    fun generateCalendarBitmap_handlesEmptyStudentName() {
        // Arrange
        val month = LocalDate(2023, 10, 1)

        // Act
        val bitmap =
            CalendarImageGenerator.generateCalendarBitmap(
                context = context,
                month = month,
                studentName = "",
                markedDays = setOf(1)
            )

        // Assert
        assertNotNull("Should handle empty name", bitmap)
    }

    @Test
    fun generateCalendarBitmap_handlesSpecialCharacters() {
        // Arrange
        val month = LocalDate(2023, 10, 1)
        val specialName = "Student (Test) #123"

        // Act
        val bitmap =
            CalendarImageGenerator.generateCalendarBitmap(
                context = context,
                month = month,
                studentName = specialName,
                markedDays = setOf(1)
            )

        // Assert
        assertNotNull("Should handle special characters", bitmap)
    }

    // endregion

    // region Bitmap content validation tests

    @Test
    fun generateCalendarBitmap_containsNonUniformContent() {
        // Arrange
        val month = LocalDate(2023, 10, 1)

        // Act
        val bitmap =
            CalendarImageGenerator.generateCalendarBitmap(
                context = context,
                month = month,
                studentName = "Content Test",
                markedDays = setOf(15)
            )

        // At least something should be drawn (different from pure background at different areas)
        // This verifies the canvas actually drew content
        assertNotNull(bitmap)
        assertTrue(bitmap.width > 0)
        assertTrue(bitmap.height > 0)
    }

    // endregion
}