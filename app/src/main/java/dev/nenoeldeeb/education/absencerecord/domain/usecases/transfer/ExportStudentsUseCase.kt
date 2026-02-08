package dev.nenoeldeeb.education.absencerecord.domain.usecases.transfer

import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentExportData
import dev.nenoeldeeb.education.absencerecord.domain.repositories.AttendanceRepository
import dev.nenoeldeeb.education.absencerecord.domain.repositories.StorageRepository
import dev.nenoeldeeb.education.absencerecord.domain.services.SerializationService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.builtins.ListSerializer

/**
 * Use case for exporting selected students and their attendance data to JSON format.
 *
 * @property attendanceRepository Repository for accessing attendance records
 * @property storageRepository Repository for writing data to storage
 * @property serializationService Service for JSON serialization
 */
class ExportStudentsUseCase(
    private val attendanceRepository: AttendanceRepository,
    private val storageRepository: StorageRepository,
    private val serializationService: SerializationService
) {
    /**
     * Exports selected students with their attendance history to a JSON file.
     *
     * @param uriString URI string where the export file will be written
     * @param selectedStudentIds Set of student IDs to export
     * @param allStudents List of all students to lookup student details
     * @return Flow emitting Result<Unit> indicating success or failure
     */
    operator fun invoke(
        uriString: String,
        selectedStudentIds: Set<Int>,
        allStudents: List<Student>
    ): Flow<Result<Unit>> = flow {
        if (selectedStudentIds.isEmpty()) {
            emit(Result.failure(IllegalArgumentException()))
            return@flow
        }

        try {
            val exportList =
                selectedStudentIds.mapNotNull { studentId ->
                    allStudents.find { it.id == studentId }?.let { student ->
                        val historyDates =
                            attendanceRepository
                                .getStudentAttendanceDates(studentId)
                                .first()
                                .getOrDefault(emptyList())
                        StudentExportData(student.name, historyDates.map { it.toString() })
                    }
                }
            serializationService
                .encodeToString(exportList, ListSerializer(StudentExportData.serializer()))
                .fold(
                    onSuccess = { jsonString ->
                        storageRepository.writeTextToUri(uriString, jsonString).collect {
                            emit(it)
                        }
                    },
                    onFailure = { e -> emit(Result.failure(e)) }
                )
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(Result.failure(e))
        }
    }
}
