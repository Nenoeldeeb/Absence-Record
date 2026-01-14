package dev.nenoeldeeb.education.absencerecord.domain.models

data class ParsedStudentImportData(
    val originalData: StudentExportData,
    val id: Int = System.identityHashCode(originalData)
)