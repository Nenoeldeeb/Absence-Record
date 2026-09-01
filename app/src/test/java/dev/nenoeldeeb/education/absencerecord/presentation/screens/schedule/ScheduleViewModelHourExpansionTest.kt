package dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule

import io.mockk.coEvery
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleViewModelHourExpansionTest : ScheduleViewModelTestBase() {
    @BeforeEach
    fun setUp() {
        commonSetUp()
    }

    @Test
    fun `toggle hour expanded adds then removes hour id from expandedHourIds`() =
        runTest {
            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(ScheduleScreenEvent.ToggleHourExpanded(1))
            assertEquals(setOf(1), viewModel.uiState.value.expandedHourIds)

            viewModel.onEvent(ScheduleScreenEvent.ToggleHourExpanded(1))
            assertEquals(emptySet(), viewModel.uiState.value.expandedHourIds)
        }

    @Test
    fun `toggle hour expanded tracks multiple hours independently`() =
        runTest {
            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(ScheduleScreenEvent.ToggleHourExpanded(1))
            viewModel.onEvent(ScheduleScreenEvent.ToggleHourExpanded(2))

            assertEquals(setOf(1, 2), viewModel.uiState.value.expandedHourIds)

            viewModel.onEvent(ScheduleScreenEvent.ToggleHourExpanded(1))
            assertEquals(setOf(2), viewModel.uiState.value.expandedHourIds)
        }

    @Test
    fun `selecting weekday keeps expanded state intact`() =
        runTest {
            coEvery { observeHoursForWeekdayUseCase(any()) } returns
                flowOf(Result.success(emptyList()))
            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(ScheduleScreenEvent.ToggleHourExpanded(3))
            viewModel.onEvent(ScheduleScreenEvent.SelectWeekday(DayOfWeek.SUNDAY))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(DayOfWeek.SUNDAY, state.selectedWeekday)
            assertEquals(setOf(3), state.expandedHourIds)
        }
}