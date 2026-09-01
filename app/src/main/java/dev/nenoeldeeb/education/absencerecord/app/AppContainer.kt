package dev.nenoeldeeb.education.absencerecord.app

import android.content.Context
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.AppDatabase
import dev.nenoeldeeb.education.absencerecord.data.repositories.AttendanceRepositoryImpl
import dev.nenoeldeeb.education.absencerecord.data.repositories.ClassFilterRepositoryImpl
import dev.nenoeldeeb.education.absencerecord.data.repositories.ReportRepositoryImpl
import dev.nenoeldeeb.education.absencerecord.data.repositories.ScheduleRepositoryImpl
import dev.nenoeldeeb.education.absencerecord.data.repositories.StorageRepositoryImpl
import dev.nenoeldeeb.education.absencerecord.data.repositories.StudentClassRepositoryImpl
import dev.nenoeldeeb.education.absencerecord.data.repositories.StudentRepositoryImpl
import dev.nenoeldeeb.education.absencerecord.data.services.DefaultDispatcherProvider
import dev.nenoeldeeb.education.absencerecord.data.services.JsonSerializationService
import dev.nenoeldeeb.education.absencerecord.domain.repositories.AttendanceRepository
import dev.nenoeldeeb.education.absencerecord.domain.repositories.ClassFilterRepository
import dev.nenoeldeeb.education.absencerecord.domain.repositories.ReportRepository
import dev.nenoeldeeb.education.absencerecord.domain.repositories.ScheduleRepository
import dev.nenoeldeeb.education.absencerecord.domain.repositories.StorageRepository
import dev.nenoeldeeb.education.absencerecord.domain.repositories.StudentClassRepository
import dev.nenoeldeeb.education.absencerecord.domain.repositories.StudentRepository
import dev.nenoeldeeb.education.absencerecord.domain.services.DispatcherProvider
import dev.nenoeldeeb.education.absencerecord.domain.services.SerializationService
import dev.nenoeldeeb.education.absencerecord.domain.usecases.AttendanceUseCases
import dev.nenoeldeeb.education.absencerecord.domain.usecases.ClassManagementUseCases
import dev.nenoeldeeb.education.absencerecord.domain.usecases.ReportUseCases
import dev.nenoeldeeb.education.absencerecord.domain.usecases.StudentManagementUseCases
import dev.nenoeldeeb.education.absencerecord.domain.usecases.schedule.ScheduleUseCases

interface AppContainer {
    val studentManagementUseCases: StudentManagementUseCases
    val attendanceUseCases: AttendanceUseCases
    val reportUseCases: ReportUseCases
    val classManagementUseCases: ClassManagementUseCases
    val classFilterRepository: ClassFilterRepository
    val scheduleRepository: ScheduleRepository
    val scheduleUseCases: ScheduleUseCases
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    private val dispatcherProvider: DispatcherProvider by lazy { DefaultDispatcherProvider() }

    private val studentRepository: StudentRepository by lazy {
        StudentRepositoryImpl(AppDatabase.getDatabase(context).studentDao())
    }

    private val attendanceRepository: AttendanceRepository by lazy {
        AttendanceRepositoryImpl(AppDatabase.getDatabase(context).attendanceDao())
    }

    private val storageRepository: StorageRepository by lazy {
        StorageRepositoryImpl(context, dispatcherProvider)
    }

    private val reportRepository: ReportRepository by lazy {
        ReportRepositoryImpl(context, dispatcherProvider)
    }

    private val serializationService: SerializationService by lazy { JsonSerializationService() }

    private val studentClassRepository: StudentClassRepository by lazy {
        StudentClassRepositoryImpl(AppDatabase.getDatabase(context).studentClassDao())
    }

    override val classFilterRepository: ClassFilterRepository by lazy {
        ClassFilterRepositoryImpl()
    }

    override val scheduleRepository: ScheduleRepository by lazy {
        ScheduleRepositoryImpl(AppDatabase.getDatabase(context).scheduleDao())
    }

    override val scheduleUseCases: ScheduleUseCases by lazy {
        ScheduleUseCases(scheduleRepository)
    }

    override val studentManagementUseCases: StudentManagementUseCases by lazy {
        StudentManagementUseCases(
            studentRepository = studentRepository,
            attendanceRepository = attendanceRepository,
            storageRepository = storageRepository,
            serializationService = serializationService,
            studentClassRepository = studentClassRepository,
            scheduleRepository = scheduleRepository
        )
    }

    override val attendanceUseCases: AttendanceUseCases by lazy {
        AttendanceUseCases(attendanceRepository = attendanceRepository)
    }

    override val reportUseCases: ReportUseCases by lazy {
        ReportUseCases(reportRepository = reportRepository)
    }

    override val classManagementUseCases: ClassManagementUseCases by lazy {
        ClassManagementUseCases(studentClassRepository = studentClassRepository, studentRepository = studentRepository)
    }
}