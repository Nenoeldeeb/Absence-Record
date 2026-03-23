package dev.nenoeldeeb.education.absencerecord.presentation.screens.report.dialogs

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.presentation.utils.DateFormatter.toMonthYearUiText
import kotlinx.datetime.LocalDate

@Composable
internal fun StudentHistoryDialog(
    student: Student,
    history: List<Pair<LocalDate, List<LocalDate>>>,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onShareMonth: (LocalDate) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.student_history_title, student.name)) },
        text = {
            if (history.isEmpty()) {
                Text(
                    stringResource(R.string.no_attendance_records),
                    textAlign = TextAlign.Center,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(history, key = { it.first.toString() }) { (month, dates) ->
                        ListItem(
                            headlineContent = {
                                Text(
                                    month.toMonthYearUiText(fullName = true).asString(),
                                    style = MaterialTheme.typography.titleSmall
                                )
                            },
                            supportingContent = {
                                Text(
                                    pluralStringResource(
                                        R.plurals.days_count,
                                        dates.size,
                                        dates.size
                                    )
                                )
                            },
                            trailingContent = {
                                IconButton(onClick = { onShareMonth(month) }) {
                                    Icon(
                                        imageVector =
                                            ImageVector.vectorResource(
                                                id = R.drawable.outline_share_24
                                            ),
                                        contentDescription =
                                            stringResource(
                                                R.string.share_calendar_image
                                            )
                                    )
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        },
        modifier = modifier
    )
}