package dev.nenoeldeeb.education.absencerecord.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class StudentExportData(
    val name: String,
    val className: String = "",
    val dates: List<String>,
    val lessonAssignments: List<LessonAssignmentExportData> = emptyList(),
    val busyAppointments: List<BusyAppointmentExportData> = emptyList()
)