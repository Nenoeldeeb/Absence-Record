package dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail

import dev.nenoeldeeb.education.absencerecord.MainDispatcherRule
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentClass
import dev.nenoeldeeb.education.absencerecord.domain.usecases.AttendanceUseCases
import dev.nenoeldeeb.education.absencerecord.domain.usecases.ClassManagementUseCases
import dev.nenoeldeeb.education.absencerecord.domain.usecases.ReportUseCases
import dev.nenoeldeeb.education.absencerecord.domain.usecases.StudentManagementUseCases
import dev.nenoeldeeb.education.absencerecord.domain.usecases.attendance.GetAttendanceHistoryForDateRangeUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.attendance.GetStudentAttendanceDatesUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.report.ShareReportUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.schedule.AssignStudentUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.schedule.DeleteBusyAppointmentUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.schedule.InsertBusyAppointmentUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.schedule.ObserveHoursForWeekdayUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.schedule.ObserveStudentScheduleUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.schedule.ScheduleUseCases
import dev.nenoeldeeb.education.absencerecord.domain.usecases.schedule.UnassignStudentUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.schedule.UpdateBusyAppointmentUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.student.DeleteStudentsUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.student.GetStudentByIdUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.student.UpdateStudentUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
open class StudentDetailViewModelTestBase {
    companion object {
        @JvmStatic
        @RegisterExtension
        val mainDispatcherRule = MainDispatcherRule()
    }

    protected lateinit var studentManagementUseCases: StudentManagementUseCases
    protected lateinit var getStudentByIdUseCase: GetStudentByIdUseCase
    protected lateinit var updateStudentUseCase: UpdateStudentUseCase
    protected lateinit var deleteStudentsUseCase: DeleteStudentsUseCase
    protected lateinit var attendanceUseCases: AttendanceUseCases
    protected lateinit var getStudentAttendanceDatesUseCase: GetStudentAttendanceDatesUseCase
    protected lateinit var getAttendanceHistoryForDateRangeUseCase: GetAttendanceHistoryForDateRangeUseCase
    protected lateinit var reportUseCases: ReportUseCases
    protected lateinit var shareReportUseCase: ShareReportUseCase
    protected lateinit var classManagementUseCases: ClassManagementUseCases
    protected lateinit var scheduleUseCases: ScheduleUseCases
    protected lateinit var observeStudentScheduleUseCase: ObserveStudentScheduleUseCase
    protected lateinit var observeHoursForWeekdayUseCase: ObserveHoursForWeekdayUseCase
    protected lateinit var assignStudentUseCase: AssignStudentUseCase
    protected lateinit var unassignStudentUseCase: UnassignStudentUseCase
    protected lateinit var insertBusyAppointmentUseCase: InsertBusyAppointmentUseCase
    protected lateinit var updateBusyAppointmentUseCase: UpdateBusyAppointmentUseCase
    protected lateinit var deleteBusyAppointmentUseCase: DeleteBusyAppointmentUseCase
    protected lateinit var viewModel: StudentDetailViewModel

    fun commonSetUp() {
        getStudentByIdUseCase = mockk(relaxed = true)
        updateStudentUseCase = mockk(relaxed = true)
        deleteStudentsUseCase = mockk(relaxed = true)
        studentManagementUseCases = mockk(relaxed = true)
        every { studentManagementUseCases.getStudentByIdUseCase } returns getStudentByIdUseCase
        every { studentManagementUseCases.updateStudentUseCase } returns updateStudentUseCase
        every { studentManagementUseCases.deleteStudentsUseCase } returns deleteStudentsUseCase
        every { getStudentByIdUseCase(any()) } returns flowOf(Result.success(null))

        getStudentAttendanceDatesUseCase = mockk(relaxed = true)
        getAttendanceHistoryForDateRangeUseCase = mockk(relaxed = true)
        attendanceUseCases = mockk(relaxed = true)
        every { attendanceUseCases.getStudentAttendanceDatesUseCase } returns getStudentAttendanceDatesUseCase
        every { attendanceUseCases.getAttendanceHistoryForDateRangeUseCase } returns
            getAttendanceHistoryForDateRangeUseCase
        every { getStudentAttendanceDatesUseCase(any()) } returns flowOf(Result.success(emptyList()))

        reportUseCases = mockk(relaxed = true)
        shareReportUseCase = mockk(relaxed = true)
        every { reportUseCases.shareReportUseCase } returns shareReportUseCase

        classManagementUseCases = mockk(relaxed = true)
        every { classManagementUseCases.getAllClassesUseCase() } returns
            flowOf(Result.success(emptyList<StudentClass>()))

        scheduleUseCases = mockk(relaxed = true)

        observeStudentScheduleUseCase = mockk(relaxed = true)
        observeHoursForWeekdayUseCase = mockk(relaxed = true)
        assignStudentUseCase = mockk(relaxed = true)
        unassignStudentUseCase = mockk(relaxed = true)
        insertBusyAppointmentUseCase = mockk(relaxed = true)
        updateBusyAppointmentUseCase = mockk(relaxed = true)
        deleteBusyAppointmentUseCase = mockk(relaxed = true)

        every { scheduleUseCases.observeStudentScheduleUseCase } returns observeStudentScheduleUseCase
        every { scheduleUseCases.observeHoursForWeekdayUseCase } returns observeHoursForWeekdayUseCase
        every { scheduleUseCases.assignStudentUseCase } returns assignStudentUseCase
        every { scheduleUseCases.unassignStudentUseCase } returns unassignStudentUseCase
        every { scheduleUseCases.insertBusyAppointmentUseCase } returns insertBusyAppointmentUseCase
        every { scheduleUseCases.updateBusyAppointmentUseCase } returns updateBusyAppointmentUseCase
        every { scheduleUseCases.deleteBusyAppointmentUseCase } returns deleteBusyAppointmentUseCase
    }

    protected fun createViewModel(studentId: Int = 1) {
        viewModel =
            StudentDetailViewModel(
                studentId,
                studentManagementUseCases,
                attendanceUseCases,
                reportUseCases,
                classManagementUseCases,
                scheduleUseCases
            )
    }

    protected val aStudent: Student get() = Student(id = 1, name = "S1", classId = 2)
    protected val aDate: LocalDate get() = LocalDate(2023, 1, 15)
}