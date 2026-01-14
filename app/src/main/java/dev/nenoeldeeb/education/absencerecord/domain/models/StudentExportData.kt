package dev.nenoeldeeb.education.absencerecord.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class StudentExportData(
    val name: String,
    val dates: List<String>
)