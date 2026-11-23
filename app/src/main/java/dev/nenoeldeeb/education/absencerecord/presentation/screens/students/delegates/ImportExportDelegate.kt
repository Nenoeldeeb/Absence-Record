package dev.nenoeldeeb.education.absencerecord.presentation.screens.students.delegates

import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.ImportResult
import dev.nenoeldeeb.education.absencerecord.domain.models.ParsedStudentImportData
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError
import dev.nenoeldeeb.education.absencerecord.domain.services.ImportCaps
import dev.nenoeldeeb.education.absencerecord.domain.usecases.StudentManagementUseCases
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenState
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText
import dev.nenoeldeeb.education.absencerecord.presentation.utils.toUiText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * One home for import/export logic: the import-selection dialog lifecycle and
 * import execution (stateful, reading the snapshot via the read-only [uiState]
 * param), plus the pure import/export state transformations below.
 * Mutates state only through `updateState`; launches only via `scope`.
 */
class ImportExportDelegate(
    private val studentManagementUseCases: StudentManagementUseCases,
    private val uiState: StateFlow<StudentsScreenState>,
    private val scope: CoroutineScope,
    private val updateState: ((StudentsScreenState) -> StudentsScreenState) -> Unit
) {
    /**
     * Parses the import file and opens the selection dialog. A null URI means
     * the file picker was cancelled: report that as before.
     */
    fun prepareImportSelectionDialog(uriString: String?) {
        if (uriString == null) {
            updateState { it.copy(error = StudentError.Cancelled.toUiText()) }
            return
        }
        scope.launch {
            studentManagementUseCases.parseImportFileUseCase(uriString)
                .onSuccess { parsedData ->
                    val (data, selectionMap, showDialog) = prepareImportDialog(parsedData.students)
                    updateState {
                        if (data == null) {
                            it.copy(
                                parsedImportData = null,
                                importSelectionMap = emptyMap(),
                                toastMessage = UiText.StringResource(R.string.no_students_found_in_file)
                            )
                        } else {
                            it.copy(
                                parsedImportData = parsedData,
                                importSelectionMap = selectionMap,
                                showImportSelectionDialog = showDialog
                            )
                        }
                    }
                }
                .onFailure { error -> updateState { state -> state.copy(error = error.toUiText()) } }
        }
    }

    fun closeImportSelectionDialog() {
        updateState {
            it.copy(
                showImportSelectionDialog = false,
                parsedImportData = null,
                importSelectionMap = emptyMap()
            )
        }
    }

    fun handleToggleImportSelection(parsedStudentId: Int) {
        updateState {
            it.copy(
                importSelectionMap = toggleImportSelection(it.importSelectionMap, parsedStudentId)
            )
        }
    }

    fun handleSelectAllImport(selectAll: Boolean) {
        updateState { state ->
            val parsed = state.parsedImportData?.students.orEmpty()
            state.copy(
                importSelectionMap = parsed.associate { it.id to selectAll }
            )
        }
    }

    fun performImport() {
        val parsedData = uiState.value.parsedImportData
        val selectionMap = uiState.value.importSelectionMap
        if (parsedData == null) return
        scope.launch {
            studentManagementUseCases.performImportUseCase(parsedData, selectionMap)
                .onSuccess { importResult ->
                    updateState { current ->
                        current.copy(
                            showImportSelectionDialog = false,
                            parsedImportData = null,
                            importSelectionMap = emptyMap(),
                            toastMessage = buildImportResultMessage(importResult)
                        )
                    }
                }
                .onFailure { error -> updateState { current -> current.copy(error = error.toUiText()) } }
        }
    }

    /**
     * Prepares the import dialog state from parsed student data.
     *
     * @return Triple of (parsed data or null, selection map, show dialog flag)
     */
    fun prepareImportDialog(
        parsedData: List<ParsedStudentImportData>
    ): Triple<List<ParsedStudentImportData>?, Map<Int, Boolean>, Boolean> {
        return if (parsedData.isEmpty()) {
            Triple(null, emptyMap(), false) // No dialog for empty data
        } else {
            Triple(
                parsedData,
                parsedData.associate { it.id to true },
                true
            )
        }
    }

    /**
     * Toggles selection state for a single parsed student in the import dialog.
     */
    fun toggleImportSelection(
        currentMap: Map<Int, Boolean>,
        parsedStudentId: Int
    ): Map<Int, Boolean> {
        return currentMap.toMutableMap().apply {
            this[parsedStudentId] = this[parsedStudentId] != true
        }
    }

    /**
     * Builds a user-facing message from import results.
     * Combines multiple parts based on what happened during import.
     */
    fun buildImportResultMessage(result: ImportResult): UiText {
        val parts = mutableListOf<UiText>()

        // Always show new students count
        parts.add(
            UiText.PluralResource(
                R.plurals.import_complete_new_students_added,
                result.newStudentsCount,
                result.newStudentsCount
            )
        )

        // Show merged count if any
        if (result.existingStudentsMergedCount > 0) {
            parts.add(
                UiText.PluralResource(
                    R.plurals.existing_students_had_dates_merged,
                    result.existingStudentsMergedCount,
                    result.existingStudentsMergedCount
                )
            )
        }

        // Show skipped dates if any
        if (result.datesSkippedCount > 0) {
            parts.add(
                UiText.PluralResource(
                    R.plurals.dates_skipped_due_to_errors,
                    result.datesSkippedCount,
                    result.datesSkippedCount
                )
            )
        }

        // Show schedule outcomes
        parts.add(
            UiText.PluralResource(
                R.plurals.import_complete_hours_added,
                result.hoursAddedCount,
                result.hoursAddedCount
            )
        )
        if (result.hoursSkippedOverlapCount > 0) {
            parts.add(
                UiText.PluralResource(
                    R.plurals.import_complete_hours_overlap_skipped,
                    result.hoursSkippedOverlapCount,
                    result.hoursSkippedOverlapCount
                )
            )
        }
        parts.add(
            UiText.PluralResource(
                R.plurals.import_complete_lessons_added,
                result.lessonsAddedCount,
                result.lessonsAddedCount
            )
        )
        if (result.lessonsRemovedBusyWinsCount > 0) {
            parts.add(
                UiText.PluralResource(
                    R.plurals.import_complete_lessons_removed_busy,
                    result.lessonsRemovedBusyWinsCount,
                    result.lessonsRemovedBusyWinsCount
                )
            )
        }
        if (result.lessonsSkippedCount > 0) {
            parts.add(
                UiText.PluralResource(
                    R.plurals.import_complete_lessons_skipped,
                    result.lessonsSkippedCount,
                    result.lessonsSkippedCount
                )
            )
        }
        if (result.malformedEntriesSkippedCount > 0) {
            parts.add(
                UiText.PluralResource(
                    R.plurals.import_complete_malformed_skipped,
                    result.malformedEntriesSkippedCount,
                    result.malformedEntriesSkippedCount
                )
            )
        }
        // Records left out because a documented cap was reached. Said plainly,
        // and naming the cap, so the teacher is never left guessing why a file
        // did not fully import (FR-058, FR-059).
        if (result.cappedEntriesSkippedCount > 0) {
            parts.add(
                UiText.PluralResource(
                    R.plurals.import_complete_capped_skipped,
                    result.cappedEntriesSkippedCount,
                    result.cappedEntriesSkippedCount
                )
            )
            if (result.studentCapHit) {
                parts.add(
                    UiText.StringResource(
                        R.string.import_complete_student_cap_hit,
                        ImportCaps.MAX_IMPORT_STUDENTS
                    )
                )
            }
        }

        return UiText.Joined(parts, "\n")
    }
}