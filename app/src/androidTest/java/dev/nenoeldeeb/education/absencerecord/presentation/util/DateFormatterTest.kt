package dev.nenoeldeeb.education.absencerecord.presentation.util

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.presentation.util.DateFormatter.toMonthYearUiText
import dev.nenoeldeeb.education.absencerecord.presentation.util.DateFormatter.toUiText
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation tests for [DateFormatter].
 *
 * These tests verify that DateFormatter correctly resolves to localized string resources on an
 * actual Android device/emulator.
 */
@RunWith(AndroidJUnit4::class)
class DateFormatterTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    // region Month.toUiText Tests

    @Test
    fun month_toUiText_fullName_resolvesToCorrectString_forAllMonths() {
        val expectedMonths =
            mapOf(
                Month.JANUARY to context.getString(R.string.month_january),
                Month.FEBRUARY to context.getString(R.string.month_february),
                Month.MARCH to context.getString(R.string.month_march),
                Month.APRIL to context.getString(R.string.month_april),
                Month.MAY to context.getString(R.string.month_may),
                Month.JUNE to context.getString(R.string.month_june),
                Month.JULY to context.getString(R.string.month_july),
                Month.AUGUST to context.getString(R.string.month_august),
                Month.SEPTEMBER to context.getString(R.string.month_september),
                Month.OCTOBER to context.getString(R.string.month_october),
                Month.NOVEMBER to context.getString(R.string.month_november),
                Month.DECEMBER to context.getString(R.string.month_december)
            )

        expectedMonths.forEach { (month, expectedString) ->
            val result = month.toUiText(fullName = true).asString(context)
            assertEquals("Month $month full name mismatch", expectedString, result)
        }
    }

    @Test
    fun month_toUiText_shortName_resolvesToCorrectString_forAllMonths() {
        val expectedMonths =
            mapOf(
                Month.JANUARY to context.getString(R.string.month_short_january),
                Month.FEBRUARY to context.getString(R.string.month_short_february),
                Month.MARCH to context.getString(R.string.month_short_march),
                Month.APRIL to context.getString(R.string.month_short_april),
                Month.MAY to context.getString(R.string.month_short_may),
                Month.JUNE to context.getString(R.string.month_short_june),
                Month.JULY to context.getString(R.string.month_short_july),
                Month.AUGUST to context.getString(R.string.month_short_august),
                Month.SEPTEMBER to context.getString(R.string.month_short_september),
                Month.OCTOBER to context.getString(R.string.month_short_october),
                Month.NOVEMBER to context.getString(R.string.month_short_november),
                Month.DECEMBER to context.getString(R.string.month_short_december)
            )

        expectedMonths.forEach { (month, expectedString) ->
            val result = month.toUiText(fullName = false).asString(context)
            assertEquals("Month $month short name mismatch", expectedString, result)
        }
    }

    // endregion

    // region DayOfWeek.toUiText Tests

    @Test
    fun dayOfWeek_toUiText_fullName_resolvesToCorrectString_forAllDays() {
        val expectedDays =
            mapOf(
                DayOfWeek.MONDAY to context.getString(R.string.day_monday),
                DayOfWeek.TUESDAY to context.getString(R.string.day_tuesday),
                DayOfWeek.WEDNESDAY to context.getString(R.string.day_wednesday),
                DayOfWeek.THURSDAY to context.getString(R.string.day_thursday),
                DayOfWeek.FRIDAY to context.getString(R.string.day_friday),
                DayOfWeek.SATURDAY to context.getString(R.string.day_saturday),
                DayOfWeek.SUNDAY to context.getString(R.string.day_sunday)
            )

        expectedDays.forEach { (day, expectedString) ->
            val result = day.toUiText(fullName = true).asString(context)
            assertEquals("Day $day full name mismatch", expectedString, result)
        }
    }

    @Test
    fun dayOfWeek_toUiText_shortName_resolvesToCorrectString_forAllDays() {
        val expectedDays =
            mapOf(
                DayOfWeek.MONDAY to context.getString(R.string.day_short_monday),
                DayOfWeek.TUESDAY to context.getString(R.string.day_short_tuesday),
                DayOfWeek.WEDNESDAY to context.getString(R.string.day_short_wednesday),
                DayOfWeek.THURSDAY to context.getString(R.string.day_short_thursday),
                DayOfWeek.FRIDAY to context.getString(R.string.day_short_friday),
                DayOfWeek.SATURDAY to context.getString(R.string.day_short_saturday),
                DayOfWeek.SUNDAY to context.getString(R.string.day_short_sunday)
            )

        expectedDays.forEach { (day, expectedString) ->
            val result = day.toUiText(fullName = false).asString(context)
            assertEquals("Day $day short name mismatch", expectedString, result)
        }
    }

    // endregion

    // region LocalDate.toMonthYearUiText Tests

    @Test
    fun localDate_toMonthYearUiText_fullName_containsYearAndMonth() {
        val date = LocalDate(2026, Month.JANUARY, 15)
        val result = date.toMonthYearUiText(fullName = true).asString(context)

        assertTrue("Result should contain year 2026", result.contains("2026"))
        assertTrue(
            "Result should contain January",
            result.contains(context.getString(R.string.month_january))
        )
    }

    @Test
    fun localDate_toMonthYearUiText_shortName_containsYearAndShortMonth() {
        val date = LocalDate(2026, Month.DECEMBER, 25)
        val result = date.toMonthYearUiText(fullName = false).asString(context)

        assertTrue("Result should contain year 2026", result.contains("2026"))
        assertTrue(
            "Result should contain Dec",
            result.contains(context.getString(R.string.month_short_december))
        )
    }

    @Test
    fun localDate_toMonthYearUiText_differentYears_formatCorrectly() {
        val dates =
            listOf(
                LocalDate(2020, Month.MARCH, 1),
                LocalDate(2025, Month.JULY, 15),
                LocalDate(2030, Month.NOVEMBER, 30)
            )

        dates.forEach { date ->
            val result = date.toMonthYearUiText(fullName = true).asString(context)
            assertTrue(
                "Result should contain year ${date.year}",
                result.contains(date.year.toString())
            )
        }
    }

    // endregion

    // region Edge Cases

    @Test
    fun month_toUiText_defaultParameter_usesFullName() {
        val month = Month.JUNE
        val withDefault = month.toUiText().asString(context)
        val withExplicitTrue = month.toUiText(fullName = true).asString(context)

        assertEquals(withExplicitTrue, withDefault)
    }

    @Test
    fun dayOfWeek_toUiText_defaultParameter_usesFullName() {
        val day = DayOfWeek.FRIDAY
        val withDefault = day.toUiText().asString(context)
        val withExplicitTrue = day.toUiText(fullName = true).asString(context)

        assertEquals(withExplicitTrue, withDefault)
    }

    // endregion

    // region UiText Type Verification (from unit tests)

    @Test
    fun toMonthYearUiText_returnsCorrectStringResourceType() {
        val date = LocalDate(2023, Month.JANUARY, 15)
        val result = date.toMonthYearUiText(fullName = true)

        // Verify the structure matches expected UiText.StringResource
        val expected =
            UiText.StringResource(
                R.string.date_format_month_year,
                UiText.StringResource(R.string.month_january),
                2023
            )
        assertEquals(expected, result)
    }

    @Test
    fun month_toUiText_returnsCorrectStringResourceForFullName() {
        assertEquals(UiText.StringResource(R.string.month_january), Month.JANUARY.toUiText(true))
        assertEquals(UiText.StringResource(R.string.month_december), Month.DECEMBER.toUiText(true))
    }

    @Test
    fun month_toUiText_returnsCorrectStringResourceForShortName() {
        assertEquals(
            UiText.StringResource(R.string.month_short_january),
            Month.JANUARY.toUiText(false)
        )
        assertEquals(
            UiText.StringResource(R.string.month_short_december),
            Month.DECEMBER.toUiText(false)
        )
    }

    @Test
    fun dayOfWeek_toUiText_returnsCorrectStringResourceForFullName() {
        assertEquals(UiText.StringResource(R.string.day_monday), DayOfWeek.MONDAY.toUiText(true))
        assertEquals(UiText.StringResource(R.string.day_sunday), DayOfWeek.SUNDAY.toUiText(true))
    }

    @Test
    fun dayOfWeek_toUiText_returnsCorrectStringResourceForShortName() {
        assertEquals(
            UiText.StringResource(R.string.day_short_monday),
            DayOfWeek.MONDAY.toUiText(false)
        )
        assertEquals(
            UiText.StringResource(R.string.day_short_sunday),
            DayOfWeek.SUNDAY.toUiText(false)
        )
    }

    // endregion
}