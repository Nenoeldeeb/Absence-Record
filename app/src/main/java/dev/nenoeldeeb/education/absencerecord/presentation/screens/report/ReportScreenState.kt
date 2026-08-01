package dev.nenoeldeeb.education.absencerecord.presentation.screens.report

import android.net.Uri
import androidx.compose.runtime.Stable
import dev.nenoeldeeb.education.absencerecord.domain.models.SortType
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentClass
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText
import kotlinx.datetime.LocalDate

@Stable
data class ReportScreenState(
    // Core data
    val sortType: SortType = SortType.ByName,
    val selectedMonth: LocalDate? = null,
    val allStudents: List<Student> = emptyList(),
    val availableMonths: List<LocalDate> = emptyList(),
    val availableClasses: List<StudentClass> = emptyList(),
    val selectedStudentForHistory: Student? = null,
    val studentHistory: List<Pair<LocalDate, List<LocalDate>>> = emptyList(),
    val selectedMonthYearForCalendarPreview: LocalDate? = null,
    // Class filter
    val selectedClassIds: Set<Int> = emptySet(),
    val classDropdownExpanded: Boolean = false,
    val isClassFilterVisible: Boolean = false,
    val isSortComponentsVisible: Boolean = false,
    // UI state
    val showHistoryDialog: Boolean = false,
    val showCalendarPreviewDialog: Boolean = false,
    val monthDropdownExpanded: Boolean = false,
    // Temporary data
    val shareFileUri: Uri? = null,
    val toastMessage: UiText? = null,
    val error: UiText? = null
)