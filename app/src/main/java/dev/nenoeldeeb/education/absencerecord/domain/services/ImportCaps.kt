package dev.nenoeldeeb.education.absencerecord.domain.services

/**
 * Bounds on what a backup import will accept (FR-056 … FR-059).
 *
 * Two independent layers, because they stop different things:
 *
 *  - **Record caps** bound what reaches the *database*. Entries past a cap are
 *    skipped and counted, never silently dropped, and never abort the rest.
 *  - **The file-size guard** bounds what is *parsed*. The import path
 *    materialises every record before any merge-time cap could apply, so a cap
 *    applied after decoding cannot prevent an out-of-memory error — the memory
 *    is already spent by the time it is consulted.
 *
 * Every cap but one is derived arithmetically. See research D9.
 */
object ImportCaps {
    /**
     * The one judgement-based figure.
     *
     * Set far beyond any class set a single teacher manages, so no legitimate
     * backup can reach it (FR-058, SC-007).
     */
    const val MAX_IMPORT_STUDENTS = 10_000

    /** Seven days of non-overlapping ten-minute blocks: 7 x 1440 / 10 (FR-057). */
    const val MAX_IMPORT_HOURS = 1_008

    /** Every block at its maximum capacity of twelve students (FR-057). */
    const val MAX_IMPORT_ASSIGNMENTS = MAX_IMPORT_HOURS * 12

    /**
     * Attendance dates retained per student.
     *
     * Capped because the unbounded date list, not the student count, dominates
     * import memory: a small file of few students with deep history costs more
     * than a large file of shallow ones.
     */
    const val MAX_IMPORT_ATTENDANCE_DATES_PER_STUDENT = 3_650

    /**
     * Largest file that will be parsed, in bytes.
     *
     * Checked *before* decoding, so an implausibly large file is refused
     * without ever being materialised (SC-010). Roughly 4.6x the realistic
     * worst case — a whole secondary school with a year of daily attendance is
     * about 7 MB — which keeps peak heap near 96 MB.
     */
    const val MAX_IMPORT_FILE_BYTES = 32L * 1024 * 1024
}