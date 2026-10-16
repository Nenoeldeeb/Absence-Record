package dev.nenoeldeeb.education.absencerecord.domain.usecases.schedule

import dev.nenoeldeeb.education.absencerecord.domain.models.HourReplanResult
import dev.nenoeldeeb.education.absencerecord.domain.repositories.ScheduleRepository
import kotlinx.datetime.DayOfWeek

/**
 * Edits a lesson block's length or start time (FR-009…FR-015).
 *
 * Call without [confirmed] first. If the change would drop students the result is
 * [HourReplanResult.NeedsConfirmation] and nothing has been written; show the
 * teacher the order and the named drop list. Call again with `confirmed = true`
 * to commit.
 */
class UpdateHourUseCase(
    private val repository: ScheduleRepository
) {
    suspend operator fun invoke(
        id: Int,
        weekday: DayOfWeek,
        startMinutes: Int,
        durationMinutes: Int,
        confirmed: Boolean = false
    ): Result<HourReplanResult> {
        return repository.updateHour(id, weekday, startMinutes, durationMinutes, confirmed)
    }
}