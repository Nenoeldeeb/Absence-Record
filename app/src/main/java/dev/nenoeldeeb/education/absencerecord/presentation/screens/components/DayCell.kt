package dev.nenoeldeeb.education.absencerecord.presentation.screens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlinx.datetime.LocalDate

@Composable
internal fun DayCell(
    day: LocalDate?,
    isCurrentDay: Boolean,
    isMarked: Boolean,
    onDateSelected: (LocalDate) -> Unit,
    description: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier =
            modifier
                .then(
                    if (day != null) {
                        Modifier.clickable { onDateSelected(day) }
                    } else {
                        Modifier
                    }
                )
                .then(
                    when {
                        isMarked ->
                            Modifier.background(
                                MaterialTheme.colorScheme.secondary
                            )

                        isCurrentDay && day != null ->
                            Modifier.background(
                                MaterialTheme.colorScheme.primary
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
                        isMarked -> MaterialTheme.colorScheme.onSecondary
                        isCurrentDay -> MaterialTheme.colorScheme.onPrimary
                        else -> MaterialTheme.colorScheme.onSurface
                    }
            )
        }
    }
}