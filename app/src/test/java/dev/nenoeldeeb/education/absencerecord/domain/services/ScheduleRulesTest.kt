package dev.nenoeldeeb.education.absencerecord.domain.services

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ScheduleRulesTest {
    // region overlaps

    @Test
    fun `overlaps returns true for fully overlapping ranges`() {
        assertTrue(ScheduleRules.overlaps(540, 600, 510, 570))
    }

    @Test
    fun `overlaps returns true when a range is contained inside another`() {
        assertTrue(ScheduleRules.overlaps(540, 660, 560, 580))
    }

    @Test
    fun `overlaps returns true when ranges are adjacent inside each other`() {
        assertTrue(ScheduleRules.overlaps(540, 600, 570, 630))
    }

    @Test
    fun `overlaps returns true when busy starts before and ends after lesson`() {
        assertTrue(ScheduleRules.overlaps(540, 600, 530, 610))
    }

    @Test
    fun `overlaps returns false when busy ends exactly at lesson start`() {
        assertFalse(ScheduleRules.overlaps(540, 600, 500, 540))
    }

    @Test
    fun `overlaps returns false when lesson ends exactly at busy start`() {
        assertFalse(ScheduleRules.overlaps(540, 600, 600, 660))
    }

    @Test
    fun `overlaps returns false for disjoint ranges`() {
        assertFalse(ScheduleRules.overlaps(540, 600, 660, 720))
    }

    @Test
    fun `overlaps returns false when ranges touch at a single point`() {
        assertFalse(ScheduleRules.overlaps(0, 60, 60, 120))
    }

    // endregion

    // region isHourStartValid

    @Test
    fun `isHourStartValid accepts an exact minute start at 547`() {
        assertTrue(ScheduleRules.isHourStartValid(547))
    }

    @Test
    fun `isHourStartValid accepts 0 as the earliest valid start`() {
        assertTrue(ScheduleRules.isHourStartValid(0))
    }

    @Test
    fun `isHourStartValid accepts 1380 as the last valid hour start`() {
        assertTrue(ScheduleRules.isHourStartValid(1380))
    }

    @Test
    fun `isHourStartValid rejects starts that would end after midnight`() {
        assertFalse(ScheduleRules.isHourStartValid(1381))
        assertFalse(ScheduleRules.isHourStartValid(1410))
        assertFalse(ScheduleRules.isHourStartValid(1440))
    }

    @Test
    fun `isHourStartValid rejects negative starts`() {
        assertFalse(ScheduleRules.isHourStartValid(-1))
        assertFalse(ScheduleRules.isHourStartValid(-30))
    }

    // endregion

    // region isBusyStartValid

    @Test
    fun `isBusyStartValid accepts any exact minute start within the day`() {
        assertTrue(ScheduleRules.isBusyStartValid(0))
        assertTrue(ScheduleRules.isBusyStartValid(547))
        assertTrue(ScheduleRules.isBusyStartValid(1439))
    }

    @Test
    fun `isBusyStartValid rejects negative starts`() {
        assertFalse(ScheduleRules.isBusyStartValid(-1))
    }

    // endregion

    // region isBusyDurationValid

    @Test
    fun `isBusyDurationValid accepts valid durations within the bounds`() {
        assertTrue(ScheduleRules.isBusyDurationValid(0, 30))
        assertTrue(ScheduleRules.isBusyDurationValid(0, 240))
        assertTrue(ScheduleRules.isBusyDurationValid(0, 360))
    }

    @Test
    fun `isBusyDurationValid rejects durations below the minimum`() {
        assertFalse(ScheduleRules.isBusyDurationValid(0, 15))
        assertFalse(ScheduleRules.isBusyDurationValid(0, 25))
    }

    @Test
    fun `isBusyDurationValid rejects durations above the maximum`() {
        assertFalse(ScheduleRules.isBusyDurationValid(0, 361))
    }

    @Test
    fun `isBusyDurationValid enforces the thirty minute step`() {
        assertTrue(ScheduleRules.isBusyDurationValid(0, 90))
        assertFalse(ScheduleRules.isBusyDurationValid(0, 45))
        assertFalse(ScheduleRules.isBusyDurationValid(0, 75))
    }

    @Test
    fun `isBusyDurationValid accepts an exact minute start`() {
        assertTrue(ScheduleRules.isBusyDurationValid(547, 60))
    }

    @Test
    fun `isBusyDurationValid accepts start plus duration ending exactly at midnight`() {
        assertTrue(ScheduleRules.isBusyDurationValid(1380, 30))
        assertTrue(ScheduleRules.isBusyDurationValid(1380, 60))
        assertTrue(ScheduleRules.isBusyDurationValid(0, 360))
    }

    @Test
    fun `isBusyDurationValid rejects start plus duration crossing midnight`() {
        assertFalse(ScheduleRules.isBusyDurationValid(1380, 90))
        assertFalse(ScheduleRules.isBusyDurationValid(1420, 30))
    }

    @Test
    fun `isBusyDurationValid rejects negative starts`() {
        assertFalse(ScheduleRules.isBusyDurationValid(-1, 30))
    }

    // endregion

    // region parseMaxStudents

    @Test
    fun `parseMaxStudents accepts ascii digits`() {
        assertEquals(5, ScheduleRules.parseMaxStudents("5"))
        assertEquals(12, ScheduleRules.parseMaxStudents("12"))
    }

    @Test
    fun `parseMaxStudents accepts arabic-indic digits`() {
        assertEquals(5, ScheduleRules.parseMaxStudents("٥"))
        assertEquals(12, ScheduleRules.parseMaxStudents("١٢"))
    }

    @Test
    fun `parseMaxStudents accepts eastern arabic-indic digits`() {
        assertEquals(5, ScheduleRules.parseMaxStudents("۵"))
        assertEquals(12, ScheduleRules.parseMaxStudents("۱۲"))
    }

    @Test
    fun `parseMaxStudents rejects blank mixed and overflowing input`() {
        assertNull(ScheduleRules.parseMaxStudents(""))
        assertNull(ScheduleRules.parseMaxStudents("   "))
        assertNull(ScheduleRules.parseMaxStudents("5a"))
        assertNull(ScheduleRules.parseMaxStudents("٥a"))
        assertNull(ScheduleRules.parseMaxStudents("1 2"))
        assertNull(ScheduleRules.parseMaxStudents("9999999999"))
    }

    // endregion

    @Test
    fun `lesson duration is sixty minutes`() {
        assert(ScheduleRules.LESSON_DURATION_MINUTES == 60)
    }

    @Test
    fun `day length is 1440 minutes`() {
        assert(ScheduleRules.DAY_MINUTES == 1440)
    }

    @Test
    fun `busy duration bounds are 30 to 360 minutes`() {
        assert(ScheduleRules.MIN_BUSY_DURATION_MINUTES == 30)
        assert(ScheduleRules.MAX_BUSY_DURATION_MINUTES == 360)
    }
}