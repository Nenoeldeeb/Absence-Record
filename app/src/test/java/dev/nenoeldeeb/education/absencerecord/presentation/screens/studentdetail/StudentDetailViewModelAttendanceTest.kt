package dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail

import android.net.Uri
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.AttendanceHistoryItem
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class StudentDetailViewModelAttendanceTest : StudentDetailViewModelTestBase() {
    @BeforeEach
    fun setUp() {
        commonSetUp()
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Uri::class)
    }

    @Test
    fun `SelectMonth sets selected month`() =
        runTest {
            // Given
            every { getStudentByIdUseCase(1) } returns flowOf(Result.success(aStudent))
            createViewModel(studentId = 1)
            advanceUntilIdle()

            val month = LocalDate(2023, Month.JANUARY, 1)

            // When
            viewModel.onEvent(StudentDetailScreenEvent.SelectMonth(month))

            // Then
            assertEquals(month, viewModel.uiState.value.selectedMonth)
        }

    @Test
    fun `share success stores file uri`() =
        runTest {
            // Given
            val uriString = "content://file"
            val mockUri = mockk<Uri>()
            mockkStatic(Uri::class)
            every { Uri.parse(uriString) } returns mockUri

            val month = LocalDate(2023, Month.JANUARY, 1)
            val item = AttendanceHistoryItem(LocalDate(2023, Month.JANUARY, 15), "S1")
            every { getStudentByIdUseCase(1) } returns flowOf(Result.success(aStudent))
            every { getAttendanceHistoryForDateRangeUseCase(1, any(), any()) } returns
                flowOf(Result.success(listOf(item)))
            coEvery { shareReportUseCase("S1", month, any()) } returns Result.success(uriString)
            createViewModel(studentId = 1)
            advanceUntilIdle()

            // When
            viewModel.onEvent(StudentDetailScreenEvent.SelectMonth(month))
            viewModel.onEvent(StudentDetailScreenEvent.ShareAttendanceReport)
            advanceUntilIdle()

            // Then
            assertEquals(mockUri, viewModel.uiState.value.shareFileUri)
            assertNull(viewModel.uiState.value.toastMessage)
        }

    @Test
    fun `share failure sets toast message`() =
        runTest {
            // Given
            val month = LocalDate(2023, Month.JANUARY, 1)
            val item = AttendanceHistoryItem(LocalDate(2023, Month.JANUARY, 15), "S1")
            every { getStudentByIdUseCase(1) } returns flowOf(Result.success(aStudent))
            every { getAttendanceHistoryForDateRangeUseCase(1, any(), any()) } returns
                flowOf(Result.success(listOf(item)))
            coEvery { shareReportUseCase("S1", month, any()) } returns
                Result.failure(StudentError.ReportGeneration)
            createViewModel(studentId = 1)
            advanceUntilIdle()

            // When
            viewModel.onEvent(StudentDetailScreenEvent.SelectMonth(month))
            viewModel.onEvent(StudentDetailScreenEvent.ShareAttendanceReport)
            advanceUntilIdle()

            // Then
            assertEquals(
                UiText.StringResource(R.string.error_generating_report),
                viewModel.uiState.value.toastMessage
            )
            assertNull(viewModel.uiState.value.shareFileUri)
        }

    @Test
    fun `share with history fetch failure sets toast message`() =
        runTest {
            // Given
            val month = LocalDate(2023, Month.JANUARY, 1)
            every { getStudentByIdUseCase(1) } returns flowOf(Result.success(aStudent))
            every { getAttendanceHistoryForDateRangeUseCase(1, any(), any()) } returns
                flowOf(Result.failure(StudentError.Database))
            createViewModel(studentId = 1)
            advanceUntilIdle()

            // When
            viewModel.onEvent(StudentDetailScreenEvent.SelectMonth(month))
            viewModel.onEvent(StudentDetailScreenEvent.ShareAttendanceReport)
            advanceUntilIdle()

            // Then
            assertEquals(
                UiText.StringResource(R.string.error_database_operation_failed),
                viewModel.uiState.value.toastMessage
            )
        }

    @Test
    fun `attendance dates reload success populates dates`() =
        runTest {
            // Given
            val dates = listOf(LocalDate(2023, 1, 15), LocalDate(2023, 2, 3))
            every { getStudentByIdUseCase(1) } returns flowOf(Result.success(aStudent))
            every { getStudentAttendanceDatesUseCase(1) } returns flowOf(Result.success(dates))

            // When
            createViewModel(studentId = 1)
            advanceUntilIdle()

            // Then
            assertEquals(dates, viewModel.uiState.value.allAttendanceDates)
            assertTrue(viewModel.uiState.value.error == null)
        }

    @Test
    fun `attendance dates reload failure sets error`() =
        runTest {
            // Given
            every { getStudentByIdUseCase(1) } returns flowOf(Result.success(aStudent))
            every { getStudentAttendanceDatesUseCase(1) } returns
                flowOf(Result.failure(StudentError.Database))

            // When
            createViewModel(studentId = 1)
            advanceUntilIdle()

            // Then
            assertTrue(viewModel.uiState.value.allAttendanceDates.isEmpty())
            assertEquals(
                UiText.StringResource(R.string.error_database_operation_failed),
                viewModel.uiState.value.error
            )
        }
}