package dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail

import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.AvailableLessonHour
import dev.nenoeldeeb.education.absencerecord.domain.models.BusyAppointment
import dev.nenoeldeeb.education.absencerecord.domain.models.HourWithOccupancy
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentLessonEntry
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentScheduleView
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText
import io.mockk.every
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class StudentDetailViewModelScheduleTest : StudentDetailViewModelTestBase() {
    @BeforeEach
    fun setUp() {
        commonSetUp()
    }

    private fun aSchedule(): StudentScheduleView =
        StudentScheduleView(
            studentId = 1,
            lessons = listOf(StudentLessonEntry(DayOfWeek.SATURDAY, 540, 600)),
            busy =
                listOf(
                    BusyAppointment(
                        id = 1,
                        studentId = 1,
                        weekday = DayOfWeek.MONDAY,
                        startMinutes = 510,
                        durationMinutes = 60
                    )
                )
        )

    @Test
    fun `schedule onSuccess populates schedule fields in state`() =
        runTest {
            val schedule = aSchedule()
            every { observeStudentScheduleUseCase(1) } returns flowOf(Result.success(schedule))

            createViewModel(studentId = 1)
            advanceUntilIdle()

            assertEquals(schedule, viewModel.uiState.value.studentSchedule)
        }

    @Test
    fun `schedule onFailure sets error`() =
        runTest {
            every { observeStudentScheduleUseCase(1) } returns
                flowOf(Result.failure(StudentError.Database))

            createViewModel(studentId = 1)
            advanceUntilIdle()

            assertEquals(
                UiText.StringResource(R.string.error_database_operation_failed),
                viewModel.uiState.value.error
            )
        }

    @Test
    fun `schedule re-emission updates the section`() =
        runTest {
            val first = aSchedule()
            val second =
                first.copy(
                    lessons = listOf(StudentLessonEntry(DayOfWeek.MONDAY, 600, 660)),
                    busy = emptyList()
                )
            every { observeStudentScheduleUseCase(1) } returns
                flowOf(Result.success(first), Result.success(second))

            createViewModel(studentId = 1)
            advanceUntilIdle()

            assertEquals(second, viewModel.uiState.value.studentSchedule)
        }

    @Test
    fun `hours onSuccess for selected weekday populates hoursForWeekday`() =
        runTest {
            val saturdayHours = listOf(hourWith(id = 1, weekday = DayOfWeek.SATURDAY, start = 540))
            val sundayHours = listOf(hourWith(id = 2, weekday = DayOfWeek.SUNDAY, start = 600))
            every { observeHoursForWeekdayUseCase(DayOfWeek.SATURDAY) } returns
                flowOf(Result.success(saturdayHours))
            every { observeHoursForWeekdayUseCase(DayOfWeek.SUNDAY) } returns
                flowOf(Result.success(sundayHours))

            createViewModel(studentId = 1)
            advanceUntilIdle()

            assertEquals(saturdayHours, viewModel.uiState.value.hoursForWeekday)

            viewModel.onEvent(StudentDetailScreenEvent.SelectScheduleWeekday(DayOfWeek.SUNDAY))
            advanceUntilIdle()

            assertEquals(sundayHours, viewModel.uiState.value.hoursForWeekday)
        }

    @Test
    fun `hours onFailure sets error`() =
        runTest {
            every { observeHoursForWeekdayUseCase(any()) } returns
                flowOf(Result.failure(StudentError.Database))

            createViewModel(studentId = 1)
            advanceUntilIdle()

            assertEquals(
                UiText.StringResource(R.string.error_database_operation_failed),
                viewModel.uiState.value.error
            )
        }

    @Test
    fun `selectScheduleWeekday updates selected weekday and closes add lesson dialog`() =
        runTest {
            createViewModel(studentId = 1)
            advanceUntilIdle()

            viewModel.onEvent(StudentDetailScreenEvent.OpenAddLessonDialog)
            viewModel.onEvent(StudentDetailScreenEvent.SelectScheduleWeekday(DayOfWeek.SUNDAY))

            val state = viewModel.uiState.value
            assertEquals(DayOfWeek.SUNDAY, state.selectedScheduleWeekday)
            assertEquals(false, state.isAddLessonDialogOpen)
        }

    @Test
    fun `selectScheduleTab toggles busy tab`() =
        runTest {
            createViewModel(studentId = 1)
            advanceUntilIdle()

            viewModel.onEvent(StudentDetailScreenEvent.SelectScheduleTab(StudentScheduleTab.Busy))

            assertEquals(StudentScheduleTab.Busy, viewModel.uiState.value.selectedScheduleTab)
        }

    private fun hourWith(
        id: Int,
        weekday: DayOfWeek = DayOfWeek.SATURDAY,
        start: Int,
        max: Int = 5
    ): HourWithOccupancy =
        HourWithOccupancy(
            hour = AvailableLessonHour(id, weekday, start, max),
            endMinutes = start + 60,
            assignedStudentIds = emptyList(),
            assignedCount = 0,
            maxStudents = max,
            remainingSlots = max,
            isFull = false
        )
}