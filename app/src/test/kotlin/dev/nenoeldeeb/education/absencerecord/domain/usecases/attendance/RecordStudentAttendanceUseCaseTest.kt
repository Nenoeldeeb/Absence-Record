package dev.nenoeldeeb.education.absencerecord.domain.usecases.attendance

import dev.nenoeldeeb.education.absencerecord.domain.models.StudentAttendance
import dev.nenoeldeeb.education.absencerecord.domain.repositories.AttendanceRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@DisplayName("RecordStudentAttendanceUseCase Tests")
class RecordStudentAttendanceUseCaseTest {
    private lateinit var repository: AttendanceRepository
    private lateinit var useCase: RecordStudentAttendanceUseCase

    @BeforeEach
    fun setup() {
        repository = mockk()
        useCase = RecordStudentAttendanceUseCase(repository)
    }

    @Test
    fun `should successfully record attendance`() =
        runTest {
            // Arrange
            val attendance = StudentAttendance(studentId = 1, date = LocalDate(2024, 12, 23))
            coEvery { repository.insertAttendance(attendance) } returns Result.success(1L)

            // Act
            val result = useCase(attendance)

            // Assert
            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { repository.insertAttendance(attendance) }
        }

    @Test
    fun `should handle repository failure`() =
        runTest {
            // Arrange
            val attendance = StudentAttendance(studentId = 1, date = LocalDate(2024, 12, 23))
            val exception = Exception("Insert failed")
            coEvery { repository.insertAttendance(attendance) } returns Result.failure(exception)

            // Act
            val result = useCase(attendance)

            // Assert
            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
            coVerify(exactly = 1) { repository.insertAttendance(attendance) }
        }

    @Test
    fun `should record attendance for different students on same date`() =
        runTest {
            // Arrange
            val date = LocalDate(2024, 12, 23)
            val attendance1 = StudentAttendance(studentId = 1, date = date)
            val attendance2 = StudentAttendance(studentId = 2, date = date)
            val attendance3 = StudentAttendance(studentId = 3, date = date)

            coEvery { repository.insertAttendance(attendance1) } returns Result.success(1L)
            coEvery { repository.insertAttendance(attendance2) } returns Result.success(1L)
            coEvery { repository.insertAttendance(attendance3) } returns Result.success(1L)

            // Act
            val result1 = useCase(attendance1)
            val result2 = useCase(attendance2)
            val result3 = useCase(attendance3)

            // Assert
            assertTrue(result1.isSuccess)
            assertTrue(result2.isSuccess)
            assertTrue(result3.isSuccess)
            coVerify(exactly = 1) { repository.insertAttendance(attendance1) }
            coVerify(exactly = 1) { repository.insertAttendance(attendance2) }
            coVerify(exactly = 1) { repository.insertAttendance(attendance3) }
        }

    @Test
    fun `should record attendance for same student on different dates`() =
        runTest {
            // Arrange
            val studentId = 1
            val attendance1 = StudentAttendance(studentId = studentId, date = LocalDate(2024, 12, 20))
            val attendance2 = StudentAttendance(studentId = studentId, date = LocalDate(2024, 12, 21))
            val attendance3 = StudentAttendance(studentId = studentId, date = LocalDate(2024, 12, 22))

            coEvery { repository.insertAttendance(attendance1) } returns Result.success(1L)
            coEvery { repository.insertAttendance(attendance2) } returns Result.success(1L)
            coEvery { repository.insertAttendance(attendance3) } returns Result.success(1L)

            // Act
            val result1 = useCase(attendance1)
            val result2 = useCase(attendance2)
            val result3 = useCase(attendance3)

            // Assert
            assertTrue(result1.isSuccess)
            assertTrue(result2.isSuccess)
            assertTrue(result3.isSuccess)
        }

    @Test
    fun `should handle duplicate attendance record`() =
        runTest {
            // Arrange
            val attendance = StudentAttendance(studentId = 1, date = LocalDate(2024, 12, 23))
            val exception = IllegalStateException("Duplicate attendance record")
            coEvery { repository.insertAttendance(attendance) } returns Result.failure(exception)

            // Act
            val result = useCase(attendance)

            // Assert
            assertTrue(result.isFailure)
            assertIs<IllegalStateException>(result.exceptionOrNull())
        }

    @Test
    fun `should handle invalid student id`() =
        runTest {
            // Arrange
            val attendance = StudentAttendance(studentId = -1, date = LocalDate(2024, 12, 23))
            val exception = IllegalArgumentException("Invalid student ID")
            coEvery { repository.insertAttendance(attendance) } returns Result.failure(exception)

            // Act
            val result = useCase(attendance)

            // Assert
            assertTrue(result.isFailure)
            assertIs<IllegalArgumentException>(result.exceptionOrNull())
        }

    @Test
    fun `should record attendance with explicit id`() =
        runTest {
            // Arrange
            val attendance = StudentAttendance(id = 100, studentId = 1, date = LocalDate(2024, 12, 23))
            coEvery { repository.insertAttendance(attendance) } returns Result.success(1L)

            // Act
            val result = useCase(attendance)

            // Assert
            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { repository.insertAttendance(attendance) }
        }

    @Test
    fun `should handle past dates`() =
        runTest {
            // Arrange
            val attendance = StudentAttendance(studentId = 1, date = LocalDate(2020, 1, 1))
            coEvery { repository.insertAttendance(attendance) } returns Result.success(1L)

            // Act
            val result = useCase(attendance)

            // Assert
            assertTrue(result.isSuccess)
        }

    @Test
    fun `should handle future dates`() =
        runTest {
            // Arrange
            val attendance = StudentAttendance(studentId = 1, date = LocalDate(2030, 12, 31))
            coEvery { repository.insertAttendance(attendance) } returns Result.success(1L)

            // Act
            val result = useCase(attendance)

            // Assert
            assertTrue(result.isSuccess)
        }

    @Test
    fun `should handle database constraint violation`() =
        runTest {
            // Arrange
            val attendance = StudentAttendance(studentId = 999, date = LocalDate(2024, 12, 23))
            val exception = IllegalStateException("Foreign key constraint violation")
            coEvery { repository.insertAttendance(attendance) } returns Result.failure(exception)

            // Act
            val result = useCase(attendance)

            // Assert
            assertTrue(result.isFailure)
            assertEquals(result.exceptionOrNull()?.message?.contains("constraint"), true)
        }
}