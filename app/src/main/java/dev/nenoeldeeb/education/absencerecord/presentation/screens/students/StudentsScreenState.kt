package dev.nenoeldeeb.education.absencerecord.presentation.screens.students

import androidx.compose.runtime.Stable
import dev.nenoeldeeb.education.absencerecord.domain.models.ParsedStudentImportData
import dev.nenoeldeeb.education.absencerecord.domain.models.SortType
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.presentation.util.UiText
import kotlinx.datetime.LocalDate

@Stable
data class StudentsScreenState(
    // Core data
    val allStudents: List<Student> = emptyList(),
    val sortType: SortType = SortType.ByName,
    val selectedMonth: LocalDate? = null,
    val newStudentName: String = "",
    // Selection state
    val isMultiSelectionMode: Boolean = false,
    val selectedStudentIds: Set<Int> = emptySet(),
    // UI state - Dialogs
    val showDataManagementDialog: Boolean = false,
    val showExportSelectionDialog: Boolean = false,
    val showImportSelectionDialog: Boolean = false,
    val showDeleteDialog: Student? = null,
    val showBulkDeleteDialog: Boolean = false,
    val showEditDialog: Student? = null,
    val showAddStudentDialog: Boolean = false,
    // Import/Export data
    val exportSelectionMap: Map<Int, Boolean> = emptyMap(),
    val parsedStudentsFromFile: List<ParsedStudentImportData>? = null,
    val importSelectionMap: Map<Int, Boolean> = emptyMap(),
    // Temporary data
    val toastMessage: UiText? = null,
    val error: UiText? = null
)