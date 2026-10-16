package dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.services.ScheduleRules
import kotlin.math.roundToInt

const val HOUR_LENGTH_SLIDER_TAG = "hour_length_slider"

/**
 * Picks a lesson block's length, 10–120 minutes in 10-minute steps (FR-048).
 *
 * The derived student count is shown beside the control and follows the drag, so
 * the consequence of the length is never hidden (FR-050, FR-053). The spoken
 * label carries BOTH the minutes and the resulting count, and the control is
 * operable by switch access and keyboard at the same granularity as a drag —
 * without that, a screen-reader user cannot tell what the number means
 * (FR-051, SC-017).
 */
@Composable
fun LengthSlider(
    durationMinutes: Int,
    onDurationChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val stepCount =
        (ScheduleRules.MAX_HOUR_DURATION_MINUTES - ScheduleRules.MIN_HOUR_DURATION_MINUTES) /
            ScheduleRules.HOUR_DURATION_STEP_MINUTES
    val steps = (stepCount - 1).coerceAtLeast(0)
    val capacity = ScheduleRules.capacityFor(durationMinutes)
    val spoken = stringResource(R.string.schedule_slider_description, durationMinutes, capacity)

    Column(modifier = modifier.fillMaxWidth().padding(top = 12.dp)) {
        Text(
            text = stringResource(R.string.schedule_duration_label),
            style = MaterialTheme.typography.labelLarge
        )
        Text(
            text = stringResource(R.string.schedule_duration_minutes, durationMinutes),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = stringResource(R.string.schedule_derived_capacity, capacity),
            style = MaterialTheme.typography.bodyMedium
        )
        Slider(
            value = durationMinutes.toFloat(),
            onValueChange = { raw -> onDurationChange(snapToStep(raw)) },
            valueRange =
                ScheduleRules.MIN_HOUR_DURATION_MINUTES.toFloat()..ScheduleRules.MAX_HOUR_DURATION_MINUTES.toFloat(),
            // discrete stops: the slider cannot rest between two lengths
            steps = steps,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag(HOUR_LENGTH_SLIDER_TAG)
                    // Merge, never clear: clearing would strip the slider's
                    // adjustBy action and break switch/keyboard operation (FR-051).
                    .semantics(mergeDescendants = true) { contentDescription = spoken }
        )
    }
}

/** Rounds a raw drag value onto the nearest legal length (FR-049, FR-052). */
private fun snapToStep(raw: Float): Int {
    val step = ScheduleRules.HOUR_DURATION_STEP_MINUTES
    val snapped = (raw / step).roundToInt() * step
    return snapped.coerceIn(
        ScheduleRules.MIN_HOUR_DURATION_MINUTES,
        ScheduleRules.MAX_HOUR_DURATION_MINUTES
    )
}