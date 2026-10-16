package dev.nenoeldeeb.education.absencerecord.data.datasources.local.daos

import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.AppDatabase
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.TestDatabaseModule
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.entities.StudentEntity
import dev.nenoeldeeb.education.absencerecord.domain.models.AssignmentRemovalReport
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
 * Commitment edits re-validate against each student's OWN slot, not the block's
 * whole extent (FR-038).
 *
 * The regression this guards: a commitment created *later inside* a block used
 * to evict a student whose ten minutes it never touched. That is the same
 * over-broad check FR-027 removed from the assignment path, still present on
 * the commitment path.
 */
@RunWith(AndroidJUnit4::class)
class ScheduleDaoCommitmentRevalidationTest {
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

    private suspend fun insertStudent(name: String = "Student"): Int =
        studentDao.insertStudent(StudentEntity(name = name)).toInt()

    /** A 9:00–10:00 block; the lone student holds 9:00–9:10. */
    private suspend fun blockAtNine(duration: Int = 60): Int = scheduleDao.insertHour(saturday, 540, duration).toInt()

    // region FR-038: a commitment that misses the slot must not evict

    @Test
    fun commitmentLaterInsideTheBlockDoesNotEvict() =
        runTest {
            val hourId = blockAtNine()
            val studentId = insertStudent()
            scheduleDao.assignStudent(hourId, studentId, saturday)

            // 9:30–10:00 sits inside the block but after the 9:00–9:10 slot
            val report = scheduleDao.insertBusyAppointment(studentId, saturday, 570, 30)

            assertTrue(
                "a commitment the slot never touches must not evict",
                report.removedStudentIds.isEmpty()
            )
            assertEquals(1, scheduleDao.countAssignmentsForHour(hourId))
        }

    @Test
    fun commitmentAtTheSlotBoundaryDoesNotEvict() =
        runTest {
            val hourId = blockAtNine()
            val studentId = insertStudent()
            scheduleDao.assignStudent(hourId, studentId, saturday)

            // starts exactly when the slot ends: half-open, so no collision
            val report = scheduleDao.insertBusyAppointment(studentId, saturday, 550, 30)

            assertTrue(report.removedStudentIds.isEmpty())
            assertEquals(1, scheduleDao.countAssignmentsForHour(hourId))
        }

    @Test
    fun movingACommitmentOntoTheSlotEvicts() =
        runTest {
            val hourId = blockAtNine()
            val studentId = insertStudent()
            scheduleDao.assignStudent(hourId, studentId, saturday)
            // start harmless, far away
            scheduleDao.insertBusyAppointment(studentId, saturday, 1200, 30)
            val busyId =
                scheduleDao.getBusyAppointmentsForStudentAndWeekday(studentId, saturday).single().id

            // drag it onto the 9:00–9:10 slot
            val report = scheduleDao.updateBusyAppointment(busyId, 540, 30)

            assertEquals(listOf(studentId), report.removedStudentIds)
            assertEquals(0, scheduleDao.countAssignmentsForHour(hourId))
        }

    @Test
    fun movingACommitmentThatStillMissesTheSlotKeepsTheStudent() =
        runTest {
            val hourId = blockAtNine()
            val studentId = insertStudent()
            scheduleDao.assignStudent(hourId, studentId, saturday)
            // 11:00–11:30 is well clear of the 9:00–9:10 slot
            scheduleDao.insertBusyAppointment(studentId, saturday, 1200, 30)
            val busyId =
                scheduleDao.getBusyAppointmentsForStudentAndWeekday(studentId, saturday).single().id

            // move it to 10:30 — still clear of the slot, so nothing is evicted
            val report = scheduleDao.updateBusyAppointment(busyId, 630, 30)

            assertTrue(report.removedStudentIds.isEmpty())
            assertEquals(1, scheduleDao.countAssignmentsForHour(hourId))
        }

    // endregion

    // region the derived slot, not the block start, decides

    @Test
    fun aSecondStudentsCommitmentIsJudgedAgainstTheirOwnSlot() =
        runTest {
            val hourId = blockAtNine()
            val first = insertStudent("Adam")
            val second = insertStudent("Zoe")
            // Adam leaves soonest, so he runs first and holds 9:00–9:10
            scheduleDao.insertBusyAppointment(first, saturday, 660, 30)
            scheduleDao.insertBusyAppointment(second, saturday, 660, 30)
            scheduleDao.assignStudent(hourId, first, saturday)
            scheduleDao.assignStudent(hourId, second, saturday)

            // 9:20–9:50 is past Zoe's 9:10–9:20 slot: inside the block, but not hers
            val report = scheduleDao.insertBusyAppointment(second, saturday, 560, 30)

            assertTrue(report.removedStudentIds.isEmpty())
            assertEquals(2, scheduleDao.countAssignmentsForHour(hourId))
        }

    @Test
    fun aCommitmentCanReorderRatherThanEvict() =
        runTest {
            val hourId = blockAtNine()
            val adam = insertStudent("Adam")
            val zoe = insertStudent("Zoe")
            // equal commitments, so the alphabetical tie-break puts Adam first
            scheduleDao.insertBusyAppointment(adam, saturday, 660, 30)
            scheduleDao.insertBusyAppointment(zoe, saturday, 660, 30)
            scheduleDao.assignStudent(hourId, adam, saturday)
            scheduleDao.assignStudent(hourId, zoe, saturday)

            // 9:10–9:40 would land on Zoe's 9:10–9:20 slot IF she stayed second…
            val report = scheduleDao.insertBusyAppointment(zoe, saturday, 550, 30)

            // …but 9:10 is now her EARLIEST commitment, so the derived order puts
            // her first at 9:00–9:10. Ranges are half-open, so a commitment
            // starting exactly at 9:10 does not touch it. The re-plan resolves
            // the collision by re-sequencing rather than by eviction.
            assertTrue(report.removedStudentIds.isEmpty())
            assertEquals(2, scheduleDao.countAssignmentsForHour(hourId))
        }

    @Test
    fun aCommitmentOnTheBlockStartEvictsTheStudentHoldingThatSlot() =
        runTest {
            val hourId = blockAtNine()
            val adam = insertStudent("Adam")
            val zoe = insertStudent("Zoe")
            scheduleDao.insertBusyAppointment(adam, saturday, 660, 30)
            scheduleDao.insertBusyAppointment(zoe, saturday, 660, 30)
            scheduleDao.assignStudent(hourId, adam, saturday)
            scheduleDao.assignStudent(hourId, zoe, saturday)

            // 9:00 is earlier than both existing commitments, so both re-sort to
            // hold it — and whoever lands on 9:00–9:10 must be evicted.
            val report = scheduleDao.insertBusyAppointment(zoe, saturday, 540, 30)

            assertEquals(listOf(zoe), report.removedStudentIds)
            assertEquals(1, scheduleDao.countAssignmentsForHour(hourId))
        }

    // endregion

    // region unrelated commitments are untouched

    @Test
    fun aCommitmentOnAnotherDayDoesNotEvict() =
        runTest {
            val hourId = blockAtNine()
            val studentId = insertStudent()
            scheduleDao.assignStudent(hourId, studentId, saturday)

            val report = scheduleDao.insertBusyAppointment(studentId, sunday(), 540, 30)

            assertEquals(AssignmentRemovalReport(emptyList()), report)
            assertEquals(1, scheduleDao.countAssignmentsForHour(hourId))
        }

    private fun sunday(): Int = DayOfWeek.SUNDAY.isoDayNumber

    // endregion
}