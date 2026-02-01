package dev.nenoeldeeb.education.absencerecord.presentation.utils

import dev.nenoeldeeb.education.absencerecord.R
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month

/**
 * Utility object for formatting dates to localized UiText.
 * Provides extension functions for LocalDate, Month, and DayOfWeek.
 */
object DateFormatter {
    fun LocalDate.toMonthYearUiText(fullName: Boolean = true): UiText {
        return UiText.StringResource(
            R.string.date_format_month_year,
            this.month.toUiText(fullName = fullName),
            this.year
        )
    }

    fun Month.toUiText(fullName: Boolean = true): UiText {
        val resId =
            if (fullName) {
                when (this) {
                    Month.JANUARY -> R.string.month_january
                    Month.FEBRUARY -> R.string.month_february
                    Month.MARCH -> R.string.month_march
                    Month.APRIL -> R.string.month_april
                    Month.MAY -> R.string.month_may
                    Month.JUNE -> R.string.month_june
                    Month.JULY -> R.string.month_july
                    Month.AUGUST -> R.string.month_august
                    Month.SEPTEMBER -> R.string.month_september
                    Month.OCTOBER -> R.string.month_october
                    Month.NOVEMBER -> R.string.month_november
                    Month.DECEMBER -> R.string.month_december
                }
            } else {
                when (this) {
                    Month.JANUARY -> R.string.month_short_january
                    Month.FEBRUARY -> R.string.month_short_february
                    Month.MARCH -> R.string.month_short_march
                    Month.APRIL -> R.string.month_short_april
                    Month.MAY -> R.string.month_short_may
                    Month.JUNE -> R.string.month_short_june
                    Month.JULY -> R.string.month_short_july
                    Month.AUGUST -> R.string.month_short_august
                    Month.SEPTEMBER -> R.string.month_short_september
                    Month.OCTOBER -> R.string.month_short_october
                    Month.NOVEMBER -> R.string.month_short_november
                    Month.DECEMBER -> R.string.month_short_december
                }
            }
        return UiText.StringResource(resId)
    }

    fun DayOfWeek.toUiText(fullName: Boolean = true): UiText {
        val resId =
            if (fullName) {
                when (this) {
                    DayOfWeek.MONDAY -> R.string.day_monday
                    DayOfWeek.TUESDAY -> R.string.day_tuesday
                    DayOfWeek.WEDNESDAY -> R.string.day_wednesday
                    DayOfWeek.THURSDAY -> R.string.day_thursday
                    DayOfWeek.FRIDAY -> R.string.day_friday
                    DayOfWeek.SATURDAY -> R.string.day_saturday
                    DayOfWeek.SUNDAY -> R.string.day_sunday
                }
            } else {
                when (this) {
                    DayOfWeek.MONDAY -> R.string.day_short_monday
                    DayOfWeek.TUESDAY -> R.string.day_short_tuesday
                    DayOfWeek.WEDNESDAY -> R.string.day_short_wednesday
                    DayOfWeek.THURSDAY -> R.string.day_short_thursday
                    DayOfWeek.FRIDAY -> R.string.day_short_friday
                    DayOfWeek.SATURDAY -> R.string.day_short_saturday
                    DayOfWeek.SUNDAY -> R.string.day_short_sunday
                }
            }
        return UiText.StringResource(resId)
    }
}