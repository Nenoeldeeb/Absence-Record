package dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.nenoeldeeb.education.absencerecord.domain.models.SortType
import dev.nenoeldeeb.education.absencerecord.domain.usecases.StudentManagementUseCases
import dev.nenoeldeeb.education.absencerecord.domain.usecases.schedule.ScheduleUseCases
import dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule.delegates.HourManagementDelegate
import dev.nenoeldeeb.education.absencerecord.presentation.utils.toUiText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleViewModel(
    private val scheduleUseCases: ScheduleUseCases,
    private val studentManagementUseCases: StudentManagementUseCases
) : ViewModel() {
    private val _uiState = MutableStateFlow(ScheduleScreenState())
    val uiState: StateFlow<ScheduleScreenState> = _uiState.asStateFlow()

    private val hourManagementDelegate =
        HourManagementDelegate(
            scheduleUseCases = scheduleUseCases,
            scope = viewModelScope,
            updateState = { transform -> _uiState.update(transform) }
        )

    init {
        observeStudents()
        observeHoursForWeekday()
    }

    private fun observeStudents() {
        viewModelScope.launch {
            studentManagementUseCases.getAllStudentsUseCase(SortType.ByName, null)
                .collectLatest { result ->
                    result
                        .onSuccess { students ->
                            _uiState.update {
                                it.copy(allStudents = students, isLoading = false)
                            }
                        }
                        .onFailure { e ->
                            _uiState.update {
                                it.copy(error = e.toUiText(), isLoading = false)
                            }
                        }
                }
        }
    }

    private fun observeHoursForWeekday() {
        viewModelScope.launch {
            combine(
                _uiState.map { it.selectedWeekday }.distinctUntilChanged(),
                _uiState.map { it.hoursRetryToken }.distinctUntilChanged()
            ) { weekday, _ -> weekday }
                .flatMapLatest { weekday ->
                    scheduleUseCases.observeHoursForWeekdayUseCase(weekday)
                }
                .collectLatest { result ->
                    result
                        .onSuccess { hours ->
                            _uiState.update {
                                it.copy(
                                    hoursForWeekday = hours,
                                    isLoading = false,
                                    isHoursLoading = false,
                                    hoursError = null
                                )
                            }
                        }
                        .onFailure { e ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isHoursLoading = false,
                                    hoursError = e.toUiText()
                                )
                            }
                        }
                }
        }
    }

    fun onEvent(event: ScheduleScreenEvent) {
        when (event) {
            is ScheduleScreenEvent.SelectWeekday ->
                _uiState.update { it.copy(selectedWeekday = event.weekday) }

            is ScheduleScreenEvent.ToggleHourExpanded ->
                _uiState.update {
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
                hourManagementDelegate.openHourDialog(event.hour)

            is ScheduleScreenEvent.DismissHourDialog ->
                hourManagementDelegate.dismissHourDialog()

            is ScheduleScreenEvent.SetHourStart ->
                hourManagementDelegate.setHourStart(event.minutes)

            is ScheduleScreenEvent.SetHourMaxStudents ->
                hourManagementDelegate.setHourMaxStudents(event.value)

            is ScheduleScreenEvent.SaveHour ->
                hourManagementDelegate.saveHour(_uiState.value)

            is ScheduleScreenEvent.ConfirmDeleteHour ->
                hourManagementDelegate.confirmDeleteHour(event.hour, _uiState.value)

            is ScheduleScreenEvent.DismissDeleteHourDialog ->
                hourManagementDelegate.dismissDeleteHourDialog()

            is ScheduleScreenEvent.UndoDeleteHour ->
                hourManagementDelegate.undoDeleteHour(_uiState.value)

            is ScheduleScreenEvent.ConsumeDeletedHour ->
                _uiState.update { it.copy(deletedHour = null, deletedHourAssignedIds = emptyList()) }

            is ScheduleScreenEvent.ConsumeError ->
                _uiState.update { it.copy(error = null) }

            is ScheduleScreenEvent.ConsumeToastMessage ->
                _uiState.update { it.copy(toastMessage = null) }

            is ScheduleScreenEvent.RetryLoadHours ->
                _uiState.update {
                    it.copy(
                        hoursRetryToken = it.hoursRetryToken + 1,
                        isHoursLoading = true,
                        hoursError = null
                    )
                }
        }
    }
}