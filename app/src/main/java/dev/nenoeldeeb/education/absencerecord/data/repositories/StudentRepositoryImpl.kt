package dev.nenoeldeeb.education.absencerecord.data.repositories

import dev.nenoeldeeb.education.absencerecord.data.datasources.local.daos.StudentDao
import dev.nenoeldeeb.education.absencerecord.data.mappers.toStudent
import dev.nenoeldeeb.education.absencerecord.data.mappers.toStudentEntity
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError
import dev.nenoeldeeb.education.absencerecord.domain.repositories.StudentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class StudentRepositoryImpl(
    private val studentDao: StudentDao
) : StudentRepository {
    override fun getAllStudents(): Flow<Result<List<Student>>> {
        return studentDao.getAllStudents()
            .map { entities -> Result.success(entities.map { it.toStudent() }) }
            .catch { emit(Result.failure(StudentError.Database)) }
    }

    override fun getStudentById(studentId: Int): Flow<Result<Student?>> {
        return studentDao.getStudentById(studentId)
            .map { entity -> Result.success(entity?.toStudent()) }
            .catch { emit(Result.failure(StudentError.Database)) }
    }

    override fun getAllStudentsSortedByAttendance(): Flow<Result<List<Student>>> {
        return studentDao.getAllStudentsSortedByAttendance()
            .map { entities -> Result.success(entities.map { it.toStudent() }) }
            .catch { emit(Result.failure(StudentError.Database)) }
    }

    override suspend fun insertStudent(student: Student): Result<Long> {
        return runCatching {
            studentDao.insertStudent(student.toStudentEntity())
        }.recoverCatching { throw StudentError.Database }
    }

    override suspend fun updateStudent(student: Student): Result<Unit> {
        return runCatching {
            studentDao.updateStudent(student.toStudentEntity())
        }.recoverCatching { throw StudentError.Database }
    }

    override suspend fun unassignStudentsFromClass(classId: Int): Result<Unit> {
        return try {
            studentDao.unassignStudentsFromClass(classId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(StudentError.Database)
        }
    }

    override suspend fun deleteStudents(students: List<Student>): Result<Unit> {
        return runCatching {
            studentDao.deleteStudents(students.map { it.toStudentEntity() })
        }.recoverCatching { throw StudentError.Database }
    }

    override fun getStudentsActiveInMonthSortedByName(monthYear: String): Flow<Result<List<Student>>> {
        return studentDao.getStudentsActiveInMonthSortedByName(monthYear)
            .map { entities -> Result.success(entities.map { it.toStudent() }) }
            .catch { emit(Result.failure(StudentError.Database)) }
    }

    override fun getAllStudentsSortedByAttendanceForMonth(monthYear: String): Flow<Result<List<Student>>> {
        return studentDao.getAllStudentsSortedByAttendanceForMonth(monthYear)
            .map { entities -> Result.success(entities.map { it.toStudent() }) }
            .catch { emit(Result.failure(StudentError.Database)) }
    }
}