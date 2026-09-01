package dev.nenoeldeeb.education.absencerecord.presentation.screens.students

import android.net.Uri
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.ImportResult
import dev.nenoeldeeb.education.absencerecord.domain.models.ParsedImportData
import dev.nenoeldeeb.education.absencerecord.domain.models.ParsedStudentImportData
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentExportData
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class StudentsViewModelImportTest : StudentsViewModelTestBase() {
    @BeforeEach
    fun setUp() {
        commonSetUp()
    }

    @Test
    fun `PrepareImportSelectionDialog with valid Uri parses file`() =
        runTest {
            // Given
            val parsedImportData =
                ParsedImportData(
                    students = listOf(ParsedStudentImportData(StudentExportData("S1", "", emptyList()), 1)),
                    availableHours = emptyList()
                )
            every { getAllStudentsUseCase(any(), any()) } returns flowOf(Result.success(emptyList()))
            coEvery { parseImportFileUseCase(any()) } returns Result.success(parsedImportData)
            createViewModel()
            advanceUntilIdle()

            val uri = mockk<Uri>()
            every { uri.toString() } returns "content://file"

            // When
            viewModel.onEvent(StudentsScreenEvent.PrepareImportSelectionDialog(uri))
            advanceUntilIdle()

            // Then
            assertEquals(parsedImportData, viewModel.uiState.value.parsedImportData)
            assertTrue(viewModel.uiState.value.showImportSelectionDialog)
        }

    @Test
    fun `PrepareImportSelectionDialog with null Uri sets toast`() =
        runTest {
            // Given
            every { getAllStudentsUseCase(any(), any()) } returns flowOf(Result.success(emptyList()))
            createViewModel()
            advanceUntilIdle()

            // When
            viewModel.onEvent(StudentsScreenEvent.PrepareImportSelectionDialog(null))

            // Then
            assertEquals(
                UiText.StringResource(R.string.file_selection_cancelled),
                viewModel.uiState.value.error
            )
        }

    @Test
    fun `PrepareImportSelectionDialog parse error sets error`() =
        runTest {
            // Given
            every { getAllStudentsUseCase(any(), any()) } returns flowOf(Result.success(emptyList()))
            coEvery { parseImportFileUseCase(any()) } returns
                Result.failure(StudentError.ImportParse)
            createViewModel()
            advanceUntilIdle()

            val uri = mockk<Uri>()
            every { uri.toString() } returns "uri"

            // When
            viewModel.onEvent(StudentsScreenEvent.PrepareImportSelectionDialog(uri))
            advanceUntilIdle()

            // Then
            assertEquals(
                UiText.StringResource(R.string.error_parsing_import_file),
                viewModel.uiState.value.error
            )
        }

    @Test
    fun `PrepareImportSelectionDialog unsupported version sets error`() =
        runTest {
            // Given
            every { getAllStudentsUseCase(any(), any()) } returns flowOf(Result.success(emptyList()))
            coEvery { parseImportFileUseCase(any()) } returns
                Result.failure(StudentError.UnsupportedBackupVersion(9))
            createViewModel()
            advanceUntilIdle()

            val uri = mockk<Uri>()
            every { uri.toString() } returns "uri"

            // When
            viewModel.onEvent(StudentsScreenEvent.PrepareImportSelectionDialog(uri))
            advanceUntilIdle()

            // Then
            assertEquals(
                UiText.StringResource(R.string.error_unsupported_backup_version, 9),
                viewModel.uiState.value.error
            )
        }

    @Test
    fun `PerformImport calls use case`() =
        runTest {
            // Given
            val parsedImportData =
                ParsedImportData(
                    students = listOf(ParsedStudentImportData(StudentExportData("S1", "", emptyList()), 1)),
                    availableHours = emptyList()
                )

            // Mock import result
            val importResult =
                ImportResult(
                    newStudentsCount = 1,
                    existingStudentsMergedCount = 0,
                    datesSkippedCount = 0,
                    datesProcessedCount = 0
                )

            coEvery { parseImportFileUseCase(any()) } returns Result.success(parsedImportData)
            coEvery { performImportUseCase(any(), any()) } returns
                Result.success(importResult)

            createViewModel()
            advanceUntilIdle()

            val uri = mockk<Uri>()
            every { uri.toString() } returns "uri"
            viewModel.onEvent(StudentsScreenEvent.PrepareImportSelectionDialog(uri))
            advanceUntilIdle()

            // When
            viewModel.onEvent(StudentsScreenEvent.PerformImport)
            advanceUntilIdle()

            // Then
            coVerify { performImportUseCase(parsedImportData, any()) }
            assertEquals(false, viewModel.uiState.value.showImportSelectionDialog)
        }

    @Test
    fun `PerformImport failure sets error`() =
        runTest {
            // Given
            val parsedImportData =
                ParsedImportData(
                    students = listOf(ParsedStudentImportData(StudentExportData("S1", "", emptyList()), 1)),
                    availableHours = emptyList()
                )

            coEvery { parseImportFileUseCase(any()) } returns Result.success(parsedImportData)
            coEvery { performImportUseCase(any(), any()) } returns
                Result.failure(StudentError.Database)

            createViewModel()
            advanceUntilIdle()

            val uri = mockk<Uri>()
            every { uri.toString() } returns "uri"
            viewModel.onEvent(StudentsScreenEvent.PrepareImportSelectionDialog(uri))
            advanceUntilIdle()

            // When
            viewModel.onEvent(StudentsScreenEvent.PerformImport)
            advanceUntilIdle()

            // Then
            assertEquals(
                UiText.StringResource(R.string.error_database_operation_failed),
                viewModel.uiState.value.error
            )
        }
}