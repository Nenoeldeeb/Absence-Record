package dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail

import android.net.Uri
import androidx.compose.runtime.Stable
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentClass
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText
import kotlinx.datetime.LocalDate

@Stable
data class StudentDetailScreenState(
    val student: Student? = null,
    val assignedClassName: String? = null,
    val availableClasses: List<StudentClass> = emptyList(),
    val allAttendanceDates: List<LocalDate> = emptyList(),
    val selectedMonth: LocalDate? = null,
    val isEditNameDialogOpen: Boolean = false,
    val isChangeClassDialogOpen: Boolean = false,
    val isDeleteConfirmationDialogOpen: Boolean = false,
    val isLoading: Boolean = true,
    val error: UiText? = null,
    val toastMessage: UiText? = null,
    val shareFileUri: Uri? = null
)