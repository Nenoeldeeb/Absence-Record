package dev.nenoeldeeb.education.absencerecord.presentation.screens.calendar

import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText
import io.mockk.coEvery
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelFlowTest : CalendarViewModelTestBase() {
    @BeforeEach
    fun setUp() {
        commonSetUp()
    }

    @Test
    fun `selectDate triggers attendance query for that date`() =
        runTest {
            val date = LocalDate(2026, Month.FEBRUARY, 15)
            val students = listOf(Student(1, "S1"))
            val attendance =
                listOf(
                    dev.nenoeldeeb.education.absencerecord.domain.models.StudentAttendance(
                        studentId = 1,
                        date = date
                    )
                )

            coEvery { studentManagementUseCases.getAllStudentsUseCase() } returns flowOf(Result.success(students))
            coEvery { getAttendanceForDateUseCase(date) } returns flowOf(Result.success(attendance))

            createViewModel()

            viewModel.onEvent(CalendarScreenEvent.SelectDateForDialog(date))
            advanceUntilIdle()

            assertEquals(attendance, viewModel.uiState.value.studentsForSelectedDate)
        }

    @Test
    fun `changing date switches to new query`() =
        runTest {
            val dateA = LocalDate(2026, Month.FEBRUARY, 15)
            val dateB = LocalDate(2026, Month.FEBRUARY, 16)
            val students = listOf(Student(1, "S1"), Student(2, "S2"))
            val attendanceA =
                listOf(
                    dev.nenoeldeeb.education.absencerecord.domain.models.StudentAttendance(
                        studentId = 1,
                        date = dateA
                    )
                )
            val attendanceB =
                listOf(
                    dev.nenoeldeeb.education.absencerecord.domain.models.StudentAttendance(
                        studentId = 2,
                        date = dateB
                    )
                )

            coEvery { studentManagementUseCases.getAllStudentsUseCase() } returns flowOf(Result.success(students))

            val dateSlot = slot<LocalDate>()
            coEvery { getAttendanceForDateUseCase(capture(dateSlot)) } answers {
                when (dateSlot.captured) {
                    dateA -> flowOf(Result.success(attendanceA))
                    dateB -> flowOf(Result.success(attendanceB))
                    else -> flowOf(Result.success(emptyList()))
                }
            }

            createViewModel()

            viewModel.onEvent(CalendarScreenEvent.SelectDateForDialog(dateA))
            advanceUntilIdle()

            assertEquals(attendanceA, viewModel.uiState.value.studentsForSelectedDate)

            viewModel.onEvent(CalendarScreenEvent.SelectDateForDialog(dateB))
            advanceUntilIdle()

            assertEquals(attendanceB, viewModel.uiState.value.studentsForSelectedDate)
        }

    @Test
    fun `null date clears students for selected date`() =
        runTest {
            val date = LocalDate(2026, Month.FEBRUARY, 15)
            val students = listOf(Student(1, "S1"))
            val attendance =
                listOf(
                    dev.nenoeldeeb.education.absencerecord.domain.models.StudentAttendance(
                        studentId = 1,
                        date = date
                    )
                )

            coEvery { studentManagementUseCases.getAllStudentsUseCase() } returns flowOf(Result.success(students))
            coEvery { getAttendanceForDateUseCase(date) } returns flowOf(Result.success(attendance))

            createViewModel()

            viewModel.onEvent(CalendarScreenEvent.SelectDateForDialog(date))
            advanceUntilIdle()
            assertEquals(attendance, viewModel.uiState.value.studentsForSelectedDate)

            viewModel.onEvent(CalendarScreenEvent.SelectDateForDialog(null))
            advanceUntilIdle()

            assertEquals(emptyList(), viewModel.uiState.value.studentsForSelectedDate)
        }

    @Test
    fun `database updates automatically propagate to UI state`() =
        runTest {
            val date = LocalDate(2026, Month.FEBRUARY, 15)
            val students = listOf(Student(1, "S1"), Student(2, "S2"))
            val attendanceFlow =
                MutableSharedFlow<Result<List<dev.nenoeldeeb.education.absencerecord.domain.models.StudentAttendance>>>(
                    replay = 1
                )

            coEvery { studentManagementUseCases.getAllStudentsUseCase() } returns flowOf(Result.success(students))
            coEvery { getAttendanceForDateUseCase(date) } returns attendanceFlow

            val initialAttendance =
                listOf(
                    dev.nenoeldeeb.education.absencerecord.domain.models.StudentAttendance(
                        studentId = 1,
                        date = date
                    )
                )
            attendanceFlow.emit(Result.success(initialAttendance))

            createViewModel()

            viewModel.onEvent(CalendarScreenEvent.SelectDateForDialog(date))
            advanceUntilIdle()

            assertEquals(initialAttendance, viewModel.uiState.value.studentsForSelectedDate)

            val updatedAttendance =
                listOf(
                    dev.nenoeldeeb.education.absencerecord.domain.models.StudentAttendance(
                        studentId = 1,
                        date = date
                    ),
                    dev.nenoeldeeb.education.absencerecord.domain.models.StudentAttendance(
                        studentId = 2,
                        date = date
                    )
                )
            attendanceFlow.emit(Result.success(updatedAttendance))
            advanceUntilIdle()

            assertEquals(updatedAttendance, viewModel.uiState.value.studentsForSelectedDate)
        }

    @Test
    fun `attendance query failure shows error`() =
        runTest {
            val date = LocalDate(2026, Month.FEBRUARY, 15)
            val students = listOf(Student(1, "S1"))

            coEvery { studentManagementUseCases.getAllStudentsUseCase() } returns flowOf(Result.success(students))
            coEvery { getAttendanceForDateUseCase(date) } returns flowOf(Result.failure(StudentError.Database))

            createViewModel()

            viewModel.onEvent(CalendarScreenEvent.SelectDateForDialog(date))
            advanceUntilIdle()

            assertEquals(
                UiText.StringResource(R.string.error_database_operation_failed),
                viewModel.uiState.value.error
            )
        }
}