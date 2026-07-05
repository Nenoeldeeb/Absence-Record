package dev.nenoeldeeb.education.absencerecord.domain.models

sealed class StudentError(
    message: String = "",
    cause: Throwable? = null
) : Throwable(message, cause) {
    data object Database : StudentError("A database operation failed")

    data object DuplicateClass : StudentError("A class with this name already exists")

    data object FileRead : StudentError("Error reading file")

    data object FileWrite : StudentError("Error writing file")

    data object ImportParse : StudentError("Error parsing import file")

    data object ReportGeneration : StudentError("Failed to generate report")

    data class Validation(override val message: String) : StudentError(message)

    data object Cancelled : StudentError("Operation cancelled")
}