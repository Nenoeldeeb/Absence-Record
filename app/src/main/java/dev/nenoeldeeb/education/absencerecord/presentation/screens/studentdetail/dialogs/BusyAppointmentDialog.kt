@file:OptIn(ExperimentalMaterial3Api::class)

package dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.TimePickerDialogDefaults
import androidx.compose.material3.TimePickerDisplayMode
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentLessonEntry
import dev.nenoeldeeb.education.absencerecord.domain.services.ScheduleRules
import dev.nenoeldeeb.education.absencerecord.presentation.screens.components.StartTimeField
import dev.nenoeldeeb.education.absencerecord.presentation.utils.TimeFormatter.formatTime
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText
import kotlin.math.roundToInt

internal const val TIME_PICKER_CONFIRM_TAG = "busy_time_picker_confirm"
internal const val TIME_PICKER_DISMISS_TAG = "busy_time_picker_dismiss"
internal const val TIME_PICKER_MODE_TOGGLE_TAG = "busy_time_picker_mode_toggle"
internal const val BUSY_DURATION_SLIDER_TAG = "busy_duration_slider"

private const val MIN_BUSY_DURATION_MINUTES = 30
private const val MAX_BUSY_DURATION_MINUTES = 360
private const val BUSY_DURATION_STEP_MINUTES = 30

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun BusyAppointmentDialog(
    startMinutes: Int,
    durationMinutes: Int,
    validationError: UiText?,
    isEdit: Boolean,
    conflictLessons: List<StudentLessonEntry>,
    onStartSelected: (Int) -> Unit,
    onDurationSelected: (Int) -> Unit,
    onSave: () -> Unit,
    onConfirmConflict: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showTimePicker by remember { mutableStateOf(false) }
    var duration by remember(durationMinutes) { mutableFloatStateOf(durationMinutes.toFloat()) }
    val effectiveDuration = duration.roundToInt()
    val isValid = ScheduleRules.isBusyDurationValid(startMinutes, effectiveDuration)
    val errorText =
        validationError?.asString()
            ?: if (isValid) null else stringResource(R.string.error_busy_duration_invalid)
    val showConflict = conflictLessons.isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (isEdit) {
                        R.string.schedule_edit_busy_title
                    } else {
                        R.string.schedule_add_busy_title
                    }
                ),
                modifier = Modifier.semantics { heading() }
            )
        },
        text = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
            ) {
                StartTimeField(
                    startMinutes = startMinutes,
                    onOpenTimePicker = { showTimePicker = true }
                )
                Text(
                    text = stringResource(R.string.schedule_busy_duration),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 12.dp)
                )
                val durationDescription = stringResource(R.string.schedule_busy_duration)
                val durationHoursText =
                    if (effectiveDuration % 60 == 0) {
                        "${effectiveDuration / 60}"
                    } else {
                        "${effectiveDuration / 60}.5"
                    }
                val durationValueText =
                    stringResource(
                        R.string.schedule_busy_duration_hours,
                        durationHoursText
                    )
                val endsAtText =
                    stringResource(
                        R.string.schedule_busy_ends_at,
                        formatTime(startMinutes + effectiveDuration)
                    )
                FlowRow(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = durationValueText,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = stringResource(R.string.schedule_busy_duration_range),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Slider(
                    value = duration,
                    onValueChange = { newValue ->
                        duration =
                            (newValue / BUSY_DURATION_STEP_MINUTES).roundToInt() *
                            BUSY_DURATION_STEP_MINUTES.toFloat()
                    },
                    valueRange =
                        MIN_BUSY_DURATION_MINUTES.toFloat()..MAX_BUSY_DURATION_MINUTES.toFloat(),
                    steps =
                        (MAX_BUSY_DURATION_MINUTES - MIN_BUSY_DURATION_MINUTES) /
                            BUSY_DURATION_STEP_MINUTES - 1,
                    onValueChangeFinished = { onDurationSelected(effectiveDuration) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .testTag(BUSY_DURATION_SLIDER_TAG)
                            .semantics {
                                contentDescription = durationDescription
                                stateDescription = "$durationValueText, $endsAtText"
                            }
                )
                if (errorText != null) {
                    Text(
                        text = errorText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                } else {
                    Text(
                        text =
                            stringResource(
                                R.string.schedule_busy_ends_at,
                                formatTime(startMinutes + effectiveDuration)
                            ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
                if (showConflict) {
                    ConflictPreview(conflictLessons)
                }
            }
        },
        confirmButton = {
            if (showConflict) {
                TextButton(
                    onClick = onConfirmConflict,
                    enabled = isValid,
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                ) {
                    Text(stringResource(R.string.student_detail_busy_conflict_confirm))
                }
            } else {
                TextButton(
                    onClick = onSave,
                    enabled = isValid,
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                ) {
                    Text(stringResource(R.string.action_save))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        modifier = modifier
    )

    if (showTimePicker) {
        val timePickerState =
            rememberTimePickerState(
                initialHour = startMinutes / 60,
                initialMinute = startMinutes % 60
            )
        var timePickerMode by remember { mutableStateOf(TimePickerDisplayMode.Picker) }
        TimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            title = {
                TimePickerDialogDefaults.Title(displayMode = timePickerMode)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onStartSelected(timePickerState.hour * 60 + timePickerState.minute)
                        showTimePicker = false
                    },
                    modifier =
                        Modifier
                            .testTag(TIME_PICKER_CONFIRM_TAG)
                            .defaultMinSize(minHeight = 48.dp)
                ) {
                    Text(stringResource(R.string.action_set))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showTimePicker = false },
                    modifier =
                        Modifier
                            .testTag(TIME_PICKER_DISMISS_TAG)
                            .defaultMinSize(minHeight = 48.dp)
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            modeToggleButton = {
                TimePickerDialogDefaults.DisplayModeToggle(
                    onDisplayModeChange = {
                        timePickerMode =
                            if (timePickerMode == TimePickerDisplayMode.Picker) {
                                TimePickerDisplayMode.Input
                            } else {
                                TimePickerDisplayMode.Picker
                            }
                    },
                    displayMode = timePickerMode,
                    modifier = Modifier.testTag(TIME_PICKER_MODE_TOGGLE_TAG)
                )
            }
        ) {
            if (timePickerMode == TimePickerDisplayMode.Picker) {
                TimePicker(state = timePickerState)
            } else {
                TimeInput(state = timePickerState)
            }
        }
    }
}