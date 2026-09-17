package dev.nenoeldeeb.education.absencerecord.presentation.screens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate

@Composable
internal fun DayCell(
    day: LocalDate?,
    isCurrentDay: Boolean,
    isMarked: Boolean,
    onDateSelected: (LocalDate) -> Unit,
    description: String,
    interactive: Boolean = true,
    modifier: Modifier = Modifier
) {
    Box(
        modifier =
            modifier
                .then(
                    if (day != null && interactive) {
                        Modifier.clickable(
                            role = Role.Button,
                            onClickLabel = description
                        ) { onDateSelected(day) }
                    } else {
                        Modifier
                    }
                )
                .then(
                    when {
                        isCurrentDay && isMarked && day != null ->
                            Modifier.background(
                                MaterialTheme.colorScheme.primary,
                                CircleShape
                            ).border(
                                2.dp,
                                MaterialTheme.colorScheme.secondary,
                                CircleShape
                            )

                        isCurrentDay && day != null ->
                            Modifier.background(
                                MaterialTheme.colorScheme.primary,
                                CircleShape
                            )

                        isMarked && day != null ->
                            Modifier
                                .background(
                                    MaterialTheme.colorScheme.secondary,
                                    CircleShape
                                )
                                .border(
                                    2.dp,
                                    MaterialTheme.colorScheme.onSecondaryContainer,
                                    CircleShape
                                )

                        else -> Modifier
                    }
                )
                .semantics {
                    day?.let {
                        contentDescription = description
                    }
                },
        contentAlignment = Alignment.Center
    ) {
        day?.let {
            Text(
                text = it.day.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color =
                    when {
                        isCurrentDay -> MaterialTheme.colorScheme.onPrimary
                        isMarked -> MaterialTheme.colorScheme.onSecondary
                        else -> MaterialTheme.colorScheme.onSurface
                    }
            )
        }
    }
}