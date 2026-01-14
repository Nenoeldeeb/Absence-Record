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

@DisplayName("DeleteStudentsUseCase Tests")
class DeleteStudentsUseCaseTest {
    private lateinit var repository: StudentRepository
    private lateinit var useCase: DeleteStudentsUseCase

    @BeforeEach
    fun setup() {
        repository = mockk()
        useCase = DeleteStudentsUseCase(repository)
    }

    @Test
    fun `should successfully delete single student`() =
        runTest {
            // Arrange
            val student = Student(id = 1, name = "John Doe")
            val students = listOf(student)
            coEvery { repository.deleteStudents(students) } returns Result.success(Unit)

            // Act
            val result = useCase(students)

            // Assert
            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { repository.deleteStudents(students) }
        }

    @Test
    fun `should successfully delete multiple students`() =
        runTest {
            // Arrange
            val students =
                listOf(
                    Student(id = 1, name = "Student 1"),
                    Student(id = 2, name = "Student 2"),
                    Student(id = 3, name = "Student 3")
                )
            coEvery { repository.deleteStudents(students) } returns Result.success(Unit)

            // Act
            val result = useCase(students)

            // Assert
            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { repository.deleteStudents(students) }
        }

    @Test
    fun `should handle deleting empty list`() =
        runTest {
            // Arrange
            val emptyList = emptyList<Student>()
            coEvery { repository.deleteStudents(emptyList) } returns Result.success(Unit)

            // Act
            val result = useCase(emptyList)

            // Assert
            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { repository.deleteStudents(emptyList) }
        }

    @Test
    fun `should handle repository failure`() =
        runTest {
            // Arrange
            val students = listOf(Student(id = 1, name = "Test"))
            val exception = Exception("Delete failed")
            coEvery { repository.deleteStudents(students) } returns Result.failure(exception)

            // Act
            val result = useCase(students)

            // Assert
            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
            coVerify(exactly = 1) { repository.deleteStudents(students) }
        }

    @Test
    fun `should handle deleting non-existent students`() =
        runTest {
            // Arrange
            val students =
                listOf(
                    Student(id = 999, name = "Non Existent 1"),
                    Student(id = 1000, name = "Non Existent 2")
                )
            val exception = NoSuchElementException("Students not found")
            coEvery { repository.deleteStudents(students) } returns Result.failure(exception)

            // Act
            val result = useCase(students)

            // Assert
            assertTrue(result.isFailure)
            assertIs<NoSuchElementException>(result.exceptionOrNull())
            coVerify(exactly = 1) { repository.deleteStudents(students) }
        }

    @Test
    fun `should delete large batch of students`() =
        runTest {
            // Arrange
            val students = (1..100).map { Student(id = it, name = "Student $it") }
            coEvery { repository.deleteStudents(students) } returns Result.success(Unit)

            // Act
            val result = useCase(students)

            // Assert
            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { repository.deleteStudents(students) }
        }

    @Test
    fun `should handle database constraint violation`() =
        runTest {
            // Arrange
            val students = listOf(Student(id = 1, name = "Test"))
            val exception = IllegalStateException("Foreign key constraint violation")
            coEvery { repository.deleteStudents(students) } returns Result.failure(exception)

            // Act
            val result = useCase(students)

            // Assert
            assertTrue(result.isFailure)
            assertIs<IllegalStateException>(result.exceptionOrNull())
            coVerify(exactly = 1) { repository.deleteStudents(students) }
        }

    @Test
    fun `should delete students with special characters in names`() =
        runTest {
            // Arrange
            val students =
                listOf(
                    Student(id = 1, name = "José María"),
                    Student(id = 2, name = "O'Connor"),
                    Student(id = 3, name = "محمد أحمد")
                )
            coEvery { repository.deleteStudents(students) } returns Result.success(Unit)

            // Act
            val result = useCase(students)

            // Assert
            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { repository.deleteStudents(students) }
        }

    @Test
    fun `should handle deletion timeout exception`() =
        runTest {
            // Arrange
            val students = listOf(Student(id = 1, name = "Test"))
            val exception = RuntimeException("Operation timeout")
            coEvery { repository.deleteStudents(students) } returns Result.failure(exception)

            // Act
            val result = useCase(students)

            // Assert
            assertTrue(result.isFailure)
            val message = result.exceptionOrNull()?.message
            assertEquals(message?.contains("timeout"), true)
        }

    @Test
    fun `should delete students with default id zero`() =
        runTest {
            // Arrange
            val students = listOf(Student(id = 0, name = "Default ID Student"))
            coEvery { repository.deleteStudents(students) } returns Result.success(Unit)

            // Act
            val result = useCase(students)

            // Assert
            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { repository.deleteStudents(students) }
        }
}