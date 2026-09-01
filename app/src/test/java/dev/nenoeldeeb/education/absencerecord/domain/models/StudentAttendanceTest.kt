package dev.nenoeldeeb.education.absencerecord.domain.models

import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@DisplayName("StudentAttendance Tests")
class StudentAttendanceTest {
    @Test
    fun `should create StudentAttendance with default id`() {
        // Arrange
        val date = LocalDate(2024, 12, 23)

        // Act
        val attendance = StudentAttendance(studentId = 42, date = date)

        // Assert
        assertEquals(0, attendance.id)
        assertEquals(42, attendance.studentId)
        assertEquals(date, attendance.date)
    }

    @Test
    fun `should create StudentAttendance with explicit id`() {
        // Arrange
        val date = LocalDate(2024, 12, 23)

        // Act
        val attendance = StudentAttendance(id = 100, studentId = 42, date = date)

        // Assert
        assertEquals(100, attendance.id)
        assertEquals(42, attendance.studentId)
        assertEquals(date, attendance.date)
    }

    @Test
    fun `should handle different date formats`() {
        // Arrange & Act
        val attendance1 = StudentAttendance(studentId = 1, date = LocalDate(2024, 1, 1))
        val attendance2 = StudentAttendance(studentId = 1, date = LocalDate(2024, 12, 31))
        val attendance3 =
            StudentAttendance(studentId = 1, date = LocalDate(2000, 2, 29)) // Leap year

        // Assert
        assertEquals("2024-01-01", attendance1.date.toString())
        assertEquals("2024-12-31", attendance2.date.toString())
        assertEquals("2000-02-29", attendance3.date.toString())
    }

    @Test
    fun `should support equality comparison`() {
        // Arrange
        val date = LocalDate(2024, 12, 23)
        val attendance1 = StudentAttendance(id = 1, studentId = 42, date = date)
        val attendance2 = StudentAttendance(id = 1, studentId = 42, date = date)
        val attendance3 = StudentAttendance(id = 2, studentId = 42, date = date)
        val attendance4 = StudentAttendance(id = 1, studentId = 43, date = date)
        val attendance5 = StudentAttendance(id = 1, studentId = 42, date = LocalDate(2024, 12, 24))

        // Assert
        assertEquals(attendance2, attendance1)
        assertNotEquals(attendance3, attendance1)
        assertNotEquals(attendance4, attendance1)
        assertNotEquals(attendance5, attendance1)
        assertEquals(attendance2.hashCode(), attendance1.hashCode())
    }

    @Test
    fun `should support copy functionality`() {
        // Arrange
        val original = StudentAttendance(id = 1, studentId = 42, date = LocalDate(2024, 12, 23))

        // Act
        val copiedWithNewDate = original.copy(date = LocalDate(2024, 12, 24))
        val copiedWithNewStudentId = original.copy(studentId = 100)

        // Assert
        assertEquals(LocalDate(2024, 12, 24), copiedWithNewDate.date)
        assertEquals(42, copiedWithNewDate.studentId)
        assertEquals(100, copiedWithNewStudentId.studentId)
        assertEquals(LocalDate(2024, 12, 23), copiedWithNewStudentId.date)
    }

    @Test
    fun `should handle past dates`() {
        // Arrange & Act
        val attendance = StudentAttendance(studentId = 1, date = LocalDate(1990, 1, 1))

        // Assert
        assertEquals(1990, attendance.date.year)
    }

    @Test
    fun `should handle future dates`() {
        // Arrange & Act
        val attendance = StudentAttendance(studentId = 1, date = LocalDate(2050, 12, 31))

        // Assert
        assertEquals(2050, attendance.date.year)
    }

    @Test
    fun `should handle same student with multiple dates`() {
        // Arrange
        val studentId = 42
        val attendance1 = StudentAttendance(studentId = studentId, date = LocalDate(2024, 12, 20))
        val attendance2 = StudentAttendance(studentId = studentId, date = LocalDate(2024, 12, 21))
        val attendance3 = StudentAttendance(studentId = studentId, date = LocalDate(2024, 12, 22))

        // Assert
        assertEquals(studentId, attendance1.studentId)
        assertEquals(studentId, attendance2.studentId)
        assertEquals(studentId, attendance3.studentId)
        val list = listOf(attendance1, attendance2, attendance3)
        assertEquals(3, list.size)
        assertTrue(list.all { it.studentId == studentId })
    }

    @Test
    fun `should handle multiple students on same date`() {
        // Arrange
        val date = LocalDate(2024, 12, 23)
        val attendance1 = StudentAttendance(studentId = 1, date = date)
        val attendance2 = StudentAttendance(studentId = 2, date = date)
        val attendance3 = StudentAttendance(studentId = 3, date = date)

        // Assert
        assertEquals(date, attendance1.date)
        assertEquals(date, attendance2.date)
        assertEquals(date, attendance3.date)
        val list = listOf(attendance1, attendance2, attendance3)
        assertEquals(3, list.size)
        assertTrue(list.all { it.date == date })
    }
}