package dev.nenoeldeeb.education.absencerecord.presentation.screens.students

import android.net.Uri
import dev.nenoeldeeb.education.absencerecord.MainDispatcherRule
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.ImportResult
import dev.nenoeldeeb.education.absencerecord.domain.models.ParsedStudentImportData
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentExportData
import dev.nenoeldeeb.education.absencerecord.domain.usecases.StudentManagementUseCases
import dev.nenoeldeeb.education.absencerecord.domain.usecases.student.AddStudentUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.student.DeleteStudentsUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.student.GetAllStudentsUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.student.UpdateStudentUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.transfer.ExportStudentsUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.transfer.ImportStudentsUseCase
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.delegates.ImportExportDelegate
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.delegates.SelectionStateDelegate
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Suppress("UnusedFlow")
@OptIn(ExperimentalCoroutinesApi::class)
class StudentsViewModelTest {
    companion object {
        @JvmStatic @RegisterExtension
        val mainDispatcherRule = MainDispatcherRule()
    }

    private lateinit var addStudentUseCase: AddStudentUseCase
    private lateinit var updateStudentUseCase: UpdateStudentUseCase
    private lateinit var deleteStudentsUseCase: DeleteStudentsUseCase
    private lateinit var getAllStudentsUseCase: GetAllStudentsUseCase
    private lateinit var importStudentsUseCase: ImportStudentsUseCase
    private lateinit var exportStudentsUseCase: ExportStudentsUseCase
    private lateinit var studentManagementUseCases: StudentManagementUseCases
    private lateinit var selectionDelegate: SelectionStateDelegate
    private lateinit var importExportDelegate: ImportExportDelegate
    private lateinit var viewModel: StudentsViewModel

    @BeforeEach
    fun setUp() {
        addStudentUseCase = mockk(relaxed = true)
        updateStudentUseCase = mockk(relaxed = true)
        deleteStudentsUseCase = mockk(relaxed = true)
        getAllStudentsUseCase = mockk(relaxed = true)
        importStudentsUseCase = mockk(relaxed = true)
        exportStudentsUseCase = mockk(relaxed = true)

        studentManagementUseCases = mockk(relaxed = true)
        every { studentManagementUseCases.addStudentUseCase } returns addStudentUseCase
        every { studentManagementUseCases.updateStudentUseCase } returns updateStudentUseCase
        every { studentManagementUseCases.deleteStudentsUseCase } returns deleteStudentsUseCase
        every { studentManagementUseCases.getAllStudentsUseCase } returns getAllStudentsUseCase
        every { studentManagementUseCases.importStudentsUseCase } returns importStudentsUseCase
        every { studentManagementUseCases.exportStudentsUseCase } returns exportStudentsUseCase

        selectionDelegate = SelectionStateDelegate()
        importExportDelegate = ImportExportDelegate()
    }

    private fun createViewModel() {
        viewModel =
            StudentsViewModel(
                studentManagementUseCases,
                selectionDelegate,
                importExportDelegate
            )
    }

    @Test
    fun `init loads students successfully`() =
        runTest {
            // Given
            val students = listOf(Student(id = 1, name = "John"), Student(id = 2, name = "Doe"))
            every { getAllStudentsUseCase(any(), any()) } returns flowOf(Result.success(students))

            // When
            createViewModel()
            advanceUntilIdle()

            // Then
            assertEquals(students, viewModel.uiState.value.allStudents)
            assertNull(viewModel.uiState.value.error)
        }

    @Test
    fun `init handles load failure`() =
        runTest {
            // Given
            val errorMsg = "Database error"
            every { getAllStudentsUseCase(any(), any()) } returns
                flowOf(Result.failure(Exception(errorMsg)))

            // When
            createViewModel()
            advanceUntilIdle()

            // Then
            assertEquals(emptyList<Student>(), viewModel.uiState.value.allStudents)
            val expectedError = UiText.StringResource(R.string.error_loading_students, errorMsg)
            assertEquals(expectedError, viewModel.uiState.value.error)
        }

    @Test
    fun `AddStudent event with valid name calls use case`() =
        runTest {
            // Given
            val students = emptyList<Student>()
            every { getAllStudentsUseCase(any(), any()) } returns flowOf(Result.success(students))
            coEvery { addStudentUseCase(any()) } returns Result.success(1L)
            createViewModel()
            advanceUntilIdle()

            // When
            viewModel.onEvent(StudentsScreenEvent.AddStudent("New Student"))
            advanceUntilIdle()

            // Then
            coVerify { addStudentUseCase(match { it.name == "New Student" }) }
            assertEquals(
                UiText.StringResource(R.string.student_added, "New Student"),
                viewModel.uiState.value.toastMessage
            )
        }

    @Test
    fun `AddStudent event with empty name sets error toast`() =
        runTest {
            // Given
            every { getAllStudentsUseCase(any(), any()) } returns flowOf(Result.success(emptyList()))
            createViewModel()
            advanceUntilIdle()

            // When
            viewModel.onEvent(StudentsScreenEvent.AddStudent("  "))

            // Then
            coVerify(exactly = 0) { addStudentUseCase(any()) }
            assertEquals(
                UiText.StringResource(R.string.student_name_cannot_be_empty),
                viewModel.uiState.value.toastMessage
            )
        }

    @Test
    fun `UpdateStudent event calls use case`() =
        runTest {
            // Given
            val student = Student(id = 1, name = "Old Name")
            every { getAllStudentsUseCase(any(), any()) } returns
                flowOf(Result.success(listOf(student)))
            coEvery { updateStudentUseCase(any()) } returns Result.success(Unit)
            createViewModel()
            advanceUntilIdle()

            // When
            viewModel.onEvent(StudentsScreenEvent.UpdateStudent(student, "New Name"))
            advanceUntilIdle()

            // Then
            coVerify { updateStudentUseCase(match { it.id == 1 && it.name == "New Name" }) }
            assertEquals(
                UiText.StringResource(R.string.student_updated, "New Name"),
                viewModel.uiState.value.toastMessage
            )
        }

    @Test
    fun `UpdateStudent with empty name sets error toast`() =
        runTest {
            // Given
            val student = Student(id = 1, name = "Old Name")
            every { getAllStudentsUseCase(any(), any()) } returns
                flowOf(Result.success(listOf(student)))
            createViewModel()
            advanceUntilIdle()

            // When
            viewModel.onEvent(StudentsScreenEvent.UpdateStudent(student, "  "))
            advanceUntilIdle()

            // Then
            coVerify(exactly = 0) { updateStudentUseCase(any()) }
            assertEquals(
                UiText.StringResource(R.string.student_name_cannot_be_empty),
                viewModel.uiState.value.toastMessage
            )
        }

    @Test
    fun `UpdateStudent failure sets error msg`() =
        runTest {
            // Given
            val student = Student(id = 1, name = "Old Name")
            val errorMsg = "Update failed"
            every { getAllStudentsUseCase(any(), any()) } returns
                flowOf(Result.success(listOf(student)))
            coEvery { updateStudentUseCase(any()) } returns Result.failure(Exception(errorMsg))
            createViewModel()
            advanceUntilIdle()

            // When
            viewModel.onEvent(StudentsScreenEvent.UpdateStudent(student, "New Name"))
            advanceUntilIdle()

            // Then
            val expectedError = UiText.StringResource(R.string.error_updating_student, errorMsg)
            assertEquals(expectedError, viewModel.uiState.value.error)
        }

    @Test
    fun `DeleteSelectedStudents calls use case`() =
        runTest {
            // Given
            val student1 = Student(id = 1, name = "S1")
            val student2 = Student(id = 2, name = "S2")
            every { getAllStudentsUseCase(any(), any()) } returns
                flowOf(Result.success(listOf(student1, student2)))
            coEvery { deleteStudentsUseCase(any()) } returns Result.success(Unit)
            createViewModel()
            advanceUntilIdle()

            // Select students
            viewModel.onEvent(StudentsScreenEvent.ToggleStudentSelection(1))

            // When
            viewModel.onEvent(StudentsScreenEvent.DeleteSelectedStudents)
            advanceUntilIdle()

            // Then
            coVerify { deleteStudentsUseCase(match { it.size == 1 && it[0].id == 1 }) }
            assertTrue(viewModel.uiState.value.selectedStudentIds.isEmpty())
            assertEquals(
                UiText.StringResource(R.string.students_deleted_successfully),
                viewModel.uiState.value.toastMessage
            )
        }

    @Test
    fun `DeleteSelectedStudents failure sets error msg`() =
        runTest {
            // Given
            val student1 = Student(id = 1, name = "S1")
            val errorMsg = "Delete failed"
            every { getAllStudentsUseCase(any(), any()) } returns
                flowOf(Result.success(listOf(student1)))
            coEvery { deleteStudentsUseCase(any()) } returns Result.failure(Exception(errorMsg))
            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(StudentsScreenEvent.ToggleStudentSelection(1))

            // When
            viewModel.onEvent(StudentsScreenEvent.DeleteSelectedStudents)
            advanceUntilIdle()

            // Then
            val expectedError = UiText.StringResource(R.string.error_deleting_student, errorMsg)
            assertEquals(expectedError, viewModel.uiState.value.error)
        }

    @Test
    fun `PrepareImportSelectionDialog with valid Uri parses file`() =
        runTest {
            // Given
            val parsedData = listOf(ParsedStudentImportData(StudentExportData("S1", emptyList()), 1))
            every { getAllStudentsUseCase(any(), any()) } returns flowOf(Result.success(emptyList()))
            every { importStudentsUseCase.parseFile(any()) } returns flowOf(Result.success(parsedData))
            createViewModel()
            advanceUntilIdle()

            val uri = mockk<Uri>()
            every { uri.toString() } returns "content://file"

            // When
            viewModel.onEvent(StudentsScreenEvent.PrepareImportSelectionDialog(uri))
            advanceUntilIdle()

            // Then
            assertEquals(parsedData, viewModel.uiState.value.parsedStudentsFromFile)
            assertTrue(viewModel.uiState.value.showImportSelectionDialog)
        }

    @Test
    fun `PrepareImportSelectionDialog with null Uri sets toast`() =
        runTest {
            // Given
            every { getAllStudentsUseCase(any(), any()) } returns flowOf(Result.success(emptyList()))
            createViewModel()
            advanceUntilIdle()

            // When
            viewModel.onEvent(StudentsScreenEvent.PrepareImportSelectionDialog(null))

            // Then
            assertEquals(
                UiText.StringResource(R.string.file_selection_cancelled),
                viewModel.uiState.value.toastMessage
            )
        }

    @Test
    fun `PrepareImportSelectionDialog parse error sets error`() =
        runTest {
            // Given
            val errorMsg = "Parse failed"
            every { getAllStudentsUseCase(any(), any()) } returns flowOf(Result.success(emptyList()))
            every { importStudentsUseCase.parseFile(any()) } returns
                flowOf(Result.failure(Exception(errorMsg)))
            createViewModel()
            advanceUntilIdle()

            val uri = mockk<Uri>()
            every { uri.toString() } returns "uri"

            // When
            viewModel.onEvent(StudentsScreenEvent.PrepareImportSelectionDialog(uri))
            advanceUntilIdle()

            // Then
            val expectedError = UiText.StringResource(R.string.error_reading_or_parsing_file, errorMsg)
            assertEquals(expectedError, viewModel.uiState.value.error)
        }

    @Test
    fun `PerformImport calls use case`() =
        runTest {
            // Given
            val parsedData = listOf(ParsedStudentImportData(StudentExportData("S1", emptyList()), 1))

            // Mock import result
            val importResult =
                ImportResult(
                    newStudentsCount = 1,
                    existingStudentsMergedCount = 0,
                    datesSkippedCount = 0,
                    datesProcessedCount = 0
                )

            every { importStudentsUseCase.parseFile(any()) } returns flowOf(Result.success(parsedData))
            every { importStudentsUseCase.performImport(any(), any()) } returns
                flowOf(Result.success(importResult))

            createViewModel()
            advanceUntilIdle()

            val uri = mockk<Uri>()
            every { uri.toString() } returns "uri"
            viewModel.onEvent(StudentsScreenEvent.PrepareImportSelectionDialog(uri))
            advanceUntilIdle()

            // When
            viewModel.onEvent(StudentsScreenEvent.PerformImport)
            advanceUntilIdle()

            // Then
            coVerify { importStudentsUseCase.performImport(parsedData, any()) }
            assertEquals(false, viewModel.uiState.value.showImportSelectionDialog)
        }

    @Test
    fun `PerformImport failure sets error`() =
        runTest {
            // Given
            val parsedData = listOf(ParsedStudentImportData(StudentExportData("S1", emptyList()), 1))
            val errorMsg = "Import failed"

            every { importStudentsUseCase.parseFile(any()) } returns flowOf(Result.success(parsedData))
            every { importStudentsUseCase.performImport(any(), any()) } returns
                flowOf(Result.failure(Exception(errorMsg)))

            createViewModel()
            advanceUntilIdle()

            val uri = mockk<Uri>()
            every { uri.toString() } returns "uri"
            viewModel.onEvent(StudentsScreenEvent.PrepareImportSelectionDialog(uri))
            advanceUntilIdle()

            // When
            viewModel.onEvent(StudentsScreenEvent.PerformImport)
            advanceUntilIdle()

            // Then
            val expectedError = UiText.StringResource(R.string.error_during_import, errorMsg)
            assertEquals(expectedError, viewModel.uiState.value.error)
        }

    @Test
    fun `ExportSelectedStudents calls use case`() =
        runTest {
            // Given
            val student = Student(id = 1, name = "S1")
            every { getAllStudentsUseCase(any(), any()) } returns
                flowOf(Result.success(listOf(student)))
            every { exportStudentsUseCase(any(), any(), any()) } returns flowOf(Result.success(Unit))

            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(StudentsScreenEvent.ToggleStudentSelection(1))

            val uri = mockk<Uri>()
            every { uri.toString() } returns "content://export"

            // When
            viewModel.onEvent(StudentsScreenEvent.ExportSelectedStudents(uri))
            advanceUntilIdle()

            // Then
            coVerify { exportStudentsUseCase("content://export", setOf(1), listOf(student)) }
            assertEquals(
                UiText.StringResource(R.string.data_exported_successfully),
                viewModel.uiState.value.toastMessage
            )
        }

    @Test
    fun `ExportSelectedStudents failure sets error`() =
        runTest {
            // Given
            val student = Student(id = 1, name = "S1")
            val errorMsg = "Export failed"
            every { getAllStudentsUseCase(any(), any()) } returns
                flowOf(Result.success(listOf(student)))
            every { exportStudentsUseCase(any(), any(), any()) } returns
                flowOf(Result.failure(Exception(errorMsg)))

            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(StudentsScreenEvent.ToggleStudentSelection(1))

            val uri = mockk<Uri>()
            every { uri.toString() } returns "uri"

            // When
            viewModel.onEvent(StudentsScreenEvent.ExportSelectedStudents(uri))
            advanceUntilIdle()

            // Then
            val expectedError = UiText.StringResource(R.string.error_creating_export_data, errorMsg)
            assertEquals(expectedError, viewModel.uiState.value.error)
        }

    @Test
    fun `ExportAndDeleteSelectedStudents success calls export and delete`() =
        runTest {
            // Given
            val student = Student(id = 1, name = "S1")
            every { getAllStudentsUseCase(any(), any()) } returns
                flowOf(Result.success(listOf(student)))
            every { exportStudentsUseCase(any(), any(), any()) } returns flowOf(Result.success(Unit))
            coEvery { deleteStudentsUseCase(any()) } returns Result.success(Unit)

            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(StudentsScreenEvent.ToggleStudentSelection(1))

            val uri = mockk<Uri>()
            every { uri.toString() } returns "uri"

            // When
            viewModel.onEvent(StudentsScreenEvent.ExportAndDeleteSelectedStudents(uri))
            advanceUntilIdle()

            // Then
            coVerify { exportStudentsUseCase("uri", setOf(1), listOf(student)) }
            coVerify { deleteStudentsUseCase(match { it.size == 1 && it[0].id == 1 }) }
            assertEquals(
                UiText.StringResource(R.string.data_exported_and_deleted_successfully),
                viewModel.uiState.value.toastMessage
            )
        }

    @Test
    fun `ExportAndDelete failure in export sets error`() =
        runTest {
            // Given
            val student = Student(id = 1, name = "S1")
            val errorMsg = "Export failed"
            every { getAllStudentsUseCase(any(), any()) } returns
                flowOf(Result.success(listOf(student)))
            every { exportStudentsUseCase(any(), any(), any()) } returns
                flowOf(Result.failure(Exception(errorMsg)))

            createViewModel()
            advanceUntilIdle()
            viewModel.onEvent(StudentsScreenEvent.ToggleStudentSelection(1))

            val uri = mockk<Uri>()
            every { uri.toString() } returns "uri"

            // When
            viewModel.onEvent(StudentsScreenEvent.ExportAndDeleteSelectedStudents(uri))
            advanceUntilIdle()

            // Then
            coVerify(exactly = 0) { deleteStudentsUseCase(any()) }
            val expectedError = UiText.StringResource(R.string.error_creating_export_data, errorMsg)
            assertEquals(expectedError, viewModel.uiState.value.error)
        }

    @Test
    fun `ExportAndDelete failure in delete sets error`() =
        runTest {
            // Given
            val student = Student(id = 1, name = "S1")
            val errorMsg = "Delete failed"
            every { getAllStudentsUseCase(any(), any()) } returns
                flowOf(Result.success(listOf(student)))
            every { exportStudentsUseCase(any(), any(), any()) } returns flowOf(Result.success(Unit))
            coEvery { deleteStudentsUseCase(any()) } returns Result.failure(Exception(errorMsg))

            createViewModel()
            advanceUntilIdle()
            viewModel.onEvent(StudentsScreenEvent.ToggleStudentSelection(1))

            val uri = mockk<Uri>()
            every { uri.toString() } returns "uri"

            // When
            viewModel.onEvent(StudentsScreenEvent.ExportAndDeleteSelectedStudents(uri))
            advanceUntilIdle()

            // Then
            val expectedError = UiText.StringResource(R.string.error_deleting_student, errorMsg)
            assertEquals(expectedError, viewModel.uiState.value.error)
        }

    @Test
    fun `Simple state updates`() =
        runTest {
            every { getAllStudentsUseCase(any(), any()) } returns flowOf(Result.success(emptyList()))
            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(StudentsScreenEvent.UpdateNewStudentName("Name"))
            assertEquals("Name", viewModel.uiState.value.newStudentName)

            viewModel.onEvent(StudentsScreenEvent.ConsumeToastMessage)
            assertNull(viewModel.uiState.value.toastMessage)

            val student = Student(1, "S1")
            viewModel.onEvent(StudentsScreenEvent.ShowStudentDialog(student = student, show = true))
            assertTrue(viewModel.uiState.value.showAddStudentDialog)
            assertEquals(student, viewModel.uiState.value.showEditDialog)

            viewModel.onEvent(StudentsScreenEvent.ShowBulkDeleteDialog)
            assertTrue(viewModel.uiState.value.showBulkDeleteDialog)

            viewModel.onEvent(StudentsScreenEvent.DismissBulkDeleteDialog)
            assert(!viewModel.uiState.value.showBulkDeleteDialog)

            viewModel.onEvent(StudentsScreenEvent.CloseImportSelectionDialog)
            assert(!viewModel.uiState.value.showImportSelectionDialog)
            assertNull(viewModel.uiState.value.parsedStudentsFromFile)
        }
}