package dev.nenoeldeeb.education.absencerecord.presentation.screens.students

import android.net.Uri
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentClass
import dev.nenoeldeeb.education.absencerecord.presentation.screens.components.ClassFilter

sealed interface StudentsScreenEvent {
    // UI Interactions
    data class UpdateNewStudentName(val name: String) : StudentsScreenEvent

    data class AddStudent(val name: String, val classId: Int?) : StudentsScreenEvent

    data class UpdateStudent(val student: Student, val newName: String, val newClassId: Int?) :
        StudentsScreenEvent

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

    data object ToggleStudentsSelection : StudentsScreenEvent

    data object DeleteSelectedStudents : StudentsScreenEvent

    data class ExportSelectedStudents(val uri: Uri) : StudentsScreenEvent

    data class ExportAndDeleteSelectedStudents(val uri: Uri) : StudentsScreenEvent

    data object ShowBulkDeleteDialog : StudentsScreenEvent

    data object DismissBulkDeleteDialog : StudentsScreenEvent

    data class ShowStudentDialog(val student: Student?, val show: Boolean = false) :
        StudentsScreenEvent

    // Class management
    data class SelectClassFilter(val filter: ClassFilter) : StudentsScreenEvent

    data object ToggleClassFilterVisibility : StudentsScreenEvent

    data class ToggleClassDropdown(val expanded: Boolean) : StudentsScreenEvent

    data class ShowManageClassesDialog(val show: Boolean) : StudentsScreenEvent

    data class AddClass(val name: String) : StudentsScreenEvent

    data class RenameClass(val studentClass: StudentClass, val newName: String) :
        StudentsScreenEvent

    data class DeleteClass(val studentClass: StudentClass) : StudentsScreenEvent
}
