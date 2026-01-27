package dev.nenoeldeeb.education.absencerecord.domain.usecases

import dev.nenoeldeeb.education.absencerecord.domain.repositories.AttendanceRepository
import dev.nenoeldeeb.education.absencerecord.domain.usecases.attendance.DeleteStudentAttendanceUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.attendance.GetAttendanceForDateUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.attendance.GetAttendanceHistoryForDateRangeUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.attendance.GetAvailableMonthsUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.attendance.GetStudentAttendanceDatesUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.attendance.RecordStudentAttendanceUseCase

data class AttendanceUseCases(private val attendanceRepository: AttendanceRepository) {
    val recordStudentAttendanceUseCase: RecordStudentAttendanceUseCase = RecordStudentAttendanceUseCase(attendanceRepository)
    val deleteStudentAttendanceUseCase: DeleteStudentAttendanceUseCase = DeleteStudentAttendanceUseCase(attendanceRepository)
    val getAttendanceForDateUseCase: GetAttendanceForDateUseCase = GetAttendanceForDateUseCase(attendanceRepository)
    val getStudentAttendanceDatesUseCase: GetStudentAttendanceDatesUseCase =
        GetStudentAttendanceDatesUseCase(attendanceRepository)
    val getAttendanceHistoryForDateRangeUseCase: GetAttendanceHistoryForDateRangeUseCase =
        GetAttendanceHistoryForDateRangeUseCase(attendanceRepository)
    val getAvailableMonthsUseCase: GetAvailableMonthsUseCase = GetAvailableMonthsUseCase(attendanceRepository)
}