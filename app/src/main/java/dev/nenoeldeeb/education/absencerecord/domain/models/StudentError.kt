package dev.nenoeldeeb.education.absencerecord.domain.models

import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText

sealed class StudentError(
    message: String = "",
    cause: Throwable? = null
) : Throwable(message, cause) {
    data object Database : StudentError("A database operation failed")

    data object DuplicateClass : StudentError("A class with this name already exists")

    data object FileRead : StudentError("Error reading file")

    data object ImportParse : StudentError("Error parsing import file")

    data class Validation(override val message: String) : StudentError(message)

    data object Cancelled : StudentError("Operation cancelled")
}

fun StudentError.toUiText(): UiText =
    when (this) {
        is StudentError.Database -> UiText.StringResource(R.string.error_database_operation_failed)
        is StudentError.DuplicateClass -> UiText.StringResource(R.string.error_class_name_already_exists)
        is StudentError.FileRead -> UiText.StringResource(R.string.error_reading_file)
        is StudentError.ImportParse -> UiText.StringResource(R.string.error_parsing_import_file)
        is StudentError.Validation -> UiText.DynamicString(message)
        is StudentError.Cancelled -> UiText.StringResource(R.string.file_selection_cancelled)
    }

fun Throwable.toUiText(): UiText =
    when (this) {
        is StudentError -> toUiText()
        else -> UiText.DynamicString(message ?: "Unknown error")
    }