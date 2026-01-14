package dev.nenoeldeeb.education.absencerecord.app

import android.content.Context
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.AppDatabase
import dev.nenoeldeeb.education.absencerecord.data.repositories.AttendanceRepositoryImpl
import dev.nenoeldeeb.education.absencerecord.data.repositories.ReportRepositoryImpl
import dev.nenoeldeeb.education.absencerecord.data.repositories.StorageRepositoryImpl
import dev.nenoeldeeb.education.absencerecord.data.repositories.StudentRepositoryImpl
import dev.nenoeldeeb.education.absencerecord.data.utils.DefaultDispatcherProvider
import dev.nenoeldeeb.education.absencerecord.data.utils.JsonSerializationService
import dev.nenoeldeeb.education.absencerecord.domain.repositories.AttendanceRepository
import dev.nenoeldeeb.education.absencerecord.domain.repositories.ReportRepository
import dev.nenoeldeeb.education.absencerecord.domain.repositories.StorageRepository
import dev.nenoeldeeb.education.absencerecord.domain.repositories.StudentRepository
import dev.nenoeldeeb.education.absencerecord.domain.services.DispatcherProvider
import dev.nenoeldeeb.education.absencerecord.domain.services.SerializationService
import dev.nenoeldeeb.education.absencerecord.domain.usecases.AttendanceUseCases
import dev.nenoeldeeb.education.absencerecord.domain.usecases.ReportUseCases
import dev.nenoeldeeb.education.absencerecord.domain.usecases.StudentManagementUseCases
import dev.nenoeldeeb.education.absencerecord.domain.usecases.attendance.DeleteStudentAttendanceUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.attendance.GetAttendanceForDateUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.attendance.GetAttendanceHistoryForDateRangeUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.attendance.GetAvailableMonthsUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.attendance.GetStudentAttendanceDatesUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.attendance.RecordStudentAttendanceUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.report.ShareReportUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.student.AddStudentUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.student.DeleteStudentsUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.student.GetAllStudentsUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.student.UpdateStudentUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.transfer.ExportStudentsUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.transfer.ImportStudentsUseCase

interface AppContainer {
    val studentManagementUseCases: StudentManagementUseCases
    val attendanceUseCases: AttendanceUseCases
    val reportUseCases: ReportUseCases
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    private val dispatcherProvider: DispatcherProvider by lazy {
        DefaultDispatcherProvider()
    }

    private val studentRepository: StudentRepository by lazy {
        StudentRepositoryImpl(AppDatabase.getDatabase(context).studentDao())
    }

    private val attendanceRepository: AttendanceRepository by lazy {
        AttendanceRepositoryImpl(AppDatabase.getDatabase(context).attendanceDao())
    }

    private val storageRepository: StorageRepository by lazy {
        StorageRepositoryImpl(context)
    }

    private val reportRepository: ReportRepository by lazy {
        ReportRepositoryImpl(context, dispatcherProvider)
    }

    private val serializationService: SerializationService by lazy {
        JsonSerializationService()
    }

    override val studentManagementUseCases: StudentManagementUseCases by lazy {
        StudentManagementUseCases(
            addStudentUseCase = AddStudentUseCase(studentRepository),
            updateStudentUseCase = UpdateStudentUseCase(studentRepository),
            deleteStudentsUseCase = DeleteStudentsUseCase(studentRepository),
            getAllStudentsUseCase = GetAllStudentsUseCase(studentRepository),
            importStudentsUseCase =
                ImportStudentsUseCase(
                    studentRepository,
                    attendanceRepository,
                    storageRepository,
                    serializationService
                ),
            exportStudentsUseCase =
                ExportStudentsUseCase(
                    studentRepository,
                    attendanceRepository,
                    storageRepository,
                    serializationService
                )
        )
    }

    override val attendanceUseCases: AttendanceUseCases by lazy {
        AttendanceUseCases(
            recordStudentAttendanceUseCase = RecordStudentAttendanceUseCase(attendanceRepository),
            deleteStudentAttendanceUseCase = DeleteStudentAttendanceUseCase(attendanceRepository),
            getAttendanceForDateUseCase = GetAttendanceForDateUseCase(attendanceRepository),
            getStudentAttendanceDatesUseCase = GetStudentAttendanceDatesUseCase(attendanceRepository),
            getAttendanceHistoryForDateRangeUseCase = GetAttendanceHistoryForDateRangeUseCase(attendanceRepository),
            getAvailableMonthsUseCase = GetAvailableMonthsUseCase(attendanceRepository)
        )
    }

    override val reportUseCases: ReportUseCases by lazy {
        ReportUseCases(
            shareReportUseCase = ShareReportUseCase(reportRepository)
        )
    }
}