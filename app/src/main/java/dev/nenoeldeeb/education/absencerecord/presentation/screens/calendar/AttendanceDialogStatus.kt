package dev.nenoeldeeb.education.absencerecord.presentation.screens.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.nenoeldeeb.education.absencerecord.R

@Composable
internal fun DialogStatusSlot(
    showEmptyHint: Boolean,
    isFutureDate: Boolean,
    modifier: Modifier = Modifier
) {
    if (!showEmptyHint && !isFutureDate) return
    val futureWarning = stringResource(R.string.calendar_future_date_warning)
    Column(
        modifier = modifier.padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (isFutureDate) {
            Text(
                text = futureWarning,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { error(futureWarning) }
            )
        }
        if (showEmptyHint) {
            Text(
                text = stringResource(R.string.calendar_filter_empty_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}