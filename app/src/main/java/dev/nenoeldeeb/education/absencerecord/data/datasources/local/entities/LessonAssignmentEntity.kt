package dev.nenoeldeeb.education.absencerecord.data.datasources.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "lesson_assignments",
    foreignKeys = [
        ForeignKey(
            entity = AvailableLessonHourEntity::class,
            parentColumns = ["id"],
            childColumns = ["availableHourId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["studentId", "weekday"], unique = true),
        Index("availableHourId"),
        Index("studentId")
    ]
)
data class LessonAssignmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val availableHourId: Int,
    val studentId: Int,
    val weekday: Int
)