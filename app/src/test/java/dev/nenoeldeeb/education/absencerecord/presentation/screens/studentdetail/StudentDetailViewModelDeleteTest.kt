package dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail

import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.AssignmentRemovalReport
import dev.nenoeldeeb.education.absencerecord.domain.models.AvailableLessonHour
import dev.nenoeldeeb.education.absencerecord.domain.models.BusyAppointment
import dev.nenoeldeeb.education.absencerecord.domain.models.HourWithOccupancy
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentLessonEntry
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentScheduleView
import dev.nenoeldeeb.education.absencerecord.domain.usecases.attendance.RecordStudentAttendanceUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.student.AddStudentUseCase
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class StudentDetailViewModelDeleteTest : StudentDetailViewModelTestBase() {
    private lateinit var addStudentUseCase: AddStudentUseCase
    private lateinit var recordAttendanceUseCase: RecordStudentAttendanceUseCase

    @BeforeEach
    fun setUp() {
        commonSetUp()
        addStudentUseCase = mockk()
        recordAttendanceUseCase = mockk()
        every { studentManagementUseCases.addStudentUseCase } returns addStudentUseCase
        every { attendanceUseCases.recordStudentAttendanceUseCase } returns recordAttendanceUseCase
    }

    private fun TestScope.setUpWith(
        dates: List<LocalDate> = emptyList(),
        schedule: StudentScheduleView = StudentScheduleView(1, emptyList(), emptyList()),
        hours: List<HourWithOccupancy> = emptyList()
    ) {
        every { getStudentByIdUseCase(1) } returns flowOf(Result.success(aStudent))
        every { getStudentAttendanceDatesUseCase(1) } returns flowOf(Result.success(dates))
        every { observeStudentScheduleUseCase(1) } returns flowOf(Result.success(schedule))
        every { observeHoursForWeekdayUseCase(any()) } returns flowOf(Result.success(hours))
        createViewModel(studentId = 1)
        advanceUntilIdle()
    }

    private fun hourAt(
        id: Int = 2,
        start: Int = 540
    ): HourWithOccupancy =
        HourWithOccupancy(
            hour = AvailableLessonHour(id, DayOfWeek.SATURDAY, start, 5),
            endMinutes = start + 60,
            assignedStudentIds = emptyList(),
            assignedCount = 0,
            maxStudents = 5,
            remainingSlots = 5,
            isFull = false
        )

    @Test
    fun `delete success stores undo backup instead of navigating`() =
        runTest {
            coEvery { deleteStudentsUseCase(any()) } returns Result.success(Unit)
            setUpWith(
                dates = listOf(LocalDate(2023, 1, 15)),
                schedule =
                    StudentScheduleView(
                        1,
                        lessons = listOf(StudentLessonEntry(DayOfWeek.SATURDAY, 540, 600)),
                        busy =
                            listOf(
                                BusyAppointment(7, 1, DayOfWeek.SATURDAY, 510, 60)
                            )
                    )
            )

            val effects = mutableListOf<StudentDetailUiEffect>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiEffect.collect { effects.add(it) }
            }

            viewModel.onEvent(StudentDetailScreenEvent.ToggleDeleteConfirmationDialog)
            viewModel.onEvent(StudentDetailScreenEvent.ConfirmDeleteStudent)
            advanceUntilIdle()

            assertTrue(effects.isEmpty())
            assertFalse(viewModel.uiState.value.isDeleteConfirmationDialogOpen)
            assertNull(viewModel.uiState.value.error)
            assertEquals(aStudent, viewModel.uiState.value.deletedStudentBackup?.student)
            assertEquals(
                listOf(LocalDate(2023, 1, 15)),
                viewModel.uiState.value.deletedStudentBackup?.attendanceDates
            )
            assertEquals(1, viewModel.uiState.value.deletedStudentLessons.size)
            assertEquals(1, viewModel.uiState.value.deletedStudentBusy.size)
        }

    @Test
    fun `delete failure sets error and emits no navigation effect`() =
        runTest {
            coEvery { deleteStudentsUseCase(any()) } returns Result.failure(StudentError.Database)
            setUpWith()

            val effects = mutableListOf<StudentDetailUiEffect>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiEffect.collect { effects.add(it) }
            }

            viewModel.onEvent(StudentDetailScreenEvent.ToggleDeleteConfirmationDialog)
            viewModel.onEvent(StudentDetailScreenEvent.ConfirmDeleteStudent)
            advanceUntilIdle()

            assertTrue(effects.isEmpty())
            assertEquals(
                UiText.StringResource(R.string.error_database_operation_failed),
                viewModel.uiState.value.error
            )
            assertNull(viewModel.uiState.value.deletedStudentBackup)
        }

    @Test
    fun `undo delete restores student dates lessons busy then navigates`() =
        runTest {
            coEvery { deleteStudentsUseCase(any()) } returns Result.success(Unit)
            coEvery { addStudentUseCase(any()) } returns Result.success(10L)
            coEvery { recordAttendanceUseCase(any()) } returns Result.success(1L)
            coEvery { assignStudentUseCase(any(), any(), any()) } returns Result.success(Unit)
            coEvery { insertBusyAppointmentUseCase(any(), any(), any(), any()) } returns
                Result.success(AssignmentRemovalReport(emptyList()))
            setUpWith(
                dates = listOf(LocalDate(2023, 1, 15)),
                schedule =
                    StudentScheduleView(
                        1,
                        lessons = listOf(StudentLessonEntry(DayOfWeek.SATURDAY, 540, 600)),
                        busy =
                            listOf(
                                BusyAppointment(7, 1, DayOfWeek.SATURDAY, 510, 60)
                            )
                    ),
                hours = listOf(hourAt())
            )

            val effects = mutableListOf<StudentDetailUiEffect>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiEffect.collect { effects.add(it) }
            }

            viewModel.onEvent(StudentDetailScreenEvent.ConfirmDeleteStudent)
            advanceUntilIdle()
            viewModel.onEvent(StudentDetailScreenEvent.UndoDeleteStudent)
            advanceUntilIdle()

            coVerify(exactly = 1) { addStudentUseCase(match { it.name == "S1" && it.classId == 2 }) }
            coVerify(exactly = 1) { recordAttendanceUseCase(match { it.studentId == 10 }) }
            coVerify(exactly = 1) { assignStudentUseCase(2, 10, DayOfWeek.SATURDAY) }
            coVerify(exactly = 1) {
                insertBusyAppointmentUseCase(10, DayOfWeek.SATURDAY, 510, 60)
            }
            assertNull(viewModel.uiState.value.deletedStudentBackup)
            assertEquals(
                listOf<StudentDetailUiEffect>(StudentDetailUiEffect.NavigateBack),
                effects
            )
        }

    @Test
    fun `undo delete failure sets error`() =
        runTest {
            coEvery { deleteStudentsUseCase(any()) } returns Result.success(Unit)
            coEvery { addStudentUseCase(any()) } returns Result.failure(StudentError.Database)
            setUpWith()

            viewModel.onEvent(StudentDetailScreenEvent.ConfirmDeleteStudent)
            advanceUntilIdle()
            viewModel.onEvent(StudentDetailScreenEvent.UndoDeleteStudent)
            advanceUntilIdle()

            assertEquals(
                UiText.StringResource(R.string.error_database_operation_failed),
                viewModel.uiState.value.error
            )
            assertNull(viewModel.uiState.value.deletedStudentBackup)
        }

    @Test
    fun `dismiss deleted student clears backup and navigates`() =
        runTest {
            coEvery { deleteStudentsUseCase(any()) } returns Result.success(Unit)
            setUpWith()

            val effects = mutableListOf<StudentDetailUiEffect>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiEffect.collect { effects.add(it) }
            }

            viewModel.onEvent(StudentDetailScreenEvent.ConfirmDeleteStudent)
            advanceUntilIdle()
            viewModel.onEvent(StudentDetailScreenEvent.DismissDeletedStudent)
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.deletedStudentBackup)
            assertEquals(
                listOf<StudentDetailUiEffect>(StudentDetailUiEffect.NavigateBack),
                effects
            )
        }
}