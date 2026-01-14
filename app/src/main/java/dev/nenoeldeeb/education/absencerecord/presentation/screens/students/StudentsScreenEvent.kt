package dev.nenoeldeeb.education.absencerecord.presentation.screens.students

import android.net.Uri
import dev.nenoeldeeb.education.absencerecord.domain.models.SortType
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import kotlinx.datetime.LocalDate

sealed interface StudentsScreenEvent {
    // UI Interactions
    data class UpdateNewStudentName(val name: String) : StudentsScreenEvent

    data class AddStudent(val name: String) : StudentsScreenEvent

    data class UpdateStudent(val student: Student, val newName: String) : StudentsScreenEvent

    data class UpdateSortType(val sortType: SortType) : StudentsScreenEvent

    data class UpdateSelectedMonth(val month: LocalDate?) : StudentsScreenEvent

    data object ClearMonthFilter : StudentsScreenEvent

    // Dialogs
    data class PrepareImportSelectionDialog(val uri: Uri?) : StudentsScreenEvent

    data object CloseImportSelectionDialog : StudentsScreenEvent

    data class ToggleImportSelection(val parsedStudentId: Int) : StudentsScreenEvent

    data object PerformImport : StudentsScreenEvent

    // Toast
    data object ConsumeToastMessage : StudentsScreenEvent

    // Dialog visibility control from screen
    data class ToggleStudentSelection(val studentId: Int) : StudentsScreenEvent

    data object ToggleSelectionMode : StudentsScreenEvent

    data object SelectAllStudents : StudentsScreenEvent

    data object ClearSelection : StudentsScreenEvent

    data object DeleteSelectedStudents : StudentsScreenEvent

    data class ExportSelectedStudents(val uri: Uri) : StudentsScreenEvent

    data class ExportAndDeleteSelectedStudents(val uri: Uri) : StudentsScreenEvent

    data object ShowBulkDeleteDialog : StudentsScreenEvent

    data object DismissBulkDeleteDialog : StudentsScreenEvent

    data class ShowStudentDialog(val student: Student?, val show: Boolean = false) : StudentsScreenEvent
}