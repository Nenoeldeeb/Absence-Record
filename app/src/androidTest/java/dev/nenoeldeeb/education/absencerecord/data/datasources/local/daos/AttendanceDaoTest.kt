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

/**
 * Instrumentation tests for [AttendanceDao].
 *
 * These tests verify that Room-generated SQL queries for attendance operations execute correctly
 * against a real SQLite database.
 */
@RunWith(AndroidJUnit4::class)
class AttendanceDaoTest {
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

    // region getAttendanceForDate tests

    @Test
    fun getAttendanceForDate_emptyDatabase_returnsEmptyList() =
        runTest {
            val attendance = attendanceDao.getAttendanceForDate(LocalDate(2023, 10, 15)).first()
            assertTrue(attendance.isEmpty())
        }

    @Test
    fun getAttendanceForDate_returnsOnlyAttendanceForSpecificDate() =
        runTest {
            // Arrange
            val studentId = studentDao.insertStudent(StudentEntity(name = "Student")).toInt()

            attendanceDao.insertAttendance(
                StudentAttendanceEntity(studentId = studentId, date = LocalDate(2023, 10, 15))
            )
            attendanceDao.insertAttendance(
                StudentAttendanceEntity(studentId = studentId, date = LocalDate(2023, 10, 16))
            )

            // Act
            val attendance = attendanceDao.getAttendanceForDate(LocalDate(2023, 10, 15)).first()

            // Assert
            assertEquals(1, attendance.size)
            assertEquals(LocalDate(2023, 10, 15), attendance[0].date)
        }

    @Test
    fun getAttendanceForDate_returnsMultipleStudentsForSameDate() =
        runTest {
            val studentId1 = studentDao.insertStudent(StudentEntity(name = "Student 1")).toInt()
            val studentId2 = studentDao.insertStudent(StudentEntity(name = "Student 2")).toInt()

            val date = LocalDate(2023, 10, 15)
            attendanceDao.insertAttendance(StudentAttendanceEntity(studentId = studentId1, date = date))
            attendanceDao.insertAttendance(StudentAttendanceEntity(studentId = studentId2, date = date))

            val attendance = attendanceDao.getAttendanceForDate(date).first()

            assertEquals(2, attendance.size)
        }

    // endregion

    // region insertAttendance tests

    @Test
    fun insertAttendance_insertsSuccessfully() =
        runTest {
            val studentId = studentDao.insertStudent(StudentEntity(name = "Student")).toInt()

            val insertedId =
                attendanceDao.insertAttendance(
                    StudentAttendanceEntity(
                        studentId = studentId,
                        date = LocalDate(2023, 10, 15)
                    )
                )

            assertTrue(insertedId > 0)
            val attendance = attendanceDao.getAttendanceForDate(LocalDate(2023, 10, 15)).first()
            assertEquals(1, attendance.size)
        }

    @Test
    fun insertAttendance_duplicateStudentAndDate_returnsNegativeOne() =
        runTest {
            // Arrange - unique constraint on (studentId, date)
            val studentId = studentDao.insertStudent(StudentEntity(name = "Student")).toInt()
            val date = LocalDate(2023, 10, 15)

            attendanceDao.insertAttendance(StudentAttendanceEntity(studentId = studentId, date = date))

            // Act - try to insert same combination
            val duplicateId =
                attendanceDao.insertAttendance(
                    StudentAttendanceEntity(studentId = studentId, date = date)
                )

            // Assert - OnConflictStrategy.IGNORE returns -1
            assertEquals(-1L, duplicateId)
        }

    @Test
    fun insertAttendance_sameStudentDifferentDates_insertsAll() =
        runTest {
            val studentId = studentDao.insertStudent(StudentEntity(name = "Student")).toInt()

            val id1 =
                attendanceDao.insertAttendance(
                    StudentAttendanceEntity(
                        studentId = studentId,
                        date = LocalDate(2023, 10, 15)
                    )
                )
            val id2 =
                attendanceDao.insertAttendance(
                    StudentAttendanceEntity(
                        studentId = studentId,
                        date = LocalDate(2023, 10, 16)
                    )
                )

            assertTrue(id1 > 0)
            assertTrue(id2 > 0)
        }

    // endregion

    // region deleteAttendance tests

    @Test
    fun deleteAttendance_deletesSpecificRecord() =
        runTest {
            val studentId = studentDao.insertStudent(StudentEntity(name = "Student")).toInt()
            val date1 = LocalDate(2023, 10, 15)
            val date2 = LocalDate(2023, 10, 16)

            attendanceDao.insertAttendance(StudentAttendanceEntity(studentId = studentId, date = date1))
            attendanceDao.insertAttendance(StudentAttendanceEntity(studentId = studentId, date = date2))

            // Delete only the first one
            attendanceDao.deleteAttendance(studentId, date1)

            val remainingDate1 = attendanceDao.getAttendanceForDate(date1).first()
            val remainingDate2 = attendanceDao.getAttendanceForDate(date2).first()

            assertTrue(remainingDate1.isEmpty())
            assertEquals(1, remainingDate2.size)
        }

    @Test
    fun deleteAttendance_nonExistentRecord_noError() =
        runTest {
            // Should not throw when deleting non-existent record
            attendanceDao.deleteAttendance(999, LocalDate(2023, 10, 15))
            // Test passes if no exception is thrown
        }

    // endregion

    // region getStudentAttendanceDates tests

    @Test
    fun getStudentAttendanceDates_returnsDatesSortedDescending() =
        runTest {
            val studentId = studentDao.insertStudent(StudentEntity(name = "Student")).toInt()

            // Insert dates out of order
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
            assertEquals(LocalDate(2023, 10, 20), dates[0]) // Most recent first
            assertEquals(LocalDate(2023, 10, 15), dates[1])
            assertEquals(LocalDate(2023, 10, 10), dates[2]) // Oldest last
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

            // Insert dates: one before range, two in range, one after
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

            // Should only include dates between 10th and 25th inclusive
            assertEquals(2, history.size)
            assertEquals(LocalDate(2023, 10, 20), history[0].date) // DESC order
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

            // Insert exactly on start and end boundaries
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

            // Both boundary dates should be included
            assertEquals(2, history.size)
        }

    // endregion

    // region getDistinctDatesWithAttendance tests

    @Test
    fun getDistinctDatesWithAttendance_returnsUniqueDatesSortedDescending() =
        runTest {
            val studentId1 = studentDao.insertStudent(StudentEntity(name = "Student 1")).toInt()
            val studentId2 = studentDao.insertStudent(StudentEntity(name = "Student 2")).toInt()

            // Multiple students on same date should produce only one date entry
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

            assertEquals(2, dates.size) // Only 2 unique dates
            assertEquals(LocalDate(2023, 10, 15), dates[0]) // DESC order
            assertEquals(LocalDate(2023, 10, 10), dates[1])
        }

    @Test
    fun getDistinctDatesWithAttendance_emptyWhenNoAttendance() =
        runTest {
            // Just create a student without any attendance
            studentDao.insertStudent(StudentEntity(name = "Student"))

            val dates = attendanceDao.getDistinctDatesWithAttendance().first()

            assertTrue(dates.isEmpty())
        }

    // endregion
}