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

@DisplayName("GetStudentAttendanceDatesUseCase Tests")
class GetStudentAttendanceDatesUseCaseTest {
    private lateinit var repository: AttendanceRepository
    private lateinit var useCase: GetStudentAttendanceDatesUseCase

    @BeforeEach
    fun setup() {
        repository = mockk()
        useCase = GetStudentAttendanceDatesUseCase(repository)
    }

    @Test
    fun `should retrieve attendance dates for student`() =
        runTest {
            // Arrange
            val studentId = 1
            val dates = listOf(LocalDate(2024, 1, 1), LocalDate(2024, 1, 5))
            every { repository.getStudentAttendanceDates(studentId) } returns
                flowOf(Result.success(dates))

            // Act
            val result = useCase(studentId).first()

            // Assert
            assertTrue(result.isSuccess)
            assertEquals(dates, result.getOrNull())
            verify(exactly = 1) { repository.getStudentAttendanceDates(studentId) }
        }

    @Test
    fun `should handle empty dates list`() =
        runTest {
            // Arrange
            val studentId = 1
            every { repository.getStudentAttendanceDates(studentId) } returns
                flowOf(Result.success(emptyList()))

            // Act
            val result = useCase(studentId).first()

            // Assert
            assertTrue(result.isSuccess)
            assertTrue(result.getOrNull()?.isEmpty() == true)
        }

    @Test
    fun `should handle repository failure`() =
        runTest {
            // Arrange
            val studentId = 1
            val exception = Exception("DB Error")
            every { repository.getStudentAttendanceDates(studentId) } returns
                flowOf(Result.failure(exception))

            // Act
            val result = useCase(studentId).first()

            // Assert
            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
        }

    @Test
    fun `should handle invalid student id`() =
        runTest {
            // Arrange
            val studentId = -1
            every { repository.getStudentAttendanceDates(studentId) } returns
                flowOf(Result.success(emptyList()))

            // Act
            val result = useCase(studentId).first()

            // Assert
            assertTrue(result.isSuccess)
            assertTrue(result.getOrNull()?.isEmpty() == true)
            verify(exactly = 1) { repository.getStudentAttendanceDates(studentId) }
        }
}