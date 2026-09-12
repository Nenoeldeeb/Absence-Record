@file:OptIn(ExperimentalMaterial3Api::class)

package dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.services.ScheduleRules
import dev.nenoeldeeb.education.absencerecord.presentation.screens.components.StartTimeField
import dev.nenoeldeeb.education.absencerecord.presentation.utils.TimeFormatter.formatTime
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText

const val HOUR_CAPACITY_OVERFILL_TAG = "hour_capacity_overfill_warning"
const val HOUR_TIME_PICKER_MODE_TOGGLE_TAG = "hour_time_picker_mode_toggle"
const val HOUR_TIME_PICKER_CONFIRM_TAG = "hour_time_picker_confirm"
const val HOUR_TIME_PICKER_DISMISS_TAG = "hour_time_picker_dismiss"

@Composable
fun HourDialog(
    startMinutes: Int,
    maxStudents: String,
    validationError: UiText?,
    isEdit: Boolean,
    onStartSelected: (Int) -> Unit,
    onMaxStudentsChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    assignedCount: Int = 0
) {
    var isTimePickerOpen by remember { mutableStateOf(false) }
    var displayMode by remember { mutableStateOf(TimePickerDisplayMode.Picker) }
    val isStartValid = ScheduleRules.isHourStartValid(startMinutes)
    val enteredMax = ScheduleRules.parseMaxStudents(maxStudents)
    val showsOverfillPreview =
        isEdit && enteredMax != null && enteredMax < assignedCount && validationError == null
    // Step from zero when the field holds no parseable number, so the
    // stepper always lands on a valid capacity instead of preserving garbage.
    val stepperBase = enteredMax ?: 0

    fun onStepperClick(next: Int) {
        onMaxStudentsChange(next.toString())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (isEdit) {
                        R.string.schedule_edit_hour_title
                    } else {
                        R.string.schedule_add_hour_title
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
                    isError = !isStartValid,
                    onOpenTimePicker = {
                        displayMode = TimePickerDisplayMode.Picker
                        isTimePickerOpen = true
                    }
                )
                Text(
                    text =
                        if (!isStartValid) {
                            stringResource(R.string.error_hour_passes_midnight)
                        } else {
                            stringResource(
                                R.string.schedule_hour_ends_at,
                                formatTime(
                                    startMinutes + ScheduleRules.LESSON_DURATION_MINUTES
                                )
                            )
                        },
                    style = MaterialTheme.typography.bodyMedium,
                    color =
                        if (!isStartValid) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    modifier = Modifier.padding(top = 12.dp)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    IconButton(
                        onClick = { onStepperClick(stepperBase - 1) },
                        enabled = stepperBase > 1,
                        modifier =
                            Modifier
                                .defaultMinSize(48.dp, 48.dp)
                                .testTag("hour_capacity_decrease")
                    ) {
                        Icon(
                            imageVector =
                                ImageVector.vectorResource(R.drawable.outline_remove_24),
                            contentDescription =
                                stringResource(R.string.schedule_decrease_capacity)
                        )
                    }
                    OutlinedTextField(
                        value = maxStudents,
                        onValueChange = onMaxStudentsChange,
                        label = { Text(stringResource(R.string.schedule_max_students)) },
                        singleLine = true,
                        isError = validationError != null || showsOverfillPreview,
                        supportingText =
                            validationError?.let {
                                {
                                    Text(it.asString())
                                }
                            },
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            ),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { onStepperClick(stepperBase + 1) },
                        enabled = stepperBase < ScheduleRules.MAX_HOUR_CAPACITY,
                        modifier =
                            Modifier
                                .defaultMinSize(48.dp, 48.dp)
                                .testTag("hour_capacity_increase")
                    ) {
                        Icon(
                            imageVector =
                                ImageVector.vectorResource(R.drawable.outline_add_24),
                            contentDescription =
                                stringResource(R.string.schedule_increase_capacity)
                        )
                    }
                }
                if (showsOverfillPreview) {
                    Text(
                        text = stringResource(R.string.error_max_below_assigned),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier =
                            Modifier
                                .padding(top = 8.dp)
                                .testTag(HOUR_CAPACITY_OVERFILL_TAG)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSave,
                enabled = isStartValid,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
            ) {
                Text(stringResource(R.string.action_save))
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

    if (isTimePickerOpen) {
        val timePickerState =
            rememberTimePickerState(
                initialHour = startMinutes / 60,
                initialMinute = startMinutes % 60
            )
        TimePickerDialog(
            onDismissRequest = { isTimePickerOpen = false },
            title = { Text(stringResource(R.string.schedule_start_time)) },
            modeToggleButton = {
                TimePickerDialogDefaults.DisplayModeToggle(
                    onDisplayModeChange = {
                        displayMode =
                            if (displayMode == TimePickerDisplayMode.Picker) {
                                TimePickerDisplayMode.Input
                            } else {
                                TimePickerDisplayMode.Picker
                            }
                    },
                    displayMode = displayMode,
                    modifier = Modifier.testTag(HOUR_TIME_PICKER_MODE_TOGGLE_TAG)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onStartSelected(timePickerState.hour * 60 + timePickerState.minute)
                        isTimePickerOpen = false
                    },
                    modifier =
                        Modifier
                            .testTag(HOUR_TIME_PICKER_CONFIRM_TAG)
                            .defaultMinSize(minHeight = 48.dp)
                ) {
                    Text(stringResource(R.string.action_set))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { isTimePickerOpen = false },
                    modifier =
                        Modifier
                            .testTag(HOUR_TIME_PICKER_DISMISS_TAG)
                            .defaultMinSize(minHeight = 48.dp)
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            when (displayMode) {
                TimePickerDisplayMode.Picker -> TimePicker(state = timePickerState)
                TimePickerDisplayMode.Input -> TimeInput(state = timePickerState)
            }
        }
    }
}