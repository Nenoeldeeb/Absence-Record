package dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule

import dev.nenoeldeeb.education.absencerecord.R
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
class ScheduleViewModelCapacityTest : ScheduleViewModelTestBase() {
    @BeforeEach
    fun setUp() {
        commonSetUp()
    }

    @Test
    fun `hour save with arabic-indic digits parses and inserts`() =
        runTest {
            coEvery { observeHoursForWeekdayUseCase(any()) } returns
                flowOf(Result.success(emptyList()))
            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(ScheduleScreenEvent.OpenHourDialog(null))
            viewModel.onEvent(ScheduleScreenEvent.SetHourStart(540))
            viewModel.onEvent(ScheduleScreenEvent.SetHourMaxStudents("٥"))
            coEvery { insertHourUseCase(any(), any(), any()) } returns Result.success(1)

            viewModel.onEvent(ScheduleScreenEvent.SaveHour)
            advanceUntilIdle()

            coVerify(exactly = 1) { insertHourUseCase(DayOfWeek.SATURDAY, 540, 5) }
            assertEquals(false, viewModel.uiState.value.isHourDialogOpen)
        }

    @Test
    fun `hour save with non-numeric max students shows capacity inline error without calling use case`() =
        runTest {
            coEvery { observeHoursForWeekdayUseCase(any()) } returns
                flowOf(Result.success(emptyList()))
            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(ScheduleScreenEvent.OpenHourDialog(null))
            viewModel.onEvent(ScheduleScreenEvent.SetHourStart(540))
            viewModel.onEvent(ScheduleScreenEvent.SetHourMaxStudents("5a"))

            viewModel.onEvent(ScheduleScreenEvent.SaveHour)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(true, state.isHourDialogOpen)
            assertEquals(
                UiText.StringResource(R.string.error_capacity_invalid),
                state.hourValidationError
            )
            coVerify(exactly = 0) { insertHourUseCase(any(), any(), any()) }
        }
}