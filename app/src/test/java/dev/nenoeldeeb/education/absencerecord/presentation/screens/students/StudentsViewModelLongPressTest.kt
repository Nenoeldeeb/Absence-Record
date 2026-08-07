package dev.nenoeldeeb.education.absencerecord.presentation.screens.students

import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import io.mockk.every
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class StudentsViewModelLongPressTest : StudentsViewModelTestBase() {
    @BeforeEach
    fun setUp() {
        commonSetUp()
    }

    @Test
    fun `long press enters selection mode and selects the long pressed student`() =
        runTest {
            // Given
            val students = listOf(Student(id = 1, name = "S1"), Student(id = 2, name = "S2"))
            every { getAllStudentsUseCase(any(), any()) } returns flowOf(Result.success(students))
            createViewModel()
            advanceUntilIdle()

            // When: the screen dispatches these events on long-press
            handleStudentLongPressBehavior(
                isMultiSelectionMode = viewModel.uiState.value.isMultiSelectionMode,
                studentId = 1,
                onEnterSelectionMode = {
                    viewModel.onEvent(StudentsScreenEvent.ToggleSelectionMode)
                },
                onSelectStudent = {
                    viewModel.onEvent(StudentsScreenEvent.ToggleStudentSelection(it))
                }
            )
            advanceUntilIdle()

            // Then: selection mode active, long-pressed student selected
            assertTrue(viewModel.uiState.value.isMultiSelectionMode)
            assertEquals(setOf(1), viewModel.uiState.value.selectedStudentIds)
        }

    @Test
    fun `tap after long press toggles selection without navigating`() =
        runTest {
            // Given: long press entered selection mode with student 1 selected
            val students = listOf(Student(id = 1, name = "S1"), Student(id = 2, name = "S2"))
            every { getAllStudentsUseCase(any(), any()) } returns flowOf(Result.success(students))
            createViewModel()
            advanceUntilIdle()

            handleStudentLongPressBehavior(
                isMultiSelectionMode = viewModel.uiState.value.isMultiSelectionMode,
                studentId = 1,
                onEnterSelectionMode = {
                    viewModel.onEvent(StudentsScreenEvent.ToggleSelectionMode)
                },
                onSelectStudent = {
                    viewModel.onEvent(StudentsScreenEvent.ToggleStudentSelection(it))
                }
            )

            val navigatedIds = mutableListOf<Int>()

            // When: teacher taps another student in selection mode
            viewModel.onEvent(StudentsScreenEvent.ToggleStudentSelection(2))
            handleStudentClickBehavior(
                isMultiSelectionMode = viewModel.uiState.value.isMultiSelectionMode,
                student = Student(id = 3, name = "S3"),
                onNavigateToDetail = { navigatedIds.add(it) },
                onToggleStudentSelection = {
                    viewModel.onEvent(StudentsScreenEvent.ToggleStudentSelection(it))
                }
            )
            advanceUntilIdle()

            // Then: both toggled students selected, no navigation
            assertEquals(setOf(1, 2, 3), viewModel.uiState.value.selectedStudentIds)
            assertTrue(navigatedIds.isEmpty())
            assertTrue(viewModel.uiState.value.isMultiSelectionMode)
        }

    @Test
    fun `long press while already in selection mode does not alter selection`() =
        runTest {
            // Given
            val students = listOf(Student(id = 1, name = "S1"))
            every { getAllStudentsUseCase(any(), any()) } returns flowOf(Result.success(students))
            createViewModel()
            advanceUntilIdle()

            var enterSelectionModeCalls = 0

            // When: additional long-presses while already in selection mode
            handleStudentLongPressBehavior(
                isMultiSelectionMode = true,
                studentId = 1,
                onEnterSelectionMode = { enterSelectionModeCalls++ },
                onSelectStudent = {
                    viewModel.onEvent(StudentsScreenEvent.ToggleStudentSelection(it))
                }
            )

            // Then: selection mode entry is not re-triggered
            assertEquals(0, enterSelectionModeCalls)
            assertTrue(viewModel.uiState.value.selectedStudentIds.isEmpty())
        }

    @Test
    fun `long press does not trigger the single-tap navigation path`() =
        runTest {
            // Given
            val students = listOf(Student(id = 1, name = "S1"))
            every { getAllStudentsUseCase(any(), any()) } returns flowOf(Result.success(students))
            createViewModel()
            advanceUntilIdle()

            val navigatedIds = mutableListOf<Int>()

            // Where: a long press would navigate if the tap path were accidentally triggered
            handleStudentLongPressBehavior(
                isMultiSelectionMode = viewModel.uiState.value.isMultiSelectionMode,
                studentId = 1,
                onEnterSelectionMode = {
                    viewModel.onEvent(StudentsScreenEvent.ToggleSelectionMode)
                },
                onSelectStudent = {
                    viewModel.onEvent(StudentsScreenEvent.ToggleStudentSelection(it))
                }
            )

            // Simulate a subsequent tap: it must toggle, not navigate
            handleStudentClickBehavior(
                isMultiSelectionMode = viewModel.uiState.value.isMultiSelectionMode,
                student = Student(id = 1, name = "S1"),
                onNavigateToDetail = { navigatedIds.add(it) },
                onToggleStudentSelection = {
                    viewModel.onEvent(StudentsScreenEvent.ToggleStudentSelection(it))
                }
            )
            advanceUntilIdle()

            // Then: the tapped student was toggled off (it was selected), never navigated
            assertTrue(navigatedIds.isEmpty())
            assertTrue(viewModel.uiState.value.selectedStudentIds.isEmpty())
        }
}