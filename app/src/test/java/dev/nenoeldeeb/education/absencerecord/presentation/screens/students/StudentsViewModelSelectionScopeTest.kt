package dev.nenoeldeeb.education.absencerecord.presentation.screens.students

import dev.nenoeldeeb.education.absencerecord.data.repositories.ClassFilterRepositoryImpl
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError
import io.mockk.every
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class StudentsViewModelSelectionScopeTest : StudentsViewModelTestBase() {
    private val enrolledA = Student(id = 1, name = "Enrolled A", classId = 1)
    private val enrolledB = Student(id = 2, name = "Enrolled B", classId = 2)
    private val unassigned = Student(id = 3, name = "Unassigned", classId = null)
    private val roster = listOf(enrolledA, enrolledB, unassigned)

    @BeforeEach
    fun setUp() {
        commonSetUp()
    }

    private fun setUpRoster() {
        every { getAllStudentsUseCase(any(), any()) } returns flowOf(Result.success(roster))
    }

    @Test
    fun `browse mode with full coverage still hides unassigned`() =
        runTest {
            // Given all roster classes checked but selection mode off
            setUpRoster()
            val repository = ClassFilterRepositoryImpl()
            repository.toggleClass(1)
            repository.toggleClass(2)
            createViewModel(classFilterRepository = repository)
            advanceUntilIdle()

            // Then browse filter semantics are preserved
            assertEquals(listOf(enrolledA, enrolledB), viewModel.uiState.value.allStudents)
            assertNull(viewModel.uiState.value.error)
        }

    @Test
    fun `selection mode with full coverage includes unassigned`() =
        runTest {
            // Given
            setUpRoster()
            val repository = ClassFilterRepositoryImpl()
            repository.toggleClass(1)
            repository.toggleClass(2)
            createViewModel(classFilterRepository = repository)
            advanceUntilIdle()

            // When entering selection mode
            viewModel.onEvent(StudentsScreenEvent.ToggleSelectionMode)
            advanceUntilIdle()

            // Then the whole roster is listed in roster order and selectable
            assertEquals(roster, viewModel.uiState.value.allStudents)
            assertNull(viewModel.uiState.value.error)
        }

    @Test
    fun `selection mode with partial coverage excludes unassigned`() =
        runTest {
            // Given only one class checked
            setUpRoster()
            val repository = ClassFilterRepositoryImpl()
            repository.toggleClass(1)
            createViewModel(classFilterRepository = repository)
            advanceUntilIdle()

            // When entering selection mode
            viewModel.onEvent(StudentsScreenEvent.ToggleSelectionMode)
            advanceUntilIdle()

            // Then unassigned stay hidden
            assertEquals(listOf(enrolledA), viewModel.uiState.value.allStudents)
        }

    @Test
    fun `exiting selection mode drops unassigned again`() =
        runTest {
            // Given full coverage in selection mode
            setUpRoster()
            val repository = ClassFilterRepositoryImpl()
            repository.toggleClass(1)
            repository.toggleClass(2)
            createViewModel(classFilterRepository = repository)
            advanceUntilIdle()
            viewModel.onEvent(StudentsScreenEvent.ToggleSelectionMode)
            advanceUntilIdle()
            assertTrue(unassigned in viewModel.uiState.value.allStudents)

            // When leaving selection mode
            viewModel.onEvent(StudentsScreenEvent.ToggleSelectionMode)
            advanceUntilIdle()

            // Then browse semantics return
            assertEquals(listOf(enrolledA, enrolledB), viewModel.uiState.value.allStudents)
        }

    @Test
    fun `select-all with full coverage selects unassigned too`() =
        runTest {
            // Given
            setUpRoster()
            val repository = ClassFilterRepositoryImpl()
            repository.toggleClass(1)
            repository.toggleClass(2)
            createViewModel(classFilterRepository = repository)
            advanceUntilIdle()
            viewModel.onEvent(StudentsScreenEvent.ToggleSelectionMode)
            advanceUntilIdle()

            // When toggling select-all
            viewModel.onEvent(StudentsScreenEvent.ToggleStudentsSelection)
            advanceUntilIdle()

            // Then unassigned is included for deletion or export
            assertEquals(setOf(1, 2, 3), viewModel.uiState.value.selectedStudentIds)
        }

    @Test
    fun `empty filter in selection mode still shows unassigned only`() =
        runTest {
            // Given no class checked
            setUpRoster()
            createViewModel(classFilterRepository = ClassFilterRepositoryImpl())
            advanceUntilIdle()

            // When entering selection mode
            viewModel.onEvent(StudentsScreenEvent.ToggleSelectionMode)
            advanceUntilIdle()

            // Then empty-filter semantics are preserved
            assertEquals(listOf(unassigned), viewModel.uiState.value.allStudents)
        }

    @Test
    fun `students load failure surfaces error state`() =
        runTest {
            // Given the roster flow fails
            every { getAllStudentsUseCase(any(), any()) } returns
                flowOf(Result.failure(StudentError.Database))
            createViewModel(classFilterRepository = ClassFilterRepositoryImpl())
            advanceUntilIdle()

            // Then the error is bound to state
            assertNotNull(viewModel.uiState.value.error)
        }
}