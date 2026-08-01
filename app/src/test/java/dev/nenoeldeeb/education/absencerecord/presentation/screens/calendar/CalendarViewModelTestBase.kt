package dev.nenoeldeeb.education.absencerecord.presentation.screens.calendar

import dev.nenoeldeeb.education.absencerecord.MainDispatcherRule
import dev.nenoeldeeb.education.absencerecord.data.repositories.ClassFilterRepositoryImpl
import dev.nenoeldeeb.education.absencerecord.domain.repositories.ClassFilterRepository
import dev.nenoeldeeb.education.absencerecord.domain.usecases.AttendanceUseCases
import dev.nenoeldeeb.education.absencerecord.domain.usecases.ClassManagementUseCases
import dev.nenoeldeeb.education.absencerecord.domain.usecases.StudentManagementUseCases
import dev.nenoeldeeb.education.absencerecord.domain.usecases.attendance.DeleteStudentAttendanceUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.attendance.GetAttendanceForDateUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.attendance.GetAvailableMonthsUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.attendance.RecordStudentAttendanceUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
open class CalendarViewModelTestBase {
    companion object {
        @JvmStatic
        @RegisterExtension
        val mainDispatcherRule = MainDispatcherRule()
    }

    protected lateinit var getAvailableMonthsUseCase: GetAvailableMonthsUseCase
    protected lateinit var getAttendanceForDateUseCase: GetAttendanceForDateUseCase
    protected lateinit var recordStudentAttendanceUseCase: RecordStudentAttendanceUseCase
    protected lateinit var deleteStudentAttendanceUseCase: DeleteStudentAttendanceUseCase
    protected lateinit var attendanceUseCases: AttendanceUseCases
    protected lateinit var studentManagementUseCases: StudentManagementUseCases
    protected lateinit var viewModel: CalendarViewModel

    fun commonSetUp() {
        getAvailableMonthsUseCase = mockk(relaxed = true)
        getAttendanceForDateUseCase = mockk(relaxed = true)
        recordStudentAttendanceUseCase = mockk(relaxed = true)
        deleteStudentAttendanceUseCase = mockk(relaxed = true)

        attendanceUseCases = mockk(relaxed = true)
        every { attendanceUseCases.recordStudentAttendanceUseCase } returns recordStudentAttendanceUseCase
        every { attendanceUseCases.deleteStudentAttendanceUseCase } returns deleteStudentAttendanceUseCase
        every { attendanceUseCases.getAttendanceForDateUseCase } returns getAttendanceForDateUseCase
        every { attendanceUseCases.getAvailableMonthsUseCase } returns getAvailableMonthsUseCase

        studentManagementUseCases = mockk(relaxed = true)
    }

    protected fun createViewModel(classFilterRepository: ClassFilterRepository = ClassFilterRepositoryImpl()) {
        viewModel =
            CalendarViewModel(
                attendanceUseCases,
                studentManagementUseCases,
                mockk<ClassManagementUseCases>(relaxed = true),
                classFilterRepository
            )
    }
}