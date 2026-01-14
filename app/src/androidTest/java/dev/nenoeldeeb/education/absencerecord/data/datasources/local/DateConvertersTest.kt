package dev.nenoeldeeb.education.absencerecord.data.datasources.local

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation tests for [DateConverters].
 *
 * These tests verify that Room TypeConverters correctly serialize and deserialize [LocalDate]
 * values for SQLite storage.
 */
@RunWith(AndroidJUnit4::class)
class DateConvertersTest {
    // region fromLocalDate tests

    @Test
    fun fromLocalDate_validDate_returnsIsoString() {
        val date = LocalDate(2023, 10, 15)

        val result = DateConverters.fromLocalDate(date)

        assertEquals("2023-10-15", result)
    }

    @Test
    fun fromLocalDate_nullDate_returnsNull() {
        val result = DateConverters.fromLocalDate(null)

        assertNull(result)
    }

    @Test
    fun fromLocalDate_leapYearDate_handlesCorrectly() {
        val date = LocalDate(2024, 2, 29)

        val result = DateConverters.fromLocalDate(date)

        assertEquals("2024-02-29", result)
    }

    @Test
    fun fromLocalDate_yearBoundaries_handlesCorrectly() {
        val startOfYear = LocalDate(2023, 1, 1)
        val endOfYear = LocalDate(2023, 12, 31)

        assertEquals("2023-01-01", DateConverters.fromLocalDate(startOfYear))
        assertEquals("2023-12-31", DateConverters.fromLocalDate(endOfYear))
    }

    // endregion

    // region toLocalDate tests

    @Test
    fun toLocalDate_validString_returnsLocalDate() {
        val dateString = "2023-10-15"

        val result = DateConverters.toLocalDate(dateString)

        assertEquals(LocalDate(2023, 10, 15), result)
    }

    @Test
    fun toLocalDate_nullString_returnsNull() {
        val result = DateConverters.toLocalDate(null)

        assertNull(result)
    }

    @Test
    fun toLocalDate_leapYearDate_handlesCorrectly() {
        val dateString = "2024-02-29"

        val result = DateConverters.toLocalDate(dateString)

        assertEquals(LocalDate(2024, 2, 29), result)
    }

    // endregion

    // region round-trip tests

    @Test
    fun roundTrip_preservesDate() {
        val originalDate = LocalDate(2023, 10, 15)

        val asString = DateConverters.fromLocalDate(originalDate)
        val backToDate = DateConverters.toLocalDate(asString)

        assertEquals(originalDate, backToDate)
    }

    @Test
    fun roundTrip_multipleConversions_preservesDate() {
        val originalDate = LocalDate(2023, 6, 30)

        // Convert multiple times
        var dateString = DateConverters.fromLocalDate(originalDate)
        var date = DateConverters.toLocalDate(dateString)
        dateString = DateConverters.fromLocalDate(date)
        date = DateConverters.toLocalDate(dateString)

        assertEquals(originalDate, date)
    }

    @Test
    fun roundTrip_edgeDates() {
        val dates =
            listOf(
                LocalDate(2000, 1, 1),
                LocalDate(2023, 12, 31),
                LocalDate(2024, 2, 29),
                LocalDate(1970, 1, 1)
            )

        for (original in dates) {
            val converted = DateConverters.toLocalDate(DateConverters.fromLocalDate(original))
            assertEquals("Failed for date: $original", original, converted)
        }
    }

    // endregion
}