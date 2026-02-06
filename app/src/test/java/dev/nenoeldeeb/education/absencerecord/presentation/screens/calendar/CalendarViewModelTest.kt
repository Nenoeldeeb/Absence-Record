package dev.nenoeldeeb.education.absencerecord.presentation.screens.calendar

import dev.nenoeldeeb.education.absencerecord.MainDispatcherRule
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentAttendance
import dev.nenoeldeeb.education.absencerecord.domain.usecases.AttendanceUseCases
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
        every { attendanceUseCases.recordStudentAttendanceUseCase } returns
            recordStudentAttendanceUseCase
        every { attendanceUseCases.deleteStudentAttendanceUseCase } returns
            deleteStudentAttendanceUseCase
        every { attendanceUseCases.getAttendanceForDateUseCase } returns getAttendanceForDateUseCase
        every { attendanceUseCases.getAvailableMonthsUseCase } returns getAvailableMonthsUseCase

        studentManagementUseCases = mockk(relaxed = true)
    }

    private fun createViewModel() {
        viewModel = CalendarViewModel(attendanceUseCases, studentManagementUseCases)
    }

    @Test
    fun `init loads data successfully`() = runTest {
        // Given
        val months = listOf(LocalDate(2023, Month.JANUARY, 1))
        val students = listOf(Student(1, "S1"))

        coEvery { getAvailableMonthsUseCase() } returns flowOf(Result.success(months))
        coEvery { studentManagementUseCases.getAllStudentsUseCase() } returns
                flowOf(Result.success(students))

        // When
        createViewModel()
        advanceUntilIdle()

        // Then
        assertEquals(students, viewModel.uiState.value.allStudents)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `GetStudentsForDate loads attendance for date`() = runTest {
        // Given
        val date = LocalDate(2023, Month.JANUARY, 15)
        val students = listOf(StudentAttendance(1, 1, date))
        coEvery { getAttendanceForDateUseCase(date) } returns flowOf(Result.success(students))

        // When
        createViewModel() // init
        viewModel.onEvent(CalendarScreenEvent.GetStudentsForDate(date))
        advanceUntilIdle()

        // Then
        assertEquals(students, viewModel.uiState.value.studentsForSelectedDate)
    }

    @Test
    fun `MarkStudentAttendance calls use case and refreshes list`() = runTest {
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
    fun `MarkStudentAttendance shows error on failure`() = runTest {
        // Given
        val date = LocalDate(2023, Month.JANUARY, 15)
        val errorMsg = "Msg"
        coEvery { recordStudentAttendanceUseCase(any()) } returns
                Result.failure(Exception(errorMsg))
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
    fun `DeleteStudentAttendance calls use case and refreshes list`() = runTest {
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
    fun `DeleteStudentAttendance failure shows error`() = runTest {
        // Given
        val date = LocalDate(2023, Month.JANUARY, 15)
        val errorMsg = "Msg"
        coEvery { deleteStudentAttendanceUseCase(any(), any()) } returns
                Result.failure(Exception(errorMsg))
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
    fun `SelectDateForDialog updates state`() = runTest {
        createViewModel()
        val date = LocalDate(2023, Month.MARCH, 10)

        // When
        viewModel.onEvent(CalendarScreenEvent.SelectDateForDialog(date))

        // Then
        assertEquals(date, viewModel.uiState.value.selectedDateForDialog)
    }

    @Test
    fun `init handles students load failure`() = runTest {
        // Given
        val errorMsg = "Students error"
        coEvery { getAvailableMonthsUseCase() } returns flowOf(Result.success(emptyList()))
        coEvery { studentManagementUseCases.getAllStudentsUseCase() } returns
                flowOf(Result.failure(Exception(errorMsg)))

        // When
        createViewModel()
        advanceUntilIdle()

        // Then
        val expectedError = UiText.StringResource(R.string.error_loading_students, errorMsg)
        assertEquals(expectedError, viewModel.uiState.value.error)
    }

    @Test
    fun `GetStudentsForDate failure sets error`() = runTest {
        // Given
        val date = LocalDate(2023, Month.JANUARY, 15)
        val errorMsg = "Attendance error"
        coEvery { getAttendanceForDateUseCase(date) } returns
                flowOf(Result.failure(Exception(errorMsg)))

        createViewModel()
        // When
        viewModel.onEvent(CalendarScreenEvent.GetStudentsForDate(date))
        advanceUntilIdle()

        // Then
        val expectedError = UiText.StringResource(R.string.error_loading_attendance, errorMsg)
        assertEquals(expectedError, viewModel.uiState.value.error)
    }
}
