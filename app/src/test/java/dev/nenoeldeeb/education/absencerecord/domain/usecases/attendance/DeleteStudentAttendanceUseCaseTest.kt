package dev.nenoeldeeb.education.absencerecord.domain.usecases.attendance

import dev.nenoeldeeb.education.absencerecord.domain.repositories.AttendanceRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@DisplayName("DeleteStudentAttendanceUseCase Tests")
class DeleteStudentAttendanceUseCaseTest {
    private lateinit var repository: AttendanceRepository
    private lateinit var useCase: DeleteStudentAttendanceUseCase

    @BeforeEach
    fun setup() {
        repository = mockk()
        useCase = DeleteStudentAttendanceUseCase(repository)
    }

    @Test
    fun `should successfully delete attendance`() =
        runTest {
            // Arrange
            val studentId = 1
            val date = LocalDate(2024, 12, 23)
            coEvery { repository.deleteAttendance(studentId, date) } returns Result.success(Unit)

            // Act
            val result = useCase(studentId, date)

            // Assert
            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { repository.deleteAttendance(studentId, date) }
        }

    @Test
    fun `should handle repository failure`() =
        runTest {
            // Arrange
            val studentId = 1
            val date = LocalDate(2024, 12, 23)
            val exception = Exception("Delete failed")
            coEvery { repository.deleteAttendance(studentId, date) } returns Result.failure(exception)

            // Act
            val result = useCase(studentId, date)

            // Assert
            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
            coVerify(exactly = 1) { repository.deleteAttendance(studentId, date) }
        }

    @Test
    fun `should handle deleting non-existent attendance`() =
        runTest {
            // Arrange
            val studentId = 999
            val date = LocalDate(2024, 12, 23)
            val exception = NoSuchElementException("Attendance record not found")
            coEvery { repository.deleteAttendance(studentId, date) } returns Result.failure(exception)

            // Act
            val result = useCase(studentId, date)

            // Assert
            assertTrue(result.isFailure)
            assertIs<NoSuchElementException>(result.exceptionOrNull())
        }

    @Test
    fun `should delete attendance for different students on same date`() =
        runTest {
            // Arrange
            val date = LocalDate(2024, 12, 23)
            coEvery { repository.deleteAttendance(1, date) } returns Result.success(Unit)
            coEvery { repository.deleteAttendance(2, date) } returns Result.success(Unit)
            coEvery { repository.deleteAttendance(3, date) } returns Result.success(Unit)

            // Act
            val result1 = useCase(1, date)
            val result2 = useCase(2, date)
            val result3 = useCase(3, date)

            // Assert
            assertTrue(result1.isSuccess)
            assertTrue(result2.isSuccess)
            assertTrue(result3.isSuccess)
            coVerify(exactly = 1) { repository.deleteAttendance(1, date) }
            coVerify(exactly = 1) { repository.deleteAttendance(2, date) }
            coVerify(exactly = 1) { repository.deleteAttendance(3, date) }
        }

    @Test
    fun `should delete attendance for same student on different dates`() =
        runTest {
            // Arrange
            val studentId = 1
            val date1 = LocalDate(2024, 12, 20)
            val date2 = LocalDate(2024, 12, 21)
            val date3 = LocalDate(2024, 12, 22)

            coEvery { repository.deleteAttendance(studentId, date1) } returns Result.success(Unit)
            coEvery { repository.deleteAttendance(studentId, date2) } returns Result.success(Unit)
            coEvery { repository.deleteAttendance(studentId, date3) } returns Result.success(Unit)

            // Act
            val result1 = useCase(studentId, date1)
            val result2 = useCase(studentId, date2)
            val result3 = useCase(studentId, date3)

            // Assert
            assertTrue(result1.isSuccess)
            assertTrue(result2.isSuccess)
            assertTrue(result3.isSuccess)
        }

    @Test
    fun `should handle invalid student id`() =
        runTest {
            // Arrange
            val invalidStudentId = -1
            val date = LocalDate(2024, 12, 23)
            val exception = IllegalArgumentException("Invalid student ID")
            coEvery { repository.deleteAttendance(invalidStudentId, date) } returns
                Result.failure(exception)

            // Act
            val result = useCase(invalidStudentId, date)

            // Assert
            assertTrue(result.isFailure)
            assertIs<IllegalArgumentException>(result.exceptionOrNull())
        }

    @Test
    fun `should handle past dates`() =
        runTest {
            // Arrange
            val studentId = 1
            val pastDate = LocalDate(2020, 1, 1)
            coEvery { repository.deleteAttendance(studentId, pastDate) } returns Result.success(Unit)

            // Act
            val result = useCase(studentId, pastDate)

            // Assert
            assertTrue(result.isSuccess)
        }

    @Test
    fun `should handle future dates`() =
        runTest {
            // Arrange
            val studentId = 1
            val futureDate = LocalDate(2030, 12, 31)
            coEvery { repository.deleteAttendance(studentId, futureDate) } returns Result.success(Unit)

            // Act
            val result = useCase(studentId, futureDate)

            // Assert
            assertTrue(result.isSuccess)
        }

    @Test
    fun `should handle multiple deletions of same record`() =
        runTest {
            // Arrange
            val studentId = 1
            val date = LocalDate(2024, 12, 23)
            coEvery { repository.deleteAttendance(studentId, date) } returns
                Result.success(Unit) andThen
                Result.failure(NoSuchElementException("Already deleted"))

            // Act
            val result1 = useCase(studentId, date)
            val result2 = useCase(studentId, date)

            // Assert
            assertTrue(result1.isSuccess)
            assertTrue(result2.isFailure)
            coVerify(exactly = 2) { repository.deleteAttendance(studentId, date) }
        }

    @Test
    fun `should handle database error during deletion`() =
        runTest {
            // Arrange
            val studentId = 1
            val date = LocalDate(2024, 12, 23)
            val exception = RuntimeException("Database connection lost")
            coEvery { repository.deleteAttendance(studentId, date) } returns Result.failure(exception)

            // Act
            val result = useCase(studentId, date)

            // Assert
            assertTrue(result.isFailure)
            assertEquals(result.exceptionOrNull()?.message?.contains("Database"), true)
        }
}