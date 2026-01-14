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
 * Instrumentation tests for [StudentDao].
 *
 * These tests verify that Room-generated SQL queries execute correctly
 * against a real SQLite database on an Android device/emulator.
 */
@RunWith(AndroidJUnit4::class)
class StudentDaoTest {
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

    // region getAllStudents tests

    @Test
    fun getAllStudents_emptyDatabase_returnsEmptyList() =
        runTest {
            val students = studentDao.getAllStudents().first()
            assertTrue(students.isEmpty())
        }

    @Test
    fun getAllStudents_returnsStudentsSortedByNameCaseInsensitive() =
        runTest {
            // Arrange - insert students out of alphabetical order with mixed case
            studentDao.insertStudent(StudentEntity(name = "Zack"))
            studentDao.insertStudent(StudentEntity(name = "alice"))
            studentDao.insertStudent(StudentEntity(name = "Bob"))

            // Act
            val students = studentDao.getAllStudents().first()

            // Assert - should be sorted alphabetically, case-insensitive
            assertEquals(3, students.size)
            assertEquals("alice", students[0].name)
            assertEquals("Bob", students[1].name)
            assertEquals("Zack", students[2].name)
        }

    // endregion

    // region getAllStudentsSortedByAttendance tests

    @Test
    fun getAllStudentsSortedByAttendance_sortsDescendingByAttendanceCountThenByName() =
        runTest {
            // Arrange
            val studentId1 = studentDao.insertStudent(StudentEntity(name = "Alice")).toInt()
            val studentId2 = studentDao.insertStudent(StudentEntity(name = "Bob")).toInt()
            studentDao.insertStudent(StudentEntity(name = "Charlie")).toInt()

            // Give Bob 2 attendances, Alice 1, Charlie 0
            attendanceDao.insertAttendance(
                StudentAttendanceEntity(studentId = studentId2, date = LocalDate(2023, 10, 1))
            )
            attendanceDao.insertAttendance(
                StudentAttendanceEntity(studentId = studentId2, date = LocalDate(2023, 10, 2))
            )
            attendanceDao.insertAttendance(
                StudentAttendanceEntity(studentId = studentId1, date = LocalDate(2023, 10, 1))
            )

            // Act
            val students = studentDao.getAllStudentsSortedByAttendance().first()

            // Assert - Bob (2), Alice (1), Charlie (0)
            assertEquals(3, students.size)
            assertEquals("Bob", students[0].name)
            assertEquals("Alice", students[1].name)
            assertEquals("Charlie", students[2].name)
        }

    @Test
    fun getAllStudentsSortedByAttendance_sameCountSortsByName() =
        runTest {
            // Arrange - both students have 1 attendance
            val studentId1 = studentDao.insertStudent(StudentEntity(name = "Zack")).toInt()
            val studentId2 = studentDao.insertStudent(StudentEntity(name = "Alice")).toInt()

            attendanceDao.insertAttendance(
                StudentAttendanceEntity(studentId = studentId1, date = LocalDate(2023, 10, 1))
            )
            attendanceDao.insertAttendance(
                StudentAttendanceEntity(studentId = studentId2, date = LocalDate(2023, 10, 1))
            )

            // Act
            val students = studentDao.getAllStudentsSortedByAttendance().first()

            // Assert - same attendance count, sorted by name: Alice, Zack
            assertEquals(2, students.size)
            assertEquals("Alice", students[0].name)
            assertEquals("Zack", students[1].name)
        }

    // endregion

    // region insertStudent tests

    @Test
    fun insertStudent_insertsSuccessfully() =
        runTest {
            val insertedId = studentDao.insertStudent(StudentEntity(name = "Test Student"))

            assertTrue(insertedId > 0)
            val students = studentDao.getAllStudents().first()
            assertEquals(1, students.size)
            assertEquals("Test Student", students[0].name)
        }

    @Test
    fun insertStudent_duplicateName_stillInserts() =
        runTest {
            // Note: The schema doesn't have unique constraint on name, only IGNORE on primary key
            studentDao.insertStudent(StudentEntity(name = "Duplicate"))
            val secondId = studentDao.insertStudent(StudentEntity(name = "Duplicate"))

            assertTrue(secondId > 0) // Both insertions succeed (different IDs)
            val students = studentDao.getAllStudents().first()
            assertEquals(2, students.size)
        }

    // endregion

    // region updateStudent tests

    @Test
    fun updateStudent_updatesExistingStudent() =
        runTest {
            val insertedId = studentDao.insertStudent(StudentEntity(name = "Original")).toInt()

            studentDao.updateStudent(StudentEntity(id = insertedId, name = "Updated"))

            val students = studentDao.getAllStudents().first()
            assertEquals(1, students.size)
            assertEquals("Updated", students[0].name)
        }

    // endregion

    // region deleteStudents tests

    @Test
    fun deleteStudents_deletesMultipleStudents() =
        runTest {
            val id1 = studentDao.insertStudent(StudentEntity(name = "Student 1")).toInt()
            val id2 = studentDao.insertStudent(StudentEntity(name = "Student 2")).toInt()
            studentDao.insertStudent(StudentEntity(name = "Student 3"))

            studentDao.deleteStudents(
                listOf(
                    StudentEntity(id = id1, name = "Student 1"),
                    StudentEntity(id = id2, name = "Student 2")
                )
            )

            val remaining = studentDao.getAllStudents().first()
            assertEquals(1, remaining.size)
            assertEquals("Student 3", remaining[0].name)
        }

    @Test
    fun deleteStudents_cascadesToAttendance() =
        runTest {
            // Arrange
            val studentId = studentDao.insertStudent(StudentEntity(name = "Student")).toInt()
            attendanceDao.insertAttendance(
                StudentAttendanceEntity(studentId = studentId, date = LocalDate(2023, 10, 1))
            )

            // Verify attendance exists
            val attendanceBefore = attendanceDao.getAttendanceForDate(LocalDate(2023, 10, 1)).first()
            assertEquals(1, attendanceBefore.size)

            // Act - delete the student
            studentDao.deleteStudents(listOf(StudentEntity(id = studentId, name = "Student")))

            // Assert - attendance should also be deleted (CASCADE)
            val attendanceAfter = attendanceDao.getAttendanceForDate(LocalDate(2023, 10, 1)).first()
            assertTrue(attendanceAfter.isEmpty())
        }

    // endregion

    // region getStudentsActiveInMonthSortedByName tests

    @Test
    fun getStudentsActiveInMonthSortedByName_returnsOnlyStudentsWithAttendanceInMonth() =
        runTest {
            // Arrange
            val studentId1 = studentDao.insertStudent(StudentEntity(name = "Active Student")).toInt()
            val studentId2 = studentDao.insertStudent(StudentEntity(name = "Inactive Student")).toInt()

            // Only studentId1 has attendance in October 2023
            attendanceDao.insertAttendance(
                StudentAttendanceEntity(studentId = studentId1, date = LocalDate(2023, 10, 15))
            )
            // studentId2 has attendance in November (different month)
            attendanceDao.insertAttendance(
                StudentAttendanceEntity(studentId = studentId2, date = LocalDate(2023, 11, 15))
            )

            // Act
            val activeStudents = studentDao.getStudentsActiveInMonthSortedByName("2023-10").first()

            // Assert
            assertEquals(1, activeStudents.size)
            assertEquals("Active Student", activeStudents[0].name)
        }

    @Test
    fun getStudentsActiveInMonthSortedByName_sortsByNameCaseInsensitive() =
        runTest {
            val studentId1 = studentDao.insertStudent(StudentEntity(name = "Zack")).toInt()
            val studentId2 = studentDao.insertStudent(StudentEntity(name = "alice")).toInt()

            attendanceDao.insertAttendance(
                StudentAttendanceEntity(studentId = studentId1, date = LocalDate(2023, 10, 1))
            )
            attendanceDao.insertAttendance(
                StudentAttendanceEntity(studentId = studentId2, date = LocalDate(2023, 10, 1))
            )

            val students = studentDao.getStudentsActiveInMonthSortedByName("2023-10").first()

            assertEquals(2, students.size)
            assertEquals("alice", students[0].name)
            assertEquals("Zack", students[1].name)
        }

    // endregion

    // region getAllStudentsSortedByAttendanceForMonth tests

    @Test
    fun getAllStudentsSortedByAttendanceForMonth_sortsByMonthSpecificAttendance() =
        runTest {
            // Arrange
            val studentId1 = studentDao.insertStudent(StudentEntity(name = "Alice")).toInt()
            val studentId2 = studentDao.insertStudent(StudentEntity(name = "Bob")).toInt()

            // Bob has more attendance in October
            attendanceDao.insertAttendance(
                StudentAttendanceEntity(studentId = studentId2, date = LocalDate(2023, 10, 1))
            )
            attendanceDao.insertAttendance(
                StudentAttendanceEntity(studentId = studentId2, date = LocalDate(2023, 10, 2))
            )
            attendanceDao.insertAttendance(
                StudentAttendanceEntity(studentId = studentId1, date = LocalDate(2023, 10, 1))
            )

            // Alice has more attendance in November (shouldn't affect October sort)
            attendanceDao.insertAttendance(
                StudentAttendanceEntity(studentId = studentId1, date = LocalDate(2023, 11, 1))
            )
            attendanceDao.insertAttendance(
                StudentAttendanceEntity(studentId = studentId1, date = LocalDate(2023, 11, 2))
            )
            attendanceDao.insertAttendance(
                StudentAttendanceEntity(studentId = studentId1, date = LocalDate(2023, 11, 3))
            )

            // Act - get October sorted
            val students = studentDao.getAllStudentsSortedByAttendanceForMonth("2023-10").first()

            // Assert - Bob (2 in Oct), Alice (1 in Oct)
            assertEquals(2, students.size)
            assertEquals("Bob", students[0].name)
            assertEquals("Alice", students[1].name)
        }

    @Test
    fun getAllStudentsSortedByAttendanceForMonth_includesStudentsWithZeroAttendance() =
        runTest {
            val studentId1 = studentDao.insertStudent(StudentEntity(name = "Active")).toInt()
            studentDao.insertStudent(StudentEntity(name = "Inactive"))

            attendanceDao.insertAttendance(
                StudentAttendanceEntity(studentId = studentId1, date = LocalDate(2023, 10, 1))
            )

            val students = studentDao.getAllStudentsSortedByAttendanceForMonth("2023-10").first()

            // Both students should be returned
            assertEquals(2, students.size)
            assertEquals("Active", students[0].name) // 1 attendance
            assertEquals("Inactive", students[1].name) // 0 attendance
        }

    // endregion
}