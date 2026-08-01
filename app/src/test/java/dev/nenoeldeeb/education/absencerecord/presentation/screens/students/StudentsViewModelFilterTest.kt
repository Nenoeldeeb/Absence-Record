package dev.nenoeldeeb.education.absencerecord.presentation.screens.students

import dev.nenoeldeeb.education.absencerecord.data.repositories.ClassFilterRepositoryImpl
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentClass
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class StudentsViewModelFilterTest : StudentsViewModelTestBase() {
    @BeforeEach
    fun setUp() {
        commonSetUp()
    }

    @Test
    fun `filter sync updates selectedClassIds and filters students when class toggled`() =
        runTest {
            // Given
            val classId = 1
            val enrolled = Student(id = 1, name = "Enrolled", classId = classId)
            val unassigned = Student(id = 2, name = "Unassigned", classId = null)
            every { getAllStudentsUseCase(any(), any()) } returns
                flowOf(Result.success(listOf(enrolled, unassigned)))
            val repository = ClassFilterRepositoryImpl()
            createViewModel(classFilterRepository = repository)
            advanceUntilIdle()

            // When
            repository.toggleClass(classId)
            advanceUntilIdle()

            // Then
            assertEquals(setOf(classId), viewModel.uiState.value.selectedClassIds)
            assertEquals(listOf(enrolled), viewModel.uiState.value.allStudents)
        }

    @Test
    fun `unchecking all classes shows only unassigned students`() =
        runTest {
            // Given
            val enrolled = Student(id = 1, name = "Enrolled", classId = 1)
            val unassigned = Student(id = 2, name = "Unassigned", classId = null)
            every { getAllStudentsUseCase(any(), any()) } returns
                flowOf(Result.success(listOf(enrolled, unassigned)))
            val repository = ClassFilterRepositoryImpl()
            repository.toggleClass(1)
            createViewModel(classFilterRepository = repository)
            advanceUntilIdle()
            assertEquals(listOf(enrolled), viewModel.uiState.value.allStudents)

            // When
            repository.toggleClass(1)
            advanceUntilIdle()

            // Then
            assertTrue(viewModel.uiState.value.selectedClassIds.isEmpty())
            assertEquals(listOf(unassigned), viewModel.uiState.value.allStudents)
        }

    @Test
    fun `ToggleClassFilter event delegates to repository`() =
        runTest {
            // Given
            every { getAllStudentsUseCase(any(), any()) } returns
                flowOf(Result.success(emptyList<Student>()))
            val repository = ClassFilterRepositoryImpl()
            createViewModel(classFilterRepository = repository)
            advanceUntilIdle()

            // When
            viewModel.onEvent(StudentsScreenEvent.ToggleClassFilter(1))
            advanceUntilIdle()

            // Then
            assertTrue(1 in viewModel.uiState.value.selectedClassIds)
        }

    @Test
    fun `DeleteClass removes the class id from the filter`() =
        runTest {
            // Given
            val classId = 1
            val studentClass = StudentClass(id = classId, name = "Class A")
            every { getAllStudentsUseCase(any(), any()) } returns
                flowOf(Result.success(emptyList<Student>()))
            coEvery { deleteClassUseCase(any()) } returns Result.success(Unit)
            val repository = ClassFilterRepositoryImpl()
            repository.toggleClass(classId)
            createViewModel(classFilterRepository = repository)
            advanceUntilIdle()
            assertTrue(classId in viewModel.uiState.value.selectedClassIds)

            // When
            viewModel.onEvent(StudentsScreenEvent.DeleteClass(studentClass))
            advanceUntilIdle()

            // Then
            assertFalse(classId in viewModel.uiState.value.selectedClassIds)
        }
}