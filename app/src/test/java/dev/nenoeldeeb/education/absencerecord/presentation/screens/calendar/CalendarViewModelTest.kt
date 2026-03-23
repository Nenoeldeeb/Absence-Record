package dev.nenoeldeeb.education.absencerecord.presentation.screens.calendar

import dev.nenoeldeeb.education.absencerecord.MainDispatcherRule
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.usecases.AttendanceUseCases
import dev.nenoeldeeb.education.absencerecord.domain.usecases.ClassManagementUseCases
import dev.nenoeldeeb.education.absencerecord.domain.usecases.StudentManagementUseCases
import dev.nenoeldeeb.education.absencerecord.domain.usecases.attendance.DeleteStudentAttendanceUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.attendance.GetAttendanceForDateUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.attendance.GetAvailableMonthsUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.attendance.RecordStudentAttendanceUseCase
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {
    companion object {
        @JvmStatic
        @RegisterExtension
        val mainDispatcherRule = MainDispatcherRule()
    }

    private lateinit var getAvailableMonthsUseCase: GetAvailableMonthsUseCase
    private lateinit var getAttendanceForDateUseCase: GetAttendanceForDateUseCase
    private lateinit var recordStudentAttendanceUseCase: RecordStudentAttendanceUseCase
    private lateinit var deleteStudentAttendanceUseCase: DeleteStudentAttendanceUseCase
    private lateinit var attendanceUseCases: AttendanceUseCases
    private lateinit var studentManagementUseCases: StudentManagementUseCases
    private lateinit var viewModel: CalendarViewModel

    @BeforeEach
    fun setUp() {
        getAvailableMonthsUseCase = mockk(relaxed = true)
        getAttendanceForDateUseCase = mockk(relaxed = true)
        recordStudentAttendanceUseCase = mockk(relaxed = true)
        deleteStudentAttendanceUseCase = mockk(relaxed = true)

        attendanceUseCases = mockk(relaxed = true)
        every { attendanceUseCases.recordStudentAttendanceUseCase } returns recordStudentAttendanceUseCase
        every { attendanceUseCases.deleteStudentAttendanceUseCase } returns deleteStudentAttendanceUseCase
        every { attendanceUseCases.getAttendanceForDateUseCase } returns getAttendanceForDateUseCase
        every { attendanceUseCases.getAvailableMonthsUseCase } returns getAvailableMonthsUseCase

        studentManagementUseCases = mockk(relaxed = true)
    }

    private fun createViewModel() {
        viewModel =
            CalendarViewModel(
                attendanceUseCases,
                studentManagementUseCases,
                mockk<ClassManagementUseCases>(relaxed = true)
            )
    }

    @Test
    fun `init loads data successfully`() =
        runTest {
            // Given
            val months = listOf(LocalDate(2023, Month.JANUARY, 1))
            val students = listOf(Student(1, "S1"))

            coEvery { getAvailableMonthsUseCase() } returns flowOf(Result.success(months))
            coEvery { studentManagementUseCases.getAllStudentsUseCase() } returns flowOf(Result.success(students))

            // When
            createViewModel()
            advanceUntilIdle()

            // Then
            assertEquals(students, viewModel.uiState.value.allStudents)
            assertNull(viewModel.uiState.value.error)
        }

    @Test
    fun `MarkStudentAttendance calls use case and refreshes list`() =
        runTest {
            // Given
            val date = LocalDate(2023, Month.JANUARY, 15)
            coEvery { recordStudentAttendanceUseCase(any()) } returns Result.success(1L)
            coEvery { getAttendanceForDateUseCase(date) } returns flowOf(Result.success(emptyList()))

            createViewModel()

            // When
            viewModel.onEvent(CalendarScreenEvent.MarkStudentAttendance(1, date))
            advanceUntilIdle()

            // Then
            coVerify { recordStudentAttendanceUseCase(match { it.studentId == 1 && it.date == date }) }
        }

    @Test
    fun `MarkStudentAttendance shows error on failure`() =
        runTest {
            // Given
            val date = LocalDate(2023, Month.JANUARY, 15)
            val errorMsg = "Msg"
            coEvery { recordStudentAttendanceUseCase(any()) } returns Result.failure(Exception(errorMsg))
            createViewModel()

            // When
            viewModel.onEvent(CalendarScreenEvent.MarkStudentAttendance(1, date))
            advanceUntilIdle()

            // Then
            assertEquals(
                UiText.StringResource(R.string.error_marking_attendance, errorMsg),
                viewModel.uiState.value.error
            )
        }

    @Test
    fun `DeleteStudentAttendance calls use case and refreshes list`() =
        runTest {
            // Given
            val date = LocalDate(2023, Month.JANUARY, 15)
            coEvery { deleteStudentAttendanceUseCase(any(), any()) } returns Result.success(Unit)
            coEvery { getAttendanceForDateUseCase(date) } returns flowOf(Result.success(emptyList()))

            createViewModel()

            // When
            viewModel.onEvent(CalendarScreenEvent.DeleteStudentAttendance(1, date))
            advanceUntilIdle()

            // Then
            coVerify { deleteStudentAttendanceUseCase(1, date) }
        }

    @Test
    fun `DeleteStudentAttendance failure shows error`() =
        runTest {
            // Given
            val date = LocalDate(2023, Month.JANUARY, 15)
            val errorMsg = "Msg"
            coEvery { deleteStudentAttendanceUseCase(any(), any()) } returns Result.failure(Exception(errorMsg))
            createViewModel()

            // When
            viewModel.onEvent(CalendarScreenEvent.DeleteStudentAttendance(1, date))
            advanceUntilIdle()

            // Then
            assertEquals(
                UiText.StringResource(R.string.error_deleting_attendance, errorMsg),
                viewModel.uiState.value.error
            )
        }

    @Test
    fun `SelectDateForDialog updates state`() =
        runTest {
            createViewModel()
            val date = LocalDate(2023, Month.MARCH, 10)

            // When
            viewModel.onEvent(CalendarScreenEvent.SelectDateForDialog(date))

            // Then
            assertEquals(date, viewModel.uiState.value.selectedDateForDialog)
        }

    @Test
    fun `init handles students load failure`() =
        runTest {
            // Given
            val errorMsg = "Students error"
            coEvery { getAvailableMonthsUseCase() } returns flowOf(Result.success(emptyList()))
            coEvery { studentManagementUseCases.getAllStudentsUseCase() } returns flowOf(Result.failure(Exception(errorMsg)))

            // When
            createViewModel()
            advanceUntilIdle()

            // Then
            val expectedError = UiText.StringResource(R.string.error_loading_students, errorMsg)
            assertEquals(expectedError, viewModel.uiState.value.error)
        }

    // region Reactive Flow Behavior Tests (flatMapLatest Pattern)

    @Test
    fun `selectDate triggers attendance query for that date`() =
        runTest {
            // Given
            val date = LocalDate(2026, Month.FEBRUARY, 15)
            val attendance =
                listOf(
                    dev.nenoeldeeb.education.absencerecord.domain.models.StudentAttendance(
                        studentId = 1,
                        date = date
                    )
                )

            coEvery { studentManagementUseCases.getAllStudentsUseCase() } returns flowOf(Result.success(emptyList()))
            coEvery { getAttendanceForDateUseCase(date) } returns flowOf(Result.success(attendance))

            createViewModel()

            // When - select a date
            viewModel.onEvent(CalendarScreenEvent.SelectDateForDialog(date))
            advanceUntilIdle()

            // Then - attendance list should be populated
            assertEquals(attendance, viewModel.uiState.value.studentsForSelectedDate)
        }

    @Test
    fun `changing date switches to new query`() =
        runTest {
            // Given - two different dates with different attendance
            val dateA = LocalDate(2026, Month.FEBRUARY, 15)
            val dateB = LocalDate(2026, Month.FEBRUARY, 16)
            val attendanceA =
                listOf(
                    dev.nenoeldeeb.education.absencerecord.domain.models.StudentAttendance(
                        studentId = 1,
                        date = dateA
                    )
                )
            val attendanceB =
                listOf(
                    dev.nenoeldeeb.education.absencerecord.domain.models.StudentAttendance(
                        studentId = 2,
                        date = dateB
                    )
                )

            coEvery { studentManagementUseCases.getAllStudentsUseCase() } returns flowOf(Result.success(emptyList()))

            // Mock getAttendanceForDateUseCase to return different lists for different dates
            val dateSlot = slot<LocalDate>()
            coEvery { getAttendanceForDateUseCase(capture(dateSlot)) } answers {
                when (dateSlot.captured) {
                    dateA -> flowOf(Result.success(attendanceA))
                    dateB -> flowOf(Result.success(attendanceB))
                    else -> flowOf(Result.success(emptyList()))
                }
            }

            createViewModel()

            // When - first select date A, then change to date B
            viewModel.onEvent(CalendarScreenEvent.SelectDateForDialog(dateA))
            advanceUntilIdle()

            // Then - should have date A's attendance
            assertEquals(attendanceA, viewModel.uiState.value.studentsForSelectedDate)

            // When - change to date B
            viewModel.onEvent(CalendarScreenEvent.SelectDateForDialog(dateB))
            advanceUntilIdle()

            // Then - should now have date B's attendance (flatMapLatest switched queries)
            assertEquals(attendanceB, viewModel.uiState.value.studentsForSelectedDate)
        }

    @Test
    fun `null date clears students for selected date`() =
        runTest {
            // Given
            val date = LocalDate(2026, Month.FEBRUARY, 15)
            val attendance =
                listOf(
                    dev.nenoeldeeb.education.absencerecord.domain.models.StudentAttendance(
                        studentId = 1,
                        date = date
                    )
                )

            coEvery { studentManagementUseCases.getAllStudentsUseCase() } returns flowOf(Result.success(emptyList()))
            coEvery { getAttendanceForDateUseCase(date) } returns flowOf(Result.success(attendance))

            createViewModel()

            // When - select a date to load attendance
            viewModel.onEvent(CalendarScreenEvent.SelectDateForDialog(date))
            advanceUntilIdle()
            assertEquals(attendance, viewModel.uiState.value.studentsForSelectedDate)

            // When - clear the selected date (dismiss dialog)
            viewModel.onEvent(CalendarScreenEvent.SelectDateForDialog(null))
            advanceUntilIdle()

            // Then - attendance list should be cleared
            assertEquals(emptyList(), viewModel.uiState.value.studentsForSelectedDate)
        }

    @Test
    fun `database updates automatically propagate to UI state`() =
        runTest {
            // Given - Use a MutableSharedFlow to simulate database emissions
            val date = LocalDate(2026, Month.FEBRUARY, 15)
            val attendanceFlow =
                MutableSharedFlow<Result<List<dev.nenoeldeeb.education.absencerecord.domain.models.StudentAttendance>>>(
                    replay = 1
                )

            coEvery { studentManagementUseCases.getAllStudentsUseCase() } returns flowOf(Result.success(emptyList()))
            coEvery { getAttendanceForDateUseCase(date) } returns attendanceFlow

            // Initial attendance
            val initialAttendance =
                listOf(
                    dev.nenoeldeeb.education.absencerecord.domain.models.StudentAttendance(
                        studentId = 1,
                        date = date
                    )
                )
            attendanceFlow.emit(Result.success(initialAttendance))

            createViewModel()

            // When - select the date
            viewModel.onEvent(CalendarScreenEvent.SelectDateForDialog(date))
            advanceUntilIdle()

            // Then - initial attendance loaded
            assertEquals(initialAttendance, viewModel.uiState.value.studentsForSelectedDate)

            // When - database emits new attendance (simulating insert)
            val updatedAttendance =
                listOf(
                    dev.nenoeldeeb.education.absencerecord.domain.models.StudentAttendance(
                        studentId = 1,
                        date = date
                    ),
                    dev.nenoeldeeb.education.absencerecord.domain.models.StudentAttendance(
                        studentId = 2,
                        date = date
                    )
                )
            attendanceFlow.emit(Result.success(updatedAttendance))
            advanceUntilIdle()

            // Then - UI state should automatically update without manual refresh
            assertEquals(updatedAttendance, viewModel.uiState.value.studentsForSelectedDate)
        }

    @Test
    fun `attendance query failure shows error`() =
        runTest {
            // Given
            val date = LocalDate(2026, Month.FEBRUARY, 15)
            val errorMsg = "Database error"

            coEvery { studentManagementUseCases.getAllStudentsUseCase() } returns flowOf(Result.success(emptyList()))
            coEvery { getAttendanceForDateUseCase(date) } returns flowOf(Result.failure(Exception(errorMsg)))

            createViewModel()

            // When - select a date that will fail
            viewModel.onEvent(CalendarScreenEvent.SelectDateForDialog(date))
            advanceUntilIdle()

            // Then - error state should be set
            val expectedError =
                UiText.StringResource(
                    R.string.error_loading_attendance,
                    errorMsg
                )
            assertEquals(expectedError, viewModel.uiState.value.error)
        }

    // endregion
}