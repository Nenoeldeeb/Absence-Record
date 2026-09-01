package dev.nenoeldeeb.education.absencerecord.domain.usecases.attendance

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

@DisplayName("GetAvailableMonthsUseCase Tests")
class GetAvailableMonthsUseCaseTest {
    private lateinit var repository: AttendanceRepository
    private lateinit var useCase: GetAvailableMonthsUseCase

    @BeforeEach
    fun setup() {
        repository = mockk()
        useCase = GetAvailableMonthsUseCase(repository)
    }

    @Test
    fun `should retrieve distinct months sorted descending`() =
        runTest {
            // Arrange
            val dates =
                listOf(
                    LocalDate(2024, 1, 15),
                    LocalDate(2024, 1, 20),
                    LocalDate(2024, 2, 10),
                    LocalDate(2023, 12, 25)
                )
            every { repository.getDistinctDatesWithAttendance() } returns flowOf(Result.success(dates))

            // Act
            val result = useCase().first()

            // Assert
            assertTrue(result.isSuccess)
            val months = result.getOrNull()
            assertEquals(3, months?.size)
            assertEquals(
                listOf(
                    LocalDate(2024, 2, 1),
                    LocalDate(2024, 1, 1),
                    LocalDate(2023, 12, 1)
                ),
                months
            )
        }

    @Test
    fun `should distinct duplicate months`() =
        runTest {
            // Arrange
            val dates = listOf(LocalDate(2024, 1, 1), LocalDate(2024, 1, 15), LocalDate(2024, 1, 31))
            every { repository.getDistinctDatesWithAttendance() } returns flowOf(Result.success(dates))

            // Act
            val result = useCase().first()

            // Assert
            assertTrue(result.isSuccess)
            val months = result.getOrNull()
            assertEquals(1, months?.size)
            assertEquals(LocalDate(2024, 1, 1), months?.first())
        }

    @Test
    fun `should handle empty dates list`() =
        runTest {
            // Arrange
            every { repository.getDistinctDatesWithAttendance() } returns
                flowOf(Result.success(emptyList()))

            // Act
            val result = useCase().first()

            // Assert
            assertTrue(result.isSuccess)
            assertEquals(0, result.getOrNull()?.size)
        }

    @Test
    fun `should handle repository failure`() =
        runTest {
            // Arrange
            val exception = Exception("Database error")
            every { repository.getDistinctDatesWithAttendance() } returns
                flowOf(Result.failure(exception))

            // Act
            val result = useCase().first()

            // Assert
            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
        }

    @Test
    fun `should handle leap years correctly`() =
        runTest {
            // Arrange
            val dates = listOf(LocalDate(2024, 2, 29))
            every { repository.getDistinctDatesWithAttendance() } returns flowOf(Result.success(dates))

            // Act
            val result = useCase().first()

            // Assert
            assertTrue(result.isSuccess)
            assertEquals(listOf(LocalDate(2024, 2, 1)), result.getOrNull())
        }

    @Test
    fun `should handle dates spanning multiple years`() =
        runTest {
            // Arrange
            val dates = listOf(LocalDate(2023, 1, 1), LocalDate(2024, 1, 1), LocalDate(2025, 1, 1))
            every { repository.getDistinctDatesWithAttendance() } returns flowOf(Result.success(dates))

            // Act
            val result = useCase().first()

            // Assert
            assertTrue(result.isSuccess)
            assertEquals(
                listOf(
                    LocalDate(2025, 1, 1),
                    LocalDate(2024, 1, 1),
                    LocalDate(2023, 1, 1)
                ),
                result.getOrNull()
            )
        }

    @Test
    fun `should verify flow interactions`() =
        runTest {
            // Arrange
            every { repository.getDistinctDatesWithAttendance() } returns
                flowOf(Result.success(emptyList()))

            // Act
            useCase().first()

            // Assert
            verify(exactly = 1) { repository.getDistinctDatesWithAttendance() }
        }
}