package dev.nenoeldeeb.education.absencerecord.presentation.screens.students.delegates

import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.ImportResult
import dev.nenoeldeeb.education.absencerecord.domain.models.ParsedImportData
import dev.nenoeldeeb.education.absencerecord.domain.models.ParsedStudentImportData
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentExportData
import dev.nenoeldeeb.education.absencerecord.domain.usecases.StudentManagementUseCases
import dev.nenoeldeeb.education.absencerecord.domain.usecases.student.AddStudentUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.student.DeleteStudentsUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.student.UpdateStudentUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.transfer.ExportStudentsUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.transfer.ParseImportFileUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.transfer.PerformImportUseCase
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class StudentActionDelegateTest {
    private lateinit var addStudentUseCase: AddStudentUseCase
    private lateinit var updateStudentUseCase: UpdateStudentUseCase
    private lateinit var deleteStudentsUseCase: DeleteStudentsUseCase
    private lateinit var exportStudentsUseCase: ExportStudentsUseCase
    private lateinit var parseImportFileUseCase: ParseImportFileUseCase
    private lateinit var performImportUseCase: PerformImportUseCase
    private lateinit var studentManagementUseCases: StudentManagementUseCases
    private lateinit var importExportDelegate: ImportExportDelegate
    private lateinit var delegate: StudentActionDelegate

    @BeforeEach
    fun setUp() {
        addStudentUseCase = mockk(relaxed = true)
        updateStudentUseCase = mockk(relaxed = true)
        deleteStudentsUseCase = mockk(relaxed = true)
        exportStudentsUseCase = mockk(relaxed = true)
        parseImportFileUseCase = mockk(relaxed = true)
        performImportUseCase = mockk(relaxed = true)

        studentManagementUseCases = mockk(relaxed = true)
        every { studentManagementUseCases.addStudentUseCase } returns addStudentUseCase
        every { studentManagementUseCases.updateStudentUseCase } returns updateStudentUseCase
        every { studentManagementUseCases.deleteStudentsUseCase } returns deleteStudentsUseCase
        every { studentManagementUseCases.exportStudentsUseCase } returns exportStudentsUseCase
        every { studentManagementUseCases.parseImportFileUseCase } returns parseImportFileUseCase
        every { studentManagementUseCases.performImportUseCase } returns performImportUseCase

        importExportDelegate = ImportExportDelegate()
        delegate = StudentActionDelegate(studentManagementUseCases, importExportDelegate)
    }

    @Test
    fun `addStudent with valid name returns success toast`() =
        runTest {
            coEvery { addStudentUseCase(any()) } returns Result.success(1L)

            val result = delegate.addStudent("Test Student", classId = null)

            assertTrue(result.isSuccess)
            val expected = UiText.StringResource(R.string.student_added, "Test Student")
            assertEquals(expected, result.getOrNull())
        }

    @Test
    fun `addStudent with blank name returns Validation error`() =
        runTest {
            val result = delegate.addStudent("  ", classId = null)

            assertTrue(result.isFailure)
            val error = result.exceptionOrNull()
            assertIs<StudentError.Validation>(error)
        }

    @Test
    fun `addStudent with DB error returns Database error`() =
        runTest {
            coEvery { addStudentUseCase(any()) } returns Result.failure(StudentError.Database)

            val result = delegate.addStudent("Test", classId = null)

            assertTrue(result.isFailure)
            val error = result.exceptionOrNull()
            assertIs<StudentError.Database>(error)
        }

    @Test
    fun `updateStudent with valid data returns success toast`() =
        runTest {
            val student = Student(id = 1, name = "Old Name")
            coEvery { updateStudentUseCase(any()) } returns Result.success(Unit)

            val result = delegate.updateStudent(student, "New Name", newClassId = null)

            assertTrue(result.isSuccess)
            val expected = UiText.StringResource(R.string.student_updated, "New Name")
            assertEquals(expected, result.getOrNull())
        }

    @Test
    fun `updateStudent with blank name returns Validation error`() =
        runTest {
            val student = Student(id = 1, name = "Old Name")

            val result = delegate.updateStudent(student, "  ", newClassId = null)

            assertTrue(result.isFailure)
            val error = result.exceptionOrNull()
            assertIs<StudentError.Validation>(error)
        }

    @Test
    fun `deleteSelectedStudents with valid IDs returns success toast`() =
        runTest {
            val students = listOf(Student(id = 1, name = "S1"), Student(id = 2, name = "S2"))
            coEvery { deleteStudentsUseCase(any()) } returns Result.success(Unit)

            val result = delegate.deleteSelectedStudents(setOf(1), students)

            assertTrue(result.isSuccess)
            assertEquals(
                UiText.StringResource(R.string.students_deleted_successfully),
                result.getOrNull()
            )
        }

    @Test
    fun `deleteSelectedStudents with empty IDs returns success`() =
        runTest {
            val students = listOf(Student(id = 1, name = "S1"))

            val result = delegate.deleteSelectedStudents(emptySet(), students)

            assertTrue(result.isSuccess)
            assertEquals(
                UiText.StringResource(R.string.students_deleted_successfully),
                result.getOrNull()
            )
        }

    @Test
    fun `deleteSelectedStudents with DB error returns Database error`() =
        runTest {
            val students = listOf(Student(id = 1, name = "S1"))
            coEvery { deleteStudentsUseCase(any()) } returns Result.failure(StudentError.Database)

            val result = delegate.deleteSelectedStudents(setOf(1), students)

            assertTrue(result.isFailure)
            assertIs<StudentError.Database>(result.exceptionOrNull())
        }

    @Test
    fun `exportSelectedStudents with valid data returns success`() =
        runTest {
            val students = listOf(Student(id = 1, name = "S1"))
            coEvery { exportStudentsUseCase(any(), any(), any()) } returns Result.success(Unit)

            val result = delegate.exportSelectedStudents("content://export", setOf(1), students)

            assertTrue(result.isSuccess)
            assertEquals(
                UiText.StringResource(R.string.data_exported_successfully),
                result.getOrNull()
            )
        }

    @Test
    fun `exportSelectedStudents with failure returns FileWrite error`() =
        runTest {
            val students = listOf(Student(id = 1, name = "S1"))
            coEvery { exportStudentsUseCase(any(), any(), any()) } returns
                Result.failure(StudentError.FileWrite)

            val result = delegate.exportSelectedStudents("content://export", setOf(1), students)

            assertTrue(result.isFailure)
            assertIs<StudentError.FileWrite>(result.exceptionOrNull())
        }

    @Test
    fun `exportAndDeleteSelectedStudents success returns combined toast`() =
        runTest {
            val students = listOf(Student(id = 1, name = "S1"))
            coEvery { exportStudentsUseCase(any(), any(), any()) } returns Result.success(Unit)
            coEvery { deleteStudentsUseCase(any()) } returns Result.success(Unit)

            val result = delegate.exportAndDeleteSelectedStudents("content://export", setOf(1), students)

            assertTrue(result.isSuccess)
            assertEquals(
                UiText.StringResource(R.string.data_exported_and_deleted_successfully),
                result.getOrNull()
            )
        }

    @Test
    fun `exportAndDeleteSelectedStudents on export failure returns FileWrite and does not delete`() =
        runTest {
            val students = listOf(Student(id = 1, name = "S1"))
            coEvery { exportStudentsUseCase(any(), any(), any()) } returns
                Result.failure(StudentError.FileWrite)

            val result = delegate.exportAndDeleteSelectedStudents("content://export", setOf(1), students)

            assertTrue(result.isFailure)
            assertIs<StudentError.FileWrite>(result.exceptionOrNull())
            coVerify(exactly = 0) { deleteStudentsUseCase(any()) }
        }

    @Test
    fun `prepareImportSelectionDialog with valid file returns parsed data`() =
        runTest {
            val parsedData =
                ParsedImportData(
                    students = listOf(ParsedStudentImportData(StudentExportData("S1", "", emptyList()), 1)),
                    availableHours = emptyList()
                )
            coEvery { parseImportFileUseCase(any()) } returns Result.success(parsedData)

            val result = delegate.prepareImportSelectionDialog("content://file")

            assertTrue(result.isSuccess)
            assertEquals(parsedData, result.getOrNull())
        }

    @Test
    fun `performImport with valid selections returns success toast`() =
        runTest {
            val parsedData =
                ParsedImportData(
                    students = listOf(ParsedStudentImportData(StudentExportData("S1", "", emptyList()), 1)),
                    availableHours = emptyList()
                )
            val importResult =
                ImportResult(
                    newStudentsCount = 1,
                    existingStudentsMergedCount = 0,
                    datesSkippedCount = 0,
                    datesProcessedCount = 0
                )
            coEvery { performImportUseCase(any(), any()) } returns Result.success(importResult)

            val result = delegate.performImport(parsedData, mapOf(1 to true))

            assertTrue(result.isSuccess)
        }

    @Test
    fun `performImport with DB failure returns Database error`() =
        runTest {
            val parsedData =
                ParsedImportData(
                    students = listOf(ParsedStudentImportData(StudentExportData("S1", "", emptyList()), 1)),
                    availableHours = emptyList()
                )
            coEvery { performImportUseCase(any(), any()) } returns
                Result.failure(StudentError.Database)

            val result = delegate.performImport(parsedData, mapOf(1 to true))

            assertTrue(result.isFailure)
            assertIs<StudentError.Database>(result.exceptionOrNull())
        }
}