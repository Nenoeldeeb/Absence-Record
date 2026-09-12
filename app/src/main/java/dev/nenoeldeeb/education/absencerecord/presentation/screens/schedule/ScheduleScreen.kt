@file:OptIn(ExperimentalMaterial3Api::class)

package dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.app.AppViewModelProvider
import dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule.components.AppointmentsTab
import dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule.components.WeekdaySelector
import dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule.dialogs.HourDialog
import dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule.dialogs.ScheduleConfirmDialog
import dev.nenoeldeeb.education.absencerecord.presentation.utils.TimeFormatter.formatTime

@Composable
fun ScheduleScreen(
    onStudentClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScheduleViewModel = viewModel(factory = AppViewModelProvider.factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ScheduleScreenContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onStudentClick = onStudentClick,
        modifier = modifier
    )
}

@Composable
fun ScheduleScreenContent(
    uiState: ScheduleScreenState,
    onEvent: (ScheduleScreenEvent) -> Unit,
    onStudentClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it.asString(context))
            onEvent(ScheduleScreenEvent.ConsumeError)
        }
    }

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let {
            snackbarHostState.showSnackbar(it.asString(context))
            onEvent(ScheduleScreenEvent.ConsumeToastMessage)
        }
    }

    LaunchedEffect(uiState.deletedHour) {
        uiState.deletedHour?.let {
            val result =
                snackbarHostState.showSnackbar(
                    message = context.getString(R.string.schedule_hour_deleted),
                    actionLabel = context.getString(R.string.action_undo),
                    duration = SnackbarDuration.Long
                )
            if (result == SnackbarResult.ActionPerformed) {
                onEvent(ScheduleScreenEvent.UndoDeleteHour)
            }
            onEvent(ScheduleScreenEvent.ConsumeDeletedHour)
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
        ) {
            WeekdaySelector(
                selectedWeekday = uiState.selectedWeekday,
                onWeekdaySelected = {
                    onEvent(ScheduleScreenEvent.SelectWeekday(it))
                },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
            AppointmentsTab(
                hours = uiState.hoursForWeekday,
                students = uiState.allStudents,
                selectedWeekday = uiState.selectedWeekday,
                expandedHourIds = uiState.expandedHourIds,
                isHoursLoading = uiState.isHoursLoading,
                hoursError = uiState.hoursError,
                onAddHour = {
                    onEvent(ScheduleScreenEvent.OpenHourDialog(null))
                },
                onToggleHourExpanded = { hourId ->
                    onEvent(ScheduleScreenEvent.ToggleHourExpanded(hourId))
                },
                onStudentClick = onStudentClick,
                onEditClick = { hour ->
                    onEvent(
                        ScheduleScreenEvent.OpenHourDialog(hour.hour)
                    )
                },
                onDeleteClick = { hour ->
                    onEvent(
                        ScheduleScreenEvent.ConfirmDeleteHour(hour.hour)
                    )
                },
                onRetry = {
                    onEvent(ScheduleScreenEvent.RetryLoadHours)
                },
                modifier = Modifier.weight(1f)
            )
        }
    }

    if (uiState.isHourDialogOpen) {
        val editingAssignedCount =
            uiState.editingHour?.let { editing ->
                uiState.hoursForWeekday
                    .firstOrNull { it.hour.id == editing.id }
                    ?.assignedCount ?: 0
            } ?: 0
        HourDialog(
            startMinutes = uiState.hourStartMinutes,
            maxStudents = uiState.hourMaxStudents,
            validationError = uiState.hourValidationError,
            isEdit = uiState.editingHour != null,
            onStartSelected = {
                onEvent(ScheduleScreenEvent.SetHourStart(it))
            },
            onMaxStudentsChange = {
                onEvent(ScheduleScreenEvent.SetHourMaxStudents(it))
            },
            onSave = { onEvent(ScheduleScreenEvent.SaveHour) },
            onDismiss = {
                onEvent(ScheduleScreenEvent.DismissHourDialog)
            },
            assignedCount = editingAssignedCount
        )
    }

    uiState.hourToDelete?.let { hour ->
        val assignedNames =
            uiState.hoursForWeekday
                .firstOrNull { it.hour.id == hour.id }
                ?.assignedStudentIds
                .orEmpty()
                .mapNotNull { id -> uiState.allStudents.firstOrNull { it.id == id }?.name }
                .sorted()
        ScheduleConfirmDialog(
            hourLabel = formatTime(hour.startMinutes),
            assignedStudentNames = assignedNames,
            onConfirm = {
                onEvent(ScheduleScreenEvent.ConfirmDeleteHour(hour))
            },
            onDismiss = {
                onEvent(ScheduleScreenEvent.DismissDeleteHourDialog)
            }
        )
    }
}