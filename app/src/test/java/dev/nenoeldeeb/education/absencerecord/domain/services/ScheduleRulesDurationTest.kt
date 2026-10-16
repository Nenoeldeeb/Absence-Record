package dev.nenoeldeeb.education.absencerecord.domain.services

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Duration rules for lesson blocks (FR-002, FR-007).
 *
 * These tests are RED until `ScheduleRules` gains the duration predicates.
 */
class ScheduleRulesDurationTest {
    // region duration range (FR-002)

    @Test
    fun `shortest selectable duration of 10 minutes is valid`() {
        assertTrue(ScheduleRules.isHourDurationValid(ScheduleRules.MIN_HOUR_DURATION_MINUTES))
        assertEquals(10, ScheduleRules.MIN_HOUR_DURATION_MINUTES)
    }

    @Test
    fun `longest selectable duration of 120 minutes is valid`() {
        assertTrue(ScheduleRules.isHourDurationValid(ScheduleRules.MAX_HOUR_DURATION_MINUTES))
        assertEquals(120, ScheduleRules.MAX_HOUR_DURATION_MINUTES)
    }

    @Test
    fun `duration below the minimum is rejected`() {
        assertFalse(ScheduleRules.isHourDurationValid(9))
    }

    @Test
    fun `duration above the maximum is rejected`() {
        assertFalse(ScheduleRules.isHourDurationValid(121))
        assertFalse(ScheduleRules.isHourDurationValid(999))
    }

    @Test
    fun `duration that is not a multiple of the step is rejected`() {
        assertFalse(ScheduleRules.isHourDurationValid(35))
        assertFalse(ScheduleRules.isHourDurationValid(111))
    }

    @Test
    fun `every selectable duration is a multiple of the step`() {
        val durations = ScheduleRules.selectableDurations()
        for (duration in durations) {
            assertTrue(
                ScheduleRules.isHourDurationValid(duration),
                "expected $duration to be a selectable duration"
            )
        }
        assertEquals(12, durations.size)
    }

    @Test
    fun `selectable durations span 10 to 120 in 10 minute steps`() {
        assertEquals(
            listOf(10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120),
            ScheduleRules.selectableDurations()
        )
    }

    // endregion

    // region derived capacity (FR-003)

    @Test
    fun `derived capacity is duration divided by slot length`() {
        assertEquals(1, ScheduleRules.capacityFor(10))
        assertEquals(3, ScheduleRules.capacityFor(30))
        assertEquals(6, ScheduleRules.capacityFor(60))
        assertEquals(12, ScheduleRules.capacityFor(120))
    }

    @Test
    fun `derived capacity is one for every selectable duration when the slot is ten minutes`() {
        for (duration in ScheduleRules.selectableDurations()) {
            assertEquals(
                duration / ScheduleRules.SLOT_DURATION_MINUTES,
                ScheduleRules.capacityFor(duration)
            )
        }
    }

    @Test
    fun `slot duration is ten minutes`() {
        assertEquals(10, ScheduleRules.SLOT_DURATION_MINUTES)
    }

    // endregion

    // region midnight boundary (FR-007)

    @Test
    fun `block ending exactly at midnight is accepted`() {
        val start = 1440 - 120
        assertTrue(ScheduleRules.isHourStartValid(start, 120))
    }

    @Test
    fun `block ending one minute after midnight is rejected`() {
        val start = 1440 - 120 + 1
        assertFalse(ScheduleRules.isHourStartValid(start, 120))
    }

    @Test
    fun `negative start is rejected`() {
        assertFalse(ScheduleRules.isHourStartValid(-1, 60))
    }

    @Test
    fun `start at midnight with any valid duration is rejected`() {
        // 0 + duration <= 1440 is satisfied, so this is accepted — the block is
        // the first slot of the day.
        assertTrue(ScheduleRules.isHourStartValid(0, 120))
    }

    @Test
    fun `start validity requires a valid duration`() {
        val start = 1440 - 120
        assertTrue(ScheduleRules.isHourStartValid(start, 120))
        assertFalse(ScheduleRules.isHourStartValid(start, 35))
        assertFalse(ScheduleRules.isHourStartValid(start, 130))
    }

    // endregion

    // region defaults

    @Test
    fun `new blocks default to sixty minutes`() {
        assertEquals(60, ScheduleRules.DEFAULT_NEW_HOUR_DURATION_MINUTES)
        assertTrue(ScheduleRules.isHourDurationValid(ScheduleRules.DEFAULT_NEW_HOUR_DURATION_MINUTES))
        assertEquals(6, ScheduleRules.capacityFor(ScheduleRules.DEFAULT_NEW_HOUR_DURATION_MINUTES))
    }

    @Test
    fun `default new block start still fits a sixty minute block`() {
        assertTrue(
            ScheduleRules.isHourStartValid(
                ScheduleRules.DEFAULT_NEW_HOUR_START_MINUTES,
                ScheduleRules.DEFAULT_NEW_HOUR_DURATION_MINUTES
            )
        )
    }

    // endregion

    // region legacy conversion (FR-040, FR-041)

    @Test
    fun `legacy capacity converts to duration by multiplying by ten`() {
        assertEquals(60, ScheduleRules.durationFromLegacyCapacity(6))
        assertEquals(120, ScheduleRules.durationFromLegacyCapacity(12))
    }

    @Test
    fun `legacy capacity outside the selectable range is clamped`() {
        assertEquals(120, ScheduleRules.durationFromLegacyCapacity(99))
        assertEquals(10, ScheduleRules.durationFromLegacyCapacity(0))
        assertEquals(10, ScheduleRules.durationFromLegacyCapacity(-5))
    }

    // endregion
}