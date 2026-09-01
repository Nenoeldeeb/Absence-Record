package dev.nenoeldeeb.education.absencerecord.domain.usecases.student

import dev.nenoeldeeb.education.absencerecord.domain.models.SortType
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.repositories.StudentRepository
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

@DisplayName("GetAllStudentsUseCase Tests")
class GetAllStudentsUseCaseTest {
    private lateinit var repository: StudentRepository
    private lateinit var useCase: GetAllStudentsUseCase

    @BeforeEach
    fun setup() {
        repository = mockk()
        useCase = GetAllStudentsUseCase(repository)
    }

    @Test
    fun `should get all students sorted by name without month filter`() =
        runTest {
            // Arrange
            val students = listOf(Student(id = 1, name = "Alice"), Student(id = 2, name = "Bob"))
            every { repository.getAllStudents() } returns flowOf(Result.success(students))

            // Act
            val result = useCase(sortType = SortType.ByName, month = null).first()

            // Assert
            assertTrue(result.isSuccess)
            assertEquals(students, result.getOrNull())
            verify(exactly = 1) { repository.getAllStudents() }
            verify(exactly = 0) { repository.getStudentsActiveInMonthSortedByName(any()) }
        }

    @Test
    fun `should get all students sorted by attendance without month filter`() =
        runTest {
            // Arrange
            val students = listOf(Student(id = 1, name = "Alice"), Student(id = 2, name = "Bob"))
            every { repository.getAllStudentsSortedByAttendance() } returns
                flowOf(Result.success(students))

            // Act
            val result = useCase(sortType = SortType.ByAttendance, month = null).first()

            // Assert
            assertTrue(result.isSuccess)
            assertEquals(students, result.getOrNull())
            verify(exactly = 1) { repository.getAllStudentsSortedByAttendance() }
        }

    @Test
    fun `should get students sorted by name with month filter`() =
        runTest {
            // Arrange
            val month = LocalDate(2024, 12, 1)
            val students = listOf(Student(id = 1, name = "Alice"))
            every { repository.getStudentsActiveInMonthSortedByName("2024-12") } returns
                flowOf(Result.success(students))

            // Act
            val result = useCase(sortType = SortType.ByName, month = month).first()

            // Assert
            assertTrue(result.isSuccess)
            assertEquals(students, result.getOrNull())
            verify(exactly = 1) { repository.getStudentsActiveInMonthSortedByName("2024-12") }
            verify(exactly = 0) { repository.getAllStudents() }
        }

    @Test
    fun `should get students sorted by attendance with month filter`() =
        runTest {
            // Arrange
            val month = LocalDate(2024, 12, 1)
            val students = listOf(Student(id = 1, name = "Alice"))
            every { repository.getAllStudentsSortedByAttendanceForMonth("2024-12") } returns
                flowOf(Result.success(students))

            // Act
            val result = useCase(sortType = SortType.ByAttendance, month = month).first()

            // Assert
            assertTrue(result.isSuccess)
            assertEquals(students, result.getOrNull())
            verify(exactly = 1) { repository.getAllStudentsSortedByAttendanceForMonth("2024-12") }
        }

    @Test
    fun `should format single digit month with leading zero`() =
        runTest {
            // Arrange
            val month = LocalDate(2024, 1, 15) // Month = 1 (January)
            val students = listOf(Student(id = 1, name = "Alice"))
            every { repository.getStudentsActiveInMonthSortedByName("2024-01") } returns
                flowOf(Result.success(students))

            // Act
            val result = useCase(sortType = SortType.ByName, month = month).first()

            // Assert
            assertTrue(result.isSuccess)
            verify(exactly = 1) { repository.getStudentsActiveInMonthSortedByName("2024-01") }
        }

    @Test
    fun `should format double digit month correctly`() =
        runTest {
            // Arrange
            val month = LocalDate(2024, 10, 15) // Month = 10 (October)
            val students = listOf(Student(id = 1, name = "Alice"))
            every { repository.getStudentsActiveInMonthSortedByName("2024-10") } returns
                flowOf(Result.success(students))

            // Act
            val result = useCase(sortType = SortType.ByName, month = month).first()

            // Assert
            assertTrue(result.isSuccess)
            verify(exactly = 1) { repository.getStudentsActiveInMonthSortedByName("2024-10") }
        }

    @Test
    fun `should handle empty student list`() =
        runTest {
            // Arrange
            val emptyList = emptyList<Student>()
            every { repository.getAllStudents() } returns flowOf(Result.success(emptyList))

            // Act
            val result = useCase(sortType = SortType.ByName).first()

            // Assert
            assertTrue(result.isSuccess)
            assertEquals(0, result.getOrNull()?.size)
        }

    @Test
    fun `should handle repository failure`() =
        runTest {
            // Arrange
            val exception = Exception("Database error")
            every { repository.getAllStudents() } returns flowOf(Result.failure(exception))

            // Act
            val result = useCase(sortType = SortType.ByName).first()

            // Assert
            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
        }

    @Test
    fun `should use default sort type ByName when not specified`() =
        runTest {
            // Arrange
            val students = listOf(Student(id = 1, name = "Alice"))
            every { repository.getAllStudents() } returns flowOf(Result.success(students))

            // Act
            val result = useCase().first()

            // Assert
            assertTrue(result.isSuccess)
            verify(exactly = 1) { repository.getAllStudents() }
        }

    @Test
    fun `should handle month with different years`() =
        runTest {
            // Arrange
            val month2023 = LocalDate(2023, 6, 1)
            val month2024 = LocalDate(2024, 6, 1)
            val students = listOf(Student(id = 1, name = "Alice"))

            every { repository.getStudentsActiveInMonthSortedByName("2023-06") } returns
                flowOf(Result.success(students))
            every { repository.getStudentsActiveInMonthSortedByName("2024-06") } returns
                flowOf(Result.success(students))

            // Act
            val result2023 = useCase(sortType = SortType.ByName, month = month2023).first()
            val result2024 = useCase(sortType = SortType.ByName, month = month2024).first()

            // Assert
            assertTrue(result2023.isSuccess)
            assertTrue(result2024.isSuccess)
            verify(exactly = 1) { repository.getStudentsActiveInMonthSortedByName("2023-06") }
            verify(exactly = 1) { repository.getStudentsActiveInMonthSortedByName("2024-06") }
        }

    @Test
    fun `should handle December month formatting`() =
        runTest {
            // Arrange
            val month = LocalDate(2024, 12, 31)
            val students = listOf(Student(id = 1, name = "Alice"))
            every { repository.getStudentsActiveInMonthSortedByName("2024-12") } returns
                flowOf(Result.success(students))

            // Act
            val result = useCase(sortType = SortType.ByName, month = month).first()

            // Assert
            assertTrue(result.isSuccess)
            verify(exactly = 1) { repository.getStudentsActiveInMonthSortedByName("2024-12") }
        }

    @Test
    fun `should handle flow emission correctly`() =
        runTest {
            // Arrange
            val students = listOf(Student(id = 1, name = "Alice"))
            every { repository.getAllStudents() } returns flowOf(Result.success(students))

            // Act
            val flow = useCase(sortType = SortType.ByName)
            val result = flow.first()

            // Assert
            assertTrue(result.isSuccess)
            assertEquals(1, result.getOrNull()?.size)
        }
}