package dev.nenoeldeeb.education.absencerecord.presentation.screens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.presentation.utils.DateFormatter.toMonthYearUiText
import dev.nenoeldeeb.education.absencerecord.presentation.utils.DateFormatter.toUiText
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

@Composable
fun ComposeCalendar(
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    initialMonth: LocalDate? = null,
    markedDates: Set<LocalDate> = emptySet()
) {
    val today =
        rememberSaveable {
            val curr = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            LocalDate(curr.year, curr.month, curr.day)
        }
    var displayedMonth by rememberSaveable {
        mutableStateOf(
            initialMonth ?: today
        )
    }

    val daysInMonth by remember(displayedMonth) {
        derivedStateOf { calculateDaysInMonth(displayedMonth) }
    }

    // Force LTR layout for calendar
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        BoxWithConstraints(
            modifier = modifier
        ) {
            val isLandscape = maxWidth > maxHeight
            val optimalWidth =
                if (isLandscape) {
                    // In landscape, use a size that's proportional to the height
                    minOf(maxWidth, maxHeight * 2f)
                } else {
                    // In portrait, use full width but ensure cells aren't too big
                    minOf(maxWidth, 400.dp)
                }

            Column(
                modifier =
                    Modifier
                        .width(optimalWidth)
                        .padding(8.dp)
                        .align(Alignment.Center)
            ) {
                CalendarHeader(
                    displayedMonth = displayedMonth,
                    onPreviousMonth = {
                        displayedMonth = displayedMonth.minus(1, DateTimeUnit.MONTH)
                    },
                    onNextMonth = { displayedMonth = displayedMonth.plus(1, DateTimeUnit.MONTH) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                )
                DaysOfWeekHeader(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                CalendarGrid(
                    days = daysInMonth,
                    today = today,
                    markedDates = markedDates,
                    onDateSelected = onDateSelected,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun CalendarHeader(
    displayedMonth: LocalDate,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Force LTR layout for header
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        val headerColor = MaterialTheme.colorScheme.onSurface
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousMonth) {
                Icon(
                    ImageVector.vectorResource(id = R.drawable.outline_chevron_left_24),
                    stringResource(R.string.previous_month),
                    tint = headerColor
                )
            }
            Text(
                text = displayedMonth.toMonthYearUiText(fullName = true).asString(),
                style = MaterialTheme.typography.titleMedium,
                color = headerColor
            )
            IconButton(onClick = onNextMonth) {
                Icon(
                    ImageVector.vectorResource(id = R.drawable.outline_chevron_right_24),
                    stringResource(R.string.next_month),
                    tint = headerColor
                )
            }
        }
    }
}

@Composable
private fun DaysOfWeekHeader(modifier: Modifier = Modifier) {
    // Force LTR layout for days header
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            // Use device locale for day names, starting from Saturday as per original logic
            // If strict Sat-Fri order is needed regardless of locale, hardcode or adjust DayOfWeek.values()
            val daysOfWeek =
                rememberSaveable {
                    // DayOfWeek enum starts with MONDAY. Adjust to start with Saturday for this calendar.
                    val weekDays = DayOfWeek.entries.toTypedArray() // MONDAY to SUNDAY
                    // Rotate to SATURDAY, SUNDAY, MONDAY ... FRIDAY
                    weekDays.drop(DayOfWeek.SATURDAY.isoDayNumber - 1) + weekDays.take(DayOfWeek.SATURDAY.isoDayNumber - 1)
                }

            daysOfWeek.forEach { day ->
                Text(
                    text = day.toUiText(fullName = false).asString(),
                    modifier = Modifier.weight(1f),
                    softWrap = false,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun CalendarGrid(
    days: List<LocalDate?>,
    today: LocalDate,
    markedDates: Set<LocalDate>,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    // Force LTR layout for calendar grid
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            // Removed horizontalArrangement and verticalArrangement, defaults are fine
            modifier = modifier
        ) {
            items(
                days.size,
                key = { index -> days[index]?.toString() ?: "empty-$index" }
            ) { index ->
                val day = days[index]
                DayCell(
                    day = day,
                    isCurrentDay = day == today,
                    isMarked = markedDates.contains(day),
                    onDateSelected = onDateSelected,
                    modifier =
                        Modifier
                            .aspectRatio(1f)
                            .padding(2.dp)
                            .clip(CircleShape)
                )
            }
        }
    }
}

@Composable
private fun DayCell(
    day: LocalDate?,
    isCurrentDay: Boolean,
    isMarked: Boolean,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val dayContentDescription =
        if (isMarked) {
            stringResource(R.string.content_description_present)
        } else if (isCurrentDay) {
            stringResource(R.string.content_description_today)
        } else {
            ""
        }
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
                        contentDescription = dayContentDescription
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

private fun calculateDaysInMonth(yearMonth: LocalDate): List<LocalDate?> {
    val firstDayOfMonth = LocalDate(yearMonth.year, yearMonth.month, 1)
    val nextMonth = firstDayOfMonth.plus(1, DateTimeUnit.MONTH)
    val lastDayOfMonth = nextMonth.minus(1, DateTimeUnit.DAY)

    // Calculate the first day offset (Saturday = 0, Sunday = 1, ..., Friday = 6)
    // DayOfWeek.SATURDAY.value is 6. (6 + 1) % 7 = 0.
    // DayOfWeek.MONDAY.value is 1. (1 + 1) % 7 = 2.
    val firstDayOfWeekCalendar = DayOfWeek.SATURDAY // Assuming calendar starts on Saturday
    var startOffset = firstDayOfMonth.dayOfWeek.isoDayNumber - firstDayOfWeekCalendar.isoDayNumber
    if (startOffset < 0) startOffset += 7

    val days = mutableListOf<LocalDate?>()
    repeat(startOffset) { days.add(null) }
    (1..lastDayOfMonth.day).forEach { dayOfMonth ->
        days.add(LocalDate(yearMonth.year, yearMonth.month, dayOfMonth))
    }
    while (days.size % 7 != 0) {
        days.add(null)
    }
    return days
}