package dev.nenoeldeeb.education.absencerecord.presentation.screens.calendar

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.Student

@Composable
internal fun StudentListItem(
    student: Student,
    isPresent: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val statusText =
        stringResource(
            if (isPresent) {
                R.string.content_description_present
            } else {
                R.string.content_description_absent
            }
        )
    val stateDescription = stringResource(R.string.student_attendance_status, student.name, statusText)
    val disabledReason = stringResource(R.string.calendar_future_date_warning)
    val fullContentDescription =
        if (enabled) {
            stateDescription
        } else {
            stringResource(
                R.string.student_attendance_status_disabled,
                student.name,
                statusText,
                disabledReason
            )
        }
    val baseNameStyle = MaterialTheme.typography.bodyLarge
    val nameStyle = remember(baseNameStyle) { baseNameStyle.copy(textDirection = TextDirection.Content) }
    val listItemColors =
        ListItemDefaults.colors(
            containerColor =
                if (isPresent) {
                    MaterialTheme.colorScheme.tertiaryContainer
                } else {
                    ListItemDefaults.colors().containerColor
                }
        )

    ListItem(
        headlineContent = {
            Text(
                text = student.name,
                textAlign = TextAlign.Start,
                color =
                    if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                style = nameStyle,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .wrapContentWidth()
            )
        },
        leadingContent = {
            Icon(
                imageVector =
                    ImageVector.vectorResource(
                        if (isPresent) {
                            R.drawable.outline_check_24
                        } else {
                            R.drawable.outline_close_24
                        }
                    ),
                contentDescription = null,
                tint =
                    when {
                        !enabled -> MaterialTheme.colorScheme.outlineVariant
                        isPresent -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.outline
                    }
            )
        },
        colors = listItemColors,
        modifier =
            modifier
                .sizeIn(minHeight = 48.dp)
                .toggleable(
                    value = isPresent,
                    enabled = enabled,
                    role = Role.Checkbox,
                    onValueChange = { onToggle() }
                )
                .semantics {
                    this.contentDescription = fullContentDescription
                    if (!enabled) {
                        disabled()
                        this.stateDescription = disabledReason
                    }
                }
    )
}