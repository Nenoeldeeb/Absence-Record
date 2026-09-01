package dev.nenoeldeeb.education.absencerecord.domain.models

import kotlinx.datetime.DayOfWeek

data class AvailableLessonHour(
    val id: Int,
    val weekday: DayOfWeek,
    val startMinutes: Int,
    val maxStudents: Int
)

data class LessonAssignment(
    val id: Int,
    val availableHourId: Int,
    val studentId: Int,
    val weekday: DayOfWeek
)

data class BusyAppointment(
    val id: Int,
    val studentId: Int,
    val weekday: DayOfWeek,
    val startMinutes: Int,
    val durationMinutes: Int
)

data class AssignmentRemovalReport(
    val removedStudentIds: List<Int>
)

data class HourWithOccupancy(
    val hour: AvailableLessonHour,
    val endMinutes: Int,
    val assignedStudentIds: List<Int>,
    val assignedCount: Int,
    val maxStudents: Int,
    val remainingSlots: Int,
    val isFull: Boolean
)

data class StudentScheduleView(
    val studentId: Int,
    val lessons: List<StudentLessonEntry>,
    val busy: List<BusyAppointment>
)

data class StudentLessonEntry(
    val weekday: DayOfWeek,
    val startMinutes: Int,
    val endMinutes: Int
)