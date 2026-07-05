package dev.nenoeldeeb.education.absencerecord.domain.usecases.transfer

import dev.nenoeldeeb.education.absencerecord.domain.models.ParsedStudentImportData
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentExportData
import dev.nenoeldeeb.education.absencerecord.domain.repositories.AttendanceRepository
import dev.nenoeldeeb.education.absencerecord.domain.repositories.StudentClassRepository
import dev.nenoeldeeb.education.absencerecord.domain.repositories.StudentRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@DisplayName("PerformImportUseCase Tests")
class PerformImportUseCaseTest {
    private lateinit var studentRepository: StudentRepository
    private lateinit var attendanceRepository: AttendanceRepository
    private lateinit var studentClassRepository: StudentClassRepository
    private lateinit var useCase: PerformImportUseCase

    @BeforeEach
    fun setup() {
        studentRepository = mockk()
        attendanceRepository = mockk()
        studentClassRepository = mockk()
        useCase = PerformImportUseCase(studentRepository, attendanceRepository, studentClassRepository)
        coEvery { studentClassRepository.getOrCreateClassByName(any()) } returns Result.success(1)
        coEvery { studentRepository.updateStudent(any()) } returns Result.success(Unit)
    }

    @Nested
    @DisplayName("invoke Tests")
    inner class InvokeTests {
        @Test
        fun `should fail if no students selected`() =
            runTest {
                val parsedData = listOf(ParsedStudentImportData(StudentExportData("John", "", emptyList())))
                val selectionMap = mapOf(parsedData[0].id to false)

                val result = useCase(parsedData, selectionMap)

                assertTrue(result.isFailure)
                assertIs<StudentError.Validation>(result.exceptionOrNull())
            }

        @Test
        fun `should import new student with attendance`() =
            runTest {
                val exportData = StudentExportData("John", "", listOf("2024-01-01"))
                val parsedData = listOf(ParsedStudentImportData(exportData))
                val selectionMap = mapOf(parsedData[0].id to true)

                every { studentRepository.getAllStudents() } returns
                    flowOf(Result.success(emptyList()))
                coEvery { studentRepository.insertStudent(any()) } returns Result.success(1L)
                coEvery { attendanceRepository.insertAttendance(any()) } returns Result.success(1L)

                val result = useCase(parsedData, selectionMap)

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
                val exportData = StudentExportData("John", "", listOf("2024-01-01"))
                val parsedData = listOf(ParsedStudentImportData(exportData))
                val selectionMap = mapOf(parsedData[0].id to true)
                val existingStudent = Student(id = 1, name = "John")

                every { studentRepository.getAllStudents() } returns
                    flowOf(Result.success(listOf(existingStudent)))
                coEvery { attendanceRepository.insertAttendance(any()) } returns Result.success(1L)

                val result = useCase(parsedData, selectionMap)

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
                val exportData = StudentExportData("John", "", listOf("invalid-date"))
                val parsedData = listOf(ParsedStudentImportData(exportData))
                val selectionMap = mapOf(parsedData[0].id to true)

                every { studentRepository.getAllStudents() } returns
                    flowOf(Result.success(emptyList()))
                coEvery { studentRepository.insertStudent(any()) } returns Result.success(1L)

                val result = useCase(parsedData, selectionMap)

                assertTrue(result.isSuccess)
                val importResult = result.getOrNull()
                assertEquals(0, importResult?.datesProcessedCount)
                assertEquals(1, importResult?.datesSkippedCount)
            }

        @Test
        fun `should handle mixed selection`() =
            runTest {
                val data1 = ParsedStudentImportData(StudentExportData("John", "", emptyList()))
                val data2 = ParsedStudentImportData(StudentExportData("Jane", "", emptyList()))
                val parsedData = listOf(data1, data2)
                val selectionMap = mapOf(data1.id to true, data2.id to false)

                every { studentRepository.getAllStudents() } returns
                    flowOf(Result.success(emptyList()))
                coEvery { studentRepository.insertStudent(any()) } returns Result.success(1L)

                val result = useCase(parsedData, selectionMap)

                assertTrue(result.isSuccess)
                val importResult = result.getOrNull()
                assertEquals(1, importResult?.newStudentsCount)

                coVerify(exactly = 1) { studentRepository.insertStudent(match { it.name == "John" }) }
                coVerify(exactly = 0) { studentRepository.insertStudent(match { it.name == "Jane" }) }
            }

        @Test
        fun `should return failure when exception occurs during import`() =
            runTest {
                val parsedData = listOf(ParsedStudentImportData(StudentExportData("John", "", emptyList())))
                val selectionMap = mapOf(parsedData[0].id to true)

                every { studentRepository.getAllStudents() } returns
                    flowOf(Result.failure(Exception("DB Error")))

                val result = useCase(parsedData, selectionMap)

                assertTrue(result.isFailure)
                assertEquals("DB Error", result.exceptionOrNull()?.message)
            }

        @Test
        fun `should import new student with class`() =
            runTest {
                val exportData = StudentExportData("John", "Class A", emptyList())
                val parsedData = listOf(ParsedStudentImportData(exportData))
                val selectionMap = mapOf(parsedData[0].id to true)

                every { studentRepository.getAllStudents() } returns
                    flowOf(Result.success(emptyList()))
                coEvery { studentClassRepository.getOrCreateClassByName("Class A") } returns
                    Result.success(10)
                coEvery { studentRepository.insertStudent(any()) } returns Result.success(1L)

                useCase(parsedData, selectionMap)

                coVerify {
                    studentRepository.insertStudent(match { it.name == "John" && it.classId == 10 })
                }
            }

        @Test
        fun `should update existing student class when merging`() =
            runTest {
                val exportData = StudentExportData("John", "Class B", emptyList())
                val parsedData = listOf(ParsedStudentImportData(exportData))
                val selectionMap = mapOf(parsedData[0].id to true)
                val existingStudent = Student(id = 1, name = "John", classId = null)

                every { studentRepository.getAllStudents() } returns
                    flowOf(Result.success(listOf(existingStudent)))
                coEvery { studentClassRepository.getOrCreateClassByName("Class B") } returns
                    Result.success(20)
                coEvery { studentRepository.updateStudent(any()) } returns Result.success(Unit)
                coEvery { attendanceRepository.insertAttendance(any()) } returns Result.success(1L)

                useCase(parsedData, selectionMap)

                coVerify {
                    studentRepository.updateStudent(match { it.id == 1 && it.classId == 20 })
                }
            }
    }
}