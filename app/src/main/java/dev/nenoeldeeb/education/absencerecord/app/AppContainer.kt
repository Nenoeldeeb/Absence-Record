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
        StorageRepositoryImpl(
            context,
            dispatcherProvider
        )
    }

    private val reportRepository: ReportRepository by lazy {
        ReportRepositoryImpl(context, dispatcherProvider)
    }

    private val serializationService: SerializationService by lazy {
        JsonSerializationService()
    }

    override val studentManagementUseCases: StudentManagementUseCases by lazy {
        StudentManagementUseCases(
            studentRepository = studentRepository,
            attendanceRepository = attendanceRepository,
            storageRepository = storageRepository,
            serializationService = serializationService
        )
    }

    override val attendanceUseCases: AttendanceUseCases by lazy {
        AttendanceUseCases(attendanceRepository = attendanceRepository)
    }

    override val reportUseCases: ReportUseCases by lazy {
        ReportUseCases(reportRepository = reportRepository)
    }
}