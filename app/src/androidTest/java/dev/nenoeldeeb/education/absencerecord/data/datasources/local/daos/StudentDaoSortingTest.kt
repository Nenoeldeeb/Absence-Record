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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StudentDaoSortingTest {
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

    // region getAllStudentsSortedByAttendance tests

    @Test
    fun getAllStudentsSortedByAttendance_sortsDescendingByAttendanceCountThenByName() =
        runTest {
            val studentId1 = studentDao.insertStudent(StudentEntity(name = "Alice")).toInt()
            val studentId2 = studentDao.insertStudent(StudentEntity(name = "Bob")).toInt()
            studentDao.insertStudent(StudentEntity(name = "Charlie")).toInt()

            attendanceDao.insertAttendance(
                StudentAttendanceEntity(studentId = studentId2, date = LocalDate(2023, 10, 1))
            )
            attendanceDao.insertAttendance(
                StudentAttendanceEntity(studentId = studentId2, date = LocalDate(2023, 10, 2))
            )
            attendanceDao.insertAttendance(
                StudentAttendanceEntity(studentId = studentId1, date = LocalDate(2023, 10, 1))
            )

            val students = studentDao.getAllStudentsSortedByAttendance().first()

            assertEquals(3, students.size)
            assertEquals("Bob", students[0].name)
            assertEquals("Alice", students[1].name)
            assertEquals("Charlie", students[2].name)
        }

    @Test
    fun getAllStudentsSortedByAttendance_sameCountSortsByName() =
        runTest {
            val studentId1 = studentDao.insertStudent(StudentEntity(name = "Zack")).toInt()
            val studentId2 = studentDao.insertStudent(StudentEntity(name = "Alice")).toInt()

            attendanceDao.insertAttendance(
                StudentAttendanceEntity(studentId = studentId1, date = LocalDate(2023, 10, 1))
            )
            attendanceDao.insertAttendance(
                StudentAttendanceEntity(studentId = studentId2, date = LocalDate(2023, 10, 1))
            )

            val students = studentDao.getAllStudentsSortedByAttendance().first()

            assertEquals(2, students.size)
            assertEquals("Alice", students[0].name)
            assertEquals("Zack", students[1].name)
        }

    // endregion

    // region getStudentsActiveInMonthSortedByName tests

    @Test
    fun getStudentsActiveInMonthSortedByName_returnsOnlyStudentsWithAttendanceInMonth() =
        runTest {
            val studentId1 = studentDao.insertStudent(StudentEntity(name = "Active Student")).toInt()
            val studentId2 = studentDao.insertStudent(StudentEntity(name = "Inactive Student")).toInt()

            attendanceDao.insertAttendance(
                StudentAttendanceEntity(studentId = studentId1, date = LocalDate(2023, 10, 15))
            )
            attendanceDao.insertAttendance(
                StudentAttendanceEntity(studentId = studentId2, date = LocalDate(2023, 11, 15))
            )

            val activeStudents = studentDao.getStudentsActiveInMonthSortedByName("2023-10").first()

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
            val studentId1 = studentDao.insertStudent(StudentEntity(name = "Alice")).toInt()
            val studentId2 = studentDao.insertStudent(StudentEntity(name = "Bob")).toInt()

            attendanceDao.insertAttendance(
                StudentAttendanceEntity(studentId = studentId2, date = LocalDate(2023, 10, 1))
            )
            attendanceDao.insertAttendance(
                StudentAttendanceEntity(studentId = studentId2, date = LocalDate(2023, 10, 2))
            )
            attendanceDao.insertAttendance(
                StudentAttendanceEntity(studentId = studentId1, date = LocalDate(2023, 10, 1))
            )

            attendanceDao.insertAttendance(
                StudentAttendanceEntity(studentId = studentId1, date = LocalDate(2023, 11, 1))
            )
            attendanceDao.insertAttendance(
                StudentAttendanceEntity(studentId = studentId1, date = LocalDate(2023, 11, 2))
            )
            attendanceDao.insertAttendance(
                StudentAttendanceEntity(studentId = studentId1, date = LocalDate(2023, 11, 3))
            )

            val students = studentDao.getAllStudentsSortedByAttendanceForMonth("2023-10").first()

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

            assertEquals(2, students.size)
            assertEquals("Active", students[0].name)
            assertEquals("Inactive", students[1].name)
        }

    // endregion
}