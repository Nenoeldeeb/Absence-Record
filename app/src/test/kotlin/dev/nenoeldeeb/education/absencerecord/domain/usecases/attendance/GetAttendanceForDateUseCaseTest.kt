package dev.nenoeldeeb.education.absencerecord.domain.usecases.attendance

import dev.nenoeldeeb.education.absencerecord.domain.models.StudentAttendance
import dev.nenoeldeeb.education.absencerecord.domain.repositories.AttendanceRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DisplayName("GetAttendanceForDateUseCase Tests")
class GetAttendanceForDateUseCaseTest {
    private lateinit var repository: AttendanceRepository
    private lateinit var useCase: GetAttendanceForDateUseCase

    @BeforeEach
    fun setup() {
        repository = mockk()
        useCase = GetAttendanceForDateUseCase(repository)
    }

    @Test
    fun `should get attendance for specific date`() =
        runTest {
            // Arrange
            val date = LocalDate(2024, 12, 23)
            val attendances =
                listOf(
                    StudentAttendance(id = 1, studentId = 1, date = date),
                    StudentAttendance(id = 2, studentId = 2, date = date)
                )
            every { repository.getAttendanceForDate(date) } returns flowOf(Result.success(attendances))

            // Act
            val result = useCase(date).first()

            // Assert
            assertTrue(result.isSuccess)
            assertEquals(attendances, result.getOrNull())
            assertEquals(2, result.getOrNull()?.size)
            verify(exactly = 1) { repository.getAttendanceForDate(date) }
        }

    @Test
    fun `should handle date with no attendances`() =
        runTest {
            // Arrange
            val date = LocalDate(2024, 12, 23)
            val emptyList = emptyList<StudentAttendance>()
            every { repository.getAttendanceForDate(date) } returns flowOf(Result.success(emptyList))

            // Act
            val result = useCase(date).first()

            // Assert
            assertTrue(result.isSuccess)
            assertEquals(0, result.getOrNull()?.size)
            verify(exactly = 1) { repository.getAttendanceForDate(date) }
        }

    @Test
    fun `should handle date with single attendance`() =
        runTest {
            // Arrange
            val date = LocalDate(2024, 12, 23)
            val attendances = listOf(StudentAttendance(id = 1, studentId = 1, date = date))
            every { repository.getAttendanceForDate(date) } returns flowOf(Result.success(attendances))

            // Act
            val result = useCase(date).first()

            // Assert
            assertTrue(result.isSuccess)
            assertEquals(1, result.getOrNull()?.size)
        }

    @Test
    fun `should handle date with many attendances`() =
        runTest {
            // Arrange
            val date = LocalDate(2024, 12, 23)
            val attendances = (1..100).map { StudentAttendance(id = it, studentId = it, date = date) }
            every { repository.getAttendanceForDate(date) } returns flowOf(Result.success(attendances))

            // Act
            val result = useCase(date).first()

            // Assert
            assertTrue(result.isSuccess)
            assertEquals(100, result.getOrNull()?.size)
        }

    @Test
    fun `should handle repository failure`() =
        runTest {
            // Arrange
            val date = LocalDate(2024, 12, 23)
            val exception = Exception("Database error")
            every { repository.getAttendanceForDate(date) } returns flowOf(Result.failure(exception))

            // Act
            val result = useCase(date).first()

            // Assert
            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
        }

    @Test
    fun `should handle past dates`() =
        runTest {
            // Arrange
            val pastDate = LocalDate(2020, 1, 1)
            val attendances = listOf(StudentAttendance(id = 1, studentId = 1, date = pastDate))
            every { repository.getAttendanceForDate(pastDate) } returns
                flowOf(Result.success(attendances))

            // Act
            val result = useCase(pastDate).first()

            // Assert
            assertTrue(result.isSuccess)
            assertEquals(1, result.getOrNull()?.size)
        }

    @Test
    fun `should handle future dates`() =
        runTest {
            // Arrange
            val futureDate = LocalDate(2030, 12, 31)
            val emptyList = emptyList<StudentAttendance>()
            every { repository.getAttendanceForDate(futureDate) } returns
                flowOf(Result.success(emptyList))

            // Act
            val result = useCase(futureDate).first()

            // Assert
            assertTrue(result.isSuccess)
            assertEquals(0, result.getOrNull()?.size)
        }

    @Test
    fun `should handle different dates independently`() =
        runTest {
            // Arrange
            val date1 = LocalDate(2024, 12, 20)
            val date2 = LocalDate(2024, 12, 21)
            val attendances1 = listOf(StudentAttendance(id = 1, studentId = 1, date = date1))
            val attendances2 =
                listOf(
                    StudentAttendance(id = 2, studentId = 2, date = date2),
                    StudentAttendance(id = 3, studentId = 3, date = date2)
                )

            every { repository.getAttendanceForDate(date1) } returns
                flowOf(Result.success(attendances1))
            every { repository.getAttendanceForDate(date2) } returns
                flowOf(Result.success(attendances2))

            // Act
            val result1 = useCase(date1).first()
            val result2 = useCase(date2).first()

            // Assert
            assertEquals(1, result1.getOrNull()?.size)
            assertEquals(2, result2.getOrNull()?.size)
            verify(exactly = 1) { repository.getAttendanceForDate(date1) }
            verify(exactly = 1) { repository.getAttendanceForDate(date2) }
        }

    @Test
    fun `should handle flow emission correctly`() =
        runTest {
            // Arrange
            val date = LocalDate(2024, 12, 23)
            val attendances = listOf(StudentAttendance(id = 1, studentId = 1, date = date))
            every { repository.getAttendanceForDate(date) } returns flowOf(Result.success(attendances))

            // Act
            val flow = useCase(date)
            val result = flow.first()

            // Assert
            assertTrue(result.isSuccess)
            assertTrue(result.getOrNull()?.isNotEmpty() == true)
        }

    @Test
    fun `should handle leap year date`() =
        runTest {
            // Arrange
            val leapYearDate = LocalDate(2024, 2, 29)
            val attendances = listOf(StudentAttendance(id = 1, studentId = 1, date = leapYearDate))
            every { repository.getAttendanceForDate(leapYearDate) } returns
                flowOf(Result.success(attendances))

            // Act
            val result = useCase(leapYearDate).first()

            // Assert
            assertTrue(result.isSuccess)
            assertEquals(1, result.getOrNull()?.size)
        }
}