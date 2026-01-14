package dev.nenoeldeeb.education.absencerecord.presentation.screens.students.delegates

import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.ImportResult
import dev.nenoeldeeb.education.absencerecord.domain.models.ParsedStudentImportData
import dev.nenoeldeeb.education.absencerecord.presentation.util.UiText

/**
 * Delegate class responsible for import/export state transformations.
 * Extracts import/export logic from StudentsViewModel to comply with Single Responsibility Principle.
 */
class ImportExportDelegate {
    /**
     * Prepares the import dialog state from parsed student data.
     *
     * @return Triple of (parsed data or null, selection map, show dialog flag)
     */
    fun prepareImportDialog(
        parsedData: List<ParsedStudentImportData>
    ): Triple<List<ParsedStudentImportData>?, Map<Int, Boolean>, Boolean> {
        return if (parsedData.isEmpty()) {
            Triple(null, emptyMap(), false) // No dialog for empty data
        } else {
            Triple(
                parsedData,
                parsedData.associate { it.id to true },
                true
            )
        }
    }

    /**
     * Toggles selection state for a single parsed student in the import dialog.
     */
    fun toggleImportSelection(
        currentMap: Map<Int, Boolean>,
        parsedStudentId: Int
    ): Map<Int, Boolean> {
        return currentMap.toMutableMap().apply {
            this[parsedStudentId] = this[parsedStudentId] != true
        }
    }

    /**
     * Builds a user-facing message from import results.
     * Combines multiple parts based on what happened during import.
     */
    fun buildImportResultMessage(result: ImportResult): UiText {
        val parts = mutableListOf<UiText>()

        // Always show new students count
        parts.add(
            UiText.PluralResource(
                R.plurals.import_complete_new_students_added,
                result.newStudentsCount,
                result.newStudentsCount
            )
        )

        // Show merged count if any
        if (result.existingStudentsMergedCount > 0) {
            parts.add(
                UiText.PluralResource(
                    R.plurals.existing_students_had_dates_merged,
                    result.existingStudentsMergedCount,
                    result.existingStudentsMergedCount
                )
            )
        }

        // Show skipped dates if any
        if (result.datesSkippedCount > 0) {
            parts.add(
                UiText.PluralResource(
                    R.plurals.dates_skipped_due_to_errors,
                    result.datesSkippedCount,
                    result.datesSkippedCount
                )
            )
        }

        return UiText.Joined(parts, "\n")
    }
}