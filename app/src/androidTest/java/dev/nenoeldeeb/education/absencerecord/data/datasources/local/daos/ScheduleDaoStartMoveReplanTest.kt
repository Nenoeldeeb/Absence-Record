package dev.nenoeldeeb.education.absencerecord.data.datasources.local.daos

import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.AppDatabase
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.TestDatabaseModule
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.entities.StudentEntity
import dev.nenoeldeeb.education.absencerecord.domain.models.DropReason
import dev.nenoeldeeb.education.absencerecord.domain.models.HourReplanResult
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
 * Moving a block's start time goes through the same confirm-first re-plan as
 * shortening it (FR-009, FR-011, FR-015, SC-015).
 *
 * A start move changes no capacity, so the over-capacity drop list is always
 * empty for one. Before this was fixed, `updateHour` branched on that list
 * alone, concluded there was nothing to ask about, wrote the new start
 * immediately, and then evicted whoever collided — with no prompt and no way to
 * decline.
 *
 * Setup: a 10:00–10:50 block holding Alice, Bob and Carol, where Bob is committed
 * from 11:05. Moving the block to 11:00 puts Bob first in the running order and
 * lands his 11:00–11:10 slot on his own commitment. Nobody else conflicts.
 */
@RunWith(AndroidJUnit4::class)
class ScheduleDaoStartMoveReplanTest {
    private lateinit var database: AppDatabase
    private lateinit var scheduleDao: ScheduleDao
    private lateinit var studentDao: StudentDao

    private val saturday = DayOfWeek.SATURDAY.isoDayNumber

    @Before
    fun setUp() {
        database = TestDatabaseModule.createInMemoryDatabase()
        scheduleDao = database.scheduleDao()
        studentDao = database.studentDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun insertStudent(name: String): Int = studentDao.insertStudent(StudentEntity(name = name)).toInt()

    /** Ids of the seeded block: 10:00–10:50, holding Alice, Bob and Carol. */
    private data class Seed(val hourId: Int, val alice: Int, val bob: Int, val carol: Int)

    private suspend fun seedBlockAtTen(): Seed {
        val hourId = scheduleDao.insertHour(saturday, 600, 50).toInt()
        val alice = insertStudent("Alice")
        val bob = insertStudent("Bob")
        val carol = insertStudent("Carol")
        scheduleDao.insertBusyAppointment(bob, saturday, 665, 30)
        scheduleDao.assignStudent(hourId, alice, saturday)
        scheduleDao.assignStudent(hourId, bob, saturday)
        scheduleDao.assignStudent(hourId, carol, saturday)
        assertEquals(3, scheduleDao.countAssignmentsForHour(hourId))
        return Seed(hourId, alice, bob, carol)
    }

    @Test
    fun movingTheStart_asksBeforeEvictingAStudentWhoseOwnSlotWouldConflict() =
        runTest {
            val seed = seedBlockAtTen()

            val proposal = scheduleDao.updateHour(seed.hourId, saturday, 660, 50, false)

            assertTrue("expected a proposal, got $proposal", proposal is HourReplanResult.NeedsConfirmation)
            val dropped = (proposal as HourReplanResult.NeedsConfirmation).dropped
            assertEquals("exactly Bob should be named", listOf(seed.bob), dropped.map { it.studentId })
            assertEquals(DropReason.SlotConflictsWithBusy, dropped.single().reason)
            assertEquals("Bob", dropped.single().studentName)
        }

    @Test
    fun movingTheStart_writesNothingUntilTheTeacherConfirms() =
        runTest {
            val seed = seedBlockAtTen()

            scheduleDao.updateHour(seed.hourId, saturday, 660, 50, false)

            // Declining is just walking away: the block must be untouched.
            val hour = scheduleDao.getAvailableHourById(seed.hourId)
            assertEquals(600, hour?.startMinutes)
            assertEquals(50, hour?.durationMinutes)
            assertEquals(3, scheduleDao.countAssignmentsForHour(seed.hourId))
        }

    @Test
    fun movingTheStart_commitsAndUnassignsOnlyTheConflictingStudent() =
        runTest {
            val seed = seedBlockAtTen()

            val applied = scheduleDao.updateHour(seed.hourId, saturday, 660, 50, true)

            assertTrue(applied is HourReplanResult.Applied)
            assertEquals(listOf(seed.bob), (applied as HourReplanResult.Applied).removedStudentIds)
            assertEquals(660, scheduleDao.getAvailableHourById(seed.hourId)?.startMinutes)
            assertEquals(2, scheduleDao.countAssignmentsForHour(seed.hourId))
        }

    @Test
    fun shorteningTheBlock_stillProposesByCapacityNotByConflict() =
        runTest {
            val seed = seedBlockAtTen()

            // 10:00 -> 20 minutes holds two, so one of the three is over capacity.
            val proposal = scheduleDao.updateHour(seed.hourId, saturday, 600, 20, false)

            val dropped = (proposal as HourReplanResult.NeedsConfirmation).dropped
            // Ordering is Bob (busy 11:05), then Alice then Carol alphabetically
            // among the commitment-less. Keep-first-N keeps Bob and Alice (FR-010),
            // so Carol is the one proposed for removal — and for capacity, not for
            // any conflict.
            assertEquals(listOf(seed.carol), dropped.map { it.studentId })
            assertEquals(DropReason.ExceedsCapacity, dropped.single().reason)
        }

    @Test
    fun aStartMoveWithNoConflictsIsAppliedWithoutAsking() =
        runTest {
            val hourId = scheduleDao.insertHour(saturday, 600, 50).toInt()
            val alice = insertStudent("Alice")
            scheduleDao.assignStudent(hourId, alice, saturday)

            val applied = scheduleDao.updateHour(hourId, saturday, 700, 50, true)

            assertTrue(applied is HourReplanResult.Applied)
            assertTrue((applied as HourReplanResult.Applied).removedStudentIds.isEmpty())
            assertEquals(1, scheduleDao.countAssignmentsForHour(hourId))
        }
}