package dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.nenoeldeeb.education.absencerecord.R

@Composable
internal fun StudentHeader(
    studentName: String,
    assignedClassName: String?,
    presentCount: Int,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = if (compact) 8.dp else 16.dp
                )
    ) {
        Text(
            text = studentName,
            style =
                MaterialTheme.typography.headlineSmall.copy(
                    textDirection = TextDirection.Content
                ),
            textAlign = TextAlign.Start,
            maxLines = if (compact) 1 else 2,
            overflow = TextOverflow.Ellipsis,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics { heading() }
        )
        Spacer(modifier = Modifier.height(if (compact) 2.dp else 4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (assignedClassName != null) {
                Text(
                    text = assignedClassName,
                    style =
                        MaterialTheme.typography.bodyLarge.copy(
                            textDirection = TextDirection.Content
                        ),
                    textAlign = TextAlign.Start,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            } else {
                Text(
                    text = stringResource(R.string.class_unassigned_label),
                    style =
                        MaterialTheme.typography.bodyMedium.copy(
                            textDirection = TextDirection.Content
                        ),
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Start,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text =
                    pluralStringResource(
                        R.plurals.present_days_count,
                        presentCount,
                        presentCount
                    ),
                style =
                    MaterialTheme.typography.bodyMedium.copy(
                        textDirection = TextDirection.Content
                    ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}