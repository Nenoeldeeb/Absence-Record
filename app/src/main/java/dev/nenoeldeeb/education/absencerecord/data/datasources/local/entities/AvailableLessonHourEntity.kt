package dev.nenoeldeeb.education.absencerecord.data.datasources.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "available_lesson_hours", indices = [Index("weekday")])
data class AvailableLessonHourEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val weekday: Int,
    val startMinutes: Int,
    /** Lesson length in minutes, 10…120 in steps of 10 (FR-002). Replaces the old maxStudents column. */
    val durationMinutes: Int
)