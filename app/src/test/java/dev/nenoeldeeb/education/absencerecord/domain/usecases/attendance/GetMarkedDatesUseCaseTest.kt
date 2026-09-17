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

@DisplayName("GetMarkedDatesUseCase Tests")
class GetMarkedDatesUseCaseTest {
    private lateinit var repository: AttendanceRepository
    private lateinit var useCase: GetMarkedDatesUseCase

    @BeforeEach
    fun setup() {
        repository = mockk()
        useCase = GetMarkedDatesUseCase(repository)
    }

    @Test
    fun `should return distinct dates on success`() =
        runTest {
            val dates = listOf(LocalDate(2026, 1, 10), LocalDate(2026, 1, 15))
            every { repository.getDistinctDatesWithAttendance() } returns flowOf(Result.success(dates))

            val result = useCase().first()

            assertTrue(result.isSuccess)
            assertEquals(dates, result.getOrNull())
        }

    @Test
    fun `should return empty list on success`() =
        runTest {
            every { repository.getDistinctDatesWithAttendance() } returns
                flowOf(Result.success(emptyList()))

            val result = useCase().first()

            assertTrue(result.isSuccess)
            assertEquals(0, result.getOrNull()?.size)
        }

    @Test
    fun `should propagate repository failure`() =
        runTest {
            val exception = Exception("Database error")
            every { repository.getDistinctDatesWithAttendance() } returns
                flowOf(Result.failure(exception))

            val result = useCase().first()

            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
        }

    @Test
    fun `should verify repository interaction`() =
        runTest {
            every { repository.getDistinctDatesWithAttendance() } returns
                flowOf(Result.success(emptyList()))

            useCase().first()

            verify(exactly = 1) { repository.getDistinctDatesWithAttendance() }
        }
}