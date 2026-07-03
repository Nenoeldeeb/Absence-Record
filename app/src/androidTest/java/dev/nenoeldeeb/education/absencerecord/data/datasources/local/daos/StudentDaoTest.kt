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
 * These tests verify that Room-generated SQL queries for CRUD student operations execute correctly
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
}