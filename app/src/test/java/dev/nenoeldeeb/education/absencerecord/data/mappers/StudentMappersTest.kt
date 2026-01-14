package dev.nenoeldeeb.education.absencerecord.data.mappers

import dev.nenoeldeeb.education.absencerecord.data.datasources.local.entities.AttendanceHistoryItemEntity
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.entities.StudentAttendanceEntity
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.entities.StudentEntity
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentAttendance
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class StudentMappersTest {
    @Test
    fun `StudentEntity to Student mapping is correct`() {
        // Arrange
        val studentEntity = StudentEntity(id = 1, name = "John Doe")

        // Act
        val student = studentEntity.toStudent()

        // Assert
        assertEquals(1, student.id)
        assertEquals("John Doe", student.name)
    }

    @Test
    fun `StudentEntity to Student mapping with empty name`() {
        // Arrange
        val studentEntity = StudentEntity(id = 2, name = "")

        // Act
        val student = studentEntity.toStudent()

        // Assert
        assertEquals(2, student.id)
        assertEquals("", student.name)
    }

    @Test
    fun `Student to StudentEntity mapping is correct`() {
        // Arrange
        val student = Student(id = 10, name = "Jane Doe")

        // Act
        val studentEntity = student.toStudentEntity()

        // Assert
        assertEquals(10, studentEntity.id)
        assertEquals("Jane Doe", studentEntity.name)
    }

    @Test
    fun `StudentAttendanceEntity to StudentAttendance mapping is correct`() {
        // Arrange
        val entity = StudentAttendanceEntity(id = 100, studentId = 1, date = LocalDate.parse("2023-10-27"))

        // Act
        val model = entity.toStudentAttendance()

        // Assert
        assertEquals(100, model.id)
        assertEquals(1, model.studentId)
        assertEquals("2023-10-27", model.date.toString())
    }

    @Test
    fun `StudentAttendance to StudentAttendanceEntity mapping is correct`() {
        // Arrange
        val model = StudentAttendance(id = 200, studentId = 2, date = LocalDate.parse("2023-11-01"))

        // Act
        val entity = model.toStudentAttendanceEntity()

        // Assert
        assertEquals(200, entity.id)
        assertEquals(2, entity.studentId)
        assertEquals("2023-11-01", entity.date.toString())
    }

    @Test
    fun `AttendanceHistoryItemEntity to AttendanceHistoryItem mapping is correct`() {
        // Arrange
        val entity = AttendanceHistoryItemEntity(date = LocalDate.parse("2023-12-25"), studentName = "Santa")

        // Act
        val item = entity.toAttendanceHistoryItem()

        // Assert
        assertEquals("2023-12-25", item.date.toString())
        assertEquals("Santa", item.name)
    }

    @Test
    fun `AttendanceHistoryItemEntity to AttendanceHistoryItem mapping with special characters`() {
        // Arrange
        val entity = AttendanceHistoryItemEntity(date = LocalDate.parse("2023-01-01"), studentName = "O'Connor")

        // Act
        val item = entity.toAttendanceHistoryItem()

        // Assert
        assertEquals("O'Connor", item.name)
    }
}