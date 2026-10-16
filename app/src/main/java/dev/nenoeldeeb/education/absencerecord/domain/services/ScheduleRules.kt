package dev.nenoeldeeb.education.absencerecord.domain.services

object ScheduleRules {
    const val DAY_MINUTES = 1440
    const val MIN_BUSY_DURATION_MINUTES = 30
    const val MAX_BUSY_DURATION_MINUTES = 360

    /** Each student occupies exactly one contiguous ten-minute slot (FR-016). */
    const val SLOT_DURATION_MINUTES = 10

    /** Selectable lesson-block lengths (FR-002). */
    const val MIN_HOUR_DURATION_MINUTES = 10
    const val MAX_HOUR_DURATION_MINUTES = 120
    const val HOUR_DURATION_STEP_MINUTES = 10

    const val DEFAULT_NEW_HOUR_START_MINUTES = 8 * 60
    const val DEFAULT_NEW_HOUR_DURATION_MINUTES = 60

    fun overlaps(
        aStart: Int,
        aEnd: Int,
        bStart: Int,
        bEnd: Int
    ): Boolean = aStart < bEnd && aEnd > bStart

    /** True for 10…120 minutes inclusive, in whole ten-minute steps (FR-002). */
    fun isHourDurationValid(durationMinutes: Int): Boolean =
        durationMinutes in MIN_HOUR_DURATION_MINUTES..MAX_HOUR_DURATION_MINUTES &&
            durationMinutes % HOUR_DURATION_STEP_MINUTES == 0

    /** Every selectable length, ascending — the twelve values the slider stops on. */
    fun selectableDurations(): List<Int> =
        (MIN_HOUR_DURATION_MINUTES..MAX_HOUR_DURATION_MINUTES step HOUR_DURATION_STEP_MINUTES).toList()

    /** The derived capacity. The only capacity value in the system (FR-003, FR-004). */
    fun capacityFor(durationMinutes: Int): Int = durationMinutes / SLOT_DURATION_MINUTES

    /** A block must be a valid length and must end by midnight (FR-002, FR-007). */
    fun isHourStartValid(
        startMinutes: Int,
        durationMinutes: Int
    ): Boolean =
        isHourDurationValid(durationMinutes) &&
            startMinutes >= 0 &&
            startMinutes + durationMinutes <= DAY_MINUTES

    /** Converts a pre-duration stored capacity into a length, clamped to range (FR-040, FR-041). */
    fun durationFromLegacyCapacity(maxStudents: Int): Int {
        val raw = maxStudents * SLOT_DURATION_MINUTES
        return raw.coerceIn(MIN_HOUR_DURATION_MINUTES, MAX_HOUR_DURATION_MINUTES)
    }

    fun isBusyStartValid(start: Int): Boolean = start >= 0

    fun isBusyDurationValid(
        start: Int,
        duration: Int
    ): Boolean =
        duration in MIN_BUSY_DURATION_MINUTES..MAX_BUSY_DURATION_MINUTES &&
            duration % 30 == 0 &&
            start >= 0 &&
            start + duration <= DAY_MINUTES
}