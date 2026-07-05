package dev.nenoeldeeb.education.absencerecord.data.repositories

import dev.nenoeldeeb.education.absencerecord.data.datasources.local.daos.StudentClassDao
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.entities.StudentClassEntity
import dev.nenoeldeeb.education.absencerecord.data.mappers.toStudentClass
import dev.nenoeldeeb.education.absencerecord.data.mappers.toStudentClassEntity
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentClass
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError
import dev.nenoeldeeb.education.absencerecord.domain.repositories.StudentClassRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class StudentClassRepositoryImpl(private val studentClassDao: StudentClassDao) :
    StudentClassRepository {
    override fun getAllClasses(): Flow<Result<List<StudentClass>>> {
        return studentClassDao
            .getAllClasses()
            .map { entities -> Result.success(entities.map { it.toStudentClass() }) }
            .catch { emit(Result.failure(StudentError.Database)) }
    }

    override suspend fun insertClass(studentClass: StudentClass): Result<Long> {
        return try {
            if (studentClassDao.classNameExists(studentClass.name)) {
                Result.failure(StudentError.DuplicateClass)
            } else {
                Result.success(studentClassDao.insertClass(studentClass.toStudentClassEntity()))
            }
        } catch (e: Exception) {
            Result.failure(StudentError.Database)
        }
    }

    override suspend fun updateClass(studentClass: StudentClass): Result<Unit> {
        return try {
            if (studentClassDao.classNameExists(studentClass.name)) {
                Result.failure(StudentError.DuplicateClass)
            } else {
                studentClassDao.updateClass(studentClass.toStudentClassEntity())
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(StudentError.Database)
        }
    }

    override suspend fun deleteClass(studentClass: StudentClass): Result<Unit> {
        return try {
            studentClassDao.deleteClass(studentClass.toStudentClassEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(StudentError.Database)
        }
    }

    override suspend fun getOrCreateClassByName(name: String): Result<Int> {
        return try {
            val existing = studentClassDao.getClassByName(name)
            if (existing != null) {
                Result.success(existing.id)
            } else {
                Result.success(studentClassDao.insertClass(StudentClassEntity(name = name)).toInt())
            }
        } catch (e: Exception) {
            Result.failure(StudentError.Database)
        }
    }
}