package dev.nenoeldeeb.education.absencerecord.domain.usecases.transfer

import dev.nenoeldeeb.education.absencerecord.domain.models.BackupExportData
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentClass
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError
import dev.nenoeldeeb.education.absencerecord.domain.repositories.AttendanceRepository
import dev.nenoeldeeb.education.absencerecord.domain.repositories.ScheduleRepository
import dev.nenoeldeeb.education.absencerecord.domain.repositories.StorageRepository
import dev.nenoeldeeb.education.absencerecord.domain.repositories.StudentClassRepository
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
import kotlin.test.assertTrue

@DisplayName("ExportStudentsUseCase Tests")
class ExportStudentsUseCaseTest {
    private lateinit var attendanceRepository: AttendanceRepository
    private lateinit var storageRepository: StorageRepository
    private lateinit var serializationService: SerializationService
    private lateinit var studentClassRepository: StudentClassRepository
    private lateinit var scheduleRepository: ScheduleRepository
    private lateinit var useCase: ExportStudentsUseCase

    @BeforeEach
    fun setup() {
        attendanceRepository = mockk()
        storageRepository = mockk()
        serializationService = mockk()
        studentClassRepository = mockk()
        scheduleRepository = mockk()
        useCase =
            ExportStudentsUseCase(
                attendanceRepository, storageRepository, serializationService, studentClassRepository, scheduleRepository
            )
        every { studentClassRepository.getAllClasses() } returns flowOf(Result.success(emptyList()))
        every { scheduleRepository.observeHours() } returns flowOf(Result.success(emptyList()))
        every { scheduleRepository.observeAssignments() } returns flowOf(Result.success(emptyList()))
        every { scheduleRepository.observeBusyAppointments() } returns flowOf(Result.success(emptyList()))
    }

    @Test
    fun `should successfully export selected students`() =
        runTest {
            // Arrange
            val uriString = "content://export"
            val selectedIds = setOf(1)
            val allStudents = listOf(Student(id = 1, name = "John"))
            val dates = listOf(LocalDate(2024, 1, 1))
            val expectedJson = "{\"version\":2}"

            every { attendanceRepository.getStudentAttendanceDates(1) } returns flowOf(Result.success(dates))
            every {
                serializationService.encodeToString<BackupExportData>(
                    any(), any()
                )
            } returns Result.success(expectedJson)
            coEvery { storageRepository.writeTextToUri(uriString, expectedJson) } returns Result.success(Unit)

            // Act
            val result = useCase(uriString, selectedIds, allStudents)

            // Assert
            assertTrue(result.isSuccess)

            // Verify data passed to serializer
            val capturedBackup = slot<BackupExportData>()
            verify {
                serializationService.encodeToString(
                    capture(capturedBackup),
                    any()
                )
            }
            assertEquals(1, capturedBackup.captured.students.size)
            assertEquals("John", capturedBackup.captured.students[0].name)
            assertEquals(listOf("2024-01-01"), capturedBackup.captured.students[0].dates)
            assertTrue(capturedBackup.captured.availableHours.isEmpty())
        }

    @Test
    fun `should fail if no students selected`() =
        runTest {
            // Arrange
            val result = useCase("uri", emptySet(), emptyList())

            // Assert
            assertTrue(result.isFailure)
            assertEquals(StudentError.Validation("No students selected"), result.exceptionOrNull())
        }

    @Test
    fun `should ignore students not found in list`() =
        runTest {
            // Arrange
            val uriString = "content://export"
            val selectedIds = setOf(999)
            val allStudents = listOf(Student(id = 1, name = "John"))
            val expectedJson = "{\"version\":2,\"students\":[]}"

            every {
                serializationService.encodeToString<BackupExportData>(
                    any(), any()
                )
            } returns Result.success(expectedJson)
            coEvery { storageRepository.writeTextToUri(uriString, expectedJson) } returns Result.success(Unit)

            // Act
            val result = useCase(uriString, selectedIds, allStudents)

            // Assert
            assertTrue(result.isSuccess)
            val capturedBackup = slot<BackupExportData>()
            verify {
                serializationService.encodeToString(
                    capture(capturedBackup),
                    any()
                )
            }
            assertTrue(capturedBackup.captured.students.isEmpty())
        }

    @Test
    fun `should handle empty attendance history`() =
        runTest {
            // Arrange
            val uriString = "content://export"
            val selectedIds = setOf(1)
            val allStudents = listOf(Student(id = 1, name = "John"))

            every { attendanceRepository.getStudentAttendanceDates(1) } returns flowOf(Result.success(emptyList()))
            every { serializationService.encodeToString<BackupExportData>(any(), any()) } returns Result.success("json")
            coEvery { storageRepository.writeTextToUri(any(), any()) } returns Result.success(Unit)

            // Act
            val result = useCase(uriString, selectedIds, allStudents)

            // Assert
            assertTrue(result.isSuccess)
            val capturedBackup = slot<BackupExportData>()
            verify {
                serializationService.encodeToString(
                    capture(capturedBackup),
                    any()
                )
            }
            assertTrue(capturedBackup.captured.students[0].dates.isEmpty())
        }

    @Test
    fun `should handle serialization failure`() =
        runTest {
            // Arrange
            val exception = Exception("Serialization error")
            val selectedIds = setOf(1)
            val allStudents = listOf(Student(id = 1, name = "John"))

            every { attendanceRepository.getStudentAttendanceDates(1) } returns flowOf(Result.success(emptyList()))
            every {
                serializationService.encodeToString<BackupExportData>(any(), any())
            } returns Result.failure(exception)

            // Act
            val result = useCase("uri", selectedIds, allStudents)

            // Assert
            assertTrue(result.isFailure)
            assertEquals(StudentError.FileWrite, result.exceptionOrNull())
        }

    @Test
    fun `should handle storage failure`() =
        runTest {
            // Arrange
            val exception = Exception("Write error")
            val selectedIds = setOf(1)
            val allStudents = listOf(Student(id = 1, name = "John"))

            every { attendanceRepository.getStudentAttendanceDates(1) } returns flowOf(Result.success(emptyList()))
            every { serializationService.encodeToString<BackupExportData>(any(), any()) } returns Result.success("json")
            coEvery { storageRepository.writeTextToUri(any(), any()) } returns Result.failure(exception)

            // Act
            val result = useCase("uri", selectedIds, allStudents)

            // Assert
            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
        }

    @Test
    fun `should handle repository failure gracefully`() =
        runTest {
            // Arrange
            val selectedIds = setOf(1)
            val allStudents = listOf(Student(id = 1, name = "John"))
            val expectedJson = "{}"

            every { attendanceRepository.getStudentAttendanceDates(1) } returns
                flowOf(Result.failure(RuntimeException("Unexpected error")))
            every {
                serializationService.encodeToString<BackupExportData>(any(), any())
            } returns Result.success(expectedJson)
            coEvery { storageRepository.writeTextToUri(any(), expectedJson) } returns Result.success(Unit)

            // Act
            val result = useCase("uri", selectedIds, allStudents)

            // Assert
            assertTrue(result.isSuccess)
        }

    @Test
    fun `should include class name when student is associated with a class`() =
        runTest {
            // Arrange
            val uriString = "content://export"
            val selectedIds = setOf(1)
            val allStudents = listOf(Student(id = 1, name = "John", classId = 10))
            val dates = listOf(LocalDate(2024, 1, 1))
            val classes = listOf(StudentClass(id = 10, name = "Class A"))

            every { studentClassRepository.getAllClasses() } returns flowOf(Result.success(classes))
            every { attendanceRepository.getStudentAttendanceDates(1) } returns flowOf(Result.success(dates))
            every {
                serializationService.encodeToString<BackupExportData>(
                    any(), any()
                )
            } returns Result.success("json")
            coEvery { storageRepository.writeTextToUri(any(), any()) } returns Result.success(Unit)

            // Act
            useCase(uriString, selectedIds, allStudents)

            // Assert
            val capturedBackup = slot<BackupExportData>()
            verify {
                serializationService.encodeToString(
                    capture(capturedBackup),
                    any()
                )
            }
            assertEquals(1, capturedBackup.captured.students.size)
            assertEquals("Class A", capturedBackup.captured.students[0].className)
        }
}