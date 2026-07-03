package dev.nenoeldeeb.education.absencerecord.data.datasources.local.daos

import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.AppDatabase
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.TestDatabaseModule
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.entities.StudentAttendanceEntity
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.entities.StudentEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AttendanceDaoQueryTest {
    private lateinit var database: AppDatabase
    private lateinit var studentDao: StudentDao
    private lateinit var attendanceDao: AttendanceDao

    @Before
    fun setUp() {
        database = TestDatabaseModule.createInMemoryDatabase()
        studentDao = database.studentDao()
        attendanceDao = database.attendanceDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // region getStudentAttendanceDates tests

    @Test
    fun getStudentAttendanceDates_returnsDatesSortedDescending() =
        runTest {
            val studentId = studentDao.insertStudent(StudentEntity(name = "Student")).toInt()

            attendanceDao.insertAttendance(
                StudentAttendanceEntity(studentId = studentId, date = LocalDate(2023, 10, 15))
            )
            attendanceDao.insertAttendance(
                StudentAttendanceEntity(studentId = studentId, date = LocalDate(2023, 10, 20))
            )
            attendanceDao.insertAttendance(
                StudentAttendanceEntity(studentId = studentId, date = LocalDate(2023, 10, 10))
            )

            val dates = attendanceDao.getStudentAttendanceDates(studentId).first()

            assertEquals(3, dates.size)
            assertEquals(LocalDate(2023, 10, 20), dates[0])
            assertEquals(LocalDate(2023, 10, 15), dates[1])
            assertEquals(LocalDate(2023, 10, 10), dates[2])
        }

    @Test
    fun getStudentAttendanceDates_returnsOnlyDatesForSpecificStudent() =
        runTest {
            val studentId1 = studentDao.insertStudent(StudentEntity(name = "Student 1")).toInt()
            val studentId2 = studentDao.insertStudent(StudentEntity(name = "Student 2")).toInt()

            attendanceDao.insertAttendance(
                StudentAttendanceEntity(studentId = studentId1, date = LocalDate(2023, 10, 15))
            )
            attendanceDao.insertAttendance(
                StudentAttendanceEntity(studentId = studentId2, date = LocalDate(2023, 10, 16))
            )

            val dates = attendanceDao.getStudentAttendanceDates(studentId1).first()

            assertEquals(1, dates.size)
            assertEquals(LocalDate(2023, 10, 15), dates[0])
        }

    // endregion

    // region getAttendanceHistoryForDateRange tests

    @Test
    fun getAttendanceHistoryForDateRange_returnsRecordsWithinRange() =
        runTest {
            val studentId = studentDao.insertStudent(StudentEntity(name = "Test Student")).toInt()

            attendanceDao.insertAttendance(
                StudentAttendanceEntity(studentId = studentId, date = LocalDate(2023, 10, 1))
            )
            attendanceDao.insertAttendance(
                StudentAttendanceEntity(studentId = studentId, date = LocalDate(2023, 10, 15))
            )
            attendanceDao.insertAttendance(
                StudentAttendanceEntity(studentId = studentId, date = LocalDate(2023, 10, 20))
            )
            attendanceDao.insertAttendance(
                StudentAttendanceEntity(studentId = studentId, date = LocalDate(2023, 10, 31))
            )

            val history =
                attendanceDao
                    .getAttendanceHistoryForDateRange(
                        studentId = studentId,
                        startDate = LocalDate(2023, 10, 10),
                        endDate = LocalDate(2023, 10, 25)
                    )
                    .first()

            assertEquals(2, history.size)
            assertEquals(LocalDate(2023, 10, 20), history[0].date)
            assertEquals(LocalDate(2023, 10, 15), history[1].date)
        }

    @Test
    fun getAttendanceHistoryForDateRange_includesStudentName() =
        runTest {
            val studentId = studentDao.insertStudent(StudentEntity(name = "Alice")).toInt()

            attendanceDao.insertAttendance(
                StudentAttendanceEntity(studentId = studentId, date = LocalDate(2023, 10, 15))
            )

            val history =
                attendanceDao
                    .getAttendanceHistoryForDateRange(
                        studentId = studentId,
                        startDate = LocalDate(2023, 10, 1),
                        endDate = LocalDate(2023, 10, 31)
                    )
                    .first()

            assertEquals(1, history.size)
            assertEquals("Alice", history[0].studentName)
        }

    @Test
    fun getAttendanceHistoryForDateRange_inclusiveOfBoundaryDates() =
        runTest {
            val studentId = studentDao.insertStudent(StudentEntity(name = "Student")).toInt()

            attendanceDao.insertAttendance(
                StudentAttendanceEntity(studentId = studentId, date = LocalDate(2023, 10, 10))
            )
            attendanceDao.insertAttendance(
                StudentAttendanceEntity(studentId = studentId, date = LocalDate(2023, 10, 20))
            )

            val history =
                attendanceDao
                    .getAttendanceHistoryForDateRange(
                        studentId = studentId,
                        startDate = LocalDate(2023, 10, 10),
                        endDate = LocalDate(2023, 10, 20)
                    )
                    .first()

            assertEquals(2, history.size)
        }

    // endregion

    // region getDistinctDatesWithAttendance tests

    @Test
    fun getDistinctDatesWithAttendance_returnsUniqueDatesSortedDescending() =
        runTest {
            val studentId1 = studentDao.insertStudent(StudentEntity(name = "Student 1")).toInt()
            val studentId2 = studentDao.insertStudent(StudentEntity(name = "Student 2")).toInt()

            attendanceDao.insertAttendance(
                StudentAttendanceEntity(studentId = studentId1, date = LocalDate(2023, 10, 15))
            )
            attendanceDao.insertAttendance(
                StudentAttendanceEntity(studentId = studentId2, date = LocalDate(2023, 10, 15))
            )
            attendanceDao.insertAttendance(
                StudentAttendanceEntity(studentId = studentId1, date = LocalDate(2023, 10, 10))
            )

            val dates = attendanceDao.getDistinctDatesWithAttendance().first()

            assertEquals(2, dates.size)
            assertEquals(LocalDate(2023, 10, 15), dates[0])
            assertEquals(LocalDate(2023, 10, 10), dates[1])
        }

    @Test
    fun getDistinctDatesWithAttendance_emptyWhenNoAttendance() =
        runTest {
            studentDao.insertStudent(StudentEntity(name = "Student"))

            val dates = attendanceDao.getDistinctDatesWithAttendance().first()

            assertTrue(dates.isEmpty())
        }

    // endregion
}