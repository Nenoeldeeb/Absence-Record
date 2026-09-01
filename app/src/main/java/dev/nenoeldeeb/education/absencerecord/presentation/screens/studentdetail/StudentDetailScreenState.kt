package dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail

import android.net.Uri
import androidx.compose.runtime.Stable
import dev.nenoeldeeb.education.absencerecord.domain.models.BusyAppointment
import dev.nenoeldeeb.education.absencerecord.domain.models.HourWithOccupancy
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentClass
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentLessonEntry
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentScheduleView
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

@Stable
data class StudentDetailScreenState(
    val student: Student? = null,
    val assignedClassName: String? = null,
    val availableClasses: List<StudentClass> = emptyList(),
    val allAttendanceDates: List<LocalDate> = emptyList(),
    val selectedMonth: LocalDate? = null,
    val studentSchedule: StudentScheduleView? = null,
    val selectedScheduleWeekday: DayOfWeek = DayOfWeek.SATURDAY,
    val selectedScheduleTab: StudentScheduleTab = StudentScheduleTab.Lessons,
    val hoursForWeekday: List<HourWithOccupancy> = emptyList(),
    val isAddLessonDialogOpen: Boolean = false,
    val selectedLessonHourId: Int? = null,
    val isBusyDialogOpen: Boolean = false,
    val editingBusyAppointment: BusyAppointment? = null,
    val busyStartMinutes: Int = 0,
    val busyDurationMinutes: Int = 30,
    val busyValidationError: UiText? = null,
    val busyConflictLessons: List<StudentLessonEntry> = emptyList(),
    val isEditNameDialogOpen: Boolean = false,
    val isChangeClassDialogOpen: Boolean = false,
    val isDeleteConfirmationDialogOpen: Boolean = false,
    val isLoading: Boolean = true,
    val error: UiText? = null,
    val toastMessage: UiText? = null,
    val shareFileUri: Uri? = null
)

enum class StudentScheduleTab {
    Lessons,
    Busy
}