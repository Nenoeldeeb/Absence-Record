package dev.nenoeldeeb.education.absencerecord.presentation.screens.students.delegates

import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.ImportResult
import dev.nenoeldeeb.education.absencerecord.domain.models.ParsedStudentImportData
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentExportData
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ImportExportDelegateTest {
    private val delegate = ImportExportDelegate()

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
        assertEquals(3, parts.size)
        // Check first part (new students)
        assertEquals(
            UiText.PluralResource(R.plurals.import_complete_new_students_added, 2, 2),
            parts[0]
        )
    }
}