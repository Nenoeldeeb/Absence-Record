package dev.nenoeldeeb.education.absencerecord.presentation.screens.calendar

import androidx.compose.runtime.Stable
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentAttendance
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText
import kotlinx.datetime.LocalDate

@Stable
data class CalendarScreenState(
    val studentsForSelectedDate: List<StudentAttendance> = emptyList(),
    val allStudents: List<Student> = emptyList(),
    val selectedDateForDialog: LocalDate? = null,
    val error: UiText? = null
)