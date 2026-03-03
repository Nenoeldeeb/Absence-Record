package dev.nenoeldeeb.education.absencerecord.data.datasources.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "classes")
data class StudentClassEntity(@PrimaryKey(autoGenerate = true) val id: Int = 0, val name: String)
