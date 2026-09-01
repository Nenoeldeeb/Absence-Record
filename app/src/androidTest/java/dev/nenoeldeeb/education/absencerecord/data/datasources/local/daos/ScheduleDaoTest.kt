package dev.nenoeldeeb.education.absencerecord.data.datasources.local.daos

import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.AppDatabase
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.TestDatabaseModule
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.entities.StudentEntity
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
            val id = scheduleDao.insertHour(saturday, 540, 5)

            assertTrue(id > 0)
            val hours = scheduleDao.observeHoursForWeekday(saturday).first()
            assertEquals(1, hours.size)
            assertEquals(540, hours.single().startMinutes)
            assertEquals(5, hours.single().maxStudents)
        }

    @Test
    fun insertHour_rejectsOverlapOnSameWeekday() =
        runTest {
            scheduleDao.insertHour(saturday, 540, 5)

            val error = captureFailure { scheduleDao.insertHour(saturday, 570, 5) }

            assertTrue(error is StudentError.HourOverlap)
        }

    @Test
    fun insertHour_allowsAdjacentHourOnSameWeekday() =
        runTest {
            scheduleDao.insertHour(saturday, 540, 5)

            val id = scheduleDao.insertHour(saturday, 600, 5)

            assertTrue(id > 0)
        }

    @Test
    fun insertHour_allowsSameStartOnDifferentWeekday() =
        runTest {
            scheduleDao.insertHour(saturday, 540, 5)

            val id = scheduleDao.insertHour(sunday, 540, 5)

            assertTrue(id > 0)
        }

    // endregion

    // region updateHour

    @Test
    fun updateHour_rejectsCapacityDropBelowAssignedCount() =
        runTest {
            val hourId = scheduleDao.insertHour(saturday, 540, 2).toInt()
            val studentA = insertStudent("A")
            val studentB = insertStudent("B")
            scheduleDao.assignStudent(hourId, studentA, saturday)
            scheduleDao.assignStudent(hourId, studentB, saturday)

            val error =
                captureFailure { scheduleDao.updateHour(hourId, saturday, 540, 1) }

            assertTrue(error is StudentError.MaxBelowAssigned)
        }

    @Test
    fun updateHour_rejectsOverlapExcludingSelf() =
        runTest {
            val id = scheduleDao.insertHour(saturday, 540, 5).toInt()
            scheduleDao.insertHour(saturday, 660, 5)

            val error = captureFailure { scheduleDao.updateHour(id, saturday, 690, 5) }

            assertTrue(error is StudentError.HourOverlap)
        }

    @Test
    fun updateHour_returnsRemovedStudentIdsForBusyConflict() =
        runTest {
            val hourId = scheduleDao.insertHour(saturday, 540, 5).toInt()
            val studentId = insertStudent()
            scheduleDao.assignStudent(hourId, studentId, saturday)
            scheduleDao.insertBusyAppointment(studentId, saturday, 480, 60)

            val report = scheduleDao.updateHour(hourId, saturday, 510, 5)

            assertEquals(listOf(studentId), report.removedStudentIds)
            assertEquals(0, scheduleDao.countAssignmentsForHour(hourId))
        }

    // endregion

    // region assignStudent

    @Test
    fun assignStudent_rejectsWhenHourIsFull() =
        runTest {
            val hourId = scheduleDao.insertHour(saturday, 540, 1).toInt()
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
            val hour1 = scheduleDao.insertHour(saturday, 540, 5).toInt()
            val hour2 = scheduleDao.insertHour(saturday, 660, 5).toInt()
            val studentId = insertStudent()
            scheduleDao.assignStudent(hour1, studentId, saturday)

            val error =
                captureFailure { scheduleDao.assignStudent(hour2, studentId, saturday) }

            assertTrue(error is StudentError.AssignmentExists)
        }

    @Test
    fun assignStudent_allowsSameStudentOnDifferentWeekday() =
        runTest {
            val hour1 = scheduleDao.insertHour(saturday, 540, 5).toInt()
            val hour2 = scheduleDao.insertHour(sunday, 540, 5).toInt()
            val studentId = insertStudent()

            scheduleDao.assignStudent(hour1, studentId, saturday)
            scheduleDao.assignStudent(hour2, studentId, sunday)

            assertEquals(
                2,
                scheduleDao.countAssignmentsForHour(hour1) + scheduleDao.countAssignmentsForHour(hour2)
            )
        }

    @Test
    fun assignStudent_rejectsWhenHourOverlapsBusyAppointment() =
        runTest {
            val hourId = scheduleDao.insertHour(saturday, 540, 5).toInt()
            val studentId = insertStudent()
            scheduleDao.insertBusyAppointment(studentId, saturday, 570, 60)

            val error =
                captureFailure { scheduleDao.assignStudent(hourId, studentId, saturday) }

            assertTrue(error is StudentError.BusyConflict)
        }

    // endregion

    // region busy appointments auto-removal

    @Test
    fun insertBusyAppointment_removesOverlappingAssignments() =
        runTest {
            val hourId = scheduleDao.insertHour(saturday, 540, 5).toInt()
            val studentId = insertStudent()
            scheduleDao.assignStudent(hourId, studentId, saturday)

            val report = scheduleDao.insertBusyAppointment(studentId, saturday, 570, 60)

            assertEquals(listOf(studentId), report.removedStudentIds)
            assertEquals(0, scheduleDao.countAssignmentsForHour(hourId))
        }

    @Test
    fun insertBusyAppointment_keepsNonOverlappingAssignments() =
        runTest {
            val hourId = scheduleDao.insertHour(saturday, 540, 5).toInt()
            val studentId = insertStudent()
            scheduleDao.assignStudent(hourId, studentId, saturday)

            val report = scheduleDao.insertBusyAppointment(studentId, saturday, 1200, 30)

            assertTrue(report.removedStudentIds.isEmpty())
            assertEquals(1, scheduleDao.countAssignmentsForHour(hourId))
        }

    @Test
    fun updateBusyAppointment_removesOverlappingAssignments() =
        runTest {
            val hourId = scheduleDao.insertHour(saturday, 540, 5).toInt()
            val studentId = insertStudent()
            scheduleDao.assignStudent(hourId, studentId, saturday)
            scheduleDao.insertBusyAppointment(studentId, saturday, 1200, 30)
            val busyId =
                scheduleDao.getBusyAppointmentsForStudentAndWeekday(studentId, saturday).single().id

            val report = scheduleDao.updateBusyAppointment(busyId, 570, 60)

            assertEquals(listOf(studentId), report.removedStudentIds)
            assertEquals(0, scheduleDao.countAssignmentsForHour(hourId))
        }

    // endregion

    // region foreign key cascade

    @Test
    fun deletingHour_cascadesToAssignments() =
        runTest {
            val hourId = scheduleDao.insertHour(saturday, 540, 5).toInt()
            val studentId = insertStudent()
            scheduleDao.assignStudent(hourId, studentId, saturday)

            scheduleDao.deleteHour(hourId)

            assertTrue(scheduleDao.getAssignmentsForHour(hourId).isEmpty())
        }

    @Test
    fun deletingStudent_cascadesToAssignmentsAndBusy() =
        runTest {
            val hourId = scheduleDao.insertHour(saturday, 540, 5).toInt()
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
            val hourId = scheduleDao.insertHour(saturday, 540, 2).toInt()
            val studentA = insertStudent("A")
            val studentB = insertStudent("B")

            scheduleDao.assignStudent(hourId, studentA, saturday)
            assertEquals(1, scheduleDao.countAssignmentsForHour(hourId))

            scheduleDao.assignStudent(hourId, studentB, saturday)
            assertEquals(2, scheduleDao.countAssignmentsForHour(hourId))
        }

    // endregion
}