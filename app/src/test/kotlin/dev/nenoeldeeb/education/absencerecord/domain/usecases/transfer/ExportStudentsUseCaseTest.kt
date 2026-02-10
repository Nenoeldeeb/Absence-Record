package dev.nenoeldeeb.education.absencerecord.domain.usecases.transfer

import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentExportData
import dev.nenoeldeeb.education.absencerecord.domain.repositories.AttendanceRepository
import dev.nenoeldeeb.education.absencerecord.domain.repositories.StorageRepository
import dev.nenoeldeeb.education.absencerecord.domain.repositories.StudentRepository
import dev.nenoeldeeb.education.absencerecord.domain.services.SerializationService
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@DisplayName("ExportStudentsUseCase Tests")
class ExportStudentsUseCaseTest {
    private lateinit var studentRepository: StudentRepository
    private lateinit var attendanceRepository: AttendanceRepository
    private lateinit var storageRepository: StorageRepository
    private lateinit var serializationService: SerializationService
    private lateinit var useCase: ExportStudentsUseCase

    @BeforeEach
    fun setup() {
        studentRepository = mockk()
        attendanceRepository = mockk()
        storageRepository = mockk()
        serializationService = mockk()
        useCase = ExportStudentsUseCase(
            attendanceRepository, storageRepository, serializationService
        )
    }

    @Test
    fun `should successfully export selected students`() = runTest {
        // Arrange
        val uriString = "content://export"
        val selectedIds = setOf(1)
        val allStudents = listOf(Student(id = 1, name = "John"))
        val dates = listOf(LocalDate(2024, 1, 1))
        val expectedJson = "[{\"name\":\"John\",\"dates\":[\"2024-01-01\"]}]"

        every { attendanceRepository.getStudentAttendanceDates(1) } returns flowOf(Result.success(dates))
        every {
            serializationService.encodeToString<List<StudentExportData>>(
                any(),
                any()
            )
        } returns Result.success(expectedJson)
        coEvery { storageRepository.writeTextToUri(uriString, expectedJson) } returns Result.success(Unit)

        // Act
        val result = useCase(uriString, selectedIds, allStudents)

        // Assert
        assertTrue(result.isSuccess)

        // Verify data passed to serializer
        val capturedList = slot<List<StudentExportData>>()
        verify {
            serializationService.encodeToString(
                capture(capturedList), any()
            )
        }
        assertEquals(1, capturedList.captured.size)
        assertEquals("John", capturedList.captured[0].name)
        assertEquals(listOf("2024-01-01"), capturedList.captured[0].dates)
    }

    @Test
    fun `should fail if no students selected`() = runTest {
        // Arrange
        val result = useCase("uri", emptySet(), emptyList())

        // Assert
        assertTrue(result.isFailure)
        assertIs<IllegalArgumentException>(result.exceptionOrNull())
    }

    @Test
    fun `should ignore students not found in list`() = runTest {
        // Arrange
        val uriString = "content://export"
        val selectedIds = setOf(999) // ID not in allStudents
        val allStudents = listOf(Student(id = 1, name = "John"))
        val expectedJson = "[]"

        every {
            serializationService.encodeToString<List<StudentExportData>>(
                any(),
                any()
            )
        } returns Result.success(expectedJson)
        coEvery { storageRepository.writeTextToUri(uriString, expectedJson) } returns Result.success(Unit)

        // Act
        val result = useCase(uriString, selectedIds, allStudents)

        // Assert
        assertTrue(result.isSuccess)
        val capturedList = slot<List<StudentExportData>>()
        verify {
            serializationService.encodeToString(
                capture(capturedList), any()
            )
        }
        assertTrue(capturedList.captured.isEmpty())
    }

    @Test
    fun `should handle empty attendance history`() = runTest {
        // Arrange
        val uriString = "content://export"
        val selectedIds = setOf(1)
        val allStudents = listOf(Student(id = 1, name = "John"))

        every { attendanceRepository.getStudentAttendanceDates(1) } returns flowOf(Result.success(emptyList()))
        every { serializationService.encodeToString<List<StudentExportData>>(any(), any()) } returns Result.success("json")
        coEvery { storageRepository.writeTextToUri(any(), any()) } returns Result.success(Unit)

        // Act
        val result = useCase(uriString, selectedIds, allStudents)

        // Assert
        assertTrue(result.isSuccess)
        val capturedList = slot<List<StudentExportData>>()
        verify {
            serializationService.encodeToString(
                capture(capturedList), any()
            )
        }
        assertTrue(capturedList.captured[0].dates.isEmpty())
    }

    @Test
    fun `should handle serialization failure`() = runTest {
        // Arrange
        val exception = Exception("Serialization error")
        val selectedIds = setOf(1)
        val allStudents = listOf(Student(id = 1, name = "John"))

        every { attendanceRepository.getStudentAttendanceDates(1) } returns flowOf(Result.success(emptyList()))
        every { serializationService.encodeToString<List<StudentExportData>>(any(), any()) } returns Result.failure(exception)

        // Act
        val result = useCase("uri", selectedIds, allStudents)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `should handle storage failure`() = runTest {
        // Arrange
        val exception = Exception("Write error")
        val selectedIds = setOf(1)
        val allStudents = listOf(Student(id = 1, name = "John"))

        every { attendanceRepository.getStudentAttendanceDates(1) } returns flowOf(Result.success(emptyList()))
        every { serializationService.encodeToString<List<StudentExportData>>(any(), any()) } returns Result.success("json")
        coEvery { storageRepository.writeTextToUri(any(), any()) } returns Result.failure(exception)

        // Act
        val result = useCase("uri", selectedIds, allStudents)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `should handle exception during data gathering`() = runTest {
        // Arrange - Force exception during finding/mapping
        val selectedIds = setOf(1)
        val allStudents = listOf(Student(id = 1, name = "John"))

        // Mock repo to throw exception
        every { attendanceRepository.getStudentAttendanceDates(1) } throws RuntimeException("Unexpected error")

        // Act
        val result = useCase("uri", selectedIds, allStudents)

        // Assert
        assertTrue(result.isFailure)
        assertIs<RuntimeException>(result.exceptionOrNull())
    }
}