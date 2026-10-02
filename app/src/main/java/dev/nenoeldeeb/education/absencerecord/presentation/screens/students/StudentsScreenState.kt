package dev.nenoeldeeb.education.absencerecord.presentation.screens.students

import androidx.compose.runtime.Stable
import dev.nenoeldeeb.education.absencerecord.domain.models.DeletedStudentBackup
import dev.nenoeldeeb.education.absencerecord.domain.models.ParsedImportData
import dev.nenoeldeeb.education.absencerecord.domain.models.SortType
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentClass
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText
import kotlinx.datetime.LocalDate

@Stable
data class StudentsScreenState(
    // Core data
    val allStudents: List<Student> = emptyList(),
    val totalStudentsCount: Int = 0,
    val availableClasses: List<StudentClass> = emptyList(),
    val newStudentName: String = "",
    // Class filter
    val selectedClassIds: Set<Int> = emptySet(),
    val classDropdownExpanded: Boolean = false,
    val isClassFilterVisible: Boolean = false,
    val showManageClassesDialog: Boolean = false,
    // Search (in-place TopAppBar filter, session only)
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    // Sort
    val sortType: SortType = SortType.ByName,
    val isSortPanelVisible: Boolean = false,
    val availableMonths: List<LocalDate> = emptyList(),
    val selectedMonth: LocalDate? = null,
    val isMonthDropdownExpanded: Boolean = false,
    // Selection state
    val isMultiSelectionMode: Boolean = false,
    val selectedStudentIds: Set<Int> = emptySet(),
    // UI state - Dialogs
    val showImportSelectionDialog: Boolean = false,
    val showBulkDeleteDialog: Boolean = false,
    val showExportDeleteDialog: Boolean = false,
    val showAddStudentDialog: Boolean = false,
    // Undo backup for destructive deletes (students + attendance dates)
    val lastDeletedBackup: List<DeletedStudentBackup> = emptyList(),
    // Import/Export data
    val parsedImportData: ParsedImportData? = null,
    val importSelectionMap: Map<Int, Boolean> = emptyMap(),
    // Temporary data
    val toastMessage: UiText? = null,
    val error: UiText? = null
)