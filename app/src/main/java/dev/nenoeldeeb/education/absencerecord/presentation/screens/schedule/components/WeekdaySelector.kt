package dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import dev.nenoeldeeb.education.absencerecord.presentation.utils.DateFormatter.toUiText
import kotlinx.datetime.DayOfWeek

@Composable
fun WeekdaySelector(
    selectedWeekday: DayOfWeek,
    onWeekdaySelected: (DayOfWeek) -> Unit,
    modifier: Modifier = Modifier
) {
    val saturdayFirst = DayOfWeek.entries.drop(5) + DayOfWeek.entries.take(5)
    Row(
        modifier =
            modifier
                .testTag("weekday_selector_chips")
                .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        saturdayFirst.forEach { weekday ->
            val label = weekday.toUiText(fullName = true).asString()
            FilterChip(
                selected = weekday == selectedWeekday,
                onClick = { onWeekdaySelected(weekday) },
                label = { Text(label) },
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
            )
        }
    }
}