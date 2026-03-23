package dev.nenoeldeeb.education.absencerecord.domain.usecases.transfer

import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentExportData
import dev.nenoeldeeb.education.absencerecord.domain.repositories.AttendanceRepository
import dev.nenoeldeeb.education.absencerecord.domain.repositories.StorageRepository
import dev.nenoeldeeb.education.absencerecord.domain.repositories.StudentClassRepository
import dev.nenoeldeeb.education.absencerecord.domain.services.SerializationService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.serialization.builtins.ListSerializer

/**
 * Use case for exporting selected students and their attendance data to JSON format.
 *
 * @property attendanceRepository Repository for accessing attendance records
 * @property storageRepository Repository for writing data to storage
 * @property serializationService Service for JSON serialization
 * @property studentClassRepository Repository for accessing class information
 */
class ExportStudentsUseCase(
    private val attendanceRepository: AttendanceRepository,
    private val storageRepository: StorageRepository,
    private val serializationService: SerializationService,
    private val studentClassRepository: StudentClassRepository
) {
    /**
     * Exports selected students with their attendance history to a JSON file.
     *
     * @param uriString URI string where the export file will be written
     * @param selectedStudentIds Set of student IDs to export
     * @param allStudents List of all students to lookup student details
     * @return Result<Unit> indicating success or failure
     */
    suspend operator fun invoke(
        uriString: String,
        selectedStudentIds: Set<Int>,
        allStudents: List<Student>
    ): Result<Unit> {
        if (selectedStudentIds.isEmpty()) {
            return Result.failure(IllegalArgumentException())
        }

        return try {
            val classes = studentClassRepository.getAllClasses().first().getOrDefault(emptyList())
            val classMap = classes.associate { it.id to it.name }

            val exportList =
                selectedStudentIds.mapNotNull { studentId ->
                    allStudents.find { it.id == studentId }?.let { student ->
                        val historyDates =
                            attendanceRepository
                                .getStudentAttendanceDates(studentId)
                                .first()
                                .getOrDefault(emptyList())
                        StudentExportData(
                            name = student.name,
                            dates = historyDates.map { it.toString() },
                            className = student.classId?.let { classMap[it] } ?: ""
                        )
                    }
                }
            val serializationResult =
                serializationService
                    .encodeToString(exportList, ListSerializer(StudentExportData.serializer()))

            when {
                serializationResult.isSuccess -> {
                    val jsonString =
                        serializationResult.getOrNull() ?: return Result.failure(
                            IllegalStateException("Serialization succeeded but returned null")
                        )
                    storageRepository.writeTextToUri(uriString, jsonString)
                }

                else -> Result.failure(serializationResult.exceptionOrNull() ?: Exception("Unknown error"))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }
}