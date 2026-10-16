package dev.nenoeldeeb.education.absencerecord.domain.services

// Cap admission, split out of the merge engine so that file stays readable.
//
// Every function here follows the same shape: when a cap is reached the entry
// is COUNTED and refused, never silently dropped and never allowed to abort the
// rest of the file (FR-056 … FR-059).

/**
 * True while another student record is still within its cap.
 *
 * Counted rather than truncated: a file past a cap is not corrupt, so the
 * remainder still imports and the teacher is told what was left out
 * (FR-059, SC-010).
 */
internal fun ImportCounters.admitStudents(): Boolean =
    if (newCount + mergedCount >= ImportCaps.MAX_IMPORT_STUDENTS) {
        studentsCapped++
        false
    } else {
        true
    }

internal fun ImportCounters.admitHours(): Boolean =
    if (hoursAdded >= ImportCaps.MAX_IMPORT_HOURS) {
        hoursCapped++
        false
    } else {
        true
    }

internal fun ImportCounters.admitLessons(): Boolean =
    if (lessonsAdded >= ImportCaps.MAX_IMPORT_ASSIGNMENTS) {
        lessonsCapped++
        false
    } else {
        true
    }

/**
 * How many of one student's attendance dates to keep.
 *
 * Capped per student, not globally: the date list is what dominates import
 * memory, and a file of few students with deep history costs more than a
 * file of many with shallow ones (FR-056).
 */
fun retainedDateCount(total: Int): Int = minOf(total, ImportCaps.MAX_IMPORT_ATTENDANCE_DATES_PER_STUDENT)