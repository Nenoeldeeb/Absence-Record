package dev.nenoeldeeb.education.absencerecord.presentation.screens.components

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.nenoeldeeb.education.absencerecord.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTimePickerDialog(
    initialMinutes: Int,
    onTimeSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: @Composable (displayMode: TimePickerDisplayMode) -> Unit = {
        TimePickerDialogDefaults.Title(displayMode = it)
    },
    confirmButtonTag: String = "",
    dismissButtonTag: String = "",
    modeToggleTag: String = ""
) {
    val timePickerState =
        rememberTimePickerState(
            initialHour = initialMinutes / 60,
            initialMinute = initialMinutes % 60
        )
    var displayMode by remember { mutableStateOf(TimePickerDisplayMode.Picker) }

    TimePickerDialog(
        onDismissRequest = onDismiss,
        title = { title(displayMode) },
        confirmButton = {
            TextButton(
                onClick = {
                    onTimeSelected(timePickerState.hour * 60 + timePickerState.minute)
                    onDismiss()
                },
                modifier =
                    Modifier
                        .then(if (confirmButtonTag.isNotEmpty()) Modifier.testTag(confirmButtonTag) else Modifier)
                        .defaultMinSize(minHeight = 48.dp)
            ) {
                Text(stringResource(R.string.action_set))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier =
                    Modifier
                        .then(if (dismissButtonTag.isNotEmpty()) Modifier.testTag(dismissButtonTag) else Modifier)
                        .defaultMinSize(minHeight = 48.dp)
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        },
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
                modifier = Modifier.then(if (modeToggleTag.isNotEmpty()) Modifier.testTag(modeToggleTag) else Modifier)
            )
        },
        modifier = modifier
    ) {
        when (displayMode) {
            TimePickerDisplayMode.Picker -> TimePicker(state = timePickerState)
            TimePickerDisplayMode.Input -> TimeInput(state = timePickerState)
        }
    }
}