package dev.nenoeldeeb.education.absencerecord.data.datasources.local.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.entities.StudentClassEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentClassDao {
    @Query("SELECT * FROM classes ORDER BY name COLLATE NOCASE ASC")
    fun getAllClasses(): Flow<List<StudentClassEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM classes WHERE name = :name COLLATE NOCASE)")
    suspend fun classNameExists(name: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertClass(studentClass: StudentClassEntity): Long

    @Update
    suspend fun updateClass(studentClass: StudentClassEntity)

    @Delete
    suspend fun deleteClass(studentClass: StudentClassEntity)
}
