package dev.nenoeldeeb.education.absencerecord.presentation.screens.calendar

import androidx.compose.runtime.Stable
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentAttendance
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentClass
import dev.nenoeldeeb.education.absencerecord.presentation.screens.components.ClassFilter
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText
import kotlinx.datetime.LocalDate

@Stable
data class CalendarScreenState(
    val studentsForSelectedDate: List<StudentAttendance> = emptyList(),
    val allStudents: List<Student> = emptyList(),
    val availableClasses: List<StudentClass> = emptyList(),
    val selectedClassFilter: ClassFilter = ClassFilter.All,
    val classDropdownExpanded: Boolean = false,
    val isClassFilterVisible: Boolean = false,
    val selectedDateForDialog: LocalDate? = null,
    val error: UiText? = null
)
