package dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule

import androidx.compose.runtime.Stable
import dev.nenoeldeeb.education.absencerecord.domain.models.AvailableLessonHour
import dev.nenoeldeeb.education.absencerecord.domain.models.HourReplanResult
import dev.nenoeldeeb.education.absencerecord.domain.models.HourWithOccupancy
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.services.ScheduleRules
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText
import kotlinx.datetime.DayOfWeek

@Stable
data class ScheduleScreenState(
    val selectedWeekday: DayOfWeek = DayOfWeek.SATURDAY,
    val allStudents: List<Student> = emptyList(),
    val hoursForWeekday: List<HourWithOccupancy> = emptyList(),
    val expandedHourIds: Set<Int> = emptySet(),
    val isHourDialogOpen: Boolean = false,
    val editingHour: AvailableLessonHour? = null,
    val hourStartMinutes: Int = ScheduleRules.DEFAULT_NEW_HOUR_START_MINUTES,
    val hourDurationMinutes: Int = ScheduleRules.DEFAULT_NEW_HOUR_DURATION_MINUTES,
    val hourValidationError: UiText? = null,
    val pendingReplan: HourReplanResult.NeedsConfirmation? = null,
    val hourToDelete: AvailableLessonHour? = null,
    val deletedHour: AvailableLessonHour? = null,
    val deletedHourAssignedIds: List<Int> = emptyList(),
    val isLoading: Boolean = true,
    val isHoursLoading: Boolean = true,
    val hoursError: UiText? = null,
    val hoursRetryToken: Int = 0,
    val error: UiText? = null,
    val toastMessage: UiText? = null
)