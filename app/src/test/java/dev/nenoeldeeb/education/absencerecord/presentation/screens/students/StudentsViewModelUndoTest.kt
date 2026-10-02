package dev.nenoeldeeb.education.absencerecord.presentation.screens.students

import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentAttendance
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError
import dev.nenoeldeeb.education.absencerecord.domain.usecases.attendance.GetStudentAttendanceDatesUseCase
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class StudentsViewModelUndoTest : StudentsViewModelTestBase() {
    private lateinit var datesUseCase: GetStudentAttendanceDatesUseCase
    private lateinit var recordUseCase: RecordStudentAttendanceUseCase

    @BeforeEach
    fun setUp() {
        commonSetUp()
        datesUseCase = mockk(relaxed = true)
        recordUseCase = mockk(relaxed = true)
        every { attendanceUseCases.getStudentAttendanceDatesUseCase } returns datesUseCase
        every { attendanceUseCases.recordStudentAttendanceUseCase } returns recordUseCase
    }

    @Test
    fun `Show and dismiss export delete dialog toggles state`() =
        runTest {
            every { getAllStudentsUseCase(any(), any()) } returns flowOf(Result.success(emptyList()))
            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(StudentsScreenEvent.ShowExportDeleteDialog)
            assertTrue(viewModel.uiState.value.showExportDeleteDialog)

            viewModel.onEvent(StudentsScreenEvent.DismissExportDeleteDialog)
            assertFalse(viewModel.uiState.value.showExportDeleteDialog)
        }

    @Test
    fun `Delete success stores backup with attendance dates`() =
        runTest {
            val date = LocalDate(2026, 9, 1)
            val student = Student(id = 1, name = "S1")
            every { getAllStudentsUseCase(any(), any()) } returns flowOf(Result.success(listOf(student)))
            every { datesUseCase(any()) } returns flowOf(Result.success(listOf(date)))
            coEvery { deleteStudentsUseCase(any()) } returns Result.success(Unit)

            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(StudentsScreenEvent.ToggleStudentSelection(1))
            viewModel.onEvent(StudentsScreenEvent.DeleteSelectedStudents)
            advanceUntilIdle()

            val backup = viewModel.uiState.value.lastDeletedBackup
            assertEquals(1, backup.size)
            assertEquals(student, backup[0].student)
            assertEquals(listOf(date), backup[0].attendanceDates)
            assertEquals(
                UiText.StringResource(R.string.students_deleted_successfully),
                viewModel.uiState.value.toastMessage
            )
        }

    @Test
    fun `Undo success reinserts students and attendance then clears backup`() =
        runTest {
            val date = LocalDate(2026, 9, 1)
            val student = Student(id = 1, name = "S1")
            every { getAllStudentsUseCase(any(), any()) } returns flowOf(Result.success(listOf(student)))
            every { datesUseCase(any()) } returns flowOf(Result.success(listOf(date)))
            coEvery { deleteStudentsUseCase(any()) } returns Result.success(Unit)
            coEvery { addStudentUseCase(any()) } returns Result.success(10L)
            coEvery { recordUseCase(any()) } returns Result.success(5L)

            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(StudentsScreenEvent.ToggleStudentSelection(1))
            viewModel.onEvent(StudentsScreenEvent.DeleteSelectedStudents)
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.lastDeletedBackup.isNotEmpty())

            viewModel.onEvent(StudentsScreenEvent.UndoDeleteStudents)
            advanceUntilIdle()

            coVerify { addStudentUseCase(match { it.name == "S1" }) }
            coVerify { recordUseCase(match<StudentAttendance> { it.studentId == 10 && it.date == date }) }
            assertTrue(viewModel.uiState.value.lastDeletedBackup.isEmpty())
        }

    @Test
    fun `Undo failure sets error when reinsert fails`() =
        runTest {
            val date = LocalDate(2026, 9, 1)
            val student = Student(id = 1, name = "S1")
            every { getAllStudentsUseCase(any(), any()) } returns flowOf(Result.success(listOf(student)))
            every { datesUseCase(any()) } returns flowOf(Result.success(listOf(date)))
            coEvery { deleteStudentsUseCase(any()) } returns Result.success(Unit)
            coEvery { addStudentUseCase(any()) } returns Result.failure(StudentError.Database)

            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(StudentsScreenEvent.ToggleStudentSelection(1))
            viewModel.onEvent(StudentsScreenEvent.DeleteSelectedStudents)
            advanceUntilIdle()

            viewModel.onEvent(StudentsScreenEvent.UndoDeleteStudents)
            advanceUntilIdle()

            assertEquals(
                UiText.StringResource(R.string.error_database_operation_failed),
                viewModel.uiState.value.error
            )
            assertTrue(viewModel.uiState.value.lastDeletedBackup.isEmpty())
        }

    @Test
    fun `ConsumeUndoBackup clears backup without restoring`() =
        runTest {
            val student = Student(id = 1, name = "S1")
            every { getAllStudentsUseCase(any(), any()) } returns flowOf(Result.success(listOf(student)))
            every { datesUseCase(any()) } returns flowOf(Result.success(emptyList()))
            coEvery { deleteStudentsUseCase(any()) } returns Result.success(Unit)

            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(StudentsScreenEvent.ToggleStudentSelection(1))
            viewModel.onEvent(StudentsScreenEvent.DeleteSelectedStudents)
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.lastDeletedBackup.isNotEmpty())

            viewModel.onEvent(StudentsScreenEvent.ConsumeUndoBackup)
            assertTrue(viewModel.uiState.value.lastDeletedBackup.isEmpty())
            coVerify(exactly = 0) { addStudentUseCase(any()) }
        }
}