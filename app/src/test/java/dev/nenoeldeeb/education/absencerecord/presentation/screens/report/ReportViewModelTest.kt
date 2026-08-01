package dev.nenoeldeeb.education.absencerecord.presentation.screens.report

import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.data.repositories.ClassFilterRepositoryImpl
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText
import io.mockk.coEvery
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class ReportViewModelTest : ReportViewModelTestBase() {
    @BeforeEach
    fun setUp() {
        commonSetUp()
    }

    @Test
    fun `SelectStudentForHistory loads history`() =
        runTest {
            val student = Student(1, "S1")
            val historyDates = listOf(LocalDate(2023, Month.JANUARY, 15))
            coEvery { getStudentAttendanceDatesUseCase(1) } returns flowOf(Result.success(historyDates))

            createViewModel()

            viewModel.onEvent(ReportScreenEvent.SelectStudentForHistory(student))
            advanceUntilIdle()

            assertEquals(student, viewModel.uiState.value.selectedStudentForHistory)
            assertEquals(1, viewModel.uiState.value.studentHistory.size)
            assertEquals(
                LocalDate(2023, Month.JANUARY, 1),
                viewModel.uiState.value.studentHistory[0].first
            )
        }

    @Test
    fun `State updates`() =
        runTest {
            createViewModel()

            val month = LocalDate(2023, Month.FEBRUARY, 1)
            viewModel.onEvent(ReportScreenEvent.UpdateSelectedMonth(month))
            assertEquals(month, viewModel.uiState.value.selectedMonth)

            viewModel.onEvent(ReportScreenEvent.ClearMonthFilter)
            assertNull(viewModel.uiState.value.selectedMonth)

            viewModel.onEvent(ReportScreenEvent.ToggleSortType)

            val toast = UiText.DynamicString("Toast")
            viewModel.onEvent(ReportScreenEvent.ShowToast(toast))
            assertEquals(toast, viewModel.uiState.value.toastMessage)
            viewModel.onEvent(ReportScreenEvent.ConsumeToastMessage)
            assertNull(viewModel.uiState.value.toastMessage)

            viewModel.onEvent(ReportScreenEvent.ShowHistoryDialog(true))
            assertEquals(true, viewModel.uiState.value.showHistoryDialog)

            viewModel.onEvent(ReportScreenEvent.ShowCalendarPreviewDialog(true))
            assertEquals(true, viewModel.uiState.value.showCalendarPreviewDialog)

            val previewMonth = LocalDate(2023, Month.APRIL, 1)
            viewModel.onEvent(ReportScreenEvent.SelectMonthYearForCalendarPreview(previewMonth))
            assertEquals(previewMonth, viewModel.uiState.value.selectedMonthYearForCalendarPreview)

            viewModel.onEvent(ReportScreenEvent.ToggleMonthDropdown(true))
            assertEquals(true, viewModel.uiState.value.monthDropdownExpanded)
            viewModel.onEvent(ReportScreenEvent.ToggleMonthDropdown(false))
            assertEquals(false, viewModel.uiState.value.monthDropdownExpanded)
        }

    @Test
    fun `init handles load failures`() =
        runTest {
            coEvery { studentManagementUseCases.getAllStudentsUseCase(any(), any()) } returns
                flowOf(Result.failure(StudentError.Database))
            coEvery { attendanceUseCases.getAvailableMonthsUseCase() } returns
                flowOf(Result.failure(StudentError.Database))

            createViewModel()
            advanceUntilIdle()

            assertEquals(
                UiText.StringResource(R.string.error_database_operation_failed),
                viewModel.uiState.value.error
            )
        }

    @Test
    fun `repository toggle syncs selectedClassIds and filters students`() =
        runTest {
            val repo = ClassFilterRepositoryImpl()
            val enrolled =
                listOf(
                    Student(1, "S1", classId = 10),
                    Student(2, "S2", classId = 10)
                )
            val unassigned =
                listOf(
                    Student(3, "S3", classId = null),
                    Student(4, "S4", classId = null)
                )
            coEvery { studentManagementUseCases.getAllStudentsUseCase(any(), any()) } returns
                flowOf(Result.success(enrolled + unassigned))

            createViewModel(repo)
            advanceUntilIdle()

            assertEquals(emptySet<Int>(), viewModel.uiState.value.selectedClassIds)
            assertEquals(
                unassigned.map { it.id }.toSet(),
                viewModel.uiState.value.allStudents.map { it.id }.toSet()
            )

            repo.toggleClass(10)
            advanceUntilIdle()

            assertEquals(setOf(10), viewModel.uiState.value.selectedClassIds)
            assertEquals(
                enrolled.map { it.id }.toSet(),
                viewModel.uiState.value.allStudents.map { it.id }.toSet()
            )

            repo.toggleClass(10)
            advanceUntilIdle()

            assertEquals(emptySet<Int>(), viewModel.uiState.value.selectedClassIds)
            assertEquals(
                unassigned.map { it.id }.toSet(),
                viewModel.uiState.value.allStudents.map { it.id }.toSet()
            )
        }

    @Test
    fun `ToggleClassFilter event delegates to repository`() =
        runTest {
            val repo = ClassFilterRepositoryImpl()

            createViewModel(repo)
            advanceUntilIdle()

            viewModel.onEvent(ReportScreenEvent.ToggleClassFilter(10))
            advanceUntilIdle()

            assertEquals(setOf(10), viewModel.uiState.value.selectedClassIds)
        }
}