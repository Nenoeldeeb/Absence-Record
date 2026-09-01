package dev.nenoeldeeb.education.absencerecord.domain.models

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

@DisplayName("ParsedStudentImportData Tests")
class ParsedStudentImportDataTest {
    @Test
    fun `should create ParsedStudentImportData with generated id`() {
        // Arrange
        val exportData =
            StudentExportData(name = "John Doe", dates = listOf("2024-12-23", "2024-12-24"))

        // Act
        val parsed = ParsedStudentImportData(exportData)

        // Assert
        assertEquals(exportData, parsed.originalData)
        assertNotEquals(0, parsed.id)
    }

    @Test
    fun `should generate consistent id for same object instance`() {
        // Arrange
        val exportData = StudentExportData(name = "John Doe", dates = listOf("2024-12-23"))

        // Act
        val parsed1 = ParsedStudentImportData(exportData)
        val parsed2 = ParsedStudentImportData(exportData)

        // Assert - Same object instance should generate same identity hash code
        assertEquals(parsed2.id, parsed1.id)
    }

    @Test
    fun `should generate different ids for different object instances with same data`() {
        // Arrange
        val exportData1 = StudentExportData(name = "John Doe", dates = listOf("2024-12-23"))
        val exportData2 = StudentExportData(name = "John Doe", dates = listOf("2024-12-23"))

        // Act
        val parsed1 = ParsedStudentImportData(exportData1)
        val parsed2 = ParsedStudentImportData(exportData2)

        // Assert - Different object instances should have different identity hash codes
        assertNotEquals(parsed2.id, parsed1.id)
    }

    @Test
    fun `should handle empty dates list`() {
        // Arrange
        val exportData = StudentExportData(name = "John Doe", dates = emptyList())

        // Act
        val parsed = ParsedStudentImportData(exportData)

        // Assert
        assertTrue(parsed.originalData.dates.isEmpty())
        assertNotEquals(0, parsed.id)
    }

    @Test
    fun `should handle large dates list`() {
        // Arrange
        val dates =
            (1..365).map { day ->
                "2024-${(day / 30 + 1).toString().padStart(2, '0')}-${(day % 30 + 1).toString().padStart(2, '0')}"
            }
        val exportData = StudentExportData(name = "John Doe", dates = dates)

        // Act
        val parsed = ParsedStudentImportData(exportData)

        // Assert
        assertEquals(365, parsed.originalData.dates.size)
    }

    @Test
    fun `should support equality comparison`() {
        // Arrange
        val exportData1 = StudentExportData(name = "John", dates = listOf("2024-12-23"))
        val exportData2 = StudentExportData(name = "John", dates = listOf("2024-12-23"))
        val parsed1 = ParsedStudentImportData(exportData1)
        val parsed2 = ParsedStudentImportData(exportData1) // Same instance
        val parsed3 = ParsedStudentImportData(exportData2) // Different instance, same data

        // Assert
        assertEquals(parsed2, parsed1) // Same underlying object
        assertNotEquals(parsed3, parsed1) // Different underlying object
    }

    @Test
    fun `should support copy functionality`() {
        // Arrange
        val exportData1 = StudentExportData(name = "John", dates = listOf("2024-12-23"))
        val exportData2 = StudentExportData(name = "Jane", dates = listOf("2024-12-24"))
        val original = ParsedStudentImportData(exportData1)

        // Act
        val copied = original.copy(originalData = exportData2)

        // Assert
        assertEquals(exportData2, copied.originalData)
        assertNotEquals(original.originalData, copied.originalData)
    }

    @Test
    fun `should preserve original data reference`() {
        // Arrange
        val exportData =
            StudentExportData(name = "John Doe", dates = listOf("2024-12-23", "2024-12-24"))

        // Act
        val parsed = ParsedStudentImportData(exportData)

        // Assert
        assertSame(exportData, parsed.originalData)
        assertEquals("John Doe", parsed.originalData.name)
        assertEquals(listOf("2024-12-23", "2024-12-24"), parsed.originalData.dates)
    }
}