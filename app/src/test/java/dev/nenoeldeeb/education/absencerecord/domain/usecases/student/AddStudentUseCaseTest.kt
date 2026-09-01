package dev.nenoeldeeb.education.absencerecord.domain.usecases.student

import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.repositories.StudentRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@DisplayName("AddStudentUseCase Tests")
class AddStudentUseCaseTest {
    private lateinit var repository: StudentRepository
    private lateinit var useCase: AddStudentUseCase

    @BeforeEach
    fun setup() {
        repository = mockk()
        useCase = AddStudentUseCase(repository)
    }

    @Test
    fun `should successfully add student and return id`() =
        runTest {
            // Arrange
            val student = Student(name = "John Doe")
            val expectedId = 42L
            coEvery { repository.insertStudent(student) } returns Result.success(expectedId)

            // Act
            val result = useCase(student)

            // Assert
            assertTrue(result.isSuccess)
            assertEquals(expectedId, result.getOrNull())
            coVerify(exactly = 1) { repository.insertStudent(student) }
        }

    @Test
    fun `should handle repository failure`() =
        runTest {
            // Arrange
            val student = Student(name = "John Doe")
            val exception = Exception("Database error")
            coEvery { repository.insertStudent(student) } returns Result.failure(exception)

            // Act
            val result = useCase(student)

            // Assert
            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
            coVerify(exactly = 1) { repository.insertStudent(student) }
        }

    @Test
    fun `should add student with empty name`() =
        runTest {
            // Arrange
            val student = Student(name = "")
            val expectedId = 10L
            coEvery { repository.insertStudent(student) } returns Result.success(expectedId)

            // Act
            val result = useCase(student)

            // Assert
            assertTrue(result.isSuccess)
            assertEquals(expectedId, result.getOrNull())
            coVerify(exactly = 1) { repository.insertStudent(student) }
        }

    @Test
    fun `should add student with special characters in name`() =
        runTest {
            // Arrange
            val student = Student(name = "José María O'Connor")
            val expectedId = 15L
            coEvery { repository.insertStudent(student) } returns Result.success(expectedId)

            // Act
            val result = useCase(student)

            // Assert
            assertTrue(result.isSuccess)
            assertEquals(expectedId, result.getOrNull())
            coVerify(exactly = 1) { repository.insertStudent(student) }
        }

    @Test
    fun `should add student with Arabic name`() =
        runTest {
            // Arrange
            val student = Student(name = "محمد أحمد")
            val expectedId = 20L
            coEvery { repository.insertStudent(student) } returns Result.success(expectedId)

            // Act
            val result = useCase(student)

            // Assert
            assertTrue(result.isSuccess)
            assertEquals(expectedId, result.getOrNull())
            coVerify(exactly = 1) { repository.insertStudent(student) }
        }

    @Test
    fun `should handle repository returning zero as valid id`() =
        runTest {
            // Arrange
            val student = Student(name = "Test Student")
            coEvery { repository.insertStudent(student) } returns Result.success(0L)

            // Act
            val result = useCase(student)

            // Assert
            assertTrue(result.isSuccess)
            assertEquals(0L, result.getOrNull())
        }

    @Test
    fun `should handle multiple sequential additions`() =
        runTest {
            // Arrange
            val student1 = Student(name = "Student 1")
            val student2 = Student(name = "Student 2")
            val student3 = Student(name = "Student 3")
            coEvery { repository.insertStudent(student1) } returns Result.success(1L)
            coEvery { repository.insertStudent(student2) } returns Result.success(2L)
            coEvery { repository.insertStudent(student3) } returns Result.success(3L)

            // Act
            val result1 = useCase(student1)
            val result2 = useCase(student2)
            val result3 = useCase(student3)

            // Assert
            assertEquals(1L, result1.getOrNull())
            assertEquals(2L, result2.getOrNull())
            assertEquals(3L, result3.getOrNull())
            coVerify(exactly = 1) { repository.insertStudent(student1) }
            coVerify(exactly = 1) { repository.insertStudent(student2) }
            coVerify(exactly = 1) { repository.insertStudent(student3) }
        }

    @Test
    fun `should handle different exception types`() =
        runTest {
            // Arrange
            val student = Student(name = "Test")
            val illegalStateException = IllegalStateException("Invalid state")
            coEvery { repository.insertStudent(student) } returns Result.failure(illegalStateException)

            // Act
            val result = useCase(student)

            // Assert
            assertTrue(result.isFailure)
            assertIs<IllegalStateException>(result.exceptionOrNull())
            assertEquals("Invalid state", result.exceptionOrNull()?.message)
        }
}