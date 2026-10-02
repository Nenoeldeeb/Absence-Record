package dev.nenoeldeeb.education.absencerecord.presentation.screens.students

import dev.nenoeldeeb.education.absencerecord.data.repositories.ClassFilterRepositoryImpl
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
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
class StudentsViewModelSearchTest : StudentsViewModelTestBase() {
    @BeforeEach
    fun setUp() {
        commonSetUp()
    }

    private fun students() =
        listOf(
            Student(id = 1, name = "Ahmed Ali", classId = null),
            Student(id = 2, name = "Sara Mohamed", classId = null),
            Student(id = 3, name = "أحمد محمد", classId = null)
        )

    @Test
    fun `UpdateSearchQuery filters by name contains case-insensitive and trimmed`() =
        runTest {
            every { getAllStudentsUseCase(any(), any()) } returns
                flowOf(Result.success(students()))
            createViewModel(classFilterRepository = ClassFilterRepositoryImpl())
            advanceUntilIdle()

            viewModel.onEvent(StudentsScreenEvent.UpdateSearchQuery("  ahmed "))
            advanceUntilIdle()

            assertEquals(
                listOf(students()[0]),
                viewModel.uiState.value.allStudents
            )
            assertNull(viewModel.uiState.value.error)
        }

    @Test
    fun `UpdateSearchQuery matches Arabic names`() =
        runTest {
            every { getAllStudentsUseCase(any(), any()) } returns
                flowOf(Result.success(students()))
            createViewModel(classFilterRepository = ClassFilterRepositoryImpl())
            advanceUntilIdle()

            viewModel.onEvent(StudentsScreenEvent.UpdateSearchQuery("أحمد"))
            advanceUntilIdle()

            assertEquals(
                listOf(students()[2]),
                viewModel.uiState.value.allStudents
            )
        }

    @Test
    fun `blank query restores scoped list`() =
        runTest {
            every { getAllStudentsUseCase(any(), any()) } returns
                flowOf(Result.success(students()))
            createViewModel(classFilterRepository = ClassFilterRepositoryImpl())
            advanceUntilIdle()
            viewModel.onEvent(StudentsScreenEvent.UpdateSearchQuery("sara"))
            advanceUntilIdle()
            assertEquals(1, viewModel.uiState.value.allStudents.size)

            viewModel.onEvent(StudentsScreenEvent.UpdateSearchQuery("   "))
            advanceUntilIdle()

            assertEquals(students(), viewModel.uiState.value.allStudents)
        }

    @Test
    fun `ToggleSearch false clears query and restores list`() =
        runTest {
            every { getAllStudentsUseCase(any(), any()) } returns
                flowOf(Result.success(students()))
            createViewModel(classFilterRepository = ClassFilterRepositoryImpl())
            advanceUntilIdle()
            viewModel.onEvent(StudentsScreenEvent.ToggleSearch(true))
            viewModel.onEvent(StudentsScreenEvent.UpdateSearchQuery("sara"))
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.isSearchActive)

            viewModel.onEvent(StudentsScreenEvent.ToggleSearch(false))
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isSearchActive)
            assertEquals("", viewModel.uiState.value.searchQuery)
            assertEquals(students(), viewModel.uiState.value.allStudents)
        }

    @Test
    fun `search applies after class filter scope`() =
        runTest {
            val enrolledSara = Student(id = 1, name = "Sara Ahmed", classId = 1)
            val unassignedSara = Student(id = 2, name = "Sara Khaled", classId = null)
            every { getAllStudentsUseCase(any(), any()) } returns
                flowOf(Result.success(listOf(enrolledSara, unassignedSara)))
            val repository = ClassFilterRepositoryImpl()
            repository.toggleClass(1)
            createViewModel(classFilterRepository = repository)
            advanceUntilIdle()
            assertEquals(listOf(enrolledSara), viewModel.uiState.value.allStudents)

            viewModel.onEvent(StudentsScreenEvent.UpdateSearchQuery("sara"))
            advanceUntilIdle()
            assertEquals(listOf(enrolledSara), viewModel.uiState.value.allStudents)

            viewModel.onEvent(StudentsScreenEvent.UpdateSearchQuery("khaled"))
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.allStudents.isEmpty())
        }
}