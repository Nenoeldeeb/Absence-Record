package dev.nenoeldeeb.education.absencerecord.domain.repositories

import dev.nenoeldeeb.education.absencerecord.domain.models.AttendanceHistoryItem
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentAttendance
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface AttendanceRepository {
    fun getAttendanceForDate(date: LocalDate): Flow<Result<List<StudentAttendance>>>

    suspend fun insertAttendance(attendance: StudentAttendance): Result<Long>

    suspend fun deleteAttendance(
        studentId: Int,
        date: LocalDate
    ): Result<Unit>

    fun getStudentAttendanceDates(studentId: Int): Flow<Result<List<LocalDate>>>

    fun getAttendanceHistoryForDateRange(
        studentId: Int,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<Result<List<AttendanceHistoryItem>>>

    fun getDistinctDatesWithAttendance(): Flow<Result<List<LocalDate>>>
}