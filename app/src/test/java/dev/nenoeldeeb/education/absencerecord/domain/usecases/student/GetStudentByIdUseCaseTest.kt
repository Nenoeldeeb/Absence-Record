package dev.nenoeldeeb.education.absencerecord.domain.usecases.student

import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError
import dev.nenoeldeeb.education.absencerecord.domain.repositories.StudentRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@DisplayName("GetStudentByIdUseCase Tests")
class GetStudentByIdUseCaseTest {
    private lateinit var repository: StudentRepository
    private lateinit var useCase: GetStudentByIdUseCase

    @BeforeEach
    fun setup() {
        repository = mockk()
        useCase = GetStudentByIdUseCase(repository)
    }

    @Test
    fun `should return the student when repository succeeds`() =
        runTest {
            // Arrange
            val student = Student(id = 5, name = "Alice", classId = 3)
            every { repository.getStudentById(5) } returns flowOf(Result.success(student))

            // Act
            val result = useCase(5).first()

            // Assert
            assertTrue(result.isSuccess)
            assertEquals(student, result.getOrNull())
            verify(exactly = 1) { repository.getStudentById(5) }
        }

    @Test
    fun `should return null when student does not exist`() =
        runTest {
            // Arrange
            every { repository.getStudentById(42) } returns flowOf(Result.success(null))

            // Act
            val result = useCase(42).first()

            // Assert
            assertTrue(result.isSuccess)
            assertNull(result.getOrNull())
            verify(exactly = 1) { repository.getStudentById(42) }
        }

    @Test
    fun `should propagate repository failure`() =
        runTest {
            // Arrange
            val failure = Result.failure<Student?>(StudentError.Database)
            every { repository.getStudentById(7) } returns flowOf(failure)

            // Act
            val result = useCase(7).first()

            // Assert
            assertTrue(result.isFailure)
            assertEquals(StudentError.Database, result.exceptionOrNull())
            verify(exactly = 1) { repository.getStudentById(7) }
        }

    @Test
    fun `should delegate the passed studentId to the repository`() =
        runTest {
            // Arrange
            val student = Student(id = 99, name = "Zoe")
            every { repository.getStudentById(99) } returns flowOf(Result.success(student))

            // Act
            val result = useCase(99).first()

            // Assert
            assertTrue(result.isSuccess)
            assertEquals(99, result.getOrNull()?.id)
            verify(exactly = 1) { repository.getStudentById(99) }
            verify(exactly = 0) { repository.getStudentById(1) }
        }
}