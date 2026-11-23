package dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.presentation.screens.components.WeekdaySelector
import dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule.components.AppointmentsTab

/**
 * Stateless screen content: Scaffold with the weekday selector and the
 * hour list, all message snackbars, and the dialogs aggregator. Instrumented
 * tests drive this composable directly through a state harness.
 */
@Composable
internal fun ScheduleScreenContent(
    uiState: ScheduleScreenState,
    onEvent: (ScheduleScreenEvent) -> Unit,
    onStudentClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val deletedHourMessage = stringResource(R.string.schedule_hour_deleted)
    val undoLabel = stringResource(R.string.action_undo)

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
                    message = deletedHourMessage,
                    actionLabel = undoLabel,
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

    ScheduleDialogs(
        uiState = uiState,
        onEvent = onEvent
    )
}