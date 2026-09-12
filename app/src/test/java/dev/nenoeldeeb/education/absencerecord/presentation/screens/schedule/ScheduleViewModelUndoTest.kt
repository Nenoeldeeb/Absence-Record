package dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule

import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.AvailableLessonHour
import dev.nenoeldeeb.education.absencerecord.domain.models.HourWithOccupancy
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleViewModelUndoTest : ScheduleViewModelTestBase() {
    @BeforeEach
    fun setUp() {
        commonSetUp()
    }

    @Test
    fun `hour delete onSuccess records deleted hour with assigned students for undo`() =
        runTest {
            val hour = AvailableLessonHour(1, DayOfWeek.SATURDAY, 540, 5)
            coEvery { observeHoursForWeekdayUseCase(any()) } returns
                flowOf(
                    Result.success(
                        listOf(hourWith(id = 1, start = 540, max = 5, assigned = 2))
                    )
                )
            coEvery { deleteHourUseCase(any()) } returns Result.success(Unit)
            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(ScheduleScreenEvent.ConfirmDeleteHour(hour))
            viewModel.onEvent(ScheduleScreenEvent.ConfirmDeleteHour(hour))
            advanceUntilIdle()

            assertEquals(hour, viewModel.uiState.value.deletedHour)
            assertEquals(listOf(1, 2), viewModel.uiState.value.deletedHourAssignedIds)
        }

    @Test
    fun `undo delete restores hour and reassigns students`() =
        runTest {
            val hour = AvailableLessonHour(1, DayOfWeek.SATURDAY, 540, 5)
            coEvery { observeHoursForWeekdayUseCase(any()) } returns
                flowOf(
                    Result.success(
                        listOf(hourWith(id = 1, start = 540, max = 5, assigned = 2))
                    )
                )
            coEvery { deleteHourUseCase(any()) } returns Result.success(Unit)
            coEvery { insertHourUseCase(any(), any(), any()) } returns Result.success(9)
            coEvery { assignStudentUseCase(any(), any(), any()) } returns Result.success(Unit)
            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(ScheduleScreenEvent.ConfirmDeleteHour(hour))
            viewModel.onEvent(ScheduleScreenEvent.ConfirmDeleteHour(hour))
            advanceUntilIdle()

            viewModel.onEvent(ScheduleScreenEvent.UndoDeleteHour)
            advanceUntilIdle()

            coVerify(exactly = 1) { insertHourUseCase(DayOfWeek.SATURDAY, 540, 5) }
            coVerify(exactly = 1) { assignStudentUseCase(9, 1, DayOfWeek.SATURDAY) }
            coVerify(exactly = 1) { assignStudentUseCase(9, 2, DayOfWeek.SATURDAY) }
            assertEquals(null, viewModel.uiState.value.deletedHour)
            assertEquals(emptyList<Int>(), viewModel.uiState.value.deletedHourAssignedIds)
        }

    @Test
    fun `undo delete with restore failure surfaces error`() =
        runTest {
            val hour = AvailableLessonHour(1, DayOfWeek.SATURDAY, 540, 5)
            coEvery { observeHoursForWeekdayUseCase(any()) } returns
                flowOf(Result.success(emptyList()))
            coEvery { deleteHourUseCase(any()) } returns Result.success(Unit)
            coEvery { insertHourUseCase(any(), any(), any()) } returns
                Result.failure(StudentError.Database)
            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(ScheduleScreenEvent.ConfirmDeleteHour(hour))
            viewModel.onEvent(ScheduleScreenEvent.ConfirmDeleteHour(hour))
            advanceUntilIdle()

            viewModel.onEvent(ScheduleScreenEvent.UndoDeleteHour)
            advanceUntilIdle()

            assertEquals(
                UiText.StringResource(R.string.error_database_operation_failed),
                viewModel.uiState.value.error
            )
        }

    private fun hourWith(
        id: Int,
        start: Int,
        max: Int,
        assigned: Int = 0
    ): HourWithOccupancy =
        HourWithOccupancy(
            hour = AvailableLessonHour(id, DayOfWeek.SATURDAY, start, max),
            endMinutes = start + 60,
            assignedStudentIds = (1..assigned).toList(),
            assignedCount = assigned,
            maxStudents = max,
            remainingSlots = max - assigned,
            isFull = assigned >= max
        )
}