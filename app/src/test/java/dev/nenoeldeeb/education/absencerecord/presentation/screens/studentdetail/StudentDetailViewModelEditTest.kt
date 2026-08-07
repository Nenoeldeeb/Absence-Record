package dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail

import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentClass
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText
import io.mockk.coEvery
import io.mockk.every
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class StudentDetailViewModelEditTest : StudentDetailViewModelTestBase() {
    @BeforeEach
    fun setUp() {
        commonSetUp()
    }

    @Test
    fun `UpdateStudentName success updates student name and closes dialog`() =
        runTest {
            // Given
            val student = Student(id = 1, name = "Old Name", classId = 2)
            every { getStudentByIdUseCase(1) } returns flowOf(Result.success(student))
            every { classManagementUseCases.getAllClassesUseCase() } returns
                flowOf(Result.success(listOf(StudentClass(id = 2, name = "Class B"))))
            coEvery { updateStudentUseCase(any()) } returns Result.success(Unit)
            createViewModel(studentId = 1)
            advanceUntilIdle()

            // When
            viewModel.onEvent(StudentDetailScreenEvent.ToggleEditNameDialog)
            viewModel.onEvent(StudentDetailScreenEvent.UpdateStudentName("  New Name  "))
            advanceUntilIdle()

            // Then
            assertEquals("New Name", viewModel.uiState.value.student?.name)
            assertFalse(viewModel.uiState.value.isEditNameDialogOpen)
            assertNull(viewModel.uiState.value.error)
        }

    @Test
    fun `UpdateStudentName with blank name keeps dialog open`() =
        runTest {
            // Given
            every { getStudentByIdUseCase(1) } returns flowOf(Result.success(aStudent))
            createViewModel(studentId = 1)
            advanceUntilIdle()
            viewModel.onEvent(StudentDetailScreenEvent.ToggleEditNameDialog)

            // When
            viewModel.onEvent(StudentDetailScreenEvent.UpdateStudentName("   "))
            advanceUntilIdle()

            // Then
            assertTrue(viewModel.uiState.value.isEditNameDialogOpen)
            assertEquals("S1", viewModel.uiState.value.student?.name)
        }

    @Test
    fun `UpdateStudentName failure sets error`() =
        runTest {
            // Given
            every { getStudentByIdUseCase(1) } returns flowOf(Result.success(aStudent))
            coEvery { updateStudentUseCase(any()) } returns Result.failure(StudentError.Database)
            createViewModel(studentId = 1)
            advanceUntilIdle()

            // When
            viewModel.onEvent(StudentDetailScreenEvent.ToggleEditNameDialog)
            viewModel.onEvent(StudentDetailScreenEvent.UpdateStudentName("Renamed"))
            advanceUntilIdle()

            // Then
            assertEquals(
                UiText.StringResource(R.string.error_database_operation_failed),
                viewModel.uiState.value.error
            )
            assertEquals("S1", viewModel.uiState.value.student?.name)
        }

    @Test
    fun `UpdateStudentClass success updates class and recomputes assigned class name`() =
        runTest {
            // Given
            val student = Student(id = 1, name = "S1", classId = 2)
            val classes =
                listOf(
                    StudentClass(id = 2, name = "Class B"),
                    StudentClass(id = 5, name = "Class Five")
                )
            every { getStudentByIdUseCase(1) } returns flowOf(Result.success(student))
            every { classManagementUseCases.getAllClassesUseCase() } returns
                flowOf(Result.success(classes))
            coEvery { updateStudentUseCase(any()) } returns Result.success(Unit)
            createViewModel(studentId = 1)
            advanceUntilIdle()
            assertEquals("Class B", viewModel.uiState.value.assignedClassName)

            // When
            viewModel.onEvent(StudentDetailScreenEvent.ToggleChangeClassDialog)
            viewModel.onEvent(StudentDetailScreenEvent.UpdateStudentClass(5))
            advanceUntilIdle()

            // Then
            assertEquals(5, viewModel.uiState.value.student?.classId)
            assertEquals("Class Five", viewModel.uiState.value.assignedClassName)
            assertFalse(viewModel.uiState.value.isChangeClassDialogOpen)
        }

    @Test
    fun `UpdateStudentClass to null clears assigned class name`() =
        runTest {
            // Given
            val student = Student(id = 1, name = "S1", classId = 2)
            every { getStudentByIdUseCase(1) } returns flowOf(Result.success(student))
            every { classManagementUseCases.getAllClassesUseCase() } returns
                flowOf(Result.success(listOf(StudentClass(id = 2, name = "Class B"))))
            coEvery { updateStudentUseCase(any()) } returns Result.success(Unit)
            createViewModel(studentId = 1)
            advanceUntilIdle()

            // When
            viewModel.onEvent(StudentDetailScreenEvent.ToggleChangeClassDialog)
            viewModel.onEvent(StudentDetailScreenEvent.UpdateStudentClass(null))
            advanceUntilIdle()

            // Then
            assertNull(viewModel.uiState.value.student?.classId)
            assertNull(viewModel.uiState.value.assignedClassName)
        }

    @Test
    fun `UpdateStudentClass failure sets error and keeps class`() =
        runTest {
            // Given
            every { getStudentByIdUseCase(1) } returns flowOf(Result.success(aStudent))
            every { classManagementUseCases.getAllClassesUseCase() } returns
                flowOf(Result.success(listOf(StudentClass(id = 3, name = "Class C"))))
            coEvery { updateStudentUseCase(any()) } returns Result.failure(StudentError.Database)
            createViewModel(studentId = 1)
            advanceUntilIdle()

            // When
            viewModel.onEvent(StudentDetailScreenEvent.ToggleChangeClassDialog)
            viewModel.onEvent(StudentDetailScreenEvent.UpdateStudentClass(3))
            advanceUntilIdle()

            // Then
            assertEquals(
                UiText.StringResource(R.string.error_database_operation_failed),
                viewModel.uiState.value.error
            )
            assertEquals(2, viewModel.uiState.value.student?.classId)
        }
}