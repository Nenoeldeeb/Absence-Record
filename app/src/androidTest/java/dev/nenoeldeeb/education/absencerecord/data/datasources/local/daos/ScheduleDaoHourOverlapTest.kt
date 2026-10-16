package dev.nenoeldeeb.education.absencerecord.data.datasources.local.daos

import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.AppDatabase
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.TestDatabaseModule
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.isoDayNumber
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Same-day block overlap, judged on **each block's own** start and length (FR-008).
 *
 * These exist because the query behind `hasOverlappingHour` used the *proposed*
 * block's length where it needed the *existing* row's length. That error is
 * invisible whenever the two lengths are equal — which is the only shape the
 * pre-existing tests used — so every case here deliberately uses **differing**
 * lengths.
 *
 * Ranges are half-open (FR-028), so two blocks that merely abut are allowed.
 */
@RunWith(AndroidJUnit4::class)
class ScheduleDaoHourOverlapTest {
    private lateinit var database: AppDatabase
    private lateinit var scheduleDao: ScheduleDao

    private val saturday = DayOfWeek.SATURDAY.isoDayNumber

    @Before
    fun setUp() {
        database = TestDatabaseModule.createInMemoryDatabase()
        scheduleDao = database.scheduleDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun <T> captureFailure(block: suspend () -> T): Throwable? =
        try {
            block()
            null
        } catch (e: Throwable) {
            e
        }

    @Test
    fun insertHour_rejectsOverlap_whenTheProposedBlockIsShorterAndLater() =
        runTest {
            // Existing 10:00–12:00. Proposed 11:00–11:10 sits squarely inside it.
            scheduleDao.insertHour(saturday, 600, 120)

            val error = captureFailure { scheduleDao.insertHour(saturday, 660, 10) }

            assertTrue("expected HourOverlap, got $error", error is StudentError.HourOverlap)
        }

    @Test
    fun insertHour_rejectsOverlap_whenTheProposedBlockIsLonger() =
        runTest {
            // Existing 11:00–11:10. Proposed 10:00–12:00 swallows it.
            scheduleDao.insertHour(saturday, 660, 10)

            val error = captureFailure { scheduleDao.insertHour(saturday, 600, 120) }

            assertTrue("expected HourOverlap, got $error", error is StudentError.HourOverlap)
        }

    @Test
    fun insertHour_allowsAdjacentBlocksOfDifferingLengths() =
        runTest {
            // Existing 10:00–10:50; proposed 10:50–11:00 merely abuts it.
            scheduleDao.insertHour(saturday, 540, 50)
            scheduleDao.insertHour(saturday, 590, 10)

            assertEquals(2, scheduleDao.observeHoursForWeekday(saturday).first().size)
        }

    @Test
    fun insertHour_allowsAdjacentBlocksWhereTheExistingOneIsTheLonger() =
        runTest {
            // Existing 10:00–11:10; proposed 11:10–11:40 starts exactly where it ends.
            scheduleDao.insertHour(saturday, 600, 70)
            scheduleDao.insertHour(saturday, 670, 30)

            assertEquals(2, scheduleDao.observeHoursForWeekday(saturday).first().size)
        }

    @Test
    fun updateHour_rejectsOverlapWithABlockOfADifferentLength() =
        runTest {
            val parked = scheduleDao.insertHour(saturday, 800, 10).toInt()
            scheduleDao.insertHour(saturday, 600, 120)

            // Moving the 10-minute block to 11:00 puts it inside 10:00–12:00.
            val error = captureFailure { scheduleDao.updateHour(parked, saturday, 660, 10, true) }

            assertTrue("expected HourOverlap, got $error", error is StudentError.HourOverlap)
        }

    @Test
    fun updateHour_rejectsOverlapWhenLengtheningWouldCreateOneAndWritesNothing() =
        runTest {
            val long = scheduleDao.insertHour(saturday, 540, 90).toInt()
            val neighbour = scheduleDao.insertHour(saturday, 700, 30).toInt()

            // 11:40 + 60 = 12:40, which runs into the 11:40–12:10 neighbour.
            val error = captureFailure { scheduleDao.updateHour(long, saturday, 700, 60, true) }

            assertTrue("expected HourOverlap, got $error", error is StudentError.HourOverlap)
            // A refused edit must leave the block exactly as it was (FR-012, FR-036).
            assertEquals(540, scheduleDao.getAvailableHourById(long)?.startMinutes)
            assertEquals(30, scheduleDao.getAvailableHourById(neighbour)?.durationMinutes)
        }
}