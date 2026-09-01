package dev.nenoeldeeb.education.absencerecord.domain.models

data class ParsedImportData(
    val students: List<ParsedStudentImportData>,
    val availableHours: List<AvailableHourExportData>
)