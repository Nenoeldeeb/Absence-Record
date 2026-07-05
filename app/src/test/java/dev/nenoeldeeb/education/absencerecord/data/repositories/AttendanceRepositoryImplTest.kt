package dev.nenoeldeeb.education.absencerecord.data.repositories

import dev.nenoeldeeb.education.absencerecord.data.datasources.local.daos.AttendanceDao
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.entities.StudentAttendanceEntity
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentAttendance
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AttendanceRepositoryImplTest {
    private val attendanceDao: AttendanceDao = mockk()
    private val repository = AttendanceRepositoryImpl(attendanceDao)

    @Test
    fun `getAttendanceForDate returns mapped attendance`() =
        runTest {
            // Arrange
            val entity = StudentAttendanceEntity(id = 1, studentId = 1, date = LocalDate.parse("2023-10-27"))
            every { attendanceDao.getAttendanceForDate(any()) } returns flowOf(listOf(entity))

            // Act
            val result =
                repository.getAttendanceForDate(LocalDate(2023, 10, 27)).first()

            // Assert
            assertTrue(result.isSuccess)
            val list = result.getOrNull()
            assertEquals(1, list?.size)
            assertEquals(1, list?.first()?.id)
            assertEquals("2023-10-27", list?.first()?.date.toString())
        }

    @Test
    fun `getAttendanceForDate returns failure on error`() =
        runTest {
            // Arrange
            val exception = RuntimeException("DB Error")
            val errorFlow =
                kotlinx.coroutines.flow.flow<List<StudentAttendanceEntity>> { throw exception }
            every { attendanceDao.getAttendanceForDate(any()) } returns errorFlow

            // Act
            val result =
                repository.getAttendanceForDate(LocalDate(2023, 10, 27)).first()

            // Assert
            assertTrue(result.isFailure)
            val error = result.exceptionOrNull()
            assertEquals(StudentError.Database, error)
        }

    @Test
    fun `insertAttendance calls dao success`() =
        runTest {
            // Arrange
            val attendance = StudentAttendance(studentId = 1, date = LocalDate.parse("2023-10-27"))
            coEvery { attendanceDao.insertAttendance(any()) } returns 1L

            // Act
            val result = repository.insertAttendance(attendance)

            // Assert
            assertTrue(result.isSuccess)
            coVerify {
                attendanceDao.insertAttendance(match { it.studentId == 1 && it.date.toString() == "2023-10-27" })
            }
        }

    @Test
    fun `deleteAttendance calls dao success`() =
        runTest {
            // Arrange
            coEvery { attendanceDao.deleteAttendance(any(), any()) } returns Unit

            // Act
            val result = repository.deleteAttendance(1, LocalDate(2023, 10, 27))

            // Assert
            assertTrue(result.isSuccess)
            coVerify {
                attendanceDao.deleteAttendance(
                    1,
                    match { it == LocalDate(2023, 10, 27) }
                )
            }
        }

    @Test
    fun `insertAttendance returns StudentError_Database on failure`() =
        runTest {
            // Arrange
            val attendance = StudentAttendance(studentId = 1, date = LocalDate.parse("2023-10-27"))
            coEvery { attendanceDao.insertAttendance(any()) } throws RuntimeException("DB Error")

            // Act
            val result = repository.insertAttendance(attendance)

            // Assert
            assertTrue(result.isFailure)
            assertEquals(StudentError.Database, result.exceptionOrNull())
        }

    @Test
    fun `deleteAttendance returns StudentError_Database on failure`() =
        runTest {
            // Arrange
            coEvery { attendanceDao.deleteAttendance(any(), any()) } throws RuntimeException("DB Error")

            // Act
            val result = repository.deleteAttendance(1, LocalDate(2023, 10, 27))

            // Assert
            assertTrue(result.isFailure)
            assertEquals(StudentError.Database, result.exceptionOrNull())
        }
}