package dev.nenoeldeeb.education.absencerecord.presentation.screens.students

import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.SortType
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class StudentsViewModelSortTest : StudentsViewModelTestBase() {
    private val december = LocalDate(2024, Month.DECEMBER, 1)

    @BeforeEach
    fun setUp() {
        commonSetUp()
    }

    @Test
    fun `selecting attendance sort reorders students and updates sortType`() =
        runTest {
            // Given
            val firstByName = Student(id = 1, name = "Alpha")
            val secondByName = Student(id = 2, name = "Beta")
            val attendanceFirst = Student(id = 3, name = "LowAttendance")
            val attendanceSecond = Student(id = 4, name = "HighAttendance")
            every { getAllStudentsUseCase(SortType.ByName, any()) } returns
                flowOf(Result.success(listOf(firstByName, secondByName)))
            every { getAllStudentsUseCase(SortType.ByAttendance, any()) } returns
                flowOf(Result.success(listOf(attendanceFirst, attendanceSecond)))
            createViewModel()
            advanceUntilIdle()

            // when
            viewModel.onEvent(
                StudentsScreenEvent.UpdateSortType(SortType.ByAttendance)
            )
            advanceUntilIdle()

            // then
            assertEquals(listOf(attendanceFirst, attendanceSecond), viewModel.uiState.value.allStudents)
            assertEquals(SortType.ByAttendance, viewModel.uiState.value.sortType)
        }

    @Test
    fun `sort failure sets error state`() =
        runTest {
            // given
            every { getAllStudentsUseCase(SortType.ByName, any()) } returns
                flowOf(Result.success(listOf(Student(id = 1, name = "S1"))))
            every { getAllStudentsUseCase(SortType.ByAttendance, any()) } returns
                flowOf(Result.failure(StudentError.Database))
            createViewModel()
            advanceUntilIdle()
            assertNull(viewModel.uiState.value.error)

            // when
            viewModel.onEvent(
                StudentsScreenEvent.UpdateSortType(SortType.ByAttendance)
            )
            advanceUntilIdle()

            // then
            assertEquals(
                UiText.StringResource(R.string.error_database_operation_failed),
                viewModel.uiState.value.error
            )
        }

    @Test
    fun `sortType persists after a subsequent unrelated event`() =
        runTest {
            // given
            every { getAllStudentsUseCase(any(), any()) } returns
                flowOf(Result.success(emptyList()))
            createViewModel()
            advanceUntilIdle()
            viewModel.onEvent(
                StudentsScreenEvent.UpdateSortType(SortType.ByAttendance)
            )
            advanceUntilIdle()

            // when
            viewModel.onEvent(StudentsScreenEvent.ToggleSortPanelVisible(false))
            advanceUntilIdle()

            // then
            assertEquals(SortType.ByAttendance, viewModel.uiState.value.sortType)
        }

    @Test
    fun `ToggleSortPanelVisible updates panel state`() =
        runTest {
            // given
            every { getAllStudentsUseCase(any(), any()) } returns
                flowOf(Result.success(emptyList()))
            createViewModel()
            advanceUntilIdle()

            // when
            viewModel.onEvent(StudentsScreenEvent.ToggleSortPanelVisible(true))
            advanceUntilIdle()

            // then
            assertTrue(viewModel.uiState.value.isSortPanelVisible)

            // when
            viewModel.onEvent(StudentsScreenEvent.ToggleSortPanelVisible(false))
            advanceUntilIdle()

            // then
            assertFalse(viewModel.uiState.value.isSortPanelVisible)
        }

    @Test
    fun `available months are loaded into state`() =
        runTest {
            // given
            val months = listOf(december, LocalDate(2024, Month.NOVEMBER, 1))
            every { getAvailableMonthsUseCase() } returns flowOf(Result.success(months))
            every { getAllStudentsUseCase(any(), any()) } returns
                flowOf(Result.success(emptyList()))
            createViewModel()
            advanceUntilIdle()

            // then
            assertEquals(months, viewModel.uiState.value.availableMonths)
        }

    @Test
    fun `available months failure sets error state`() =
        runTest {
            // given
            every { getAvailableMonthsUseCase() } returns
                flowOf(Result.failure(StudentError.Database))
            every { getAllStudentsUseCase(any(), any()) } returns
                flowOf(Result.success(emptyList()))
            createViewModel()
            advanceUntilIdle()

            // then
            assertEquals(
                UiText.StringResource(R.string.error_database_operation_failed),
                viewModel.uiState.value.error
            )
        }

    @Test
    fun `selecting a month reloads students and sets selectedMonth`() =
        runTest {
            // given
            val monthStudents = listOf(Student(id = 1, name = "DecStudent"))
            every { getAllStudentsUseCase(any(), any()) } returns
                flowOf(Result.success(emptyList()))
            every { getAllStudentsUseCase(SortType.ByAttendance, december) } returns
                flowOf(Result.success(monthStudents))
            createViewModel()
            advanceUntilIdle()
            viewModel.onEvent(StudentsScreenEvent.UpdateSortType(SortType.ByAttendance))
            advanceUntilIdle()

            // when
            viewModel.onEvent(StudentsScreenEvent.UpdateSelectedMonth(december))
            advanceUntilIdle()

            // then
            assertEquals(december, viewModel.uiState.value.selectedMonth)
            assertEquals(monthStudents, viewModel.uiState.value.allStudents)
        }

    @Test
    fun `selecting a month passes the month to the use case for both sort modes`() =
        runTest {
            // given
            every { getAllStudentsUseCase(any(), any()) } returns
                flowOf(Result.success(emptyList()))
            createViewModel()
            advanceUntilIdle()

            // when
            viewModel.onEvent(StudentsScreenEvent.UpdateSelectedMonth(december))
            advanceUntilIdle()

            // then
            verify { getAllStudentsUseCase(SortType.ByName, december) }

            // when
            viewModel.onEvent(StudentsScreenEvent.UpdateSortType(SortType.ByAttendance))
            advanceUntilIdle()

            // then
            verify { getAllStudentsUseCase(SortType.ByAttendance, december) }
        }

    @Test
    fun `clearing month resets selection and reloads without month`() =
        runTest {
            // given
            every { getAllStudentsUseCase(any(), any()) } returns
                flowOf(Result.success(emptyList()))
            createViewModel()
            advanceUntilIdle()
            viewModel.onEvent(StudentsScreenEvent.UpdateSelectedMonth(december))
            advanceUntilIdle()
            assertEquals(december, viewModel.uiState.value.selectedMonth)

            // when
            viewModel.onEvent(StudentsScreenEvent.UpdateSelectedMonth(null))
            advanceUntilIdle()

            // then
            assertNull(viewModel.uiState.value.selectedMonth)
            verify { getAllStudentsUseCase(SortType.ByName, null) }
        }

    @Test
    fun `selecting a month closes the month dropdown`() =
        runTest {
            // given
            every { getAllStudentsUseCase(any(), any()) } returns
                flowOf(Result.success(emptyList()))
            createViewModel()
            advanceUntilIdle()
            viewModel.onEvent(StudentsScreenEvent.ToggleMonthDropdown(true))
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.isMonthDropdownExpanded)

            // when
            viewModel.onEvent(StudentsScreenEvent.UpdateSelectedMonth(december))
            advanceUntilIdle()

            // then
            assertFalse(viewModel.uiState.value.isMonthDropdownExpanded)
        }
}