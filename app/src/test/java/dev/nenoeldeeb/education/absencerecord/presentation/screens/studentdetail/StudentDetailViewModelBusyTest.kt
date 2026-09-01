package dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail

import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.AssignmentRemovalReport
import dev.nenoeldeeb.education.absencerecord.domain.models.BusyAppointment
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentLessonEntry
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentScheduleView
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class StudentDetailViewModelBusyTest : StudentDetailViewModelTestBase() {
    @BeforeEach
    fun setUp() {
        commonSetUp()
    }

    private fun TestScope.setUpWith(schedule: StudentScheduleView = StudentScheduleView(1, emptyList(), emptyList())) {
        every { getStudentByIdUseCase(1) } returns flowOf(Result.success(aStudent))
        every { observeStudentScheduleUseCase(1) } returns flowOf(Result.success(schedule))
        every { observeHoursForWeekdayUseCase(any()) } returns flowOf(Result.success(emptyList()))
        createViewModel(studentId = 1)
        advanceUntilIdle()
    }

    private fun lessonAt(
        weekday: DayOfWeek = DayOfWeek.SATURDAY,
        start: Int = 540
    ): StudentLessonEntry = StudentLessonEntry(weekday, start, start + 60)

    private fun anExistingBusy(): BusyAppointment =
        BusyAppointment(
            id = 7,
            studentId = 1,
            weekday = DayOfWeek.SATURDAY,
            startMinutes = 510,
            durationMinutes = 60
        )

    @Test
    fun `openBusyDialog for add sets defaults`() =
        runTest {
            setUpWith()
            viewModel.onEvent(StudentDetailScreenEvent.OpenBusyDialog(null))

            val state = viewModel.uiState.value
            assertEquals(true, state.isBusyDialogOpen)
            assertEquals(null, state.editingBusyAppointment)
            assertEquals(0, state.busyStartMinutes)
            assertEquals(30, state.busyDurationMinutes)
            assertEquals(null, state.busyValidationError)
            assertEquals(emptyList(), state.busyConflictLessons)
        }

    @Test
    fun `openBusyDialog for edit prefills appointment values`() =
        runTest {
            setUpWith()
            viewModel.onEvent(StudentDetailScreenEvent.OpenBusyDialog(anExistingBusy()))

            assertEquals(true, viewModel.uiState.value.isBusyDialogOpen)
            assertEquals(510, viewModel.uiState.value.busyStartMinutes)
            assertEquals(60, viewModel.uiState.value.busyDurationMinutes)
        }

    @Test
    fun `openBusyDialog for edit snaps off-grid duration to nearest 30 minute step`() =
        runTest {
            setUpWith()
            viewModel.onEvent(
                StudentDetailScreenEvent.OpenBusyDialog(anExistingBusy().copy(durationMinutes = 45))
            )
            assertEquals(60, viewModel.uiState.value.busyDurationMinutes)
            assertEquals(
                anExistingBusy().copy(durationMinutes = 45),
                viewModel.uiState.value.editingBusyAppointment
            )
        }

    @Test
    fun `openBusyDialog for edit clamps duration into 30 to 360 range`() =
        runTest {
            setUpWith()
            viewModel.onEvent(
                StudentDetailScreenEvent.OpenBusyDialog(anExistingBusy().copy(durationMinutes = 400))
            )
            assertEquals(360, viewModel.uiState.value.busyDurationMinutes)
            viewModel.onEvent(
                StudentDetailScreenEvent.OpenBusyDialog(anExistingBusy().copy(durationMinutes = 20))
            )
            assertEquals(30, viewModel.uiState.value.busyDurationMinutes)
        }

    @Test
    fun `dismissBusyDialog closes and clears editing and conflicts`() =
        runTest {
            setUpWith()
            viewModel.onEvent(StudentDetailScreenEvent.OpenBusyDialog(null))
            viewModel.onEvent(StudentDetailScreenEvent.SetBusyStart(600))
            viewModel.onEvent(StudentDetailScreenEvent.SetBusyDuration(90))
            viewModel.onEvent(StudentDetailScreenEvent.DismissBusyDialog)

            val state = viewModel.uiState.value
            assertEquals(false, state.isBusyDialogOpen)
            assertEquals(null, state.editingBusyAppointment)
            assertEquals(null, state.busyValidationError)
            assertEquals(emptyList(), state.busyConflictLessons)
        }

    @Test
    fun `setBusyStart and setBusyDuration clear pending conflicts`() =
        runTest {
            setUpWith(schedule = StudentScheduleView(1, lessons = listOf(lessonAt()), busy = emptyList()))

            viewModel.onEvent(StudentDetailScreenEvent.OpenBusyDialog(null))
            viewModel.onEvent(StudentDetailScreenEvent.SetBusyStart(510))
            viewModel.onEvent(StudentDetailScreenEvent.SetBusyDuration(60))
            viewModel.onEvent(StudentDetailScreenEvent.SaveBusyAppointment)

            assertEquals(true, viewModel.uiState.value.busyConflictLessons.isNotEmpty())

            viewModel.onEvent(StudentDetailScreenEvent.SetBusyStart(1200))
            assertEquals(emptyList(), viewModel.uiState.value.busyConflictLessons)
        }

    @Test
    fun `saveBusyAppointment with invalid duration sets validation error without saving`() =
        runTest {
            coEvery { insertBusyAppointmentUseCase(any(), any(), any(), any()) } returns
                Result.success(AssignmentRemovalReport(emptyList()))
            setUpWith()

            viewModel.onEvent(StudentDetailScreenEvent.OpenBusyDialog(null))
            viewModel.onEvent(StudentDetailScreenEvent.SetBusyStart(1420))
            viewModel.onEvent(StudentDetailScreenEvent.SetBusyDuration(60))
            viewModel.onEvent(StudentDetailScreenEvent.SaveBusyAppointment)
            advanceUntilIdle()

            assertEquals(
                UiText.StringResource(R.string.error_busy_duration_invalid),
                viewModel.uiState.value.busyValidationError
            )
            coVerify(exactly = 0) { insertBusyAppointmentUseCase(any(), any(), any(), any()) }
        }

    @Test
    fun `saveBusyAppointment with no conflicts inserts and closes dialog`() =
        runTest {
            coEvery { insertBusyAppointmentUseCase(any(), any(), any(), any()) } returns
                Result.success(AssignmentRemovalReport(emptyList()))
            setUpWith()

            viewModel.onEvent(StudentDetailScreenEvent.OpenBusyDialog(null))
            viewModel.onEvent(StudentDetailScreenEvent.SetBusyStart(540))
            viewModel.onEvent(StudentDetailScreenEvent.SetBusyDuration(60))
            viewModel.onEvent(StudentDetailScreenEvent.SaveBusyAppointment)
            advanceUntilIdle()

            coVerify(exactly = 1) { insertBusyAppointmentUseCase(1, DayOfWeek.SATURDAY, 540, 60) }
            val state = viewModel.uiState.value
            assertEquals(false, state.isBusyDialogOpen)
            assertEquals(emptyList(), state.busyConflictLessons)
        }

    @Test
    fun `saveBusyAppointment with conflicting lesson shows preview without saving`() =
        runTest {
            coEvery { insertBusyAppointmentUseCase(any(), any(), any(), any()) } returns
                Result.success(AssignmentRemovalReport(emptyList()))
            setUpWith(schedule = StudentScheduleView(1, lessons = listOf(lessonAt()), busy = emptyList()))

            viewModel.onEvent(StudentDetailScreenEvent.OpenBusyDialog(null))
            viewModel.onEvent(StudentDetailScreenEvent.SetBusyStart(510))
            viewModel.onEvent(StudentDetailScreenEvent.SetBusyDuration(60))
            viewModel.onEvent(StudentDetailScreenEvent.SaveBusyAppointment)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(listOf(lessonAt()), state.busyConflictLessons)
            assertEquals(true, state.isBusyDialogOpen)
            coVerify(exactly = 0) { insertBusyAppointmentUseCase(any(), any(), any(), any()) }
        }

    @Test
    fun `saveBusyAppointment ending exactly at lesson start is not a conflict`() =
        runTest {
            coEvery { insertBusyAppointmentUseCase(any(), any(), any(), any()) } returns
                Result.success(AssignmentRemovalReport(emptyList()))
            setUpWith(schedule = StudentScheduleView(1, lessons = listOf(lessonAt()), busy = emptyList()))

            viewModel.onEvent(StudentDetailScreenEvent.OpenBusyDialog(null))
            viewModel.onEvent(StudentDetailScreenEvent.SetBusyStart(600))
            viewModel.onEvent(StudentDetailScreenEvent.SetBusyDuration(60))
            viewModel.onEvent(StudentDetailScreenEvent.SaveBusyAppointment)
            advanceUntilIdle()

            assertEquals(emptyList(), viewModel.uiState.value.busyConflictLessons)
            coVerify(exactly = 1) { insertBusyAppointmentUseCase(1, DayOfWeek.SATURDAY, 600, 60) }
        }

    @Test
    fun `confirmBusyConflict inserts after teacher confirms and closes dialog`() =
        runTest {
            coEvery { insertBusyAppointmentUseCase(any(), any(), any(), any()) } returns
                Result.success(AssignmentRemovalReport(emptyList()))
            setUpWith(schedule = StudentScheduleView(1, lessons = listOf(lessonAt()), busy = emptyList()))

            viewModel.onEvent(StudentDetailScreenEvent.OpenBusyDialog(null))
            viewModel.onEvent(StudentDetailScreenEvent.SetBusyStart(510))
            viewModel.onEvent(StudentDetailScreenEvent.SetBusyDuration(60))
            viewModel.onEvent(StudentDetailScreenEvent.SaveBusyAppointment)
            viewModel.onEvent(StudentDetailScreenEvent.ConfirmBusyConflict)
            advanceUntilIdle()

            coVerify(exactly = 1) { insertBusyAppointmentUseCase(1, DayOfWeek.SATURDAY, 510, 60) }
            val state = viewModel.uiState.value
            assertEquals(false, state.isBusyDialogOpen)
            assertEquals(emptyList(), state.busyConflictLessons)
        }

    @Test
    fun `confirmBusyConflict failure sets validation error and keeps dialog open`() =
        runTest {
            coEvery { insertBusyAppointmentUseCase(any(), any(), any(), any()) } returns
                Result.failure(StudentError.BusyConflict)
            setUpWith(schedule = StudentScheduleView(1, lessons = listOf(lessonAt()), busy = emptyList()))

            viewModel.onEvent(StudentDetailScreenEvent.OpenBusyDialog(null))
            viewModel.onEvent(StudentDetailScreenEvent.SetBusyStart(510))
            viewModel.onEvent(StudentDetailScreenEvent.SetBusyDuration(60))
            viewModel.onEvent(StudentDetailScreenEvent.SaveBusyAppointment)
            viewModel.onEvent(StudentDetailScreenEvent.ConfirmBusyConflict)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(
                UiText.StringResource(R.string.error_busy_conflict),
                state.busyValidationError
            )
            assertEquals(true, state.isBusyDialogOpen)
            assertEquals(emptyList(), state.busyConflictLessons)
        }

    @Test
    fun `editing busy appointment updates instead of inserts`() =
        runTest {
            coEvery { updateBusyAppointmentUseCase(any(), any(), any()) } returns
                Result.success(AssignmentRemovalReport(emptyList()))
            setUpWith()

            viewModel.onEvent(StudentDetailScreenEvent.OpenBusyDialog(anExistingBusy()))
            viewModel.onEvent(StudentDetailScreenEvent.SetBusyStart(600))
            viewModel.onEvent(StudentDetailScreenEvent.SaveBusyAppointment)
            advanceUntilIdle()

            coVerify(exactly = 1) { updateBusyAppointmentUseCase(7, 600, 60) }
            coVerify(exactly = 0) { insertBusyAppointmentUseCase(any(), any(), any(), any()) }
            assertEquals(false, viewModel.uiState.value.isBusyDialogOpen)
        }

    @Test
    fun `deleteBusyAppointment failure sets error`() =
        runTest {
            coEvery { deleteBusyAppointmentUseCase(any()) } returns
                Result.failure(StudentError.Database)
            setUpWith()

            viewModel.onEvent(StudentDetailScreenEvent.DeleteBusyAppointment(7))
            advanceUntilIdle()

            assertEquals(
                UiText.StringResource(R.string.error_database_operation_failed),
                viewModel.uiState.value.error
            )
        }

    @Test
    fun `deleteBusyAppointment success does not set error`() =
        runTest {
            coEvery { deleteBusyAppointmentUseCase(any()) } returns Result.success(Unit)
            setUpWith()

            viewModel.onEvent(StudentDetailScreenEvent.DeleteBusyAppointment(7))
            advanceUntilIdle()

            coVerify(exactly = 1) { deleteBusyAppointmentUseCase(7) }
            assertEquals(null, viewModel.uiState.value.error)
        }
}