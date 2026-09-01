package dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.delegates

import dev.nenoeldeeb.education.absencerecord.domain.models.StudentLessonEntry
import dev.nenoeldeeb.education.absencerecord.domain.usecases.schedule.ScheduleUseCases
import dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.StudentDetailScreenState
import dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.StudentScheduleTab
import dev.nenoeldeeb.education.absencerecord.presentation.utils.toUiText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek

@OptIn(ExperimentalCoroutinesApi::class)
class StudentScheduleTabsDelegate(
    private val scheduleUseCases: ScheduleUseCases,
    private val uiState: StateFlow<StudentDetailScreenState>,
    private val scope: CoroutineScope,
    private val updateState: ((StudentDetailScreenState) -> StudentDetailScreenState) -> Unit
) {
    fun observeHours() {
        scope.launch {
            uiState
                .map { it.selectedScheduleWeekday }
                .distinctUntilChanged()
                .flatMapLatest { weekday ->
                    scheduleUseCases.observeHoursForWeekdayUseCase(weekday)
                }
                .collectLatest { result ->
                    result
                        .onSuccess { hours ->
                            updateState { it.copy(hoursForWeekday = hours) }
                        }
                        .onFailure { e ->
                            updateState { it.copy(error = e.toUiText()) }
                        }
                }
        }
    }

    fun selectWeekday(weekday: DayOfWeek) {
        updateState {
            it.copy(
                selectedScheduleWeekday = weekday,
                hoursForWeekday = emptyList(),
                isAddLessonDialogOpen = false,
                selectedLessonHourId = null
            )
        }
    }

    fun selectTab(tab: StudentScheduleTab) {
        updateState { it.copy(selectedScheduleTab = tab) }
    }

    fun openAddLessonDialog(state: StudentDetailScreenState) {
        if (hasLessonOnSelectedWeekday(state)) return
        updateState { it.copy(isAddLessonDialogOpen = true, selectedLessonHourId = null) }
    }

    fun dismissAddLessonDialog() {
        updateState { it.copy(isAddLessonDialogOpen = false, selectedLessonHourId = null) }
    }

    fun selectLessonHour(hourId: Int) {
        updateState { it.copy(selectedLessonHourId = hourId) }
    }

    fun confirmAddLesson(state: StudentDetailScreenState) {
        val hourId = state.selectedLessonHourId ?: return
        val studentId = state.student?.id ?: return
        if (hasLessonOnSelectedWeekday(state)) return
        scope.launch {
            scheduleUseCases.assignStudentUseCase(hourId, studentId, state.selectedScheduleWeekday)
                .onSuccess {
                    updateState {
                        it.copy(isAddLessonDialogOpen = false, selectedLessonHourId = null)
                    }
                }
                .onFailure { e ->
                    updateState { it.copy(error = e.toUiText(), toastMessage = null) }
                }
        }
    }

    fun unassignLesson(
        state: StudentDetailScreenState,
        lesson: StudentLessonEntry
    ) {
        val studentId = state.student?.id ?: return
        val hourId =
            state.hoursForWeekday
                .firstOrNull {
                    it.hour.weekday == lesson.weekday &&
                        it.hour.startMinutes == lesson.startMinutes &&
                        studentId in it.assignedStudentIds
                }
                ?.hour?.id ?: return
        scope.launch {
            scheduleUseCases.unassignStudentUseCase(hourId, studentId)
                .onFailure { e ->
                    updateState { it.copy(error = e.toUiText(), toastMessage = null) }
                }
        }
    }

    private fun hasLessonOnSelectedWeekday(state: StudentDetailScreenState): Boolean =
        state.studentSchedule?.lessons.orEmpty()
            .any { it.weekday == state.selectedScheduleWeekday }
}