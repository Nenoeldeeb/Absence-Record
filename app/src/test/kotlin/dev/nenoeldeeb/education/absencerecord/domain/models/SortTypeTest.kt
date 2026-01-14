package dev.nenoeldeeb.education.absencerecord.domain.models

import org.junit.jupiter.api.DisplayName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DisplayName("SortType Tests")
class SortTypeTest {
    @Test
    fun `should have correct enum values`() {
        // Act
        val values = SortType.entries.toTypedArray()

        // Assert
        assertEquals(2, values.size)
        assertTrue(values.contains(SortType.ByName))
        assertTrue(values.contains(SortType.ByAttendance))
    }

    @Test
    fun `should have correct order`() {
        // Assert
        assertEquals(0, SortType.ByName.ordinal)
        assertEquals(1, SortType.ByAttendance.ordinal)
    }

    @Test
    fun `should convert from string`() {
        // Act & Assert
        assertEquals(SortType.ByName, SortType.valueOf("ByName"))
        assertEquals(SortType.ByAttendance, SortType.valueOf("ByAttendance"))
    }
}