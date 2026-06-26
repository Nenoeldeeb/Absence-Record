package dev.nenoeldeeb.education.absencerecord.presentation.screens.calendar

import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelBaseTest : CalendarViewModelTestBase() {
    @BeforeEach
    fun setUp() {
        commonSetUp()
    }

    @Test
    fun `init loads data successfully`() =
        runTest {
            val months = listOf(LocalDate(2023, Month.JANUARY, 1))
            val students = listOf(Student(1, "S1"))

            coEvery { getAvailableMonthsUseCase() } returns flowOf(Result.success(months))
            coEvery { studentManagementUseCases.getAllStudentsUseCase() } returns flowOf(Result.success(students))

            createViewModel()
            advanceUntilIdle()

            assertEquals(students, viewModel.uiState.value.allStudents)
            assertNull(viewModel.uiState.value.error)
        }

    @Test
    fun `MarkStudentAttendance calls use case and refreshes list`() =
        runTest {
            val date = LocalDate(2023, Month.JANUARY, 15)
            coEvery { recordStudentAttendanceUseCase(any()) } returns Result.success(1L)
            coEvery { getAttendanceForDateUseCase(date) } returns flowOf(Result.success(emptyList()))

            createViewModel()

            viewModel.onEvent(CalendarScreenEvent.MarkStudentAttendance(1, date))
            advanceUntilIdle()

            coVerify { recordStudentAttendanceUseCase(match { it.studentId == 1 && it.date == date }) }
        }

    @Test
    fun `MarkStudentAttendance shows error on failure`() =
        runTest {
            val date = LocalDate(2023, Month.JANUARY, 15)
            val errorMsg = "Msg"
            coEvery { recordStudentAttendanceUseCase(any()) } returns Result.failure(Exception(errorMsg))
            createViewModel()

            viewModel.onEvent(CalendarScreenEvent.MarkStudentAttendance(1, date))
            advanceUntilIdle()

            assertEquals(
                UiText.StringResource(R.string.error_marking_attendance, errorMsg),
                viewModel.uiState.value.error
            )
        }

    @Test
    fun `DeleteStudentAttendance calls use case and refreshes list`() =
        runTest {
            val date = LocalDate(2023, Month.JANUARY, 15)
            coEvery { deleteStudentAttendanceUseCase(any(), any()) } returns Result.success(Unit)
            coEvery { getAttendanceForDateUseCase(date) } returns flowOf(Result.success(emptyList()))

            createViewModel()

            viewModel.onEvent(CalendarScreenEvent.DeleteStudentAttendance(1, date))
            advanceUntilIdle()

            coVerify { deleteStudentAttendanceUseCase(1, date) }
        }

    @Test
    fun `DeleteStudentAttendance failure shows error`() =
        runTest {
            val date = LocalDate(2023, Month.JANUARY, 15)
            val errorMsg = "Msg"
            coEvery { deleteStudentAttendanceUseCase(any(), any()) } returns Result.failure(Exception(errorMsg))
            createViewModel()

            viewModel.onEvent(CalendarScreenEvent.DeleteStudentAttendance(1, date))
            advanceUntilIdle()

            assertEquals(
                UiText.StringResource(R.string.error_deleting_attendance, errorMsg),
                viewModel.uiState.value.error
            )
        }

    @Test
    fun `SelectDateForDialog updates state`() =
        runTest {
            createViewModel()
            val date = LocalDate(2023, Month.MARCH, 10)

            viewModel.onEvent(CalendarScreenEvent.SelectDateForDialog(date))

            assertEquals(date, viewModel.uiState.value.selectedDateForDialog)
        }

    @Test
    fun `init handles students load failure`() =
        runTest {
            val errorMsg = "Students error"
            coEvery { getAvailableMonthsUseCase() } returns flowOf(Result.success(emptyList()))
            coEvery { studentManagementUseCases.getAllStudentsUseCase() } returns flowOf(Result.failure(Exception(errorMsg)))

            createViewModel()
            advanceUntilIdle()

            val expectedError = UiText.StringResource(R.string.error_loading_students, errorMsg)
            assertEquals(expectedError, viewModel.uiState.value.error)
        }
}