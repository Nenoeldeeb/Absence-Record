package dev.nenoeldeeb.education.absencerecord.presentation.screens.students

import android.net.Uri
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError
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
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class StudentsViewModelExportTest : StudentsViewModelTestBase() {
    @BeforeEach
    fun setUp() {
        commonSetUp()
    }

    @Test
    fun `ExportSelectedStudents calls use case`() =
        runTest {
            // Given
            val student = Student(id = 1, name = "S1")
            every { getAllStudentsUseCase(any(), any()) } returns
                flowOf(Result.success(listOf(student)))
            coEvery { exportStudentsUseCase(any(), any(), any()) } returns Result.success(Unit)

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
            every { getAllStudentsUseCase(any(), any()) } returns
                flowOf(Result.success(listOf(student)))
            coEvery { exportStudentsUseCase(any(), any(), any()) } returns
                Result.failure(StudentError.FileWrite)

            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(StudentsScreenEvent.ToggleStudentSelection(1))

            val uri = mockk<Uri>()
            every { uri.toString() } returns "uri"

            // When
            viewModel.onEvent(StudentsScreenEvent.ExportSelectedStudents(uri))
            advanceUntilIdle()

            // Then
            assertEquals(
                UiText.StringResource(R.string.error_writing_file),
                viewModel.uiState.value.error
            )
        }

    @Test
    fun `ExportAndDeleteSelectedStudents success calls export and delete`() =
        runTest {
            // Given
            val student = Student(id = 1, name = "S1")
            every { getAllStudentsUseCase(any(), any()) } returns
                flowOf(Result.success(listOf(student)))
            coEvery { exportStudentsUseCase(any(), any(), any()) } returns Result.success(Unit)
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
            every { getAllStudentsUseCase(any(), any()) } returns
                flowOf(Result.success(listOf(student)))
            coEvery { exportStudentsUseCase(any(), any(), any()) } returns
                Result.failure(StudentError.FileWrite)

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
            assertEquals(
                UiText.StringResource(R.string.error_writing_file),
                viewModel.uiState.value.error
            )
        }

    @Test
    fun `ExportAndDelete failure in delete sets error`() =
        runTest {
            // Given
            val student = Student(id = 1, name = "S1")
            every { getAllStudentsUseCase(any(), any()) } returns
                flowOf(Result.success(listOf(student)))
            coEvery { exportStudentsUseCase(any(), any(), any()) } returns Result.success(Unit)
            coEvery { deleteStudentsUseCase(any()) } returns Result.failure(StudentError.Database)

            createViewModel()
            advanceUntilIdle()
            viewModel.onEvent(StudentsScreenEvent.ToggleStudentSelection(1))

            val uri = mockk<Uri>()
            every { uri.toString() } returns "uri"

            // When
            viewModel.onEvent(StudentsScreenEvent.ExportAndDeleteSelectedStudents(uri))
            advanceUntilIdle()

            // Then
            assertEquals(
                UiText.StringResource(R.string.error_database_operation_failed),
                viewModel.uiState.value.error
            )
        }
}