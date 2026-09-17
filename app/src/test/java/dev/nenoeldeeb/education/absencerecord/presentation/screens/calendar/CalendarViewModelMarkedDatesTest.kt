package dev.nenoeldeeb.education.absencerecord.presentation.screens.calendar

import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.data.repositories.ClassFilterRepositoryImpl
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError
import dev.nenoeldeeb.education.absencerecord.domain.usecases.ClassManagementUseCases
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelMarkedDatesTest : CalendarViewModelTestBase() {
    @BeforeEach
    fun setUp() {
        commonSetUp()
    }

    @Test
    fun `markedDates onSuccess binds to state`() =
        runTest {
            val dates = listOf(LocalDate(2026, 1, 10), LocalDate(2026, 1, 15))
            every { getMarkedDatesUseCase() } returns flowOf(Result.success(dates))
            coEvery { studentManagementUseCases.getAllStudentsUseCase() } returns
                flowOf(Result.success(emptyList()))

            createViewModel(ClassFilterRepositoryImpl())
            advanceUntilIdle()

            assertEquals(dates.toSet(), viewModel.uiState.value.markedDates)
            assertNull(viewModel.uiState.value.error)
        }

    @Test
    fun `markedDates onFailure binds error state`() =
        runTest {
            every { getMarkedDatesUseCase() } returns flowOf(Result.failure(StudentError.Database))
            coEvery { studentManagementUseCases.getAllStudentsUseCase() } returns
                flowOf(Result.success(emptyList()))

            createViewModel(ClassFilterRepositoryImpl())
            advanceUntilIdle()

            assertEquals(
                UiText.StringResource(R.string.error_database_operation_failed),
                viewModel.uiState.value.error
            )
        }

    @Test
    fun `mark future date is blocked with error`() =
        runTest {
            coEvery { studentManagementUseCases.getAllStudentsUseCase() } returns
                flowOf(Result.success(emptyList()))
            createViewModel(ClassFilterRepositoryImpl())
            advanceUntilIdle()

            viewModel.onEvent(
                CalendarScreenEvent.MarkStudentAttendance(1, LocalDate(2999, 1, 1))
            )
            advanceUntilIdle()

            assertEquals(
                UiText.StringResource(R.string.error_future_date_not_allowed),
                viewModel.uiState.value.error
            )
            coVerify(exactly = 0) { recordStudentAttendanceUseCase(any()) }
        }

    @Test
    fun `mark past date onSuccess sets message sound and undo`() =
        runTest {
            val student = Student(1, "Layla", classId = null)
            coEvery { studentManagementUseCases.getAllStudentsUseCase() } returns
                flowOf(Result.success(listOf(student)))
            coEvery { recordStudentAttendanceUseCase(any()) } returns Result.success(1L)
            val classManagement = mockk<ClassManagementUseCases>(relaxed = true)
            coEvery { classManagement.getAllClassesUseCase() } returns flowOf(Result.success(emptyList()))

            viewModel =
                CalendarViewModel(
                    attendanceUseCases,
                    studentManagementUseCases,
                    classManagement,
                    ClassFilterRepositoryImpl()
                )
            advanceUntilIdle()

            viewModel.onEvent(CalendarScreenEvent.MarkStudentAttendance(1, LocalDate(2026, 1, 15)))
            advanceUntilIdle()

            assertNotNull(viewModel.uiState.value.attendanceMessage)
            assertEquals(true, viewModel.uiState.value.pendingSoundIsAdd)
            assertEquals(
                LastAttendanceChange(1, LocalDate(2026, 1, 15), true),
                viewModel.uiState.value.lastAttendanceChange
            )
        }

    @Test
    fun `mark past date onFailure binds error`() =
        runTest {
            coEvery { studentManagementUseCases.getAllStudentsUseCase() } returns
                flowOf(Result.success(listOf(Student(1, "Layla"))))
            coEvery { recordStudentAttendanceUseCase(any()) } returns
                Result.failure(StudentError.Database)
            createViewModel(ClassFilterRepositoryImpl())
            advanceUntilIdle()

            viewModel.onEvent(CalendarScreenEvent.MarkStudentAttendance(1, LocalDate(2026, 1, 15)))
            advanceUntilIdle()

            assertEquals(
                UiText.StringResource(R.string.error_database_operation_failed),
                viewModel.uiState.value.error
            )
            assertNull(viewModel.uiState.value.attendanceMessage)
        }

    @Test
    fun `delete onSuccess and onFailure bind correctly`() =
        runTest {
            coEvery { studentManagementUseCases.getAllStudentsUseCase() } returns
                flowOf(Result.success(listOf(Student(1, "Omar"))))
            coEvery { deleteStudentAttendanceUseCase(any(), any()) } returns Result.success(Unit)
            createViewModel(ClassFilterRepositoryImpl())
            advanceUntilIdle()

            viewModel.onEvent(CalendarScreenEvent.DeleteStudentAttendance(1, LocalDate(2026, 1, 15)))
            advanceUntilIdle()

            assertNotNull(viewModel.uiState.value.attendanceMessage)
            assertEquals(false, viewModel.uiState.value.pendingSoundIsAdd)

            coEvery { deleteStudentAttendanceUseCase(any(), any()) } returns
                Result.failure(StudentError.Database)
            viewModel.onEvent(CalendarScreenEvent.DeleteStudentAttendance(1, LocalDate(2026, 1, 16)))
            advanceUntilIdle()

            assertEquals(
                UiText.StringResource(R.string.error_database_operation_failed),
                viewModel.uiState.value.error
            )
        }

    @Test
    fun `undo after mark triggers delete`() =
        runTest {
            coEvery { studentManagementUseCases.getAllStudentsUseCase() } returns
                flowOf(Result.success(listOf(Student(1, "Layla"))))
            coEvery { recordStudentAttendanceUseCase(any()) } returns Result.success(1L)
            coEvery { deleteStudentAttendanceUseCase(any(), any()) } returns Result.success(Unit)
            createViewModel(ClassFilterRepositoryImpl())
            advanceUntilIdle()

            viewModel.onEvent(CalendarScreenEvent.MarkStudentAttendance(1, LocalDate(2026, 1, 15)))
            advanceUntilIdle()
            viewModel.onEvent(CalendarScreenEvent.UndoLastAttendanceChange)
            advanceUntilIdle()

            coVerify { deleteStudentAttendanceUseCase(1, LocalDate(2026, 1, 15)) }
        }

    @Test
    fun `consume events clear message sound and error`() =
        runTest {
            coEvery { studentManagementUseCases.getAllStudentsUseCase() } returns
                flowOf(Result.success(listOf(Student(1, "Layla"))))
            coEvery { recordStudentAttendanceUseCase(any()) } returns Result.success(1L)
            createViewModel(ClassFilterRepositoryImpl())
            advanceUntilIdle()

            viewModel.onEvent(CalendarScreenEvent.MarkStudentAttendance(1, LocalDate(2026, 1, 15)))
            advanceUntilIdle()
            viewModel.onEvent(CalendarScreenEvent.ConsumeSound)
            viewModel.onEvent(CalendarScreenEvent.ConsumeAttendanceMessage)
            viewModel.onEvent(CalendarScreenEvent.ConsumeError)

            assertNull(viewModel.uiState.value.pendingSoundIsAdd)
            assertNull(viewModel.uiState.value.attendanceMessage)
            assertNull(viewModel.uiState.value.lastAttendanceChange)
            assertNull(viewModel.uiState.value.error)
        }
}