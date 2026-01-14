package dev.nenoeldeeb.education.absencerecord.domain.models

import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.DisplayName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@DisplayName("AttendanceHistoryItem Tests")
class AttendanceHistoryItemTest {
    @Test
    fun `should create AttendanceHistoryItem with date and name`() {
        // Arrange
        val date = LocalDate(2024, 12, 23)

        // Act
        val item = AttendanceHistoryItem(date = date, name = "John Doe")

        // Assert
        assertEquals(date, item.date)
        assertEquals("John Doe", item.name)
    }

    @Test
    fun `should support equality comparison`() {
        // Arrange
        val date = LocalDate(2024, 12, 23)
        val item1 = AttendanceHistoryItem(date, "John Doe")
        val item2 = AttendanceHistoryItem(date, "John Doe")
        val item3 = AttendanceHistoryItem(LocalDate(2024, 12, 24), "John Doe")
        val item4 = AttendanceHistoryItem(date, "Jane Smith")

        // Assert
        assertEquals(item1, item2)
        assertNotEquals(item1, item3)
        assertNotEquals(item1, item4)
        assertEquals(item1.hashCode(), item2.hashCode())
    }

    @Test
    fun `should support copy functionality`() {
        // Arrange
        val original = AttendanceHistoryItem(date = LocalDate(2024, 12, 23), name = "John Doe")

        // Act
        val copiedWithNewDate = original.copy(date = LocalDate(2024, 12, 24))
        val copiedWithNewName = original.copy(name = "Jane Smith")

        // Assert
        assertEquals(LocalDate(2024, 12, 24), copiedWithNewDate.date)
        assertEquals("John Doe", copiedWithNewDate.name)
        assertEquals(LocalDate(2024, 12, 23), copiedWithNewName.date)
        assertEquals("Jane Smith", copiedWithNewName.name)
    }

    @Test
    fun `should handle special characters in name`() {
        // Arrange & Act
        val item =
            AttendanceHistoryItem(
                date = LocalDate(2024, 12, 23),
                name = "José María O'Connor-Smith"
            )

        // Assert
        assertEquals("José María O'Connor-Smith", item.name)
    }

    @Test
    fun `should handle Arabic characters in name`() {
        // Arrange & Act
        val item = AttendanceHistoryItem(date = LocalDate(2024, 12, 23), name = "محمد أحمد")

        // Assert
        assertEquals("محمد أحمد", item.name)
    }

    @Test
    fun `should sort by date naturally`() {
        // Arrange
        val item1 = AttendanceHistoryItem(LocalDate(2024, 12, 23), "Student A")
        val item2 = AttendanceHistoryItem(LocalDate(2024, 12, 21), "Student B")
        val item3 = AttendanceHistoryItem(LocalDate(2024, 12, 25), "Student C")
        val list = listOf(item1, item2, item3)

        // Act
        val sorted = list.sortedBy { it.date }

        // Assert
        assertEquals(listOf(item2, item1, item3), sorted)
    }

    @Test
    fun `should sort by name alphabetically`() {
        // Arrange
        val date = LocalDate(2024, 12, 23)
        val item1 = AttendanceHistoryItem(date, "Charlie")
        val item2 = AttendanceHistoryItem(date, "Alice")
        val item3 = AttendanceHistoryItem(date, "Bob")
        val list = listOf(item1, item2, item3)

        // Act
        val sorted = list.sortedBy { it.name }

        // Assert
        assertEquals(listOf(item2, item3, item1), sorted)
    }

    @Test
    fun `should group by date`() {
        // Arrange
        val date1 = LocalDate(2024, 12, 23)
        val date2 = LocalDate(2024, 12, 24)
        val items =
            listOf(
                AttendanceHistoryItem(date1, "Alice"),
                AttendanceHistoryItem(date2, "Bob"),
                AttendanceHistoryItem(date1, "Charlie"),
                AttendanceHistoryItem(date2, "David")
            )

        // Act
        val grouped = items.groupBy { it.date }

        // Assert
        assertEquals(2, grouped.size)
        assertEquals(2, grouped[date1]?.size)
        assertEquals(2, grouped[date2]?.size)
    }

    @Test
    fun `should handle empty name`() {
        // Arrange & Act
        val item = AttendanceHistoryItem(date = LocalDate(2024, 12, 23), name = "")

        // Assert
        assertTrue(item.name.isEmpty())
    }

    @Test
    fun `should handle past dates`() {
        // Arrange & Act
        val item = AttendanceHistoryItem(date = LocalDate(1990, 1, 1), name = "Historical Student")

        // Assert
        assertEquals(1990, item.date.year)
    }

    @Test
    fun `should handle future dates`() {
        // Arrange & Act
        val item = AttendanceHistoryItem(date = LocalDate(2050, 12, 31), name = "Future Student")

        // Assert
        assertEquals(2050, item.date.year)
    }
}