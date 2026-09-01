package dev.nenoeldeeb.education.absencerecord.domain.usecases.transfer

import dev.nenoeldeeb.education.absencerecord.domain.models.ParsedImportData
import dev.nenoeldeeb.education.absencerecord.domain.models.ParsedStudentImportData
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentExportData
import dev.nenoeldeeb.education.absencerecord.domain.repositories.AttendanceRepository
import dev.nenoeldeeb.education.absencerecord.domain.repositories.ScheduleRepository
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
    private lateinit var scheduleRepository: ScheduleRepository
    private lateinit var useCase: PerformImportUseCase

    @BeforeEach
    fun setup() {
        studentRepository = mockk()
        attendanceRepository = mockk()
        studentClassRepository = mockk()
        scheduleRepository = mockk()
        useCase =
            PerformImportUseCase(
                studentRepository,
                attendanceRepository,
                studentClassRepository,
                scheduleRepository
            )
        coEvery { studentClassRepository.getOrCreateClassByName(any()) } returns Result.success(1)
        coEvery { studentRepository.updateStudent(any()) } returns Result.success(Unit)
        every { scheduleRepository.observeHours() } returns flowOf(Result.success(emptyList()))
        every { scheduleRepository.observeAssignments() } returns flowOf(Result.success(emptyList()))
        every { scheduleRepository.observeBusyAppointments() } returns flowOf(Result.success(emptyList()))
    }

    private fun parsedData(studentData: StudentExportData): ParsedImportData =
        ParsedImportData(
            students = listOf(ParsedStudentImportData(studentData, 1)),
            availableHours = emptyList()
        )

    @Nested
    @DisplayName("invoke Tests")
    inner class InvokeTests {
        @Test
        fun `should fail if no students selected`() =
            runTest {
                val data = parsedData(StudentExportData("John", "", emptyList()))
                val selectionMap = mapOf(data.students[0].id to false)

                val result = useCase(data, selectionMap)

                assertTrue(result.isFailure)
                assertIs<StudentError.Validation>(result.exceptionOrNull())
            }

        @Test
        fun `should import new student with attendance`() =
            runTest {
                val data = parsedData(StudentExportData("John", "", listOf("2024-01-01")))
                val selectionMap = mapOf(data.students[0].id to true)

                every { studentRepository.getAllStudents() } returns
                    flowOf(Result.success(emptyList()))
                coEvery { studentRepository.insertStudent(any()) } returns Result.success(1L)
                coEvery { attendanceRepository.insertAttendance(any()) } returns Result.success(1L)

                val result = useCase(data, selectionMap)

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
                val data = parsedData(StudentExportData("John", "", listOf("2024-01-01")))
                val selectionMap = mapOf(data.students[0].id to true)
                val existingStudent = Student(id = 1, name = "John")

                every { studentRepository.getAllStudents() } returns
                    flowOf(Result.success(listOf(existingStudent)))
                coEvery { attendanceRepository.insertAttendance(any()) } returns Result.success(1L)

                val result = useCase(data, selectionMap)

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
                val data = parsedData(StudentExportData("John", "", listOf("invalid-date")))
                val selectionMap = mapOf(data.students[0].id to true)

                every { studentRepository.getAllStudents() } returns
                    flowOf(Result.success(emptyList()))
                coEvery { studentRepository.insertStudent(any()) } returns Result.success(1L)

                val result = useCase(data, selectionMap)

                assertTrue(result.isSuccess)
                val importResult = result.getOrNull()
                assertEquals(0, importResult?.datesProcessedCount)
                assertEquals(1, importResult?.datesSkippedCount)
            }

        @Test
        fun `should handle mixed selection`() =
            runTest {
                val john = ParsedStudentImportData(StudentExportData("John", "", emptyList()), 1)
                val jane = ParsedStudentImportData(StudentExportData("Jane", "", emptyList()), 2)
                val data = ParsedImportData(listOf(john, jane), emptyList())
                val selectionMap = mapOf(john.id to true, jane.id to false)

                every { studentRepository.getAllStudents() } returns
                    flowOf(Result.success(emptyList()))
                coEvery { studentRepository.insertStudent(any()) } returns Result.success(1L)

                val result = useCase(data, selectionMap)

                assertTrue(result.isSuccess)
                val importResult = result.getOrNull()
                assertEquals(1, importResult?.newStudentsCount)

                coVerify(exactly = 1) { studentRepository.insertStudent(match { it.name == "John" }) }
                coVerify(exactly = 0) { studentRepository.insertStudent(match { it.name == "Jane" }) }
            }

        @Test
        fun `should return failure when exception occurs during import`() =
            runTest {
                val data = parsedData(StudentExportData("John", "", emptyList()))
                val selectionMap = mapOf(data.students[0].id to true)

                every { studentRepository.getAllStudents() } returns
                    flowOf(Result.failure(Exception("DB Error")))

                val result = useCase(data, selectionMap)

                assertTrue(result.isFailure)
                assertEquals("DB Error", result.exceptionOrNull()?.message)
            }

        @Test
        fun `should import new student with class`() =
            runTest {
                val data = parsedData(StudentExportData("John", "Class A", emptyList()))
                val selectionMap = mapOf(data.students[0].id to true)

                every { studentRepository.getAllStudents() } returns
                    flowOf(Result.success(emptyList()))
                coEvery { studentClassRepository.getOrCreateClassByName("Class A") } returns
                    Result.success(10)
                coEvery { studentRepository.insertStudent(any()) } returns Result.success(1L)

                useCase(data, selectionMap)

                coVerify {
                    studentRepository.insertStudent(match { it.name == "John" && it.classId == 10 })
                }
            }

        @Test
        fun `should update existing student class when merging`() =
            runTest {
                val data = parsedData(StudentExportData("John", "Class B", emptyList()))
                val selectionMap = mapOf(data.students[0].id to true)
                val existingStudent = Student(id = 1, name = "John", classId = null)

                every { studentRepository.getAllStudents() } returns
                    flowOf(Result.success(listOf(existingStudent)))
                coEvery { studentClassRepository.getOrCreateClassByName("Class B") } returns
                    Result.success(20)
                coEvery { studentRepository.updateStudent(any()) } returns Result.success(Unit)
                coEvery { attendanceRepository.insertAttendance(any()) } returns Result.success(1L)

                useCase(data, selectionMap)

                coVerify {
                    studentRepository.updateStudent(match { it.id == 1 && it.classId == 20 })
                }
            }

        @Test
        fun `should return failure when schedule repository fails`() =
            runTest {
                val data = parsedData(StudentExportData("John", "", emptyList()))
                val selectionMap = mapOf(data.students[0].id to true)

                every { scheduleRepository.observeHours() } returns
                    flowOf(Result.failure(StudentError.Database))

                val result = useCase(data, selectionMap)

                assertTrue(result.isFailure)
                assertEquals(StudentError.Database, result.exceptionOrNull())
            }
    }
}