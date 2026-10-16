package dev.nenoeldeeb.education.absencerecord.data.datasources.local.daos

import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.AppDatabase
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.TestDatabaseModule
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.entities.StudentEntity
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
class ScheduleDaoBusyTest {
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

    // region busy appointments auto-removal

    @Test
    fun insertBusyAppointment_removesOverlappingAssignments() =
        runTest {
            val hourId = scheduleDao.insertHour(saturday, 540, 50).toInt()
            val studentId = insertStudent()
            scheduleDao.assignStudent(hourId, studentId, saturday)

            // the lone student's slot is 9:00–9:10; land on it
            val report = scheduleDao.insertBusyAppointment(studentId, saturday, 540, 60)

            assertEquals(listOf(studentId), report.removedStudentIds)
            assertEquals(0, scheduleDao.countAssignmentsForHour(hourId))
        }

    @Test
    fun insertBusyAppointment_keepsStudentWhoseSlotIsClear() =
        runTest {
            val hourId = scheduleDao.insertHour(saturday, 540, 50).toInt()
            val studentId = insertStudent()
            scheduleDao.assignStudent(hourId, studentId, saturday)

            // 9:30 is inside the block but past the 9:00–9:10 slot (FR-038)
            val report = scheduleDao.insertBusyAppointment(studentId, saturday, 570, 60)

            assertTrue(report.removedStudentIds.isEmpty())
            assertEquals(1, scheduleDao.countAssignmentsForHour(hourId))
        }

    @Test
    fun insertBusyAppointment_keepsNonOverlappingAssignments() =
        runTest {
            val hourId = scheduleDao.insertHour(saturday, 540, 50).toInt()
            val studentId = insertStudent()
            scheduleDao.assignStudent(hourId, studentId, saturday)

            val report = scheduleDao.insertBusyAppointment(studentId, saturday, 1200, 30)

            assertTrue(report.removedStudentIds.isEmpty())
            assertEquals(1, scheduleDao.countAssignmentsForHour(hourId))
        }

    @Test
    fun updateBusyAppointment_removesOverlappingAssignments() =
        runTest {
            val hourId = scheduleDao.insertHour(saturday, 540, 50).toInt()
            val studentId = insertStudent()
            scheduleDao.assignStudent(hourId, studentId, saturday)
            scheduleDao.insertBusyAppointment(studentId, saturday, 1200, 30)
            val busyId =
                scheduleDao.getBusyAppointmentsForStudentAndWeekday(studentId, saturday).single().id

            val report = scheduleDao.updateBusyAppointment(busyId, 540, 60)

            assertEquals(listOf(studentId), report.removedStudentIds)
            assertEquals(0, scheduleDao.countAssignmentsForHour(hourId))
        }

    // endregion
}