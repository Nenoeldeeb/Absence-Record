package dev.nenoeldeeb.education.absencerecord.data.datasources.local.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.entities.AttendanceHistoryItemEntity
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.entities.StudentAttendanceEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM student_attendance WHERE date = :date")
    fun getAttendanceForDate(date: LocalDate): Flow<List<StudentAttendanceEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAttendance(attendance: StudentAttendanceEntity): Long

    @Query("DELETE FROM student_attendance WHERE studentId = :studentId AND date = :date")
    suspend fun deleteAttendance(
        studentId: Int,
        date: LocalDate
    )

    @Query("SELECT date FROM student_attendance WHERE studentId = :studentId ORDER BY date DESC")
    fun getStudentAttendanceDates(studentId: Int): Flow<List<LocalDate>>

    @Query(
        """
        SELECT sa.date, s.name AS studentName
        FROM student_attendance sa
        JOIN students s ON s.id = sa.studentId
        WHERE sa.studentId = :studentId
        AND sa.date BETWEEN :startDate AND :endDate
        ORDER BY sa.date DESC
    """
    )
    fun getAttendanceHistoryForDateRange(
        studentId: Int,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<AttendanceHistoryItemEntity>>

    @Query(
        """
        SELECT DISTINCT date
        FROM student_attendance
        ORDER BY date DESC
    """
    )
    fun getDistinctDatesWithAttendance(): Flow<List<LocalDate>>
}