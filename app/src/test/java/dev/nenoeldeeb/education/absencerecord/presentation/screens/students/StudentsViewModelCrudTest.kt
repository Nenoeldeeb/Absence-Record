package dev.nenoeldeeb.education.absencerecord.presentation.screens.students

import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class StudentsViewModelCrudTest : StudentsViewModelTestBase() {
    @BeforeEach
    fun setUp() {
        commonSetUp()
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
            every { getAllStudentsUseCase(any(), any()) } returns
                flowOf(Result.failure(StudentError.Database))

            // When
            createViewModel()
            advanceUntilIdle()

            // Then
            assertEquals(emptyList(), viewModel.uiState.value.allStudents)
            assertEquals(
                UiText.StringResource(R.string.error_database_operation_failed),
                viewModel.uiState.value.error
            )
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
            viewModel.onEvent(
                StudentsScreenEvent.AddStudent(
                    "New Student",
                    classId = null
                )
            )
            advanceUntilIdle()

            // Then
            coVerify { addStudentUseCase(match { it.name == "New Student" }) }
            assertEquals(
                UiText.StringResource(R.string.student_added, "New Student"),
                viewModel.uiState.value.toastMessage
            )
        }

    @Test
    fun `AddStudent event with empty name sets error`() =
        runTest {
            // Given
            every { getAllStudentsUseCase(any(), any()) } returns flowOf(Result.success(emptyList()))
            createViewModel()
            advanceUntilIdle()

            // When
            viewModel.onEvent(
                StudentsScreenEvent.AddStudent(
                    "  ",
                    classId = null
                )
            )

            // Then
            coVerify(exactly = 0) { addStudentUseCase(any()) }
            assertEquals(
                UiText.DynamicString("Name must not be blank"),
                viewModel.uiState.value.error
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
            viewModel.onEvent(
                StudentsScreenEvent.UpdateStudent(
                    student,
                    "New Name",
                    newClassId = null
                )
            )
            advanceUntilIdle()

            // Then
            coVerify { updateStudentUseCase(match { it.id == 1 && it.name == "New Name" }) }
            assertEquals(
                UiText.StringResource(R.string.student_updated, "New Name"),
                viewModel.uiState.value.toastMessage
            )
        }

    @Test
    fun `UpdateStudent with empty name sets error`() =
        runTest {
            // Given
            val student = Student(id = 1, name = "Old Name")
            every { getAllStudentsUseCase(any(), any()) } returns
                flowOf(Result.success(listOf(student)))
            createViewModel()
            advanceUntilIdle()

            // When
            viewModel.onEvent(
                StudentsScreenEvent.UpdateStudent(
                    student,
                    "  ",
                    newClassId = null
                )
            )
            advanceUntilIdle()

            // Then
            coVerify(exactly = 0) { updateStudentUseCase(any()) }
            assertEquals(
                UiText.DynamicString("Name must not be blank"),
                viewModel.uiState.value.error
            )
        }

    @Test
    fun `UpdateStudent failure sets error msg`() =
        runTest {
            // Given
            val student = Student(id = 1, name = "Old Name")
            every { getAllStudentsUseCase(any(), any()) } returns
                flowOf(Result.success(listOf(student)))
            coEvery { updateStudentUseCase(any()) } returns Result.failure(StudentError.Database)
            createViewModel()
            advanceUntilIdle()

            // When
            viewModel.onEvent(
                StudentsScreenEvent.UpdateStudent(
                    student,
                    "New Name",
                    newClassId = null
                )
            )
            advanceUntilIdle()

            // Then
            assertEquals(
                UiText.StringResource(R.string.error_database_operation_failed),
                viewModel.uiState.value.error
            )
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
            every { getAllStudentsUseCase(any(), any()) } returns
                flowOf(Result.success(listOf(student1)))
            coEvery { deleteStudentsUseCase(any()) } returns Result.failure(StudentError.Database)
            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(StudentsScreenEvent.ToggleStudentSelection(1))

            // When
            viewModel.onEvent(StudentsScreenEvent.DeleteSelectedStudents)
            advanceUntilIdle()

            // Then
            assertEquals(
                UiText.StringResource(R.string.error_database_operation_failed),
                viewModel.uiState.value.error
            )
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