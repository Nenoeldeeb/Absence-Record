package dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule

import dev.nenoeldeeb.education.absencerecord.domain.models.AvailableLessonHour
import kotlinx.datetime.DayOfWeek

sealed interface ScheduleScreenEvent {
    data class SelectWeekday(val weekday: DayOfWeek) : ScheduleScreenEvent

    data class ToggleHourExpanded(val hourId: Int) : ScheduleScreenEvent

    data class OpenHourDialog(val hour: AvailableLessonHour?) : ScheduleScreenEvent

    data object DismissHourDialog : ScheduleScreenEvent

    data class SetHourStart(val minutes: Int) : ScheduleScreenEvent

    data class SetHourDuration(val value: Int) : ScheduleScreenEvent

    data object SaveHour : ScheduleScreenEvent

    /** Commits the pending re-plan the teacher has just reviewed (FR-013). */
    data object ConfirmReplan : ScheduleScreenEvent

    /** Declines the pending re-plan, leaving the block exactly as it was (FR-012). */
    data object DismissReplan : ScheduleScreenEvent

    data class ConfirmDeleteHour(val hour: AvailableLessonHour) : ScheduleScreenEvent

    data object DismissDeleteHourDialog : ScheduleScreenEvent

    data object UndoDeleteHour : ScheduleScreenEvent

    data object ConsumeDeletedHour : ScheduleScreenEvent

    data object ConsumeError : ScheduleScreenEvent

    data object ConsumeToastMessage : ScheduleScreenEvent

    data object RetryLoadHours : ScheduleScreenEvent
}