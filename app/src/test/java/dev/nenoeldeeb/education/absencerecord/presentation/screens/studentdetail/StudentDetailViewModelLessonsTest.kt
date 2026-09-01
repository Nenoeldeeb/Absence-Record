package dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail

import dev.nenoeldeeb.education.absencerecord.R
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

@OptIn(ExperimentalCoroutinesApi::class)
class StudentDetailViewModelLessonsTest : StudentDetailViewModelTestBase() {
    @BeforeEach
    fun setUp() {
        commonSetUp()
    }

    private fun TestScope.setUpWith(
        schedule: StudentScheduleView = StudentScheduleView(1, emptyList(), emptyList()),
        hours: List<HourWithOccupancy> = emptyList()
    ) {
        every { getStudentByIdUseCase(1) } returns flowOf(Result.success(aStudent))
        every { observeStudentScheduleUseCase(1) } returns flowOf(Result.success(schedule))
        every { observeHoursForWeekdayUseCase(any()) } returns flowOf(Result.success(hours))
        createViewModel(studentId = 1)
        advanceUntilIdle()
    }

    private fun lessonAt(
        weekday: DayOfWeek = DayOfWeek.SATURDAY,
        start: Int = 540
    ): StudentLessonEntry = StudentLessonEntry(weekday, start, start + 60)

    private fun hourWith(
        id: Int,
        start: Int,
        assignedStudentIds: List<Int> = emptyList(),
        max: Int = 5
    ): HourWithOccupancy =
        HourWithOccupancy(
            hour = AvailableLessonHour(id, DayOfWeek.SATURDAY, start, max),
            endMinutes = start + 60,
            assignedStudentIds = assignedStudentIds,
            assignedCount = assignedStudentIds.size,
            maxStudents = max,
            remainingSlots = max - assignedStudentIds.size,
            isFull = assignedStudentIds.size >= max
        )

    @Test
    fun `openAddLessonDialog opens when student has no lesson on the selected weekday`() =
        runTest {
            setUpWith(
                schedule =
                    StudentScheduleView(
                        1,
                        lessons = listOf(lessonAt(DayOfWeek.MONDAY)),
                        busy = emptyList()
                    )
            )

            viewModel.onEvent(StudentDetailScreenEvent.OpenAddLessonDialog)

            val state = viewModel.uiState.value
            assertEquals(true, state.isAddLessonDialogOpen)
            assertEquals(null, state.selectedLessonHourId)
        }

    @Test
    fun `openAddLessonDialog is a no-op when student already has a lesson on the selected weekday`() =
        runTest {
            setUpWith(schedule = StudentScheduleView(1, lessons = listOf(lessonAt()), busy = emptyList()))

            viewModel.onEvent(StudentDetailScreenEvent.OpenAddLessonDialog)

            assertEquals(false, viewModel.uiState.value.isAddLessonDialogOpen)
        }

    @Test
    fun `confirmAddLesson success assigns student and closes dialog`() =
        runTest {
            coEvery { assignStudentUseCase(any(), any(), any()) } returns Result.success(Unit)
            setUpWith()

            viewModel.onEvent(StudentDetailScreenEvent.OpenAddLessonDialog)
            viewModel.onEvent(StudentDetailScreenEvent.SelectLessonHour(1))
            viewModel.onEvent(StudentDetailScreenEvent.ConfirmAddLesson)
            advanceUntilIdle()

            coVerify(exactly = 1) { assignStudentUseCase(1, 1, DayOfWeek.SATURDAY) }
            val state = viewModel.uiState.value
            assertEquals(false, state.isAddLessonDialogOpen)
            assertEquals(null, state.selectedLessonHourId)
        }

    @Test
    fun `confirmAddLesson failure sets error and keeps dialog open`() =
        runTest {
            coEvery { assignStudentUseCase(any(), any(), any()) } returns
                Result.failure(StudentError.HourFull)
            setUpWith()

            viewModel.onEvent(StudentDetailScreenEvent.OpenAddLessonDialog)
            viewModel.onEvent(StudentDetailScreenEvent.SelectLessonHour(1))
            viewModel.onEvent(StudentDetailScreenEvent.ConfirmAddLesson)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(true, state.isAddLessonDialogOpen)
            assertEquals(
                UiText.StringResource(R.string.error_hour_full),
                state.error
            )
        }

    @Test
    fun `confirmAddLesson without a selected hour does not call use case`() =
        runTest {
            coEvery { assignStudentUseCase(any(), any(), any()) } returns Result.success(Unit)
            setUpWith()

            viewModel.onEvent(StudentDetailScreenEvent.OpenAddLessonDialog)
            viewModel.onEvent(StudentDetailScreenEvent.ConfirmAddLesson)
            advanceUntilIdle()

            coVerify(exactly = 0) { assignStudentUseCase(any(), any(), any()) }
            assertEquals(true, viewModel.uiState.value.isAddLessonDialogOpen)
        }

    @Test
    fun `unassignLesson resolves hour id from hoursForWeekday and unassigns`() =
        runTest {
            coEvery { unassignStudentUseCase(any(), any()) } returns Result.success(Unit)
            setUpWith(
                schedule = StudentScheduleView(1, lessons = listOf(lessonAt()), busy = emptyList()),
                hours = listOf(hourWith(id = 2, start = 540, assignedStudentIds = listOf(1)))
            )

            viewModel.onEvent(StudentDetailScreenEvent.UnassignLesson(lessonAt()))
            advanceUntilIdle()

            coVerify(exactly = 1) { unassignStudentUseCase(2, 1) }
            assertEquals(null, viewModel.uiState.value.error)
        }

    @Test
    fun `unassignLesson failure sets error`() =
        runTest {
            coEvery { unassignStudentUseCase(any(), any()) } returns
                Result.failure(StudentError.Database)
            setUpWith(
                schedule = StudentScheduleView(1, lessons = listOf(lessonAt()), busy = emptyList()),
                hours = listOf(hourWith(id = 2, start = 540, assignedStudentIds = listOf(1)))
            )

            viewModel.onEvent(StudentDetailScreenEvent.UnassignLesson(lessonAt()))
            advanceUntilIdle()

            assertEquals(
                UiText.StringResource(R.string.error_database_operation_failed),
                viewModel.uiState.value.error
            )
        }

    @Test
    fun `unassignLesson with no matching hour does nothing`() =
        runTest {
            coEvery { unassignStudentUseCase(any(), any()) } returns Result.success(Unit)
            setUpWith(
                schedule = StudentScheduleView(1, lessons = listOf(lessonAt()), busy = emptyList()),
                hours = emptyList()
            )

            viewModel.onEvent(StudentDetailScreenEvent.UnassignLesson(lessonAt()))
            advanceUntilIdle()

            coVerify(exactly = 0) { unassignStudentUseCase(any(), any()) }
        }
}