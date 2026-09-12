package dev.nenoeldeeb.education.absencerecord.domain.services

object ScheduleRules {
    const val LESSON_DURATION_MINUTES = 60
    const val DAY_MINUTES = 1440
    const val MIN_BUSY_DURATION_MINUTES = 30
    const val MAX_BUSY_DURATION_MINUTES = 360

    const val MAX_HOUR_START_MINUTES = DAY_MINUTES - LESSON_DURATION_MINUTES

    const val DEFAULT_NEW_HOUR_START_MINUTES = 8 * 60
    const val DEFAULT_NEW_HOUR_MAX_STUDENTS = 5
    const val MAX_HOUR_CAPACITY = 99

    fun overlaps(
        aStart: Int,
        aEnd: Int,
        bStart: Int,
        bEnd: Int
    ): Boolean = aStart < bEnd && aEnd > bStart

    fun isHourStartValid(start: Int): Boolean = start >= 0 && start + LESSON_DURATION_MINUTES <= DAY_MINUTES

    /**
     * Parses a capacity field value, accepting ASCII digits as well as
     * Arabic-Indic (٠-٩) and Eastern Arabic-Indic (۰-۹) digits so numeric
     * keyboards in Arabic locales validate instead of failing silently.
     */
    fun parseMaxStudents(value: String): Int? {
        if (value.isBlank()) return null
        val ascii =
            value.trim().map { char ->
                when (char) {
                    in '0'..'9' -> char
                    in '٠'..'٩' -> '0' + (char - '٠')
                    in '۰'..'۹' -> '0' + (char - '۰')
                    else -> return null
                }
            }.joinToString("")
        return ascii.toIntOrNull()
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