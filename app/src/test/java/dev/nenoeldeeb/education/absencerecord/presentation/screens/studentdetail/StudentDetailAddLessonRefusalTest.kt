package dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail

import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.AssignmentRemovalReport
import dev.nenoeldeeb.education.absencerecord.domain.models.AvailableLessonHour
import dev.nenoeldeeb.education.absencerecord.domain.models.HourWithOccupancy
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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Add-lesson refusals reach the teacher as the SPECIFIC reason, not a generic
 * failure (FR-050), and a refusal leaves the dialog usable (FR-036).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StudentDetailAddLessonRefusalTest : StudentDetailViewModelTestBase() {
    @BeforeEach
    fun setUp() {
        commonSetUp()
    }

    /** A block the student can be offered, with no lessons yet. */
    private fun offerableHours(
        id: Int = 1,
        start: Int = 540,
        duration: Int = 60
    ): List<HourWithOccupancy> =
        listOf(
            HourWithOccupancy(
                hour = AvailableLessonHour(id, DayOfWeek.SATURDAY, start, duration),
                endMinutes = start + duration,
                assignedStudentIds = emptyList(),
                assignedCount = 0,
                slots = emptyList()
            )
        )

    private fun noLessons(): StudentScheduleView = StudentScheduleView(studentId = 1, lessons = emptyList(), busy = emptyList())

    private fun TestScope.startViewModelWith(hours: List<HourWithOccupancy>) {
        every { getStudentByIdUseCase(1) } returns flowOf(Result.success(aStudent))
        every { observeStudentScheduleUseCase(1) } returns flowOf(Result.success(noLessons()))
        every { observeHoursForWeekdayUseCase(any()) } returns flowOf(Result.success(hours))
        createViewModel(studentId = 1)
        advanceUntilIdle()
    }

    private fun TestScope.attemptAdd(hourId: Int = 1) {
        viewModel.onEvent(StudentDetailScreenEvent.OpenAddLessonDialog)
        advanceUntilIdle()
        viewModel.onEvent(StudentDetailScreenEvent.SelectLessonHour(hourId))
        viewModel.onEvent(StudentDetailScreenEvent.ConfirmAddLesson)
        advanceUntilIdle()
    }

    // region each refusal names its own reason (FR-050)

    @Test
    fun `a slot conflict refusal shows the conflict message`() =
        runTest {
            startViewModelWith(offerableHours())
            coEvery { assignStudentUseCase(any(), any(), any()) } returns
                Result.failure(StudentError.BusyConflict)

            attemptAdd()

            assertEquals(UiText.StringResource(R.string.error_busy_conflict), viewModel.uiState.value.error)
        }

    @Test
    fun `a full block refusal shows the full message`() =
        runTest {
            startViewModelWith(offerableHours())
            coEvery { assignStudentUseCase(any(), any(), any()) } returns
                Result.failure(StudentError.HourFull)

            attemptAdd()

            assertEquals(UiText.StringResource(R.string.error_hour_full), viewModel.uiState.value.error)
        }

    @Test
    fun `a second lesson refusal shows the already-booked message`() =
        runTest {
            startViewModelWith(offerableHours())
            coEvery { assignStudentUseCase(any(), any(), any()) } returns
                Result.failure(StudentError.AssignmentExists)

            attemptAdd()

            assertEquals(
                UiText.StringResource(R.string.error_assignment_exists),
                viewModel.uiState.value.error
            )
        }

    @Test
    fun `each refusal maps to a distinct message`() =
        runTest {
            val expected =
                mapOf(
                    StudentError.BusyConflict to R.string.error_busy_conflict,
                    StudentError.HourFull to R.string.error_hour_full,
                    StudentError.AssignmentExists to R.string.error_assignment_exists,
                    StudentError.HourOverlap to R.string.error_hour_overlap
                )
            for ((error, stringRes) in expected) {
                startViewModelWith(offerableHours())
                coEvery { assignStudentUseCase(any(), any(), any()) } returns Result.failure(error)

                attemptAdd()

                assertEquals(
                    UiText.StringResource(stringRes),
                    viewModel.uiState.value.error,
                    "$error must read as its own reason"
                )
            }
        }

    // endregion

    // region a refusal leaves the dialog usable and changes nothing (FR-036)

    @Test
    fun `a refusal keeps the dialog open so the teacher can pick another hour`() =
        runTest {
            startViewModelWith(offerableHours(id = 1))
            coEvery { assignStudentUseCase(any(), any(), any()) } returns
                Result.failure(StudentError.BusyConflict)

            attemptAdd()

            val state = viewModel.uiState.value
            assertTrue(state.isAddLessonDialogOpen, "the dialog must stay open after a refusal")
            assertEquals(1, state.selectedLessonHourId)
        }

    @Test
    fun `a refusal does not clear the stored lessons`() =
        runTest {
            val booked =
                StudentScheduleView(
                    studentId = 1,
                    lessons =
                        listOf(
                            StudentLessonEntry(
                                DayOfWeek.SATURDAY,
                                540,
                                600,
                                slotStartMinutes = 540,
                                slotEndMinutes = 550
                            )
                        ),
                    busy = emptyList()
                )
            every { getStudentByIdUseCase(1) } returns flowOf(Result.success(aStudent))
            every { observeStudentScheduleUseCase(1) } returns flowOf(Result.success(booked))
            every { observeHoursForWeekdayUseCase(any()) } returns
                flowOf(Result.success(offerableHours()))
            createViewModel(studentId = 1)
            advanceUntilIdle()
            coEvery { assignStudentUseCase(any(), any(), any()) } returns
                Result.failure(StudentError.BusyConflict)

            attemptAdd()

            assertEquals(1, viewModel.uiState.value.studentSchedule?.lessons?.size)
        }

    @Test
    fun `a refusal leaves no undo entry behind`() =
        runTest {
            startViewModelWith(offerableHours())
            coEvery { assignStudentUseCase(any(), any(), any()) } returns
                Result.failure(StudentError.BusyConflict)

            attemptAdd()

            // only a REMOVAL offers undo; a refused add must not
            assertNull(viewModel.uiState.value.deletedLesson)
            assertNull(viewModel.uiState.value.deletedLessonHourId)
        }

    // endregion

    // region the success path still closes the dialog

    @Test
    fun `a successful add closes the dialog and clears the error`() =
        runTest {
            startViewModelWith(offerableHours())
            coEvery { assignStudentUseCase(any(), any(), any()) } returns Result.success(Unit)

            attemptAdd()

            val state = viewModel.uiState.value
            assertFalse(state.isAddLessonDialogOpen)
            assertNull(state.selectedLessonHourId)
            assertNull(state.error)
        }

    @Test
    fun `a successful add reaches the repository with the selected hour`() =
        runTest {
            startViewModelWith(offerableHours(id = 4))
            coEvery { assignStudentUseCase(any(), any(), any()) } returns Result.success(Unit)

            attemptAdd(hourId = 4)

            coVerify { assignStudentUseCase(any(), any(), any()) }
        }

    // endregion

    // region commitment edits report what they evicted

    @Test
    fun `a commitment landing on a lesson is previewed, not applied silently`() =
        runTest {
            // The preview is computed from the student's own lessons, so the
            // fixture must hold one for the conflict branch to be reachable.
            val lesson = StudentLessonEntry(DayOfWeek.SATURDAY, 540, 600, 540, 550)
            every { getStudentByIdUseCase(1) } returns flowOf(Result.success(aStudent))
            every { observeStudentScheduleUseCase(1) } returns
                flowOf(Result.success(StudentScheduleView(1, listOf(lesson), emptyList())))
            every { observeHoursForWeekdayUseCase(any()) } returns flowOf(Result.success(emptyList()))
            createViewModel(studentId = 1)
            advanceUntilIdle()
            coEvery { insertBusyAppointmentUseCase(any(), any(), any(), any()) } returns
                Result.success(AssignmentRemovalReport(listOf(1)))

            viewModel.onEvent(StudentDetailScreenEvent.OpenBusyDialog(null))
            advanceUntilIdle()
            viewModel.onEvent(StudentDetailScreenEvent.SetBusyStart(540))
            viewModel.onEvent(StudentDetailScreenEvent.SetBusyDuration(30))
            viewModel.onEvent(StudentDetailScreenEvent.SaveBusyAppointment)
            advanceUntilIdle()

            // HELD for the teacher to confirm; the write has not happened
            assertEquals(listOf(lesson), viewModel.uiState.value.busyConflictLessons)
            coVerify(exactly = 0) { insertBusyAppointmentUseCase(any(), any(), any(), any()) }
        }

    @Test
    fun `a commitment clear of every lesson saves without asking`() =
        runTest {
            startViewModelWith(offerableHours())
            coEvery { insertBusyAppointmentUseCase(any(), any(), any(), any()) } returns
                Result.success(AssignmentRemovalReport(emptyList()))

            viewModel.onEvent(StudentDetailScreenEvent.OpenBusyDialog(null))
            advanceUntilIdle()
            viewModel.onEvent(StudentDetailScreenEvent.SetBusyStart(1200))
            viewModel.onEvent(StudentDetailScreenEvent.SetBusyDuration(30))
            viewModel.onEvent(StudentDetailScreenEvent.SaveBusyAppointment)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.busyConflictLessons.isEmpty())
            coVerify(exactly = 1) { insertBusyAppointmentUseCase(any(), any(), any(), any()) }
        }

    @Test
    fun `an invalid commitment length is rejected before any write`() =
        runTest {
            startViewModelWith(offerableHours())
            coEvery { insertBusyAppointmentUseCase(any(), any(), any(), any()) } returns
                Result.success(AssignmentRemovalReport(emptyList()))

            viewModel.onEvent(StudentDetailScreenEvent.OpenBusyDialog(null))
            advanceUntilIdle()
            viewModel.onEvent(StudentDetailScreenEvent.SetBusyStart(1200))
            viewModel.onEvent(StudentDetailScreenEvent.SetBusyDuration(45)) // not a 30-min step
            viewModel.onEvent(StudentDetailScreenEvent.SaveBusyAppointment)
            advanceUntilIdle()

            assertEquals(
                UiText.StringResource(R.string.error_busy_duration_invalid),
                viewModel.uiState.value.busyValidationError
            )
            coVerify(exactly = 0) { insertBusyAppointmentUseCase(any(), any(), any(), any()) }
        }

    // endregion
}