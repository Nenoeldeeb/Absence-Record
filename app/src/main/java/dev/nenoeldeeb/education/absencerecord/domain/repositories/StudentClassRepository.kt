package dev.nenoeldeeb.education.absencerecord.domain.repositories

import dev.nenoeldeeb.education.absencerecord.domain.models.StudentClass
import kotlinx.coroutines.flow.Flow

interface StudentClassRepository {
    fun getAllClasses(): Flow<Result<List<StudentClass>>>

    suspend fun insertClass(studentClass: StudentClass): Result<Long>

    suspend fun updateClass(studentClass: StudentClass): Result<Unit>

    suspend fun deleteClass(studentClass: StudentClass): Result<Unit>

    suspend fun getOrCreateClassByName(name: String): Result<Int>
}
