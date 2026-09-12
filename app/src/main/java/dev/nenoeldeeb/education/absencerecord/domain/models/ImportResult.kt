package dev.nenoeldeeb.education.absencerecord.domain.models

/**
 * Represents the result of an import operation.
 * Contains counts for tracking what happened during import.
 */
data class ImportResult(
    val newStudentsCount: Int,
    val existingStudentsMergedCount: Int,
    val datesProcessedCount: Int,
    val datesSkippedCount: Int,
    val hoursAddedCount: Int = 0,
    val hoursSkippedOverlapCount: Int = 0,
    val lessonsAddedCount: Int = 0,
    val lessonsRemovedBusyWinsCount: Int = 0,
    val lessonsSkippedCount: Int = 0,
    val malformedEntriesSkippedCount: Int = 0
)