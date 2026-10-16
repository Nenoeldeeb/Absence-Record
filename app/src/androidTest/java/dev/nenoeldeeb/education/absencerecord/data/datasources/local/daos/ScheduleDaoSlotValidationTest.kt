package dev.nenoeldeeb.education.absencerecord.data.datasources.local.daos

import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.AppDatabase
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.TestDatabaseModule
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.entities.StudentEntity
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.isoDayNumber
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Validation against each student's OWN ten minutes, not the whole block
 * (FR-026, FR-027, FR-028).
 *
 * Every case runs against a real in-memory database so the half-open range
 * arithmetic is exercised through the DAO rather than around it.
 */
@RunWith(AndroidJUnit4::class)
class ScheduleDaoSlotValidationTest {
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

    private suspend fun <T> captureFailure(block: suspend () -> T): Throwable? =
        try {
            block()
            null
        } catch (e: Throwable) {
            e
        }

    /** A 9:00–10:00 block; the lone student holds the first ten minutes. */
    private suspend fun blockStartingAtNine(
        start: Int = 540,
        duration: Int = 60
    ): Int = scheduleDao.insertHour(saturday, start, duration).toInt()

    // region FR-028: half-open boundaries

    @Test
    fun commitmentStartingExactlyAtSlotStartIsRejected() =
        runTest {
            val hourId = blockStartingAtNine()
            val studentId = insertStudent()
            scheduleDao.insertBusyAppointment(studentId, saturday, 540, 30)

            val error = captureFailure { scheduleDao.assignStudent(hourId, studentId, saturday) }

            assertTrue("slot 9:00–9:10 collides with a 9:00 commitment", error is StudentError.BusyConflict)
        }

    @Test
    fun commitmentStartingExactlyAtSlotEndIsAccepted() =
        runTest {
            val hourId = blockStartingAtNine()
            val studentId = insertStudent()
            // their slot is 9:00–9:10; a 9:10 commitment starts exactly when it ends
            scheduleDao.insertBusyAppointment(studentId, saturday, 550, 30)

            val error = captureFailure { scheduleDao.assignStudent(hourId, studentId, saturday) }

            assertNull("a half-open range must not collide at its end", error)
            assertEquals(1, scheduleDao.countAssignmentsForHour(hourId))
        }

    @Test
    fun commitmentEndingExactlyAtSlotStartIsAccepted() =
        runTest {
            val hourId = blockStartingAtNine()
            val studentId = insertStudent()
            // 8:30–9:00: ends exactly when the 9:00 slot starts
            scheduleDao.insertBusyAppointment(studentId, saturday, 510, 30)

            val error = captureFailure { scheduleDao.assignStudent(hourId, studentId, saturday) }

            assertNull(error)
            assertEquals(1, scheduleDao.countAssignmentsForHour(hourId))
        }

    // endregion

    // region FR-026: commitments reaching into the slot

    @Test
    fun commitmentStartingBeforeTheBlockButStillRunningIsRejected() =
        runTest {
            val hourId = blockStartingAtNine()
            val studentId = insertStudent()
            // 8:40–9:10 — began before the block and still running at 9:00
            scheduleDao.insertBusyAppointment(studentId, saturday, 520, 30)

            val error = captureFailure { scheduleDao.assignStudent(hourId, studentId, saturday) }

            assertTrue(error is StudentError.BusyConflict)
        }

    @Test
    fun commitmentSpanningTheWholeBlockIsRejected() =
        runTest {
            val hourId = blockStartingAtNine()
            val studentId = insertStudent()
            scheduleDao.insertBusyAppointment(studentId, saturday, 480, 180) // 8:00–11:00

            val error = captureFailure { scheduleDao.assignStudent(hourId, studentId, saturday) }

            assertTrue(error is StudentError.BusyConflict)
        }

    // endregion

    // region FR-027: later inside the block is not the student's problem

    @Test
    fun commitmentLaterInsideTheBlockDoesNotReject() =
        runTest {
            val hourId = blockStartingAtNine()
            val studentId = insertStudent()
            // slot is 9:00–9:10; the 9:30 commitment sits later in the block
            scheduleDao.insertBusyAppointment(studentId, saturday, 570, 30)

            val error = captureFailure { scheduleDao.assignStudent(hourId, studentId, saturday) }

            assertNull("only the student's own ten minutes may reject them", error)
            assertEquals(1, scheduleDao.countAssignmentsForHour(hourId))
        }

    @Test
    fun commitmentAfterTheBlockEntirelyDoesNotReject() =
        runTest {
            val hourId = blockStartingAtNine()
            val studentId = insertStudent()
            scheduleDao.insertBusyAppointment(studentId, saturday, 660, 30) // 11:00

            val error = captureFailure { scheduleDao.assignStudent(hourId, studentId, saturday) }

            assertNull(error)
            assertEquals(1, scheduleDao.countAssignmentsForHour(hourId))
        }

    // endregion

    // region the slot follows the derived position, not the block's start

    @Test
    fun aSecondStudentsConflictIsJudgedAgainstTheirOwnSlot() =
        runTest {
            val hourId = blockStartingAtNine()
            // Y's earliest commitment inside the block is 10:00, so they run first
            // and hold 9:00–9:10. Their 8:00 commitment is already over.
            val y = insertStudent("Adam")
            val x = insertStudent("Zoe")
            scheduleDao.insertBusyAppointment(y, saturday, 480, 30)
            scheduleDao.insertBusyAppointment(y, saturday, 600, 30)
            // X's earliest commitment is 9:05, so they sort after Y and would hold
            // 9:10–9:20 — which their own 9:05–9:35 commitment lands on.
            scheduleDao.insertBusyAppointment(x, saturday, 545, 30)
            scheduleDao.insertBusyAppointment(x, saturday, 570, 30)
            scheduleDao.assignStudent(hourId, y, saturday)

            val refused = captureFailure { scheduleDao.assignStudent(hourId, x, saturday) }

            assertTrue("their own slot collides, not the block as a whole", refused is StudentError.BusyConflict)
            assertEquals(1, scheduleDao.countAssignmentsForHour(hourId))
        }

    @Test
    fun aSecondStudentWithNoCollisionIsAccepted() =
        runTest {
            val hourId = blockStartingAtNine()
            val y = insertStudent("Adam")
            val x = insertStudent("Zoe")
            scheduleDao.insertBusyAppointment(y, saturday, 480, 30)
            scheduleDao.insertBusyAppointment(y, saturday, 600, 30)
            // X sorts second and holds 9:10–9:20; a 10:00 commitment sits later
            // inside the block but outside their own ten minutes (FR-027).
            scheduleDao.insertBusyAppointment(x, saturday, 600, 30)
            scheduleDao.assignStudent(hourId, y, saturday)

            val accepted = captureFailure { scheduleDao.assignStudent(hourId, x, saturday) }

            assertNull(accepted)
            assertEquals(2, scheduleDao.countAssignmentsForHour(hourId))
        }

    @Test
    fun aRefusedAdditionLeavesTheBlockUntouched() =
        runTest {
            val hourId = blockStartingAtNine()
            val keeper = insertStudent("Adam")
            val blocked = insertStudent("Zoe")
            scheduleDao.assignStudent(hourId, keeper, saturday)
            scheduleDao.insertBusyAppointment(blocked, saturday, 540, 30)

            val error = captureFailure { scheduleDao.assignStudent(hourId, blocked, saturday) }

            assertTrue(error is StudentError.BusyConflict)
            assertEquals("no partial state may survive a refusal", 1, scheduleDao.countAssignmentsForHour(hourId))
        }

    // endregion
}