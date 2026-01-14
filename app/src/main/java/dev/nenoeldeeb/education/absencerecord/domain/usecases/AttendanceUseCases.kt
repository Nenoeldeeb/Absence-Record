package dev.nenoeldeeb.education.absencerecord.domain.usecases

import dev.nenoeldeeb.education.absencerecord.domain.usecases.attendance.DeleteStudentAttendanceUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.attendance.GetAttendanceForDateUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.attendance.GetAttendanceHistoryForDateRangeUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.attendance.GetAvailableMonthsUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.attendance.GetStudentAttendanceDatesUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.attendance.RecordStudentAttendanceUseCase

data class AttendanceUseCases(
    val recordStudentAttendanceUseCase: RecordStudentAttendanceUseCase,
    val deleteStudentAttendanceUseCase: DeleteStudentAttendanceUseCase,
    val getAttendanceForDateUseCase: GetAttendanceForDateUseCase,
    val getStudentAttendanceDatesUseCase: GetStudentAttendanceDatesUseCase,
    val getAttendanceHistoryForDateRangeUseCase: GetAttendanceHistoryForDateRangeUseCase,
    val getAvailableMonthsUseCase: GetAvailableMonthsUseCase
)