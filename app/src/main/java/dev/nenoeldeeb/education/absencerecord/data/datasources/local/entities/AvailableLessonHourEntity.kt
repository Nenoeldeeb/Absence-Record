package dev.nenoeldeeb.education.absencerecord.data.datasources.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "available_lesson_hours", indices = [Index("weekday")])
data class AvailableLessonHourEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val weekday: Int,
    val startMinutes: Int,
    val maxStudents: Int
)