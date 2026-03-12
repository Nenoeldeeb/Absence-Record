package dev.nenoeldeeb.education.absencerecord.data.datasources.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, val name: String, val classId: Int? = null
)
