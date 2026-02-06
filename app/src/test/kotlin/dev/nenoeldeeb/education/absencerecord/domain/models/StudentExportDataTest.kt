package dev.nenoeldeeb.education.absencerecord.domain.models

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@DisplayName("StudentExportData Tests")
class StudentExportDataTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    @Test
    fun `should create StudentExportData with name and dates`() {
        // Arrange & Act
        val exportData =
            StudentExportData(name = "John Doe", dates = listOf("2024-12-23", "2024-12-24"))

        // Assert
        assertEquals("John Doe", exportData.name)
        assertEquals(listOf("2024-12-23", "2024-12-24"), exportData.dates)
    }

    @Test
    fun `should handle empty dates list`() {
        // Arrange & Act
        val exportData = StudentExportData(name = "John Doe", dates = emptyList())

        // Assert
        assertTrue(exportData.dates.isEmpty())
    }

    @Test
    fun `should serialize to JSON correctly`() {
        // Arrange
        val exportData =
            StudentExportData(name = "John Doe", dates = listOf("2024-12-23", "2024-12-24"))

        // Act
        val jsonString = json.encodeToString(StudentExportData.serializer(), exportData)

        // Assert
        assertTrue(jsonString.contains("\"name\""))
        assertTrue(jsonString.contains("\"John Doe\""))
        assertTrue(jsonString.contains("\"dates\""))
        assertTrue(jsonString.contains("2024-12-23"))
        assertTrue(jsonString.contains("2024-12-24"))
    }

    @Test
    fun `should deserialize from JSON correctly`() {
        // Arrange
        val jsonString = """{"name":"Jane Smith","dates":["2024-01-01","2024-01-02"]}"""

        // Act
        val exportData = json.decodeFromString(StudentExportData.serializer(), jsonString)

        // Assert
        assertEquals("Jane Smith", exportData.name)
        assertEquals(listOf("2024-01-01", "2024-01-02"), exportData.dates)
    }

    @Test
    fun `should deserialize with empty dates list`() {
        // Arrange
        val jsonString = """{"name":"John Doe","dates":[]}"""

        // Act
        val exportData = json.decodeFromString(StudentExportData.serializer(), jsonString)

        // Assert
        assertEquals("John Doe", exportData.name)
        assertTrue(exportData.dates.isEmpty())
    }

    @Test
    fun `should handle special characters in name during serialization`() {
        // Arrange
        val exportData =
            StudentExportData(name = "José María O'Connor", dates = listOf("2024-12-23"))

        // Act
        val jsonString = json.encodeToString(StudentExportData.serializer(), exportData)
        val deserialized = json.decodeFromString(StudentExportData.serializer(), jsonString)

        // Assert
        assertEquals("José María O'Connor", deserialized.name)
    }

    @Test
    fun `should handle Arabic characters in name during serialization`() {
        // Arrange
        val exportData = StudentExportData(name = "محمد أحمد", dates = listOf("2024-12-23"))

        // Act
        val jsonString = json.encodeToString(StudentExportData.serializer(), exportData)
        val deserialized = json.decodeFromString(StudentExportData.serializer(), jsonString)

        // Assert
        assertEquals("محمد أحمد", deserialized.name)
    }

    @Test
    fun `should support equality comparison`() {
        // Arrange
        val data1 = StudentExportData(name = "John", dates = listOf("2024-12-23"))
        val data2 = StudentExportData(name = "John", dates = listOf("2024-12-23"))
        val data3 = StudentExportData(name = "Jane", dates = listOf("2024-12-23"))
        val data4 = StudentExportData(name = "John", dates = listOf("2024-12-24"))

        // Assert
        assertEquals(data2, data1)
        assertNotEquals(data3, data1)
        assertNotEquals(data4, data1)
        assertEquals(data2.hashCode(), data1.hashCode())
    }

    @Test
    fun `should support copy functionality`() {
        // Arrange
        val original = StudentExportData(name = "John", dates = listOf("2024-12-23"))

        // Act
        val copiedWithNewName = original.copy(name = "Jane")
        val copiedWithNewDates = original.copy(dates = listOf("2024-12-24", "2024-12-25"))

        // Assert
        assertEquals("Jane", copiedWithNewName.name)
        assertEquals(listOf("2024-12-23"), copiedWithNewName.dates)
        assertEquals("John", copiedWithNewDates.name)
        assertEquals(listOf("2024-12-24", "2024-12-25"), copiedWithNewDates.dates)
    }

    @Test
    fun `should handle many dates`() {
        // Arrange
        val manyDates = (1..100).map { "2024-12-${it.toString().padStart(2, '0')}" }
        val exportData = StudentExportData(name = "John Doe", dates = manyDates)

        // Act
        val jsonString = json.encodeToString(StudentExportData.serializer(), exportData)
        val deserialized = json.decodeFromString(StudentExportData.serializer(), jsonString)

        // Assert
        assertEquals(100, deserialized.dates.size)
        assertEquals(manyDates, deserialized.dates)
    }

    @Test
    fun `should preserve date format in serialization`() {
        // Arrange
        val exportData =
            StudentExportData(name = "Test", dates = listOf("2024-01-01", "2024-12-31"))

        // Act
        val jsonString = json.encodeToString(StudentExportData.serializer(), exportData)

        // Assert
        assertTrue(jsonString.contains("2024-01-01"))
        assertTrue(jsonString.contains("2024-12-31"))
    }
}