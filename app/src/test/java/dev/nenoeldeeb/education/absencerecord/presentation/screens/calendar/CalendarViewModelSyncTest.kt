package dev.nenoeldeeb.education.absencerecord.presentation.screens.calendar

import dev.nenoeldeeb.education.absencerecord.data.repositories.ClassFilterRepositoryImpl
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentAttendance
import io.mockk.coEvery
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelSyncTest : CalendarViewModelTestBase() {
    @BeforeEach
    fun setUp() {
        commonSetUp()
    }

    @Test
    fun `filter syncs when repository toggled externally`() =
        runTest {
            val students =
                listOf(
                    Student(1, "S1", classId = 1),
                    Student(2, "S2", classId = 1),
                    Student(3, "S3", classId = null)
                )

            coEvery { studentManagementUseCases.getAllStudentsUseCase() } returns flowOf(Result.success(students))
            coEvery { getAttendanceForDateUseCase(any()) } returns flowOf(Result.success(emptyList()))

            val classFilterRepository = ClassFilterRepositoryImpl()
            createViewModel(classFilterRepository)
            advanceUntilIdle()

            assertEquals(setOf(3), viewModel.uiState.value.allStudents.map { it.id }.toSet())

            classFilterRepository.toggleClass(1)
            advanceUntilIdle()

            assertEquals(setOf(1), viewModel.uiState.value.selectedClassIds)
            assertEquals(setOf(1, 2), viewModel.uiState.value.allStudents.map { it.id }.toSet())
        }

    @Test
    fun `unchecking all classes via repository shows only unassigned students`() =
        runTest {
            val students =
                listOf(
                    Student(1, "S1", classId = 1),
                    Student(2, "S2", classId = null)
                )

            coEvery { studentManagementUseCases.getAllStudentsUseCase() } returns flowOf(Result.success(students))
            coEvery { getAttendanceForDateUseCase(any()) } returns flowOf(Result.success(emptyList()))

            val classFilterRepository = ClassFilterRepositoryImpl()
            createViewModel(classFilterRepository)
            advanceUntilIdle()

            classFilterRepository.toggleClass(1)
            advanceUntilIdle()
            assertEquals(setOf(1), viewModel.uiState.value.allStudents.map { it.id }.toSet())

            classFilterRepository.toggleClass(1)
            advanceUntilIdle()

            assertEquals(emptySet(), viewModel.uiState.value.selectedClassIds)
            assertEquals(setOf(2), viewModel.uiState.value.allStudents.map { it.id }.toSet())
        }

    @Test
    fun `attendance count is clamped when repository filter removes students`() =
        runTest {
            val date = LocalDate(2026, Month.MARCH, 1)
            val students =
                listOf(
                    Student(1, "S1", classId = 1),
                    Student(2, "S2", classId = 1),
                    Student(3, "S3", classId = 2)
                )
            val attendance =
                listOf(
                    StudentAttendance(studentId = 1, date = date),
                    StudentAttendance(studentId = 2, date = date),
                    StudentAttendance(studentId = 3, date = date)
                )

            coEvery { studentManagementUseCases.getAllStudentsUseCase() } returns flowOf(Result.success(students))
            coEvery { getAttendanceForDateUseCase(date) } returns flowOf(Result.success(attendance))

            val classFilterRepository = ClassFilterRepositoryImpl()
            createViewModel(classFilterRepository)
            advanceUntilIdle()

            viewModel.onEvent(CalendarScreenEvent.SelectDateForDialog(date))
            advanceUntilIdle()

            classFilterRepository.toggleClass(1)
            classFilterRepository.toggleClass(2)
            advanceUntilIdle()

            assertEquals(3, viewModel.uiState.value.allStudents.size)
            assertEquals(3, viewModel.uiState.value.studentsForSelectedDate.size)

            classFilterRepository.toggleClass(2)
            advanceUntilIdle()

            assertEquals(2, viewModel.uiState.value.allStudents.size)
            assertEquals(2, viewModel.uiState.value.studentsForSelectedDate.size)
        }
}