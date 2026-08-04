package dev.nenoeldeeb.education.absencerecord.presentation.screens.report

import android.net.Uri
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.AttendanceHistoryItem
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ReportViewModelSharingTest : ReportViewModelTestBase() {
    @BeforeEach
    fun setUp() {
        commonSetUp()
    }

    @AfterEach
    fun tearDown() {
        commonTearDown()
    }

    @Test
    fun `PrepareCalendarImageForSharing sharing successful`() =
        runTest {
            // Given
            val student = Student(1, "S1")
            val month = LocalDate(2023, Month.JANUARY, 1)
            val historyItem = AttendanceHistoryItem(LocalDate(2023, Month.JANUARY, 15), student.name)
            val uriString = "content://file"
            val mockUri = mockk<Uri>()

            every { Uri.parse(uriString) } returns mockUri

            coEvery { getStudentAttendanceDatesUseCase(1) } returns flowOf(Result.success(emptyList()))
            coEvery { getAttendanceHistoryForDateRangeUseCase(1, any(), any()) } returns
                flowOf(Result.success(listOf(historyItem)))

            coEvery { shareReportUseCase("S1", month, any()) } returns Result.success(uriString)

            createViewModel()
            // Must select student first
            viewModel.onEvent(ReportScreenEvent.SelectStudentForHistory(student))
            advanceUntilIdle()

            // When
            viewModel.onEvent(ReportScreenEvent.PrepareCalendarImageForSharing(month, 1, "S1"))
            advanceUntilIdle()

            // Then
            assertEquals(mockUri, viewModel.uiState.value.shareFileUri)
        }

    @Test
    fun `PrepareCalendarImageForSharing shows error when student mismatch`() =
        runTest {
            // Given
            val student = Student(1, "S1")
            createViewModel()
            viewModel.onEvent(ReportScreenEvent.SelectStudentForHistory(student))

            // When (Wrong student ID 2)
            viewModel.onEvent(
                ReportScreenEvent.PrepareCalendarImageForSharing(LocalDate(2023, 1, 1), 2, "S2")
            )

            // Then
            assertEquals(
                UiText.StringResource(R.string.error_student_not_selected_history),
                viewModel.uiState.value.toastMessage
            )
        }

    @Test
    fun `PrepareCalendarImageForSharing history fetch failure shows toast`() =
        runTest {
            // Given
            val student = Student(1, "S1")
            val month = LocalDate(2023, Month.JANUARY, 1)

            coEvery { getStudentAttendanceDatesUseCase(1) } returns flowOf(Result.success(emptyList()))
            coEvery { getAttendanceHistoryForDateRangeUseCase(1, any(), any()) } returns
                flowOf(Result.failure(StudentError.Database))

            createViewModel()
            viewModel.onEvent(ReportScreenEvent.SelectStudentForHistory(student))
            advanceUntilIdle()

            // When
            viewModel.onEvent(ReportScreenEvent.PrepareCalendarImageForSharing(month, 1, "S1"))
            advanceUntilIdle()

            // Then
            assertEquals(
                UiText.StringResource(R.string.error_database_operation_failed),
                viewModel.uiState.value.toastMessage
            )
        }

    @Test
    fun `PrepareCalendarImageForSharing share failure shows toast`() =
        runTest {
            // Given
            val student = Student(1, "S1")
            val month = LocalDate(2023, Month.JANUARY, 1)
            val historyItem = AttendanceHistoryItem(LocalDate(2023, Month.JANUARY, 15), student.name)

            coEvery { getStudentAttendanceDatesUseCase(1) } returns flowOf(Result.success(emptyList()))
            coEvery { getAttendanceHistoryForDateRangeUseCase(1, any(), any()) } returns
                flowOf(Result.success(listOf(historyItem)))

            coEvery { shareReportUseCase("S1", month, any()) } returns
                Result.failure(StudentError.ReportGeneration)

            createViewModel()
            viewModel.onEvent(ReportScreenEvent.SelectStudentForHistory(student))
            advanceUntilIdle()

            // When
            viewModel.onEvent(ReportScreenEvent.PrepareCalendarImageForSharing(month, 1, "S1"))
            advanceUntilIdle()

            // Then
            assertEquals(
                UiText.StringResource(R.string.error_generating_report),
                viewModel.uiState.value.toastMessage
            )
        }
}