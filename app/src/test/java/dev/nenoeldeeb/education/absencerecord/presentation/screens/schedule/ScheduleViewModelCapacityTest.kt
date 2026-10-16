package dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule

import dev.nenoeldeeb.education.absencerecord.domain.models.AvailableLessonHour
import dev.nenoeldeeb.education.absencerecord.domain.models.DropReason
import dev.nenoeldeeb.education.absencerecord.domain.models.DroppedStudent
import dev.nenoeldeeb.education.absencerecord.domain.models.HourReplanResult
import dev.nenoeldeeb.education.absencerecord.domain.models.HourWithOccupancy
import dev.nenoeldeeb.education.absencerecord.domain.models.PlannedHour
import dev.nenoeldeeb.education.absencerecord.domain.models.PlannedSlot
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Derived capacity and the confirm-first re-plan (FR-003, FR-009…FR-014, SC-001,
 * SC-002, SC-014, SC-016).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleViewModelCapacityTest : ScheduleViewModelTestBase() {
    @BeforeEach
    fun setUp() {
        commonSetUp()
    }

    private fun occupiedHour(
        duration: Int,
        assigned: Int
    ) = HourWithOccupancy(
        hour = AvailableLessonHour(1, DayOfWeek.SATURDAY, 540, duration),
        endMinutes = 540 + duration,
        assignedStudentIds = (1..assigned).toList(),
        assignedCount = assigned
    )

    private fun proposal(
        dropped: List<DroppedStudent>,
        kept: Int = 1
    ) = HourReplanResult.NeedsConfirmation(
        proposed =
            PlannedHour(
                blockStartMinutes = 540,
                blockDurationMinutes = 60,
                maxStudents = 6,
                slots =
                    (1..kept).map {
                        PlannedSlot(it, "Student $it", it, 540 + (it - 1) * 10, 550 + (it - 1) * 10)
                    }
            ),
        dropped = dropped
    )

    // region derived capacity (SC-002)

    @Test
    fun `capacity is derived from the selected length, not set independently`() {
        for (duration in ScheduleRules.selectableDurations()) {
            assertEquals(
                duration / 10,
                occupiedHour(duration, 0).maxStudents,
                "expected $duration minutes to hold ${duration / 10} students"
            )
        }
    }

    @Test
    fun `a ten minute block holds one student and a 120 minute block holds twelve`() {
        assertEquals(1, occupiedHour(10, 0).maxStudents)
        assertEquals(12, occupiedHour(120, 0).maxStudents)
    }

    @Test
    fun `save passes the chosen length to the use case`() =
        runTest {
            coEvery { observeHoursForWeekdayUseCase(any()) } returns
                flowOf(Result.success(emptyList()))
            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(ScheduleScreenEvent.OpenHourDialog(null))
            viewModel.onEvent(ScheduleScreenEvent.SetHourStart(540))
            viewModel.onEvent(ScheduleScreenEvent.SetHourDuration(30))
            coEvery { insertHourUseCase(any(), any(), any()) } returns Result.success(1)

            viewModel.onEvent(ScheduleScreenEvent.SaveHour)
            advanceUntilIdle()

            coVerify(exactly = 1) { insertHourUseCase(DayOfWeek.SATURDAY, 540, 30) }
            assertEquals(false, viewModel.uiState.value.isHourDialogOpen)
        }

    // endregion

    // region confirm-first re-plan (SC-014, SC-015, FR-011…FR-014)

    @Test
    fun `an edit that would drop students parks a proposal and writes nothing`() =
        runTest {
            coEvery { observeHoursForWeekdayUseCase(any()) } returns
                flowOf(Result.success(listOf(occupiedHour(60, 3))))
            createViewModel()
            advanceUntilIdle()

            val existing = AvailableLessonHour(1, DayOfWeek.SATURDAY, 540, 60)
            viewModel.onEvent(ScheduleScreenEvent.OpenHourDialog(existing))
            viewModel.onEvent(ScheduleScreenEvent.SetHourDuration(30))
            coEvery { updateHourUseCase(any(), any(), any(), any(), any()) } returns
                Result.success(
                    proposal(
                        listOf(
                            DroppedStudent(7, "Zoe", DropReason.ExceedsCapacity),
                            DroppedStudent(8, "Max", DropReason.ExceedsCapacity)
                        ),
                        kept = 1
                    )
                )

            viewModel.onEvent(ScheduleScreenEvent.SaveHour)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            // proposed, not applied
            assertTrue(state.pendingReplan != null, "expected a pending proposal")
            assertEquals(2, state.pendingReplan?.dropped?.size)
            // exactly one unconfirmed call (the preview) and no commit
            coVerify(exactly = 1) { updateHourUseCase(any(), any(), any(), any(), false) }
            coVerify(exactly = 0) { updateHourUseCase(any(), any(), any(), any(), true) }
        }

    @Test
    fun `declining clears the proposal and commits nothing`() =
        runTest {
            coEvery { observeHoursForWeekdayUseCase(any()) } returns
                flowOf(Result.success(listOf(occupiedHour(60, 3))))
            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(
                ScheduleScreenEvent.OpenHourDialog(AvailableLessonHour(1, DayOfWeek.SATURDAY, 540, 60))
            )
            viewModel.onEvent(ScheduleScreenEvent.SetHourDuration(30))
            coEvery { updateHourUseCase(any(), any(), any(), any(), any()) } returns
                Result.success(proposal(listOf(DroppedStudent(7, "Zoe", DropReason.ExceedsCapacity))))

            viewModel.onEvent(ScheduleScreenEvent.SaveHour)
            advanceUntilIdle()
            viewModel.onEvent(ScheduleScreenEvent.DismissReplan)
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.pendingReplan)
            coVerify(exactly = 0) { updateHourUseCase(any(), any(), any(), any(), true) }
        }

    @Test
    fun `confirming commits the edit exactly once`() =
        runTest {
            coEvery { observeHoursForWeekdayUseCase(any()) } returns
                flowOf(Result.success(listOf(occupiedHour(60, 3))))
            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(
                ScheduleScreenEvent.OpenHourDialog(AvailableLessonHour(1, DayOfWeek.SATURDAY, 540, 60))
            )
            viewModel.onEvent(ScheduleScreenEvent.SetHourDuration(30))
            coEvery { updateHourUseCase(any(), any(), any(), any(), false) } returns
                Result.success(
                    proposal(listOf(DroppedStudent(7, "Zoe", DropReason.ExceedsCapacity)))
                )
            coEvery { updateHourUseCase(any(), any(), any(), any(), true) } returns
                Result.success(HourReplanResult.Applied(listOf(7)))

            viewModel.onEvent(ScheduleScreenEvent.SaveHour)
            advanceUntilIdle()
            assertTrue(
                viewModel.uiState.value.pendingReplan != null,
                "expected a pending proposal before confirming"
            )
            viewModel.onEvent(ScheduleScreenEvent.ConfirmReplan)
            advanceUntilIdle()

            coVerify(exactly = 1) { updateHourUseCase(any(), any(), any(), any(), true) }
            assertNull(viewModel.uiState.value.pendingReplan)
        }

    @Test
    fun `growing the length changes no slot`() {
        val before = occupiedHour(30, 3)
        val after = before.copy(hour = before.hour.copy(durationMinutes = 120))
        // slots depend on position and start, never on the length (FR-014)
        assertEquals(before.hour.startMinutes, after.hour.startMinutes)
        assertEquals(before.assignedStudentIds, after.assignedStudentIds)
        assertTrue(after.maxStudents > before.maxStudents)
    }

    @Test
    fun `an invalid length is refused before the use case is called`() =
        runTest {
            coEvery { observeHoursForWeekdayUseCase(any()) } returns
                flowOf(Result.success(emptyList()))
            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(ScheduleScreenEvent.OpenHourDialog(null))
            viewModel.onEvent(ScheduleScreenEvent.SetHourDuration(35))
            viewModel.onEvent(ScheduleScreenEvent.SaveHour)
            advanceUntilIdle()

            assertEquals(true, viewModel.uiState.value.isHourDialogOpen)
            coVerify(exactly = 0) { insertHourUseCase(any(), any(), any()) }
        }

    // endregion
}