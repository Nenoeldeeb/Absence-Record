package dev.nenoeldeeb.education.absencerecord.presentation.screens.report.dialogs

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.presentation.screens.components.ComposeCalendar
import kotlinx.datetime.LocalDate

@Composable
internal fun CalendarPreviewDialog(
    student: Student,
    monthToPreview: LocalDate,
    datesForPreviewMonth: List<LocalDate>,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onShare: (LocalDate, Int, String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            ComposeCalendar(
                modifier = Modifier.fillMaxWidth(),
                initialMonth = monthToPreview,
                onDateSelected = {},
                markedDates = datesForPreviewMonth.toSet()
            )
        },
        confirmButton = {
            Button(onClick = { onShare(monthToPreview, student.id, student.name) }) {
                Text(stringResource(R.string.action_share_image))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        modifier = modifier
    )
}