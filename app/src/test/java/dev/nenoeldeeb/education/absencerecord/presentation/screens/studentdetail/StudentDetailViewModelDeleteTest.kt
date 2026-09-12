package dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail

import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText
import io.mockk.coEvery
import io.mockk.every
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class StudentDetailViewModelDeleteTest : StudentDetailViewModelTestBase() {
    @BeforeEach
    fun setUp() {
        commonSetUp()
    }

    @Test
    fun `delete success emits navigate back effect`() =
        runTest {
            // Given
            every { getStudentByIdUseCase(1) } returns flowOf(Result.success(aStudent))
            coEvery { deleteStudentsUseCase(any()) } returns Result.success(Unit)
            createViewModel(studentId = 1)
            advanceUntilIdle()

            val effects = mutableListOf<StudentDetailUiEffect>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiEffect.collect { effects.add(it) }
            }

            // When
            viewModel.onEvent(StudentDetailScreenEvent.ToggleDeleteConfirmationDialog)
            viewModel.onEvent(StudentDetailScreenEvent.ConfirmDeleteStudent)
            advanceUntilIdle()

            // Then
            assertEquals(
                listOf<StudentDetailUiEffect>(StudentDetailUiEffect.NavigateBack),
                effects
            )
            assertFalse(viewModel.uiState.value.isDeleteConfirmationDialogOpen)
            assertNull(viewModel.uiState.value.error)
        }

    @Test
    fun `delete failure sets error and emits no navigation effect`() =
        runTest {
            // Given
            every { getStudentByIdUseCase(1) } returns flowOf(Result.success(aStudent))
            coEvery { deleteStudentsUseCase(any()) } returns Result.failure(StudentError.Database)
            createViewModel(studentId = 1)
            advanceUntilIdle()

            val effects = mutableListOf<StudentDetailUiEffect>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiEffect.collect { effects.add(it) }
            }

            // When
            viewModel.onEvent(StudentDetailScreenEvent.ToggleDeleteConfirmationDialog)
            viewModel.onEvent(StudentDetailScreenEvent.ConfirmDeleteStudent)
            advanceUntilIdle()

            // Then
            assertTrue(effects.isEmpty())
            assertEquals(
                UiText.StringResource(R.string.error_database_operation_failed),
                viewModel.uiState.value.error
            )
        }
}