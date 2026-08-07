package dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail

import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentClass
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText
import io.mockk.every
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class StudentDetailViewModelLoadTest : StudentDetailViewModelTestBase() {
    @BeforeEach
    fun setUp() {
        commonSetUp()
    }

    @Test
    fun `load success sets student dates and marks loaded`() =
        runTest {
            // Given
            val student = Student(id = 1, name = "S1", classId = 2)
            val classes = listOf(StudentClass(id = 2, name = "Class B"))
            val attendanceDates = listOf(LocalDate(2023, 1, 15))
            every { getStudentByIdUseCase(1) } returns flowOf(Result.success(student))
            every { classManagementUseCases.getAllClassesUseCase() } returns
                flowOf(Result.success(classes))
            every { getStudentAttendanceDatesUseCase(1) } returns flowOf(Result.success(attendanceDates))

            // When
            createViewModel(studentId = 1)
            advanceUntilIdle()

            // Then
            assertEquals(student, viewModel.uiState.value.student)
            assertEquals(classes, viewModel.uiState.value.availableClasses)
            assertEquals("Class B", viewModel.uiState.value.assignedClassName)
            assertEquals(attendanceDates, viewModel.uiState.value.allAttendanceDates)
            assertFalse(viewModel.uiState.value.isLoading)
            assertNull(viewModel.uiState.value.error)
        }

    @Test
    fun `load success with null student is handled without crash`() =
        runTest {
            // Given
            every { getStudentByIdUseCase(1) } returns flowOf(Result.success(null))

            // When
            createViewModel(studentId = 1)
            advanceUntilIdle()

            // Then
            assertNull(viewModel.uiState.value.student)
            assertFalse(viewModel.uiState.value.isLoading)
            assertNull(viewModel.uiState.value.error)
        }

    @Test
    fun `load failure sets error and marks loaded`() =
        runTest {
            // Given
            every { getStudentByIdUseCase(1) } returns flowOf(Result.failure(StudentError.Database))

            // When
            createViewModel(studentId = 1)
            advanceUntilIdle()

            // Then
            assertEquals(
                UiText.StringResource(R.string.error_database_operation_failed),
                viewModel.uiState.value.error
            )
            assertFalse(viewModel.uiState.value.isLoading)
            assertNull(viewModel.uiState.value.student)
        }

    @Test
    fun `attendance dates failure sets error`() =
        runTest {
            // Given
            every { getStudentByIdUseCase(1) } returns flowOf(Result.success(aStudent))
            every { getStudentAttendanceDatesUseCase(1) } returns
                flowOf(Result.failure(StudentError.Database))

            // When
            createViewModel(studentId = 1)
            advanceUntilIdle()

            // Then
            assertEquals(aStudent, viewModel.uiState.value.student)
            assertTrue(viewModel.uiState.value.error != null)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `assigned class name resolves from available classes`() =
        runTest {
            // Given
            val student = Student(id = 7, name = "S7", classId = 5)
            every { getStudentByIdUseCase(7) } returns flowOf(Result.success(student))
            every { classManagementUseCases.getAllClassesUseCase() } returns
                flowOf(
                    Result.success(
                        listOf(
                            StudentClass(id = 5, name = "Class Five"),
                            StudentClass(id = 6, name = "Class Six")
                        )
                    )
                )

            // When
            createViewModel(studentId = 7)
            advanceUntilIdle()

            // Then
            assertEquals("Class Five", viewModel.uiState.value.assignedClassName)
        }
}