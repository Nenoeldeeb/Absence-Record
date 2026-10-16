package dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule

import dev.nenoeldeeb.education.absencerecord.domain.models.AvailableLessonHour
import dev.nenoeldeeb.education.absencerecord.domain.models.DropReason
import dev.nenoeldeeb.education.absencerecord.domain.models.DroppedStudent
import dev.nenoeldeeb.education.absencerecord.domain.models.HourReplanResult
import dev.nenoeldeeb.education.absencerecord.domain.models.HourWithOccupancy
import dev.nenoeldeeb.education.absencerecord.domain.models.PlannedHour
import dev.nenoeldeeb.education.absencerecord.domain.services.ScheduleRules
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
import kotlin.test.assertTrue

/**
 * Length state and the midnight boundary (FR-002, FR-006, FR-007, FR-015).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleViewModelLengthTest : ScheduleViewModelTestBase() {
    @BeforeEach
    fun setUp() {
        commonSetUp()
    }

    private fun hourWith(
        id: Int,
        start: Int,
        max: Int,
        assigned: Int = 0
    ): HourWithOccupancy =
        HourWithOccupancy(
            hour = AvailableLessonHour(id, DayOfWeek.SATURDAY, start, max * 10),
            endMinutes = start + max * 10,
            assignedStudentIds = (1..assigned).toList(),
            assignedCount = assigned
        )

    @Test
    fun `opening a new hour dialog seeds the default length`() =
        runTest {
            coEvery { observeHoursForWeekdayUseCase(any()) } returns
                flowOf(Result.success(emptyList()))
            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(ScheduleScreenEvent.OpenHourDialog(null))
            advanceUntilIdle()

            assertEquals(
                ScheduleRules.DEFAULT_NEW_HOUR_DURATION_MINUTES,
                viewModel.uiState.value.hourDurationMinutes
            )
        }

    @Test
    fun `opening an existing hour dialog seeds its stored length`() =
        runTest {
            coEvery { observeHoursForWeekdayUseCase(any()) } returns
                flowOf(Result.success(listOf(hourWith(id = 1, start = 540, max = 9))))
            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(
                ScheduleScreenEvent.OpenHourDialog(AvailableLessonHour(1, DayOfWeek.SATURDAY, 540, 90))
            )
            advanceUntilIdle()

            assertEquals(90, viewModel.uiState.value.hourDurationMinutes)
        }

    @Test
    fun `setting the length updates the state`() =
        runTest {
            coEvery { observeHoursForWeekdayUseCase(any()) } returns
                flowOf(Result.success(emptyList()))
            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(ScheduleScreenEvent.OpenHourDialog(null))
            viewModel.onEvent(ScheduleScreenEvent.SetHourDuration(120))
            advanceUntilIdle()

            assertEquals(120, viewModel.uiState.value.hourDurationMinutes)
        }

    @Test
    fun `a block that would run past midnight is refused before the use case`() =
        runTest {
            coEvery { observeHoursForWeekdayUseCase(any()) } returns
                flowOf(Result.success(emptyList()))
            createViewModel()
            advanceUntilIdle()

            // 1440 - 60 + 1 = 23:41; a 120-minute block would end at 01:41
            viewModel.onEvent(ScheduleScreenEvent.OpenHourDialog(null))
            viewModel.onEvent(ScheduleScreenEvent.SetHourStart(1440 - 60 + 1))
            viewModel.onEvent(ScheduleScreenEvent.SetHourDuration(120))
            coEvery { insertHourUseCase(any(), any(), any()) } returns Result.success(1)

            viewModel.onEvent(ScheduleScreenEvent.SaveHour)
            advanceUntilIdle()

            assertEquals(true, viewModel.uiState.value.isHourDialogOpen)
            coVerify(exactly = 0) { insertHourUseCase(any(), any(), any()) }
        }

    @Test
    fun `moving the start time also goes through the confirm-first re-plan`() =
        runTest {
            coEvery { observeHoursForWeekdayUseCase(any()) } returns
                flowOf(Result.success(listOf(hourWith(id = 1, start = 540, max = 6, assigned = 2))))
            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(
                ScheduleScreenEvent.OpenHourDialog(AvailableLessonHour(1, DayOfWeek.SATURDAY, 540, 60))
            )
            viewModel.onEvent(ScheduleScreenEvent.SetHourStart(600))
            coEvery { updateHourUseCase(any(), any(), any(), any(), any()) } returns
                Result.success(
                    HourReplanResult.NeedsConfirmation(
                        proposed = PlannedHour(600, 60, 6, emptyList()),
                        dropped = listOf(DroppedStudent(1, "Ali", DropReason.SlotConflictsWithBusy))
                    )
                )

            viewModel.onEvent(ScheduleScreenEvent.SaveHour)
            advanceUntilIdle()

            // a start move that would strand a student stops for confirmation too
            assertTrue(viewModel.uiState.value.pendingReplan != null)
            coVerify(exactly = 1) { updateHourUseCase(any(), any(), any(), any(), false) }
            coVerify(exactly = 0) { updateHourUseCase(any(), any(), any(), any(), true) }
        }
}