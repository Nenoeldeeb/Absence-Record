package dev.nenoeldeeb.education.absencerecord.domain.usecases.transfer

import dev.nenoeldeeb.education.absencerecord.domain.models.ParsedStudentImportData
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentExportData
import dev.nenoeldeeb.education.absencerecord.domain.repositories.AttendanceRepository
import dev.nenoeldeeb.education.absencerecord.domain.repositories.StorageRepository
import dev.nenoeldeeb.education.absencerecord.domain.repositories.StudentRepository
import dev.nenoeldeeb.education.absencerecord.domain.services.SerializationService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@DisplayName("ImportStudentsUseCase Tests")
class ImportStudentsUseCaseTest {
    private lateinit var studentRepository: StudentRepository
    private lateinit var attendanceRepository: AttendanceRepository
    private lateinit var storageRepository: StorageRepository
    private lateinit var serializationService: SerializationService
    private lateinit var useCase: ImportStudentsUseCase

    @BeforeEach
    fun setup() {
        studentRepository = mockk()
        attendanceRepository = mockk()
        storageRepository = mockk()
        serializationService = mockk()
        useCase =
            ImportStudentsUseCase(
                studentRepository,
                attendanceRepository,
                storageRepository,
                serializationService
            )
    }

    @Nested
    @DisplayName("parseFile Tests")
    inner class ParseFileTests {
        @Test
        fun `should successfully parse file`() =
            runTest {
                // Arrange
                val uriString = "content://file"
                val jsonString = "[{\"name\":\"John\",\"dates\":[\"2024-01-01\"]}]"
                val exportDataList = listOf(StudentExportData("John", listOf("2024-01-01")))

                every { storageRepository.readTextFromUri(uriString) } returns
                    flowOf(Result.success(jsonString))
                every {
                    serializationService.decodeFromString(
                        jsonString,
                        any<kotlinx.serialization.KSerializer<List<StudentExportData>>>()
                    )
                } returns Result.success(exportDataList)

                // Act
                val result = useCase.parseFile(uriString).first()

                // Assert
                assertTrue(result.isSuccess)
                val parsedList = result.getOrNull()
                assertEquals(1, parsedList?.size)
                assertEquals("John", parsedList?.first()?.originalData?.name)
            }

        @Test
        fun `should return empty list for blank file content`() =
            runTest {
                // Arrange
                val uriString = "content://empty"
                every { storageRepository.readTextFromUri(uriString) } returns
                    flowOf(Result.success("  "))

                // Act
                val result = useCase.parseFile(uriString).first()

                // Assert
                assertTrue(result.isSuccess)
                assertEquals(result.getOrNull()?.isEmpty(), true)
            }

        @Test
        fun `should handle storage repository failure`() =
            runTest {
                // Arrange
                val uriString = "content://error"
                val exception = Exception("Read error")
                every { storageRepository.readTextFromUri(uriString) } returns
                    flowOf(Result.failure(exception))

                // Act
                val result = useCase.parseFile(uriString).first()

                // Assert
                assertTrue(result.isFailure)
                assertEquals(exception, result.exceptionOrNull())
            }

        @Test
        fun `should handle serialization failure`() =
            runTest {
                // Arrange
                val uriString = "content://malformed"
                val jsonString = "{ malformed json"
                val exception = Exception("JSON Parse error")

                every { storageRepository.readTextFromUri(uriString) } returns
                    flowOf(Result.success(jsonString))
                every {
                    serializationService.decodeFromString(
                        jsonString,
                        any<kotlinx.serialization.KSerializer<List<StudentExportData>>>()
                    )
                } returns Result.failure(exception)

                // Act
                val result = useCase.parseFile(uriString).first()

                // Assert
                assertTrue(result.isFailure)
                assertEquals(exception, result.exceptionOrNull())
            }
    }

    @Nested
    @DisplayName("performImport Tests")
    inner class PerformImportTests {
        @Test
        fun `should fail if no students selected`() =
            runTest {
                // Arrange
                val parsedData = listOf(ParsedStudentImportData(StudentExportData("John", emptyList())))
                val selectionMap = mapOf(parsedData[0].id to false)

                // Act
                val result = useCase.performImport(parsedData, selectionMap).first()

                // Assert
                assertTrue(result.isFailure)
                assertIs<IllegalArgumentException>(result.exceptionOrNull())
            }

        @Test
        fun `should import new student with attendance`() =
            runTest {
                // Arrange
                val exportData = StudentExportData("John", listOf("2024-01-01"))
                val parsedData = listOf(ParsedStudentImportData(exportData))
                val selectionMap = mapOf(parsedData[0].id to true)

                // Mock check for existing students
                every { studentRepository.getAllStudents() } answers
                    {
                        flowOf(Result.success(emptyList()))
                    }

                // Mock insertion of new student
                coEvery { studentRepository.insertStudent(any()) } returns Result.success(1L)

                // Mock insertion of attendance
                coEvery { attendanceRepository.insertAttendance(any()) } returns Result.success(1L)

                // Act
                val result = useCase.performImport(parsedData, selectionMap).first()

                // Assert
                assertTrue(result.isSuccess)
                val importResult = result.getOrNull()
                assertEquals(1, importResult?.newStudentsCount)
                assertEquals(0, importResult?.existingStudentsMergedCount)
                assertEquals(1, importResult?.datesProcessedCount)

                coVerify(exactly = 1) { studentRepository.insertStudent(match { it.name == "John" }) }
                coVerify(exactly = 1) {
                    attendanceRepository.insertAttendance(
                        match { it.studentId == 1 && it.date.toString() == "2024-01-01" }
                    )
                }
            }

        @Test
        fun `should merge with existing student`() =
            runTest {
                // Arrange
                val exportData = StudentExportData("John", listOf("2024-01-01"))
                val parsedData = listOf(ParsedStudentImportData(exportData))
                val selectionMap = mapOf(parsedData[0].id to true)
                val existingStudent = Student(id = 1, name = "John")

                // Mock existing students
                every { studentRepository.getAllStudents() } answers
                    {
                        flowOf(Result.success(listOf(existingStudent)))
                    }

                // Mock insertion of attendance only
                coEvery { attendanceRepository.insertAttendance(any()) } returns Result.success(1L)

                // Act
                val result = useCase.performImport(parsedData, selectionMap).first()

                // Assert
                assertTrue(result.isSuccess)
                val importResult = result.getOrNull()
                assertEquals(0, importResult?.newStudentsCount)
                assertEquals(1, importResult?.existingStudentsMergedCount)
                assertEquals(1, importResult?.datesProcessedCount)

                coVerify(exactly = 0) { studentRepository.insertStudent(any()) }
                coVerify(exactly = 1) {
                    attendanceRepository.insertAttendance(
                        match { it.studentId == 1 && it.date.toString() == "2024-01-01" }
                    )
                }
            }

        @Test
        fun `should handle invalid dates in import`() =
            runTest {
                // Arrange
                val exportData = StudentExportData("John", listOf("invalid-date"))
                val parsedData = listOf(ParsedStudentImportData(exportData))
                val selectionMap = mapOf(parsedData[0].id to true)

                every { studentRepository.getAllStudents() } answers
                    {
                        flowOf(Result.success(emptyList()))
                    }
                coEvery { studentRepository.insertStudent(any()) } returns Result.success(1L)

                // Act
                val result = useCase.performImport(parsedData, selectionMap).first()

                // Assert
                assertTrue(result.isSuccess)
                val importResult = result.getOrNull()
                assertEquals(0, importResult?.datesProcessedCount)
                assertEquals(1, importResult?.datesSkippedCount)
            }

        @Test
        fun `should handle mixed selection`() =
            runTest {
                // Arrange
                val data1 = ParsedStudentImportData(StudentExportData("John", emptyList()))
                val data2 = ParsedStudentImportData(StudentExportData("Jane", emptyList()))
                val parsedData = listOf(data1, data2)
                val selectionMap = mapOf(data1.id to true, data2.id to false)

                every { studentRepository.getAllStudents() } answers
                    {
                        flowOf(Result.success(emptyList()))
                    }
                coEvery { studentRepository.insertStudent(any()) } returns Result.success(1L)

                // Act
                val result = useCase.performImport(parsedData, selectionMap).first()

                // Assert
                assertTrue(result.isSuccess)
                val importResult = result.getOrNull()
                assertEquals(1, importResult?.newStudentsCount)

                coVerify(exactly = 1) { studentRepository.insertStudent(match { it.name == "John" }) }
                coVerify(exactly = 0) { studentRepository.insertStudent(match { it.name == "Jane" }) }
            }

        @Test
        fun `should return failure when exception occurs during import`() =
            runTest {
                // Arrange
                val parsedData = listOf(ParsedStudentImportData(StudentExportData("John", emptyList())))
                val selectionMap = mapOf(parsedData[0].id to true)

                every { studentRepository.getAllStudents() } answers
                    {
                        flowOf(Result.failure(Exception("DB Error")))
                    }

                // Act
                val result = useCase.performImport(parsedData, selectionMap).first()

                // Assert
                assertTrue(result.isFailure)
                assertEquals("DB Error", result.exceptionOrNull()?.message)
            }
    }
}