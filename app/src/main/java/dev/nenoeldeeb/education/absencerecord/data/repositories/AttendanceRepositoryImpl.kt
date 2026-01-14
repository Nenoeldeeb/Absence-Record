package dev.nenoeldeeb.education.absencerecord.data.repositories

import dev.nenoeldeeb.education.absencerecord.data.datasources.local.daos.AttendanceDao
import dev.nenoeldeeb.education.absencerecord.data.mappers.toAttendanceHistoryItem
import dev.nenoeldeeb.education.absencerecord.data.mappers.toStudentAttendance
import dev.nenoeldeeb.education.absencerecord.data.mappers.toStudentAttendanceEntity
import dev.nenoeldeeb.education.absencerecord.domain.models.AttendanceHistoryItem
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentAttendance
import dev.nenoeldeeb.education.absencerecord.domain.repositories.AttendanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

class AttendanceRepositoryImpl(
    private val attendanceDao: AttendanceDao
) : AttendanceRepository {
    override fun getAttendanceForDate(date: LocalDate): Flow<Result<List<StudentAttendance>>> {
        return attendanceDao.getAttendanceForDate(date)
            .map { list -> Result.success(list.map { it.toStudentAttendance() }) }
            .catch { emit(Result.failure(it)) }
    }

    override suspend fun insertAttendance(attendance: StudentAttendance): Result<Long> {
        return runCatching {
            attendanceDao.insertAttendance(attendance.toStudentAttendanceEntity())
        }
    }

    override suspend fun deleteAttendance(
        studentId: Int,
        date: LocalDate
    ): Result<Unit> {
        return runCatching {
            attendanceDao.deleteAttendance(studentId, date)
        }
    }

    override fun getStudentAttendanceDates(studentId: Int): Flow<Result<List<LocalDate>>> {
        return attendanceDao.getStudentAttendanceDates(studentId)
            .map { Result.success(it) }
            .catch { emit(Result.failure(it)) }
    }

    override fun getAttendanceHistoryForDateRange(
        studentId: Int,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<Result<List<AttendanceHistoryItem>>> {
        return attendanceDao.getAttendanceHistoryForDateRange(studentId, startDate, endDate)
            .map { entities -> Result.success(entities.map { it.toAttendanceHistoryItem() }) }
            .catch { emit(Result.failure(it)) }
    }

    override fun getDistinctDatesWithAttendance(): Flow<Result<List<LocalDate>>> {
        return attendanceDao.getDistinctDatesWithAttendance()
            .map { Result.success(it) }
            .catch { emit(Result.failure(it)) }
    }
}