package dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail

import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.AssignmentRemovalReport
import dev.nenoeldeeb.education.absencerecord.domain.models.AvailableLessonHour
import dev.nenoeldeeb.education.absencerecord.domain.models.BusyAppointment
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
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class StudentDetailViewModelUndoTest : StudentDetailViewModelTestBase() {
    @BeforeEach
    fun setUp() {
        commonSetUp()
    }

    private fun lessonAt(): StudentLessonEntry = StudentLessonEntry(DayOfWeek.SATURDAY, 540, 600)

    private fun hourWith(): HourWithOccupancy =
        HourWithOccupancy(
            hour = AvailableLessonHour(2, DayOfWeek.SATURDAY, 540, 5),
            endMinutes = 600,
            assignedStudentIds = listOf(1),
            assignedCount = 1,
            maxStudents = 5,
            remainingSlots = 4,
            isFull = false
        )

    private fun busyAt(): BusyAppointment =
        BusyAppointment(
            id = 7,
            studentId = 1,
            weekday = DayOfWeek.SATURDAY,
            startMinutes = 510,
            durationMinutes = 60
        )

    private fun TestScope.setUpWith(
        schedule: StudentScheduleView,
        hours: List<HourWithOccupancy> = emptyList()
    ) {
        every { getStudentByIdUseCase(1) } returns flowOf(Result.success(aStudent))
        every { observeStudentScheduleUseCase(1) } returns flowOf(Result.success(schedule))
        every { observeHoursForWeekdayUseCase(any()) } returns flowOf(Result.success(hours))
        createViewModel(studentId = 1)
        advanceUntilIdle()
    }

    @Test
    fun `unassign success stores undoable lesson`() =
        runTest {
            coEvery { unassignStudentUseCase(any(), any()) } returns Result.success(Unit)
            setUpWith(
                schedule = StudentScheduleView(1, listOf(lessonAt()), emptyList()),
                hours = listOf(hourWith())
            )

            viewModel.onEvent(StudentDetailScreenEvent.UnassignLesson(lessonAt()))
            advanceUntilIdle()

            assertEquals(lessonAt(), viewModel.uiState.value.deletedLesson)
            assertEquals(2, viewModel.uiState.value.deletedLessonHourId)
        }

    @Test
    fun `undo unassign success reassigns and clears`() =
        runTest {
            coEvery { unassignStudentUseCase(any(), any()) } returns Result.success(Unit)
            coEvery { assignStudentUseCase(any(), any(), any()) } returns Result.success(Unit)
            setUpWith(
                schedule = StudentScheduleView(1, listOf(lessonAt()), emptyList()),
                hours = listOf(hourWith())
            )

            viewModel.onEvent(StudentDetailScreenEvent.UnassignLesson(lessonAt()))
            advanceUntilIdle()
            viewModel.onEvent(StudentDetailScreenEvent.UndoUnassignLesson)
            advanceUntilIdle()

            coVerify(exactly = 1) { assignStudentUseCase(2, 1, DayOfWeek.SATURDAY) }
            assertNull(viewModel.uiState.value.deletedLesson)
            assertNull(viewModel.uiState.value.deletedLessonHourId)
            assertNull(viewModel.uiState.value.error)
        }

    @Test
    fun `undo unassign failure sets error`() =
        runTest {
            coEvery { unassignStudentUseCase(any(), any()) } returns Result.success(Unit)
            coEvery { assignStudentUseCase(any(), any(), any()) } returns
                Result.failure(StudentError.HourFull)
            setUpWith(
                schedule = StudentScheduleView(1, listOf(lessonAt()), emptyList()),
                hours = listOf(hourWith())
            )

            viewModel.onEvent(StudentDetailScreenEvent.UnassignLesson(lessonAt()))
            advanceUntilIdle()
            viewModel.onEvent(StudentDetailScreenEvent.UndoUnassignLesson)
            advanceUntilIdle()

            assertEquals(
                UiText.StringResource(R.string.error_hour_full),
                viewModel.uiState.value.error
            )
            assertNull(viewModel.uiState.value.deletedLesson)
        }

    @Test
    fun `undo unassign without stored lesson does nothing`() =
        runTest {
            coEvery { assignStudentUseCase(any(), any(), any()) } returns Result.success(Unit)
            setUpWith(schedule = StudentScheduleView(1, emptyList(), emptyList()))

            viewModel.onEvent(StudentDetailScreenEvent.UndoUnassignLesson)
            advanceUntilIdle()

            coVerify(exactly = 0) { assignStudentUseCase(any(), any(), any()) }
        }

    @Test
    fun `consume deleted lesson clears undoable`() =
        runTest {
            coEvery { unassignStudentUseCase(any(), any()) } returns Result.success(Unit)
            setUpWith(
                schedule = StudentScheduleView(1, listOf(lessonAt()), emptyList()),
                hours = listOf(hourWith())
            )

            viewModel.onEvent(StudentDetailScreenEvent.UnassignLesson(lessonAt()))
            advanceUntilIdle()
            viewModel.onEvent(StudentDetailScreenEvent.ConsumeDeletedLesson)

            assertNull(viewModel.uiState.value.deletedLesson)
            assertNull(viewModel.uiState.value.deletedLessonHourId)
        }

    @Test
    fun `delete busy success stores undoable appointment`() =
        runTest {
            coEvery { deleteBusyAppointmentUseCase(any()) } returns Result.success(Unit)
            setUpWith(schedule = StudentScheduleView(1, emptyList(), listOf(busyAt())))

            viewModel.onEvent(StudentDetailScreenEvent.DeleteBusyAppointment(7))
            advanceUntilIdle()

            coVerify(exactly = 1) { deleteBusyAppointmentUseCase(7) }
            assertEquals(busyAt(), viewModel.uiState.value.deletedBusy)
        }

    @Test
    fun `undo delete busy success reinserts and clears`() =
        runTest {
            coEvery { deleteBusyAppointmentUseCase(any()) } returns Result.success(Unit)
            coEvery { insertBusyAppointmentUseCase(any(), any(), any(), any()) } returns
                Result.success(AssignmentRemovalReport(emptyList()))
            setUpWith(schedule = StudentScheduleView(1, emptyList(), listOf(busyAt())))

            viewModel.onEvent(StudentDetailScreenEvent.DeleteBusyAppointment(7))
            advanceUntilIdle()
            viewModel.onEvent(StudentDetailScreenEvent.UndoDeleteBusy)
            advanceUntilIdle()

            coVerify(exactly = 1) {
                insertBusyAppointmentUseCase(1, DayOfWeek.SATURDAY, 510, 60)
            }
            assertNull(viewModel.uiState.value.deletedBusy)
            assertNull(viewModel.uiState.value.error)
        }

    @Test
    fun `undo delete busy failure sets error`() =
        runTest {
            coEvery { deleteBusyAppointmentUseCase(any()) } returns Result.success(Unit)
            coEvery { insertBusyAppointmentUseCase(any(), any(), any(), any()) } returns
                Result.failure(StudentError.Database)
            setUpWith(schedule = StudentScheduleView(1, emptyList(), listOf(busyAt())))

            viewModel.onEvent(StudentDetailScreenEvent.DeleteBusyAppointment(7))
            advanceUntilIdle()
            viewModel.onEvent(StudentDetailScreenEvent.UndoDeleteBusy)
            advanceUntilIdle()

            assertEquals(
                UiText.StringResource(R.string.error_database_operation_failed),
                viewModel.uiState.value.error
            )
            assertNull(viewModel.uiState.value.deletedBusy)
        }

    @Test
    fun `consume deleted busy clears undoable`() =
        runTest {
            coEvery { deleteBusyAppointmentUseCase(any()) } returns Result.success(Unit)
            setUpWith(schedule = StudentScheduleView(1, emptyList(), listOf(busyAt())))

            viewModel.onEvent(StudentDetailScreenEvent.DeleteBusyAppointment(7))
            advanceUntilIdle()
            viewModel.onEvent(StudentDetailScreenEvent.ConsumeDeletedBusy)

            assertNull(viewModel.uiState.value.deletedBusy)
        }
}