package dev.nenoeldeeb.education.absencerecord.domain.usecases.attendance

import dev.nenoeldeeb.education.absencerecord.domain.models.AttendanceHistoryItem
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
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DisplayName("GetAttendanceHistoryForDateRangeUseCase Tests")
class GetAttendanceHistoryForDateRangeUseCaseTest {
    private lateinit var repository: AttendanceRepository
    private lateinit var useCase: GetAttendanceHistoryForDateRangeUseCase

    @BeforeEach
    fun setup() {
        repository = mockk()
        useCase = GetAttendanceHistoryForDateRangeUseCase(repository)
    }

    @Test
    fun `should get attendance history for valid date range`() =
        runTest {
            // Arrange
            val studentId = 1
            val startDate = LocalDate(2024, 1, 1)
            val endDate = LocalDate(2024, 1, 31)
            val historyItems =
                listOf(
                    AttendanceHistoryItem(LocalDate(2024, 1, 15), "Student 1"),
                    AttendanceHistoryItem(LocalDate(2024, 1, 20), "Student 1")
                )
            every { repository.getAttendanceHistoryForDateRange(studentId, startDate, endDate) } returns
                flowOf(Result.success(historyItems))

            // Act
            val result = useCase(studentId, startDate, endDate).first()

            // Assert
            assertTrue(result.isSuccess)
            assertEquals(historyItems, result.getOrNull())
            verify(exactly = 1) {
                repository.getAttendanceHistoryForDateRange(studentId, startDate, endDate)
            }
        }

    @Test
    fun `should handle empty history`() =
        runTest {
            // Arrange
            val studentId = 1
            val startDate = LocalDate(2024, 2, 1)
            val endDate = LocalDate(2024, 2, 28)
            every { repository.getAttendanceHistoryForDateRange(studentId, startDate, endDate) } returns
                flowOf(Result.success(emptyList()))

            // Act
            val result = useCase(studentId, startDate, endDate).first()

            // Assert
            assertTrue(result.isSuccess)
            assertTrue(result.getOrNull()?.isEmpty() == true)
        }

    @Test
    fun `should handle same start and end date`() =
        runTest {
            // Arrange
            val studentId = 1
            val date = LocalDate(2024, 3, 15)
            val historyItems = listOf(AttendanceHistoryItem(date, "Student 1"))
            every { repository.getAttendanceHistoryForDateRange(studentId, date, date) } returns
                flowOf(Result.success(historyItems))

            // Act
            val result = useCase(studentId, date, date).first()

            // Assert
            assertTrue(result.isSuccess)
            assertEquals(1, result.getOrNull()?.size)
        }

    @Test
    fun `should handle reversed date range`() =
        runTest {
            // Arrange
            val studentId = 1
            val startDate = LocalDate(2024, 12, 31)
            val endDate = LocalDate(2024, 1, 1) // End before start

            // Assuming repository handles this or returns empty logic.
            every { repository.getAttendanceHistoryForDateRange(studentId, startDate, endDate) } returns
                flowOf(Result.success(emptyList()))

            // Act
            val result = useCase(studentId, startDate, endDate).first()

            // Assert
            assertTrue(result.isSuccess)
            assertTrue(result.getOrNull()?.isEmpty() == true)
            verify(exactly = 1) {
                repository.getAttendanceHistoryForDateRange(studentId, startDate, endDate)
            }
        }

    @Test
    fun `should handle repository failure`() =
        runTest {
            // Arrange
            val studentId = 1
            val startDate = LocalDate(2024, 1, 1)
            val endDate = LocalDate(2024, 1, 31)
            val exception = Exception("Database error")
            every { repository.getAttendanceHistoryForDateRange(studentId, startDate, endDate) } returns
                flowOf(Result.failure(exception))

            // Act
            val result = useCase(studentId, startDate, endDate).first()

            // Assert
            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
        }

    @Test
    fun `should handle flow emission correctly`() =
        runTest {
            // Arrange
            val studentId = 1
            val startDate = LocalDate(2024, 1, 1)
            val endDate = LocalDate(2024, 1, 31)
            val historyItems = listOf(AttendanceHistoryItem(LocalDate(2024, 1, 1), "Test"))
            every { repository.getAttendanceHistoryForDateRange(studentId, startDate, endDate) } returns
                flowOf(Result.success(historyItems))

            // Act
            val flow = useCase(studentId, startDate, endDate)
            val result = flow.first()

            // Assert
            assertTrue(result.isSuccess)
            assertTrue(result.getOrNull()?.isNotEmpty() == true)
        }
}