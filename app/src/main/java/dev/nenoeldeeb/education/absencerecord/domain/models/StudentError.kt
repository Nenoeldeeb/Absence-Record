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

    data object HourOverlap : StudentError("This hour overlaps an existing appointment")

    data object HourFull : StudentError("This hour is already full")

    data object AssignmentExists : StudentError("This student already has a lesson that day")

    data object BusyConflict : StudentError("This hour overlaps a busy appointment of this student")

    data object MaxBelowAssigned :
        StudentError("Max students cannot be lower than the number of assigned students")

    data class Validation(override val message: String) : StudentError(message)

    data object Cancelled : StudentError("Operation cancelled")
}