package dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.DroppedStudent
import dev.nenoeldeeb.education.absencerecord.domain.models.HourReplanResult

const val REPLAN_CONFIRM_DIALOG_TAG = "replan_confirm_dialog"
const val REPLAN_CONFIRM_BUTTON_TAG = "replan_confirm_button"
const val REPLAN_CANCEL_BUTTON_TAG = "replan_cancel_button"
const val REPLAN_DROPPED_ITEM_TAG = "replan_dropped_item"

/**
 * Shows what an edit to a lesson block would do before anything is written.
 *
 * The teacher sees the resulting running order and, by name, every student who
 * would lose their lesson. Nothing has been changed at this point — declining
 * leaves the block exactly as it was (FR-011, FR-012, FR-045).
 */
@Composable
fun ReplanConfirmDialog(
    pending: HourReplanResult.NeedsConfirmation,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val dropped = pending.dropped
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(REPLAN_CONFIRM_DIALOG_TAG),
        title = { Text(stringResource(R.string.schedule_replan_title)) },
        text = {
            if (dropped.isEmpty()) {
                Text(stringResource(R.string.schedule_replan_stay, 0))
            } else if (pending.proposed.slots.isEmpty()) {
                // nothing would be kept — say so plainly rather than offering an
                // empty plan as though it were routine
                Text(stringResource(R.string.schedule_replan_drop_all, dropped.size))
            } else {
                Text(stringResource(R.string.schedule_replan_leave, dropped.size))
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag(REPLAN_CONFIRM_BUTTON_TAG)
            ) { Text(stringResource(R.string.schedule_confirm_replan)) }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(REPLAN_CANCEL_BUTTON_TAG)
            ) { Text(stringResource(R.string.schedule_cancel_replan)) }
        }
    )
}

/** One line per student who would be removed, spoken and visible (FR-045). */
@Composable
fun DroppedStudentLine(student: DroppedStudent) {
    Text(
        text = stringResource(R.string.schedule_dropped_student, student.studentName),
        modifier = Modifier.testTag(REPLAN_DROPPED_ITEM_TAG)
    )
}