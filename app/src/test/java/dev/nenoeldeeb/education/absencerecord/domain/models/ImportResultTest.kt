package dev.nenoeldeeb.education.absencerecord.domain.models

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@DisplayName("ImportResult Tests")
class ImportResultTest {
    @Test
    fun `should create ImportResult with all properties`() {
        // Arrange & Act
        val result =
            ImportResult(
                newStudentsCount = 5,
                existingStudentsMergedCount = 3,
                datesProcessedCount = 20,
                datesSkippedCount = 2
            )

        // Assert
        assertEquals(5, result.newStudentsCount)
        assertEquals(3, result.existingStudentsMergedCount)
        assertEquals(20, result.datesProcessedCount)
        assertEquals(2, result.datesSkippedCount)
    }

    @Test
    fun `should handle zero values`() {
        // Arrange & Act
        val result =
            ImportResult(
                newStudentsCount = 0,
                existingStudentsMergedCount = 0,
                datesProcessedCount = 0,
                datesSkippedCount = 0
            )

        // Assert
        assertEquals(0, result.newStudentsCount)
        assertEquals(0, result.existingStudentsMergedCount)
        assertEquals(0, result.datesProcessedCount)
        assertEquals(0, result.datesSkippedCount)
    }

    @Test
    fun `should handle large numbers`() {
        // Arrange & Act
        val result =
            ImportResult(
                newStudentsCount = 10_000,
                existingStudentsMergedCount = 5_000,
                datesProcessedCount = 100_000,
                datesSkippedCount = 500
            )

        // Assert
        assertEquals(10_000, result.newStudentsCount)
        assertEquals(5_000, result.existingStudentsMergedCount)
        assertEquals(100_000, result.datesProcessedCount)
        assertEquals(500, result.datesSkippedCount)
    }

    @Test
    fun `should support equality comparison`() {
        // Arrange
        val result1 =
            ImportResult(
                newStudentsCount = 5,
                existingStudentsMergedCount = 3,
                datesProcessedCount = 20,
                datesSkippedCount = 2
            )
        val result2 =
            ImportResult(
                newStudentsCount = 5,
                existingStudentsMergedCount = 3,
                datesProcessedCount = 20,
                datesSkippedCount = 2
            )
        val result3 =
            ImportResult(
                newStudentsCount = 10,
                existingStudentsMergedCount = 3,
                datesProcessedCount = 20,
                datesSkippedCount = 2
            )

        // Assert
        assertEquals(result2, result1)
        assertNotEquals(result3, result1)
        assertEquals(result2.hashCode(), result1.hashCode())
    }

    @Test
    fun `should support copy functionality`() {
        // Arrange
        val original =
            ImportResult(
                newStudentsCount = 5,
                existingStudentsMergedCount = 3,
                datesProcessedCount = 20,
                datesSkippedCount = 2
            )

        // Act
        val copied = original.copy(newStudentsCount = 10)

        // Assert
        assertEquals(10, copied.newStudentsCount)
        assertEquals(3, copied.existingStudentsMergedCount)
        assertEquals(20, copied.datesProcessedCount)
        assertEquals(2, copied.datesSkippedCount)
        assertNotEquals(original, copied)
    }

    @Test
    fun `should calculate total students processed`() {
        // Arrange
        val result =
            ImportResult(
                newStudentsCount = 5,
                existingStudentsMergedCount = 3,
                datesProcessedCount = 20,
                datesSkippedCount = 2
            )

        // Act
        val totalStudents = result.newStudentsCount + result.existingStudentsMergedCount

        // Assert
        assertEquals(8, totalStudents)
    }

    @Test
    fun `should calculate total dates encountered`() {
        // Arrange
        val result =
            ImportResult(
                newStudentsCount = 5,
                existingStudentsMergedCount = 3,
                datesProcessedCount = 20,
                datesSkippedCount = 2
            )

        // Act
        val totalDates = result.datesProcessedCount + result.datesSkippedCount

        // Assert
        assertEquals(22, totalDates)
    }
}