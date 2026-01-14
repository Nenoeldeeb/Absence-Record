package dev.nenoeldeeb.education.absencerecord.domain.models

/**
 * Represents the result of an import operation.
 * Contains counts for tracking what happened during import.
 */
data class ImportResult(
    val newStudentsCount: Int,
    val existingStudentsMergedCount: Int,
    val datesProcessedCount: Int,
    val datesSkippedCount: Int
)