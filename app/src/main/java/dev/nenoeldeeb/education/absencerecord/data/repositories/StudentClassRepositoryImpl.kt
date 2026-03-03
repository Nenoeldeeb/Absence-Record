package dev.nenoeldeeb.education.absencerecord.data.repositories

import dev.nenoeldeeb.education.absencerecord.data.datasources.local.daos.StudentClassDao
import dev.nenoeldeeb.education.absencerecord.data.mappers.toStudentClass
import dev.nenoeldeeb.education.absencerecord.data.mappers.toStudentClassEntity
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentClass
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
            .catch { emit(Result.failure(it)) }
    }

    override suspend fun insertClass(studentClass: StudentClass): Result<Long> {
        return runCatching {
            if (studentClassDao.classNameExists(studentClass.name)) {
                throw IllegalArgumentException("DUPLICATE_CLASS_NAME")
            }
            studentClassDao.insertClass(studentClass.toStudentClassEntity())
        }
    }

    override suspend fun updateClass(studentClass: StudentClass): Result<Unit> {
        return runCatching {
            if (studentClassDao.classNameExists(studentClass.name)) {
                throw IllegalArgumentException("DUPLICATE_CLASS_NAME")
            }
            studentClassDao.updateClass(studentClass.toStudentClassEntity())
        }
    }

    override suspend fun deleteClass(studentClass: StudentClass): Result<Unit> {
        return runCatching { studentClassDao.deleteClass(studentClass.toStudentClassEntity()) }
    }
}
