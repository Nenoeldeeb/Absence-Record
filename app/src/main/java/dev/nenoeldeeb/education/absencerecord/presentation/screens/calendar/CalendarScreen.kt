package dev.nenoeldeeb.education.absencerecord.presentation.screens.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.app.AppViewModelProvider
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.presentation.screens.components.ComposeCalendar
import dev.nenoeldeeb.education.absencerecord.presentation.utils.SoundPlayer
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * State-owning entry point. Deliberately thin: everything testable lives in
 * [CalendarScreenContent], which takes a plain state object and an event sink.
 *
 * Instrumented tests drive that directly — a final ViewModel cannot be mocked
 * on device, so tests must not depend on one.
 */
@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = viewModel(factory = AppViewModelProvider.factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CalendarScreenContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        modifier = modifier
    )
}

@Composable
internal fun CalendarScreenContent(
    uiState: CalendarScreenState,
    onEvent: (CalendarScreenEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val onDateSelected: (LocalDate) -> Unit = { date ->
        onEvent(CalendarScreenEvent.SelectDateForDialog(date))
    }
    val onToggleClassSelection: (Int) -> Unit = { classId ->
        onEvent(CalendarScreenEvent.ToggleClassSelection(classId))
    }
    val onToggleFilterDropdown: () -> Unit = { onEvent(CalendarScreenEvent.ToggleFilterDropdown) }
    val onClearClassFilter: () -> Unit = { onEvent(CalendarScreenEvent.ClearClassFilter) }
    val onDismissDialog: () -> Unit = { onEvent(CalendarScreenEvent.SelectDateForDialog(null)) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it.asString(context))
            onEvent(CalendarScreenEvent.ConsumeError)
        }
    }

    LaunchedEffect(uiState.attendanceMessage) {
        uiState.attendanceMessage?.let { message ->
            val result =
                if (uiState.lastAttendanceChange != null) {
                    snackbarHostState.showSnackbar(
                        message = message.asString(context),
                        actionLabel = context.getString(R.string.action_undo),
                        duration = SnackbarDuration.Short
                    )
                } else {
                    snackbarHostState.showSnackbar(message.asString(context))
                    SnackbarResult.Dismissed
                }
            if (result == SnackbarResult.ActionPerformed) {
                onEvent(CalendarScreenEvent.UndoLastAttendanceChange)
            } else {
                onEvent(CalendarScreenEvent.ConsumeAttendanceMessage)
            }
        }
    }

    LaunchedEffect(uiState.pendingSoundIsAdd) {
        uiState.pendingSoundIsAdd?.let { isAdd ->
            if (isAdd) {
                SoundPlayer.playAddAttendanceSound(context)
            } else {
                SoundPlayer.playRemoveAttendanceSound(context)
            }
            onEvent(CalendarScreenEvent.ConsumeSound)
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        BoxWithConstraints(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            val isLandscape = maxWidth > maxHeight
            if (isLandscape) {
                // Landscape: side-by-side panes so status + legend don't steal
                // vertical space from the grid. Size-driven, never orientation-locked.
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(0.32f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CalendarFilterStatus(
                            selectedClassIds = uiState.selectedClassIds,
                            availableClasses = uiState.availableClasses
                        )
                        CalendarLegend()
                    }
                    ComposeCalendar(
                        modifier = Modifier.weight(0.68f).fillMaxHeight(),
                        markedDates = uiState.markedDates,
                        onDateSelected = onDateSelected
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    CalendarFilterStatus(
                        selectedClassIds = uiState.selectedClassIds,
                        availableClasses = uiState.availableClasses
                    )
                    ComposeCalendar(
                        modifier = Modifier.weight(1f),
                        markedDates = uiState.markedDates,
                        onDateSelected = onDateSelected
                    )
                    CalendarLegend()
                }
            }
        }
    }

    uiState.selectedDateForDialog?.let { dialogDate ->
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val onToggleAttendance: (Student, LocalDate, Boolean) -> Unit =
            remember(today) {
                { student: Student, date: LocalDate, isPresent: Boolean ->
                    if (date <= today) {
                        if (isPresent) {
                            onEvent(CalendarScreenEvent.DeleteStudentAttendance(student.id, date))
                        } else {
                            onEvent(CalendarScreenEvent.MarkStudentAttendance(student.id, date))
                        }
                    }
                }
            }
        AttendanceDialog(
            allStudents = uiState.allStudents,
            studentsForSelectedDate = uiState.studentsForSelectedDate,
            selectedDate = dialogDate,
            availableClasses = uiState.availableClasses,
            selectedClassIds = uiState.selectedClassIds,
            filterDropdownExpanded = uiState.filterDropdownExpanded,
            isFutureDate = dialogDate > today,
            onDismiss = onDismissDialog,
            onToggleAttendance = onToggleAttendance,
            onToggleClassSelection = onToggleClassSelection,
            onToggleFilterDropdown = onToggleFilterDropdown,
            onClearClassFilter = onClearClassFilter
        )
    }
}