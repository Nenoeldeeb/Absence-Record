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
 * The drop list must be closed under deletion (FR-013, SC-015).
 *
 * The running order is *derived*, so removing a student re-plans everyone else's
 * slots. The set of students who cannot survive an edit is therefore not settled
 * after one pass: dropping the first conflicted student can slide a survivor
 * backwards into a slot that lands on their own commitment.
 *
 * **The setup**: a 09:00–10:00 block holding Tara and Sam.
 *  - Tara is committed 09:10–09:40, so she sorts first and holds 09:00–09:10,
 *    which is clear.
 *  - Sam is committed 08:40–09:10. That commitment starts *before* the block, so
 *    it is not a "next relevant commitment" and he sorts last, holding
 *    09:10–09:20 — also clear, because his commitment ends exactly as that slot
 *    begins.
 *
 * **The edit**: the teacher moves the block five minutes later, to 09:05. Tara
 * now sorts first and her 09:05–09:15 slot sits on her 09:10 commitment, so a
 * single pass names Tara alone. Confirming removes her — and Sam slides back into
 * 09:05–09:15, which his 08:40–09:10 commitment covers. He is unassigned having
 * never appeared in the list the teacher agreed to.
 *
 * A single-pass drop list makes the confirm gate promise something it cannot
 * keep. Searched exhaustively across every legal block length, start and
 * commitment window: 5,903 rosters behave this way.
 */
@RunWith(AndroidJUnit4::class)
class ScheduleDaoReplanFixedPointTest {
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

    /** 09:00–10:00 holding Tara (busy 09:10–09:40) and Sam (busy 08:40–09:10). */
    private suspend fun seed(): Triple<Int, Int, Int> {
        val hourId = scheduleDao.insertHour(saturday, 540, 60).toInt()
        val tara = insertStudent("Tara")
        val sam = insertStudent("Sam")
        scheduleDao.insertBusyAppointment(tara, saturday, 550, 30)
        scheduleDao.insertBusyAppointment(sam, saturday, 520, 30)
        scheduleDao.assignStudent(hourId, tara, saturday)
        scheduleDao.assignStudent(hourId, sam, saturday)
        assertEquals(2, scheduleDao.countAssignmentsForHour(hourId))
        return Triple(hourId, tara, sam)
    }

    @Test
    fun theProposalNamesEveryStudentWhoCannotSurviveNotJustTheFirstConflict() =
        runTest {
            val (hourId, tara, sam) = seed()

            val proposal = scheduleDao.updateHour(hourId, saturday, 545, 60, false)

            assertTrue("expected a proposal, got $proposal", proposal is HourReplanResult.NeedsConfirmation)
            val dropped = (proposal as HourReplanResult.NeedsConfirmation).dropped
            assertEquals(
                "Sam is clear where he stands but slides into Tara's slot once she goes, " +
                    "so the teacher must see his name too",
                setOf(tara, sam),
                dropped.map { it.studentId }.toSet()
            )
            dropped.forEach {
                assertEquals(DropReason.SlotConflictsWithBusy, it.reason)
            }
        }

    @Test
    fun confirmingRemovesExactlyTheStudentsThatWereShown() =
        runTest {
            val (hourId, _, _) = seed()

            val proposal = scheduleDao.updateHour(hourId, saturday, 545, 60, false)
            val shown = (proposal as HourReplanResult.NeedsConfirmation).dropped.map { it.studentId }.toSet()

            // The teacher's decision was "yes, remove these" — re-run with that consent.
            val applied = scheduleDao.updateHour(hourId, saturday, 545, 60, true)

            assertTrue(applied is HourReplanResult.Applied)
            assertEquals(
                "nobody may be unassigned who was not in the confirmed list",
                shown,
                (applied as HourReplanResult.Applied).removedStudentIds.toSet()
            )
            assertEquals(0, scheduleDao.countAssignmentsForHour(hourId))
        }

    @Test
    fun decliningRemovesNobody() =
        runTest {
            val (hourId, _, _) = seed()

            scheduleDao.updateHour(hourId, saturday, 545, 60, false)

            assertEquals(2, scheduleDao.countAssignmentsForHour(hourId))
            assertEquals(540, scheduleDao.getAvailableHourById(hourId)?.startMinutes)
        }
}