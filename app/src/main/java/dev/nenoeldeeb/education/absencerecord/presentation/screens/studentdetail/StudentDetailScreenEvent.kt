package dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail

import android.net.Uri
import dev.nenoeldeeb.education.absencerecord.domain.models.BusyAppointment
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentLessonEntry
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText
import kotlinx.datetime.DayOfWeek
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

    data class SelectScheduleWeekday(val weekday: DayOfWeek) : StudentDetailScreenEvent

    data class SelectScheduleTab(val tab: StudentScheduleTab) : StudentDetailScreenEvent

    data object OpenAddLessonDialog : StudentDetailScreenEvent

    data object DismissAddLessonDialog : StudentDetailScreenEvent

    data class SelectLessonHour(val hourId: Int) : StudentDetailScreenEvent

    data object ConfirmAddLesson : StudentDetailScreenEvent

    data class UnassignLesson(val lesson: StudentLessonEntry) : StudentDetailScreenEvent

    data class OpenBusyDialog(val busy: BusyAppointment?) : StudentDetailScreenEvent

    data object DismissBusyDialog : StudentDetailScreenEvent

    data class SetBusyStart(val minutes: Int) : StudentDetailScreenEvent

    data class SetBusyDuration(val duration: Int) : StudentDetailScreenEvent

    data object SaveBusyAppointment : StudentDetailScreenEvent

    data object ConfirmBusyConflict : StudentDetailScreenEvent

    data class DeleteBusyAppointment(val id: Int) : StudentDetailScreenEvent

    data object ConsumeError : StudentDetailScreenEvent

    data object ConsumeToastMessage : StudentDetailScreenEvent
}