package dev.nenoeldeeb.education.absencerecord.data.datasources.local.daos

import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.AppDatabase
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.TestDatabaseModule
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.entities.StudentEntity
import dev.nenoeldeeb.education.absencerecord.domain.models.HourReplanResult
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

@RunWith(AndroidJUnit4::class)
class ScheduleDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var scheduleDao: ScheduleDao
    private lateinit var studentDao: StudentDao

    private val saturday = DayOfWeek.SATURDAY.isoDayNumber
    private val sunday = DayOfWeek.SUNDAY.isoDayNumber

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

    // region insertHour

    @Test
    fun insertHour_insertsHourOnGivenWeekday() =
        runTest {
            val id = scheduleDao.insertHour(saturday, 540, 50)

            assertTrue(id > 0)
            val hours = scheduleDao.observeHoursForWeekday(saturday).first()
            assertEquals(1, hours.size)
            assertEquals(540, hours.single().startMinutes)
            assertEquals(50, hours.single().durationMinutes)
        }

    @Test
    fun insertHour_rejectsOverlapOnSameWeekday() =
        runTest {
            scheduleDao.insertHour(saturday, 540, 50)

            val error = captureFailure { scheduleDao.insertHour(saturday, 570, 50) }

            assertTrue(error is StudentError.HourOverlap)
        }

    @Test
    fun insertHour_allowsAdjacentHourOnSameWeekday() =
        runTest {
            scheduleDao.insertHour(saturday, 540, 50)

            val id = scheduleDao.insertHour(saturday, 600, 50)

            assertTrue(id > 0)
        }

    @Test
    fun insertHour_allowsSameStartOnDifferentWeekday() =
        runTest {
            scheduleDao.insertHour(saturday, 540, 50)

            val id = scheduleDao.insertHour(sunday, 540, 50)

            assertTrue(id > 0)
        }

    // endregion

    // region updateHour

    @Test
    fun updateHour_asksBeforeDroppingStudents() =
        runTest {
            val hourId = scheduleDao.insertHour(saturday, 540, 20).toInt()
            val studentA = insertStudent("A")
            val studentB = insertStudent("B")
            scheduleDao.assignStudent(hourId, studentA, saturday)
            scheduleDao.assignStudent(hourId, studentB, saturday)

            // unconfirmed: propose, write nothing
            val proposed = scheduleDao.updateHour(hourId, saturday, 540, 10, false)
            assertTrue(proposed is HourReplanResult.NeedsConfirmation)
            assertEquals(1, (proposed as HourReplanResult.NeedsConfirmation).dropped.size)
            assertEquals(2, scheduleDao.countAssignmentsForHour(hourId))

            // confirmed: commit
            val applied = scheduleDao.updateHour(hourId, saturday, 540, 10, true)
            assertTrue(applied is HourReplanResult.Applied)
            assertEquals(1, scheduleDao.countAssignmentsForHour(hourId))
        }

    @Test
    fun updateHour_rejectsOverlapExcludingSelf() =
        runTest {
            val id = scheduleDao.insertHour(saturday, 540, 50).toInt()
            scheduleDao.insertHour(saturday, 660, 50)

            val error = captureFailure { scheduleDao.updateHour(id, saturday, 690, 50, true) }

            assertTrue(error is StudentError.HourOverlap)
        }

    @Test
    fun updateHour_returnsRemovedStudentIdsForBusyConflict() =
        runTest {
            val hourId = scheduleDao.insertHour(saturday, 540, 50).toInt()
            val studentId = insertStudent()
            scheduleDao.assignStudent(hourId, studentId, saturday)
            scheduleDao.insertBusyAppointment(studentId, saturday, 480, 60)

            val outcome = scheduleDao.updateHour(hourId, saturday, 510, 50, true)

            assertEquals(
                listOf(studentId),
                (outcome as HourReplanResult.Applied).removedStudentIds
            )
            assertEquals(0, scheduleDao.countAssignmentsForHour(hourId))
        }

    // endregion

    // region assignStudent

    @Test
    fun assignStudent_rejectsWhenHourIsFull() =
        runTest {
            val hourId = scheduleDao.insertHour(saturday, 540, 10).toInt()
            val studentA = insertStudent("A")
            val studentB = insertStudent("B")
            scheduleDao.assignStudent(hourId, studentA, saturday)

            val error =
                captureFailure { scheduleDao.assignStudent(hourId, studentB, saturday) }

            assertTrue(error is StudentError.HourFull)
        }

    @Test
    fun assignStudent_rejectsSecondLessonForSameStudentOnSameDay() =
        runTest {
            val hour1 = scheduleDao.insertHour(saturday, 540, 50).toInt()
            val hour2 = scheduleDao.insertHour(saturday, 660, 50).toInt()
            val studentId = insertStudent()
            scheduleDao.assignStudent(hour1, studentId, saturday)

            val error =
                captureFailure { scheduleDao.assignStudent(hour2, studentId, saturday) }

            assertTrue(error is StudentError.AssignmentExists)
        }

    @Test
    fun assignStudent_allowsSameStudentOnDifferentWeekday() =
        runTest {
            val hour1 = scheduleDao.insertHour(saturday, 540, 50).toInt()
            val hour2 = scheduleDao.insertHour(sunday, 540, 50).toInt()
            val studentId = insertStudent()

            scheduleDao.assignStudent(hour1, studentId, saturday)
            scheduleDao.assignStudent(hour2, studentId, sunday)

            assertEquals(
                2,
                scheduleDao.countAssignmentsForHour(hour1) + scheduleDao.countAssignmentsForHour(hour2)
            )
        }

    @Test
    fun assignStudent_acceptsWhenBusySitsLaterInsideTheBlock() =
        runTest {
            // FR-025: only the student's own ten minutes matter. A commitment at
            // 570-630 does not touch the 540-550 slot this student occupies, so
            // the whole-block rejection this used to assert is gone.
            val hourId = scheduleDao.insertHour(saturday, 540, 50).toInt()
            val studentId = insertStudent()
            scheduleDao.insertBusyAppointment(studentId, saturday, 570, 60)

            scheduleDao.assignStudent(hourId, studentId, saturday)

            assertEquals(1, scheduleDao.countAssignmentsForHour(hourId))
        }

    @Test
    fun assignStudent_rejectsWhenBusyHitsTheirOwnSlot() =
        runTest {
            val hourId = scheduleDao.insertHour(saturday, 540, 50).toInt()
            val studentId = insertStudent()
            scheduleDao.insertBusyAppointment(studentId, saturday, 545, 30)

            val error =
                captureFailure { scheduleDao.assignStudent(hourId, studentId, saturday) }

            assertTrue(error is StudentError.BusyConflict)
        }

    // endregion

    // region foreign key cascade

    @Test
    fun deletingHour_cascadesToAssignments() =
        runTest {
            val hourId = scheduleDao.insertHour(saturday, 540, 50).toInt()
            val studentId = insertStudent()
            scheduleDao.assignStudent(hourId, studentId, saturday)

            scheduleDao.deleteHour(hourId)

            assertTrue(scheduleDao.getAssignmentsForHour(hourId).isEmpty())
        }

    @Test
    fun deletingStudent_cascadesToAssignmentsAndBusy() =
        runTest {
            val hourId = scheduleDao.insertHour(saturday, 540, 50).toInt()
            val studentId = insertStudent()
            scheduleDao.assignStudent(hourId, studentId, saturday)
            scheduleDao.insertBusyAppointment(studentId, saturday, 1200, 30)

            studentDao.deleteStudents(listOf(StudentEntity(id = studentId, name = "Student")))

            assertEquals(0, scheduleDao.countAssignmentsForHour(hourId))
            assertTrue(scheduleDao.getBusyAppointmentsForStudentAndWeekday(studentId, saturday).isEmpty())
        }

    // endregion

    // region occupancy

    @Test
    fun countAssignmentsForHour_countsOccupancy() =
        runTest {
            val hourId = scheduleDao.insertHour(saturday, 540, 20).toInt()
            val studentA = insertStudent("A")
            val studentB = insertStudent("B")

            scheduleDao.assignStudent(hourId, studentA, saturday)
            assertEquals(1, scheduleDao.countAssignmentsForHour(hourId))

            scheduleDao.assignStudent(hourId, studentB, saturday)
            assertEquals(2, scheduleDao.countAssignmentsForHour(hourId))
        }

    // endregion
}