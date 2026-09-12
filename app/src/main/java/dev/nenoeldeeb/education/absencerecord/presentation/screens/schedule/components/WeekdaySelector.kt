package dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.presentation.utils.DateFormatter.toUiText
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

@Composable
fun WeekdaySelector(
    selectedWeekday: DayOfWeek,
    onWeekdaySelected: (DayOfWeek) -> Unit,
    modifier: Modifier = Modifier
) {
    val saturdayFirst = DayOfWeek.entries.drop(5) + DayOfWeek.entries.take(5)
    val today =
        remember {
            Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).dayOfWeek
        }
    val todayLabel = stringResource(R.string.schedule_today)
    val scrollState = rememberScrollState()
    val flingBehavior = ScrollableDefaults.flingBehavior()
    // Nested-scroll deltas arrive in finger space while ScrollState and
    // FlingBehavior operate in scroll space; like foundation's Scrollable,
    // reverse once for LTR (RTL lays out mirrored, so identity there).
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    fun toScrollSpace(finger: Float): Float = if (isRtl) finger else -finger
    // The strip lives inside HorizontalPagers (Schedule page, Student Detail
    // schedule page). It is a control, not a swipe area: trap horizontal
    // gestures here so a drag over the chips scrolls the chips instead of
    // flipping pages, even when the strip is already at its scroll bound.
    // Vertical gestures still flow to the pager.
    val chipsNestedScroll =
        remember(scrollState, flingBehavior, isRtl) {
            object : NestedScrollConnection {
                override fun onPreScroll(
                    available: Offset,
                    source: NestedScrollSource
                ): Offset {
                    if (source == NestedScrollSource.SideEffect) return Offset.Zero
                    val dx = available.x
                    if (dx == 0f || scrollState.maxValue == 0) return Offset.Zero
                    scrollState.dispatchRawDelta(toScrollSpace(dx))
                    return Offset(x = dx, y = 0f)
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    val vx = available.x
                    if (vx == 0f || scrollState.maxValue == 0) return Velocity.Zero
                    scrollState.scroll {
                        with(flingBehavior) {
                            performFling(toScrollSpace(vx))
                        }
                    }
                    return Velocity(vx, 0f)
                }
            }
        }
    Row(
        modifier =
            modifier
                .testTag("weekday_selector_chips")
                .nestedScroll(chipsNestedScroll)
                .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        saturdayFirst.forEach { weekday ->
            val label = weekday.toUiText(fullName = false).asString()
            val isToday = weekday == today
            FilterChip(
                selected = weekday == selectedWeekday,
                onClick = { onWeekdaySelected(weekday) },
                label = { Text(label) },
                leadingIcon =
                    if (weekday == selectedWeekday) {
                        {
                            Icon(
                                imageVector =
                                    ImageVector.vectorResource(R.drawable.outline_check_24),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        null
                    },
                trailingIcon =
                    if (isToday) {
                        {
                            Box(
                                modifier =
                                    Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    } else {
                        null
                    },
                colors =
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        selectedLeadingIconColor =
                            MaterialTheme.colorScheme.onSecondaryContainer,
                        selectedTrailingIconColor = MaterialTheme.colorScheme.primary
                    ),
                modifier =
                    Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .semantics {
                            if (isToday) stateDescription = todayLabel
                        }
            )
        }
    }
}