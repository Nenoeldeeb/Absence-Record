package dev.nenoeldeeb.education.absencerecord.domain.models

import dev.nenoeldeeb.education.absencerecord.domain.services.ScheduleRules
import kotlinx.serialization.Serializable

/**
 * The version this build writes.
 *
 * Version 3 replaced the per-block student count with the block's length in
 * minutes. The shape change is signalled twice over — by this bump AND by the
 * field rename — so a file of either format is identifiable by either signal
 * alone (FR-043).
 */
const val BACKUP_VERSION = 3

/** The version immediately before this one, whose files still import (FR-044). */
const val PREVIOUS_BACKUP_VERSION = 2

/** Versions this build will read. Anything else is refused untouched (FR-047). */
val SUPPORTED_BACKUP_VERSIONS = setOf(BACKUP_VERSION, PREVIOUS_BACKUP_VERSION)

@Serializable
data class BackupExportData(
    val version: Int = BACKUP_VERSION,
    val students: List<StudentExportData>,
    val availableHours: List<AvailableHourExportData>
)

/**
 * One lesson block as it appears in a backup.
 *
 * Carries both length signals so the two formats share one shape: the current
 * [durationMinutes] and the previous format's [maxStudents]. Only one is ever
 * written; both are read, because a file may be of either vintage (FR-043).
 */
@Serializable
data class AvailableHourExportData(
    val weekday: Int,
    val startMinutes: Int,
    /** Length in minutes. Written by this build, never [maxStudents] (FR-042). */
    val durationMinutes: Int? = null,
    /** The previous format's student count. Read-only; never written (FR-044). */
    val maxStudents: Int? = null
) {
    /**
     * This block's length, or null when the entry cannot be trusted.
     *
     * Exactly one of the two signals must be present: carrying both is ambiguous
     * and carrying neither is incomplete, and guessing at either would silently
     * invent a lesson length (FR-046). A legacy count is multiplied by ten and
     * clamped into the selectable range so old data outside it still loads
     * (FR-041). A length already in minutes must itself be a selectable value —
     * anything else is malformed (FR-048).
     */
    fun resolvedDurationMinutes(): Int? =
        when {
            durationMinutes != null && maxStudents != null -> null
            durationMinutes != null -> durationMinutes.takeIf { ScheduleRules.isHourDurationValid(it) }
            maxStudents != null -> ScheduleRules.durationFromLegacyCapacity(maxStudents)
            else -> null
        }

    /** True when this entry could not be interpreted and must be skipped (FR-046, FR-048). */
    val isMalformed: Boolean
        get() =
            (durationMinutes != null && maxStudents != null) ||
                (durationMinutes == null && maxStudents == null) ||
                resolvedDurationMinutes() == null
}

@Serializable
data class LessonAssignmentExportData(
    val weekday: Int,
    val startMinutes: Int
)

@Serializable
data class BusyAppointmentExportData(
    val weekday: Int,
    val startMinutes: Int,
    val durationMinutes: Int
)