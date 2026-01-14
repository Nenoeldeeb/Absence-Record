package dev.nenoeldeeb.education.absencerecord.domain.usecases.student

import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.repositories.StudentRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@DisplayName("UpdateStudentUseCase Tests")
class UpdateStudentUseCaseTest {
    private lateinit var repository: StudentRepository
    private lateinit var useCase: UpdateStudentUseCase

    @BeforeEach
    fun setup() {
        repository = mockk()
        useCase = UpdateStudentUseCase(repository)
    }

    @Test
    fun `should successfully update student`() =
        runTest {
            // Arrange
            val student = Student(id = 1, name = "Updated Name")
            coEvery { repository.updateStudent(student) } returns Result.success(Unit)

            // Act
            val result = useCase(student)

            // Assert
            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { repository.updateStudent(student) }
        }

    @Test
    fun `should handle repository failure`() =
        runTest {
            // Arrange
            val student = Student(id = 1, name = "Test")
            val exception = Exception("Update failed")
            coEvery { repository.updateStudent(student) } returns Result.failure(exception)

            // Act
            val result = useCase(student)

            // Assert
            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
            coVerify(exactly = 1) { repository.updateStudent(student) }
        }

    @Test
    fun `should update student with new name`() =
        runTest {
            // Arrange
            val student = Student(id = 42, name = "New Name")
            coEvery { repository.updateStudent(student) } returns Result.success(Unit)

            // Act
            val result = useCase(student)

            // Assert
            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { repository.updateStudent(student) }
        }

    @Test
    fun `should handle updating non-existent student`() =
        runTest {
            // Arrange
            val student = Student(id = 999, name = "Non Existent")
            val exception = NoSuchElementException("Student not found")
            coEvery { repository.updateStudent(student) } returns Result.failure(exception)

            // Act
            val result = useCase(student)

            // Assert
            assertTrue(result.isFailure)
            assertIs<NoSuchElementException>(result.exceptionOrNull())
            coVerify(exactly = 1) { repository.updateStudent(student) }
        }

    @Test
    fun `should update student with empty name`() =
        runTest {
            // Arrange
            val student = Student(id = 5, name = "")
            coEvery { repository.updateStudent(student) } returns Result.success(Unit)

            // Act
            val result = useCase(student)

            // Assert
            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { repository.updateStudent(student) }
        }

    @Test
    fun `should update student with special characters`() =
        runTest {
            // Arrange
            val student = Student(id = 10, name = "José O'Connor")
            coEvery { repository.updateStudent(student) } returns Result.success(Unit)

            // Act
            val result = useCase(student)

            // Assert
            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { repository.updateStudent(student) }
        }

    @Test
    fun `should update student with Arabic name`() =
        runTest {
            // Arrange
            val student = Student(id = 15, name = "محمد أحمد")
            coEvery { repository.updateStudent(student) } returns Result.success(Unit)

            // Act
            val result = useCase(student)

            // Assert
            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { repository.updateStudent(student) }
        }

    @Test
    fun `should handle multiple sequential updates`() =
        runTest {
            // Arrange
            val student1 = Student(id = 1, name = "Name 1")
            val student2 = Student(id = 2, name = "Name 2")
            val student3 = Student(id = 3, name = "Name 3")
            coEvery { repository.updateStudent(student1) } returns Result.success(Unit)
            coEvery { repository.updateStudent(student2) } returns Result.success(Unit)
            coEvery { repository.updateStudent(student3) } returns Result.success(Unit)

            // Act
            val result1 = useCase(student1)
            val result2 = useCase(student2)
            val result3 = useCase(student3)

            // Assert
            assertTrue(result1.isSuccess)
            assertTrue(result2.isSuccess)
            assertTrue(result3.isSuccess)
            coVerify(exactly = 1) { repository.updateStudent(student1) }
            coVerify(exactly = 1) { repository.updateStudent(student2) }
            coVerify(exactly = 1) { repository.updateStudent(student3) }
        }

    @Test
    fun `should handle updating same student multiple times`() =
        runTest {
            // Arrange
            val student = Student(id = 1, name = "Test")
            coEvery { repository.updateStudent(student) } returns Result.success(Unit)

            // Act
            val result1 = useCase(student)
            val result2 = useCase(student)

            // Assert
            assertTrue(result1.isSuccess)
            assertTrue(result2.isSuccess)
            coVerify(exactly = 2) { repository.updateStudent(student) }
        }

    @Test
    fun `should handle database constraint violation`() =
        runTest {
            // Arrange
            val student = Student(id = 1, name = "Test")
            val exception = IllegalArgumentException("Constraint violation")
            coEvery { repository.updateStudent(student) } returns Result.failure(exception)

            // Act
            val result = useCase(student)

            // Assert
            assertTrue(result.isFailure)
            assertIs<IllegalArgumentException>(result.exceptionOrNull())
        }
}