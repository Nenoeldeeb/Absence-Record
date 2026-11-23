package dev.nenoeldeeb.education.absencerecord.presentation.screens.students.delegates

import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.ImportResult
import dev.nenoeldeeb.education.absencerecord.domain.models.ParsedImportData
import dev.nenoeldeeb.education.absencerecord.domain.models.ParsedStudentImportData
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentExportData
import dev.nenoeldeeb.education.absencerecord.domain.usecases.StudentManagementUseCases
import dev.nenoeldeeb.education.absencerecord.domain.usecases.transfer.ParseImportFileUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.transfer.PerformImportUseCase
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenState
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText
import dev.nenoeldeeb.education.absencerecord.presentation.utils.toUiText
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ImportExportDelegateTest {
    private val parseImportFileUseCase = mockk<ParseImportFileUseCase>(relaxed = true)
    private val performImportUseCase = mockk<PerformImportUseCase>(relaxed = true)
    private val studentManagementUseCases = mockk<StudentManagementUseCases>(relaxed = true)
    private val state = MutableStateFlow(StudentsScreenState())
    private val testScope = TestScope()

    private lateinit var delegate: ImportExportDelegate

    @BeforeEach
    fun setUp() {
        state.value = StudentsScreenState()
        every { studentManagementUseCases.parseImportFileUseCase } returns parseImportFileUseCase
        every { studentManagementUseCases.performImportUseCase } returns performImportUseCase
        delegate =
            ImportExportDelegate(studentManagementUseCases, state, testScope) { state.update(it) }
    }

    @Test
    fun `prepareImportDialog returns null data for empty input`() {
        val (data, map, show) = delegate.prepareImportDialog(emptyList())
        assertNull(data)
        assertTrue(map.isEmpty())
        assertEquals(false, show)
    }

    @Test
    fun `prepareImportDialog returns initialized data for valid input`() {
        val input = listOf(ParsedStudentImportData(StudentExportData("S1", "", emptyList()), 1))
        val (data, map, show) = delegate.prepareImportDialog(input)

        assertEquals(input, data)
        assertEquals(mapOf(1 to true), map)
        assertEquals(true, show)
    }

    @Test
    fun `toggleImportSelection toggles value`() {
        val map = mapOf(1 to true)
        val result = delegate.toggleImportSelection(map, 1)
        assertEquals(false, result[1])

        val result2 = delegate.toggleImportSelection(result, 1)
        assertEquals(true, result2[1])
    }

    @Test
    fun `buildImportResultMessage creates correct Joined text`() {
        val result =
            ImportResult(
                newStudentsCount = 2,
                existingStudentsMergedCount = 1,
                datesSkippedCount = 3,
                datesProcessedCount = 2
            )

        val uiText = delegate.buildImportResultMessage(result)

        assertTrue(uiText is UiText.Joined)
        val parts = uiText.parts
        assertEquals(5, parts.size)
        // Check first part (new students)
        assertEquals(
            UiText.PluralResource(R.plurals.import_complete_new_students_added, 2, 2),
            parts[0]
        )
    }

    @Test
    fun `prepareImportSelectionDialog with parsed students opens selection dialog and stores data`() {
        val parsedData =
            ParsedImportData(
                students = listOf(ParsedStudentImportData(StudentExportData("S1", "", emptyList()), 1)),
                availableHours = emptyList()
            )
        coEvery { parseImportFileUseCase(any()) } returns Result.success(parsedData)

        delegate.prepareImportSelectionDialog("content://file")
        testScope.advanceUntilIdle()

        assertEquals(parsedData, state.value.parsedImportData)
        assertTrue(state.value.showImportSelectionDialog)
    }

    @Test
    fun `prepareImportSelectionDialog with empty students reports toast and does not open dialog`() {
        val parsedData =
            ParsedImportData(students = emptyList(), availableHours = emptyList())
        coEvery { parseImportFileUseCase(any()) } returns Result.success(parsedData)

        delegate.prepareImportSelectionDialog("content://file")
        testScope.advanceUntilIdle()

        assertTrue(state.value.parsedImportData == null)
        assertFalse(state.value.showImportSelectionDialog)
        assertEquals(
            UiText.StringResource(R.string.no_students_found_in_file),
            state.value.toastMessage
        )
    }

    @Test
    fun `prepareImportSelectionDialog with null uri reports cancelled error`() {
        delegate.prepareImportSelectionDialog(null)

        assertEquals(StudentError.Cancelled.toUiText(), state.value.error)
    }

    @Test
    fun `prepareImportSelectionDialog parse failure stores error`() {
        coEvery { parseImportFileUseCase(any()) } returns Result.failure(StudentError.Database)

        delegate.prepareImportSelectionDialog("content://file")
        testScope.advanceUntilIdle()

        assertEquals(StudentError.Database.toUiText(), state.value.error)
    }

    @Test
    fun `handleToggleImportSelection flips the stored selection`() {
        state.update { it.copy(importSelectionMap = mapOf(1 to true)) }

        delegate.handleToggleImportSelection(1)

        assertEquals(false, state.value.importSelectionMap[1])
    }

    @Test
    fun `performImport with success clears dialog and stores result toast`() {
        state.update {
            it.copy(
                parsedImportData =
                    ParsedImportData(
                        students = listOf(ParsedStudentImportData(StudentExportData("S1", "", emptyList()), 1)),
                        availableHours = emptyList()
                    ),
                importSelectionMap = mapOf(1 to true),
                showImportSelectionDialog = true
            )
        }
        coEvery { performImportUseCase(any(), any()) } returns
            Result.success(
                ImportResult(
                    newStudentsCount = 1,
                    existingStudentsMergedCount = 0,
                    datesSkippedCount = 0,
                    datesProcessedCount = 0
                )
            )

        delegate.performImport()
        testScope.advanceUntilIdle()

        assertFalse(state.value.showImportSelectionDialog)
        assertNull(state.value.parsedImportData)
        assertTrue(state.value.toastMessage is UiText.Joined)
    }

    @Test
    fun `performImport with failure stores error`() {
        state.update {
            it.copy(
                parsedImportData = ParsedImportData(students = emptyList(), availableHours = emptyList()),
                importSelectionMap = emptyMap()
            )
        }
        coEvery { performImportUseCase(any(), any()) } returns Result.failure(StudentError.Database)

        delegate.performImport()
        testScope.advanceUntilIdle()

        assertEquals(StudentError.Database.toUiText(), state.value.error)
    }
}