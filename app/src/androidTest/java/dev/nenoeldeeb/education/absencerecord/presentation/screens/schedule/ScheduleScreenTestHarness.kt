package dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.platform.app.InstrumentationRegistry
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.AvailableLessonHour
import dev.nenoeldeeb.education.absencerecord.domain.models.HourWithOccupancy
import dev.nenoeldeeb.education.absencerecord.presentation.theme.AbsenceRecordTheme
import dev.nenoeldeeb.education.absencerecord.presentation.utils.TimeFormatter
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.DayOfWeek

internal class ScheduleScreenTestHarness(
    val composeTestRule: ComposeContentTestRule
) {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    val recordedEvents = mutableListOf<ScheduleScreenEvent>()
    val recordedStudentClicks = mutableListOf<Int>()

    fun getString(
        resId: Int,
        vararg formatArgs: Any
    ): String = context.getString(resId, *formatArgs)

    fun time(minutes: Int): String = TimeFormatter.format(minutes, context)

    fun hourWith(
        id: Int,
        start: Int,
        max: Int = 5,
        assigned: Int = 0,
        weekday: DayOfWeek = DayOfWeek.SATURDAY
    ): HourWithOccupancy =
        HourWithOccupancy(
            hour = AvailableLessonHour(id, weekday, start, max),
            endMinutes = start + 60,
            assignedStudentIds = (1..assigned).toList(),
            assignedCount = assigned,
            maxStudents = max,
            remainingSlots = max - assigned,
            isFull = assigned >= max
        )

    fun containsEvent(event: ScheduleScreenEvent): Boolean = recordedEvents.contains(event)

    fun setContent(
        stateFlow: MutableStateFlow<ScheduleScreenState>,
        onStudentClick: (Int) -> Unit = { id -> recordedStudentClicks += id }
    ) {
        recordedEvents.clear()
        recordedStudentClicks.clear()
        composeTestRule.setContent {
            AbsenceRecordTheme {
                val uiState = stateFlow.collectAsStateWithLifecycle().value
                ScheduleScreenContent(
                    uiState = uiState,
                    onEvent = { event ->
                        recordedEvents += event
                        applyEvent(stateFlow, event)
                    },
                    onStudentClick = onStudentClick
                )
            }
        }
    }

    private fun applyEvent(
        stateFlow: MutableStateFlow<ScheduleScreenState>,
        event: ScheduleScreenEvent
    ) {
        when (event) {
            is ScheduleScreenEvent.SelectWeekday ->
                stateFlow.update { it.copy(selectedWeekday = event.weekday) }

            is ScheduleScreenEvent.ToggleHourExpanded ->
                stateFlow.update {
                    it.copy(
                        expandedHourIds =
                            if (event.hourId in it.expandedHourIds) {
                                it.expandedHourIds - event.hourId
                            } else {
                                it.expandedHourIds + event.hourId
                            }
                    )
                }

            is ScheduleScreenEvent.OpenHourDialog ->
                stateFlow.update {
                    it.copy(
                        isHourDialogOpen = true,
                        editingHour = event.hour,
                        hourStartMinutes = event.hour?.startMinutes ?: 0,
                        hourMaxStudents = event.hour?.maxStudents?.toString() ?: "",
                        hourValidationError = null
                    )
                }

            is ScheduleScreenEvent.SetHourStart ->
                stateFlow.update { it.copy(hourStartMinutes = event.minutes) }

            is ScheduleScreenEvent.SetHourMaxStudents ->
                stateFlow.update { it.copy(hourMaxStudents = event.value) }

            is ScheduleScreenEvent.SaveHour ->
                stateFlow.update {
                    it.copy(
                        hourValidationError =
                            UiText.StringResource(R.string.error_hour_overlap)
                    )
                }

            is ScheduleScreenEvent.DismissHourDialog ->
                stateFlow.update { it.copy(isHourDialogOpen = false) }

            is ScheduleScreenEvent.ConfirmDeleteHour ->
                stateFlow.update { it.copy(hourToDelete = event.hour) }

            is ScheduleScreenEvent.DismissDeleteHourDialog ->
                stateFlow.update { it.copy(hourToDelete = null) }

            else -> Unit
        }
    }
}