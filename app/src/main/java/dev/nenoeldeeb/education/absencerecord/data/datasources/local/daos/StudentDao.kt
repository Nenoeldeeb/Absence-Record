package dev.nenoeldeeb.education.absencerecord.data.datasources.local.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.entities.StudentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {
    @Query("SELECT * FROM students ORDER BY name COLLATE NOCASE ASC")
    fun getAllStudents(): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE id = :studentId")
    fun getStudentById(studentId: Int): Flow<StudentEntity?>

    @Query(
        """
        SELECT students.*
        FROM students
        LEFT JOIN student_attendance ON students.id = student_attendance.studentId
        GROUP BY students.id
        ORDER BY COUNT(student_attendance.date) DESC, students.name COLLATE NOCASE ASC
    """
    )
    fun getAllStudentsSortedByAttendance(): Flow<List<StudentEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertStudent(student: StudentEntity): Long

    @Update
    suspend fun updateStudent(student: StudentEntity)

    @Query("UPDATE students SET classId = NULL WHERE classId = :classId")
    suspend fun unassignStudentsFromClass(classId: Int)

    @Delete
    suspend fun deleteStudents(students: List<StudentEntity>)

    @Query(
        """
        SELECT DISTINCT students.*
        FROM students
        JOIN student_attendance ON students.id = student_attendance.studentId
        WHERE strftime('%Y-%m', student_attendance.date) = :monthYear
        ORDER BY students.name COLLATE NOCASE ASC
    """
    )
    fun getStudentsActiveInMonthSortedByName(monthYear: String): Flow<List<StudentEntity>>

    @Query(
        """
        SELECT students.*
        FROM students
        LEFT JOIN student_attendance ON students.id = student_attendance.studentId
                                   AND strftime('%Y-%m', student_attendance.date) = :monthYear
        GROUP BY students.id
        ORDER BY COUNT(student_attendance.date) DESC, 
                 students.name COLLATE NOCASE ASC
    """
    )
    fun getAllStudentsSortedByAttendanceForMonth(monthYear: String): Flow<List<StudentEntity>>
}