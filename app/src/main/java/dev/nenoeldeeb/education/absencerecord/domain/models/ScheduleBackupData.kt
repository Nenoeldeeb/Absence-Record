package dev.nenoeldeeb.education.absencerecord.domain.models

import kotlinx.serialization.Serializable

const val BACKUP_VERSION = 2

@Serializable
data class BackupExportData(
    val version: Int = BACKUP_VERSION,
    val students: List<StudentExportData>,
    val availableHours: List<AvailableHourExportData>
)

@Serializable
data class AvailableHourExportData(
    val weekday: Int,
    val startMinutes: Int,
    val maxStudents: Int
)

@Serializable
data class LessonAssignmentExportData(
    val weekday: Int,
    val startMinutes: Int
)

@Serializable
data class BusyAppointmentExportData(
    val weekday: Int,
    val startMinutes: Int,
    val durationMinutes: Int
)