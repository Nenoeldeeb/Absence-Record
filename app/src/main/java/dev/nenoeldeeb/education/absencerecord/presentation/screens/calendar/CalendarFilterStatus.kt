package dev.nenoeldeeb.education.absencerecord.presentation.screens.calendar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.nenoeldeeb.education.absencerecord.R

@Composable
internal fun CalendarFilterStatus(
    selectedClassIds: Set<Int>,
    modifier: Modifier = Modifier
) {
    val filterLabel =
        when (selectedClassIds.size) {
            0 -> stringResource(R.string.class_filter_none)
            1 -> stringResource(R.string.class_filter_one)
            else -> stringResource(R.string.class_filter_n, selectedClassIds.size)
        }
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = filterLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.calendar_filter_scope),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}