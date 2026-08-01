package dev.nenoeldeeb.education.absencerecord.presentation.screens.calendar

import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.data.repositories.ClassFilterRepositoryImpl
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentClass
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError
import dev.nenoeldeeb.education.absencerecord.domain.usecases.ClassManagementUseCases
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest : CalendarViewModelTestBase() {
    @BeforeEach
    fun setUp() {
        commonSetUp()
    }

    // region Multi-Select Filter Tests

    @Test
    fun `ToggleClassSelection filters students by single class`() =
        runTest {
            val class1 = StudentClass(1, "Class A")
            val class2 = StudentClass(2, "Class B")
            val students =
                listOf(
                    Student(1, "Student1", classId = 1),
                    Student(2, "Student2", classId = 1),
                    Student(3, "Student3", classId = 2)
                )
            val classes = listOf(class1, class2)

            coEvery { studentManagementUseCases.getAllStudentsUseCase() } returns flowOf(Result.success(students))
            val classManagementUseCases = mockk<ClassManagementUseCases>(relaxed = true)
            coEvery { classManagementUseCases.getAllClassesUseCase() } returns flowOf(Result.success(classes))
            coEvery { getAttendanceForDateUseCase(any()) } returns flowOf(Result.success(emptyList()))

            viewModel =
                CalendarViewModel(
                    attendanceUseCases,
                    studentManagementUseCases,
                    classManagementUseCases,
                    ClassFilterRepositoryImpl()
                )
            advanceUntilIdle()

            viewModel.onEvent(CalendarScreenEvent.ToggleClassSelection(1))
            advanceUntilIdle()

            assertEquals(2, viewModel.uiState.value.allStudents.size)
            assertEquals(setOf(1), viewModel.uiState.value.selectedClassIds)
        }

    @Test
    fun `ToggleClassSelection with multiple classes merges and deduplicates`() =
        runTest {
            val class1 = StudentClass(1, "Class A")
            val class2 = StudentClass(2, "Class B")
            val students =
                listOf(
                    Student(1, "S1", classId = 1),
                    Student(2, "S2", classId = 1),
                    Student(3, "S3", classId = 2),
                    Student(4, "S4", classId = 2)
                )
            val classes = listOf(class1, class2)

            coEvery { studentManagementUseCases.getAllStudentsUseCase() } returns flowOf(Result.success(students))
            val classManagementUseCases = mockk<ClassManagementUseCases>(relaxed = true)
            coEvery { classManagementUseCases.getAllClassesUseCase() } returns flowOf(Result.success(classes))
            coEvery { getAttendanceForDateUseCase(any()) } returns flowOf(Result.success(emptyList()))

            viewModel =
                CalendarViewModel(
                    attendanceUseCases,
                    studentManagementUseCases,
                    classManagementUseCases,
                    ClassFilterRepositoryImpl()
                )
            advanceUntilIdle()

            viewModel.onEvent(CalendarScreenEvent.ToggleClassSelection(1))
            advanceUntilIdle()
            viewModel.onEvent(CalendarScreenEvent.ToggleClassSelection(2))
            advanceUntilIdle()

            assertEquals(4, viewModel.uiState.value.allStudents.size)
            assertEquals(setOf(1, 2), viewModel.uiState.value.selectedClassIds)
        }

    @Test
    fun `unchecking all classes shows only unassigned students`() =
        runTest {
            val class1 = StudentClass(1, "Class A")
            val students =
                listOf(
                    Student(1, "S1", classId = 1),
                    Student(2, "S2", classId = null)
                )
            val classes = listOf(class1)

            coEvery { studentManagementUseCases.getAllStudentsUseCase() } returns flowOf(Result.success(students))
            val classManagementUseCases = mockk<ClassManagementUseCases>(relaxed = true)
            coEvery { classManagementUseCases.getAllClassesUseCase() } returns flowOf(Result.success(classes))
            coEvery { getAttendanceForDateUseCase(any()) } returns flowOf(Result.success(emptyList()))

            viewModel =
                CalendarViewModel(
                    attendanceUseCases,
                    studentManagementUseCases,
                    classManagementUseCases,
                    ClassFilterRepositoryImpl()
                )
            advanceUntilIdle()

            viewModel.onEvent(CalendarScreenEvent.ToggleClassSelection(1))
            advanceUntilIdle()
            assertEquals(1, viewModel.uiState.value.allStudents.size)

            viewModel.onEvent(CalendarScreenEvent.ToggleClassSelection(1))
            advanceUntilIdle()

            assertEquals(1, viewModel.uiState.value.allStudents.size)
            assertEquals(setOf(2), viewModel.uiState.value.allStudents.map { it.id }.toSet())
            assertEquals(emptySet(), viewModel.uiState.value.selectedClassIds)
        }

    @Test
    fun `ToggleClassSelection shows error on students load failure`() =
        runTest {
            coEvery { studentManagementUseCases.getAllStudentsUseCase() } returns flowOf(Result.failure(StudentError.Database))
            coEvery { getAttendanceForDateUseCase(any()) } returns flowOf(Result.success(emptyList()))

            viewModel =
                CalendarViewModel(
                    attendanceUseCases,
                    studentManagementUseCases,
                    mockk<ClassManagementUseCases>(relaxed = true),
                    ClassFilterRepositoryImpl()
                )
            advanceUntilIdle()

            assertEquals(
                UiText.StringResource(R.string.error_database_operation_failed),
                viewModel.uiState.value.error
            )
        }

    @Test
    fun `selecting empty class shows zero students`() =
        runTest {
            val class1 = StudentClass(1, "Empty Class")
            val students =
                listOf(
                    Student(1, "S1", classId = 2)
                )
            val classes = listOf(class1)

            coEvery { studentManagementUseCases.getAllStudentsUseCase() } returns flowOf(Result.success(students))
            val classManagementUseCases = mockk<ClassManagementUseCases>(relaxed = true)
            coEvery { classManagementUseCases.getAllClassesUseCase() } returns flowOf(Result.success(classes))
            coEvery { getAttendanceForDateUseCase(any()) } returns flowOf(Result.success(emptyList()))

            viewModel =
                CalendarViewModel(
                    attendanceUseCases,
                    studentManagementUseCases,
                    classManagementUseCases,
                    ClassFilterRepositoryImpl()
                )
            advanceUntilIdle()

            viewModel.onEvent(CalendarScreenEvent.ToggleClassSelection(1))
            advanceUntilIdle()

            assertEquals(0, viewModel.uiState.value.allStudents.size)
            assertEquals(setOf(1), viewModel.uiState.value.selectedClassIds)
        }

    // endregion
}