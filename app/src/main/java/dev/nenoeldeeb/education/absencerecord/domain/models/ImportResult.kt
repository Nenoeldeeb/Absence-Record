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
    val malformedEntriesSkippedCount: Int = 0,
    /** Entries left out because a record cap was reached (FR-056 … FR-059). */
    val cappedEntriesSkippedCount: Int = 0,
    /** The student cap that was hit, so the teacher knows why records were left out (FR-058). */
    val studentCapHit: Boolean = false
)