package dev.nenoeldeeb.education.absencerecord.domain.services

object ScheduleRules {
    const val LESSON_DURATION_MINUTES = 60
    const val DAY_MINUTES = 1440
    const val MIN_BUSY_DURATION_MINUTES = 30
    const val MAX_BUSY_DURATION_MINUTES = 360

    const val MAX_HOUR_START_MINUTES = DAY_MINUTES - LESSON_DURATION_MINUTES

    fun overlaps(
        aStart: Int,
        aEnd: Int,
        bStart: Int,
        bEnd: Int
    ): Boolean = aStart < bEnd && aEnd > bStart

    fun isHourStartValid(start: Int): Boolean = start >= 0 && start + LESSON_DURATION_MINUTES <= DAY_MINUTES

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