package dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail

import android.net.Uri
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText
import kotlinx.datetime.LocalDate

sealed interface StudentDetailScreenEvent {
    data object ToggleEditNameDialog : StudentDetailScreenEvent

    data class UpdateStudentName(val newName: String) : StudentDetailScreenEvent

    data object ToggleChangeClassDialog : StudentDetailScreenEvent

    data class UpdateStudentClass(val classId: Int?) : StudentDetailScreenEvent

    data object ToggleDeleteConfirmationDialog : StudentDetailScreenEvent

    data object ConfirmDeleteStudent : StudentDetailScreenEvent

    data class SelectMonth(val month: LocalDate) : StudentDetailScreenEvent

    data object ShareAttendanceReport : StudentDetailScreenEvent

    data class ShareFileResult(val uri: Uri, val error: UiText?) : StudentDetailScreenEvent

    data object ConsumeError : StudentDetailScreenEvent

    data object ConsumeToastMessage : StudentDetailScreenEvent
}