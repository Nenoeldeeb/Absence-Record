package dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule

import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.AssignmentRemovalReport
import dev.nenoeldeeb.education.absencerecord.domain.models.AvailableLessonHour
import dev.nenoeldeeb.education.absencerecord.domain.models.HourWithOccupancy
import dev.nenoeldeeb.education.absencerecord.domain.models.SortType
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError
import dev.nenoeldeeb.education.absencerecord.domain.services.ScheduleRules
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleViewModelHourTest : ScheduleViewModelTestBase() {
    @BeforeEach
    fun setUp() {
        commonSetUp()
    }

    @Test
    fun `hour insert onSuccess closes dialog and empty form`() =
        runTest {
            coEvery { observeHoursForWeekdayUseCase(any()) } returns
                flowOf(Result.success(emptyList()))
            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(ScheduleScreenEvent.OpenHourDialog(null))
            viewModel.onEvent(ScheduleScreenEvent.SetHourStart(540))
            viewModel.onEvent(ScheduleScreenEvent.SetHourMaxStudents("5"))
            coEvery { insertHourUseCase(any(), any(), any()) } returns Result.success(1)

            viewModel.onEvent(ScheduleScreenEvent.SaveHour)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(false, state.isHourDialogOpen)
            assertEquals(
                ScheduleRules.DEFAULT_NEW_HOUR_START_MINUTES,
                state.hourStartMinutes
            )
            assertEquals(
                ScheduleRules.DEFAULT_NEW_HOUR_MAX_STUDENTS.toString(),
                state.hourMaxStudents
            )
            assertEquals(null, state.hourValidationError)
        }

    @Test
    fun `hour insert onSuccess adds row to observed list`() =
        runTest {
            val hoursFlow =
                MutableStateFlow<Result<List<HourWithOccupancy>>>(Result.success(emptyList()))
            coEvery { observeHoursForWeekdayUseCase(any()) } returns hoursFlow
            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(ScheduleScreenEvent.OpenHourDialog(null))
            viewModel.onEvent(ScheduleScreenEvent.SetHourStart(540))
            viewModel.onEvent(ScheduleScreenEvent.SetHourMaxStudents("5"))
            coEvery { insertHourUseCase(any(), any(), any()) } returns Result.success(1)
            viewModel.onEvent(ScheduleScreenEvent.SaveHour)
            advanceUntilIdle()

            val newHour = hourWith(id = 1, start = 540, max = 5)
            hoursFlow.value = Result.success(listOf(newHour))
            advanceUntilIdle()

            assertEquals(listOf(newHour), viewModel.uiState.value.hoursForWeekday)
        }

    @Test
    fun `hour insert HourOverlap failure keeps dialog open and shows inline error`() =
        runTest {
            coEvery { observeHoursForWeekdayUseCase(any()) } returns
                flowOf(Result.success(emptyList()))
            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(ScheduleScreenEvent.OpenHourDialog(null))
            viewModel.onEvent(ScheduleScreenEvent.SetHourStart(540))
            viewModel.onEvent(ScheduleScreenEvent.SetHourMaxStudents("5"))
            coEvery { insertHourUseCase(any(), any(), any()) } returns
                Result.failure(StudentError.HourOverlap)

            viewModel.onEvent(ScheduleScreenEvent.SaveHour)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(true, state.isHourDialogOpen)
            assertEquals(
                UiText.StringResource(R.string.error_hour_overlap),
                state.hourValidationError
            )
        }

    @Test
    fun `hour save with max students below one shows capacity inline error without calling use case`() =
        runTest {
            coEvery { observeHoursForWeekdayUseCase(any()) } returns
                flowOf(Result.success(emptyList()))
            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(ScheduleScreenEvent.OpenHourDialog(null))
            viewModel.onEvent(ScheduleScreenEvent.SetHourStart(540))
            viewModel.onEvent(ScheduleScreenEvent.SetHourMaxStudents("0"))

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

    @Test
    fun `hour delete confirm first opens confirmation then deletes`() =
        runTest {
            val hour = AvailableLessonHour(1, DayOfWeek.SATURDAY, 540, 5)
            coEvery { deleteHourUseCase(any()) } returns Result.success(Unit)
            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(ScheduleScreenEvent.ConfirmDeleteHour(hour))
            assertEquals(hour, viewModel.uiState.value.hourToDelete)

            viewModel.onEvent(ScheduleScreenEvent.ConfirmDeleteHour(hour))
            advanceUntilIdle()

            assertEquals(null, viewModel.uiState.value.hourToDelete)
            coVerify(exactly = 1) { deleteHourUseCase(hour.id) }
        }

    @Test
    fun `hour delete failure surfaces error`() =
        runTest {
            val hour = AvailableLessonHour(1, DayOfWeek.SATURDAY, 540, 5)
            coEvery { deleteHourUseCase(any()) } returns Result.failure(StudentError.Database)
            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(ScheduleScreenEvent.ConfirmDeleteHour(hour))
            viewModel.onEvent(ScheduleScreenEvent.ConfirmDeleteHour(hour))
            advanceUntilIdle()

            assertEquals(
                UiText.StringResource(R.string.error_database_operation_failed),
                viewModel.uiState.value.error
            )
        }

    @Test
    fun `selecting weekday switches hoursForWeekday to that weekday list`() =
        runTest {
            val saturdayHours =
                listOf(hourWith(id = 1, weekday = DayOfWeek.SATURDAY, start = 540, max = 5))
            val sundayHours =
                listOf(hourWith(id = 2, weekday = DayOfWeek.SUNDAY, start = 540, max = 5))
            coEvery { observeHoursForWeekdayUseCase(DayOfWeek.SATURDAY) } returns
                flowOf(Result.success(saturdayHours))
            coEvery { observeHoursForWeekdayUseCase(DayOfWeek.SUNDAY) } returns
                flowOf(Result.success(sundayHours))
            createViewModel()
            advanceUntilIdle()

            assertEquals(saturdayHours, viewModel.uiState.value.hoursForWeekday)

            viewModel.onEvent(ScheduleScreenEvent.SelectWeekday(DayOfWeek.SUNDAY))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(DayOfWeek.SUNDAY, state.selectedWeekday)
            assertEquals(sundayHours, state.hoursForWeekday)
        }

    @Test
    fun `hour update capacity-drop validation failure surfaces inline error`() =
        runTest {
            coEvery { observeHoursForWeekdayUseCase(any()) } returns
                flowOf(Result.success(emptyList()))
            createViewModel()
            advanceUntilIdle()

            val existing = AvailableLessonHour(1, DayOfWeek.SATURDAY, 540, 5)
            viewModel.onEvent(ScheduleScreenEvent.OpenHourDialog(existing))
            viewModel.onEvent(ScheduleScreenEvent.SetHourMaxStudents("2"))
            coEvery { updateHourUseCase(any(), any(), any(), any()) } returns
                Result.failure(StudentError.MaxBelowAssigned)

            viewModel.onEvent(ScheduleScreenEvent.SaveHour)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(true, state.isHourDialogOpen)
            assertEquals(
                UiText.StringResource(R.string.error_max_below_assigned),
                state.hourValidationError
            )
        }

    @Test
    fun `hour update onSuccess with removed assignments sets removal toast`() =
        runTest {
            coEvery { observeHoursForWeekdayUseCase(any()) } returns
                flowOf(Result.success(emptyList()))
            coEvery { studentManagementUseCases.getAllStudentsUseCase(SortType.ByName, null) } returns
                flowOf(Result.success(listOf(Student(1, "Ali"))))
            createViewModel()
            advanceUntilIdle()

            val existing = AvailableLessonHour(1, DayOfWeek.SATURDAY, 540, 5)
            viewModel.onEvent(ScheduleScreenEvent.OpenHourDialog(existing))
            coEvery { updateHourUseCase(any(), any(), any(), any()) } returns
                Result.success(AssignmentRemovalReport(listOf(1)))

            viewModel.onEvent(ScheduleScreenEvent.SaveHour)
            advanceUntilIdle()

            assertEquals(
                UiText.PluralResource(
                    R.plurals.removed_from_hour,
                    1,
                    UiText.FormattedTimeText(540),
                    "Ali"
                ),
                viewModel.uiState.value.toastMessage
            )
        }

    private fun hourWith(
        id: Int,
        weekday: DayOfWeek = DayOfWeek.SATURDAY,
        start: Int,
        max: Int,
        assigned: Int = 0
    ): HourWithOccupancy =
        HourWithOccupancy(
            hour = AvailableLessonHour(id, weekday, start, max),
            endMinutes = start + 60,
            assignedStudentIds = (1..assigned).toList(),
            assignedCount = assigned,
            maxStudents = max,
            remainingSlots = max - assigned,
            isFull = assigned >= max
        )
}