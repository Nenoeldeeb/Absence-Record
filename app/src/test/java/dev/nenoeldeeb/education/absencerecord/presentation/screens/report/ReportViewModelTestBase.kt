package dev.nenoeldeeb.education.absencerecord.presentation.screens.report

import android.net.Uri
import dev.nenoeldeeb.education.absencerecord.MainDispatcherRule
import dev.nenoeldeeb.education.absencerecord.data.repositories.ClassFilterRepositoryImpl
import dev.nenoeldeeb.education.absencerecord.domain.repositories.ClassFilterRepository
import dev.nenoeldeeb.education.absencerecord.domain.usecases.AttendanceUseCases
import dev.nenoeldeeb.education.absencerecord.domain.usecases.ClassManagementUseCases
import dev.nenoeldeeb.education.absencerecord.domain.usecases.ReportUseCases
import dev.nenoeldeeb.education.absencerecord.domain.usecases.StudentManagementUseCases
import dev.nenoeldeeb.education.absencerecord.domain.usecases.attendance.GetAttendanceHistoryForDateRangeUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.attendance.GetStudentAttendanceDatesUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.report.ShareReportUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
open class ReportViewModelTestBase {
    companion object {
        @JvmStatic
        @RegisterExtension
        val mainDispatcherRule = MainDispatcherRule()
    }

    protected lateinit var studentManagementUseCases: StudentManagementUseCases
    protected lateinit var attendanceUseCases: AttendanceUseCases
    protected lateinit var reportUseCases: ReportUseCases
    protected lateinit var getStudentAttendanceDatesUseCase: GetStudentAttendanceDatesUseCase
    protected lateinit var getAttendanceHistoryForDateRangeUseCase: GetAttendanceHistoryForDateRangeUseCase
    protected lateinit var shareReportUseCase: ShareReportUseCase
    protected lateinit var viewModel: ReportViewModel

    fun commonSetUp() {
        studentManagementUseCases = mockk(relaxed = true)

        getStudentAttendanceDatesUseCase = mockk(relaxed = true)
        getAttendanceHistoryForDateRangeUseCase = mockk(relaxed = true)

        attendanceUseCases = mockk(relaxed = true)
        every { attendanceUseCases.getStudentAttendanceDatesUseCase } returns getStudentAttendanceDatesUseCase
        every { attendanceUseCases.getAttendanceHistoryForDateRangeUseCase } returns getAttendanceHistoryForDateRangeUseCase
        every { attendanceUseCases.getAvailableMonthsUseCase } returns mockk(relaxed = true)

        shareReportUseCase = mockk(relaxed = true)
        reportUseCases = mockk(relaxed = true)
        every { reportUseCases.shareReportUseCase } returns shareReportUseCase

        mockkStatic(Uri::class)
        every { Uri.parse(any()) } returns mockk()
    }

    fun commonTearDown() {
        unmockkStatic(Uri::class)
    }

    protected fun createViewModel(classFilterRepository: ClassFilterRepository = mockk(relaxed = true)) {
        val repository =
            if (classFilterRepository is ClassFilterRepositoryImpl) {
                classFilterRepository
            } else {
                classFilterRepository.also { repo ->
                    every { repo.selectedClassIds } returns MutableStateFlow(emptySet())
                }
            }
        viewModel =
            ReportViewModel(
                studentManagementUseCases,
                attendanceUseCases,
                reportUseCases,
                mockk<ClassManagementUseCases>(relaxed = true),
                repository
            )
    }
}