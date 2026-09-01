package dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule

import dev.nenoeldeeb.education.absencerecord.domain.models.AvailableLessonHour
import kotlinx.datetime.DayOfWeek

sealed interface ScheduleScreenEvent {
    data class SelectWeekday(val weekday: DayOfWeek) : ScheduleScreenEvent

    data class ToggleHourExpanded(val hourId: Int) : ScheduleScreenEvent

    data class OpenHourDialog(val hour: AvailableLessonHour?) : ScheduleScreenEvent

    data object DismissHourDialog : ScheduleScreenEvent

    data class SetHourStart(val minutes: Int) : ScheduleScreenEvent

    data class SetHourMaxStudents(val value: String) : ScheduleScreenEvent

    data object SaveHour : ScheduleScreenEvent

    data class ConfirmDeleteHour(val hour: AvailableLessonHour) : ScheduleScreenEvent

    data object DismissDeleteHourDialog : ScheduleScreenEvent

    data object ConsumeError : ScheduleScreenEvent

    data object ConsumeToastMessage : ScheduleScreenEvent
}