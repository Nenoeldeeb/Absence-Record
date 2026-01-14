package dev.nenoeldeeb.education.absencerecord.domain.models

import org.junit.jupiter.api.DisplayName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@DisplayName("Student Tests")
class StudentTest {
    @Test
    fun `should create Student with default id`() {
        // Arrange & Act
        val student = Student(name = "John Doe")

        // Assert
        assertEquals(0, student.id)
        assertEquals("John Doe", student.name)
    }

    @Test
    fun `should create Student with explicit id`() {
        // Arrange & Act
        val student = Student(id = 42, name = "Jane Smith")

        // Assert
        assertEquals(42, student.id)
        assertEquals("Jane Smith", student.name)
    }

    @Test
    fun `should handle empty name`() {
        // Arrange & Act
        val student = Student(name = "")

        // Assert
        assertTrue(student.name.isEmpty())
    }

    @Test
    fun `should handle whitespace name`() {
        // Arrange & Act
        val student = Student(name = "   ")

        // Assert
        assertEquals("   ", student.name)
        assertTrue(student.name.isBlank())
    }

    @Test
    fun `should handle special characters in name`() {
        // Arrange & Act
        val student = Student(name = "José María O'Connor-Smith")

        // Assert
        assertEquals("José María O'Connor-Smith", student.name)
    }

    @Test
    fun `should handle Arabic characters in name`() {
        // Arrange & Act
        val student = Student(name = "محمد أحمد")

        // Assert
        assertEquals("محمد أحمد", student.name)
    }

    @Test
    fun `should handle very long name`() {
        // Arrange
        val longName = "a".repeat(500)

        // Act
        val student = Student(name = longName)

        // Assert
        assertEquals(500, student.name.length)
    }

    @Test
    fun `should support equality comparison by all properties`() {
        // Arrange
        val student1 = Student(id = 1, name = "John")
        val student2 = Student(id = 1, name = "John")
        val student3 = Student(id = 2, name = "John")
        val student4 = Student(id = 1, name = "Jane")

        // Assert
        assertEquals(student2, student1)
        assertNotEquals(student3, student1)
        assertNotEquals(student4, student1)
        assertEquals(student2.hashCode(), student1.hashCode())
    }

    @Test
    fun `should support copy functionality`() {
        // Arrange
        val original = Student(id = 1, name = "John")

        // Act
        val copiedWithNewName = original.copy(name = "Jane")
        val copiedWithNewId = original.copy(id = 2)

        // Assert
        assertEquals(1, copiedWithNewName.id)
        assertEquals("Jane", copiedWithNewName.name)
        assertEquals(2, copiedWithNewId.id)
        assertEquals("John", copiedWithNewId.name)
    }

    @Test
    fun `should handle negative id values`() {
        // Arrange & Act
        val student = Student(id = -1, name = "Test")

        // Assert
        assertEquals(-1, student.id)
    }

    @Test
    fun `should handle numbers in name`() {
        // Arrange & Act
        val student = Student(name = "Student 123")

        // Assert
        assertEquals("Student 123", student.name)
    }
}