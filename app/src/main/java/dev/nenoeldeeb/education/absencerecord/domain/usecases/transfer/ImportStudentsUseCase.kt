package dev.nenoeldeeb.education.absencerecord.domain.usecases.transfer

import dev.nenoeldeeb.education.absencerecord.domain.models.ImportResult
import dev.nenoeldeeb.education.absencerecord.domain.models.ParsedStudentImportData
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentAttendance
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentExportData
import dev.nenoeldeeb.education.absencerecord.domain.repositories.AttendanceRepository
import dev.nenoeldeeb.education.absencerecord.domain.repositories.StorageRepository
import dev.nenoeldeeb.education.absencerecord.domain.repositories.StudentRepository
import dev.nenoeldeeb.education.absencerecord.domain.services.SerializationService
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import kotlinx.serialization.builtins.ListSerializer
import kotlin.coroutines.cancellation.CancellationException

class ImportStudentsUseCase(
    private val studentRepository: StudentRepository,
    private val attendanceRepository: AttendanceRepository,
    private val storageRepository: StorageRepository,
    private val serializationService: SerializationService
) {
    suspend fun parseFile(uriString: String): Result<List<ParsedStudentImportData>> {
        return try {
            val result = storageRepository.readTextFromUri(uriString)
            result.fold(
                onSuccess = { jsonString ->
                    if (jsonString.isBlank()) {
                        Result.success(emptyList())
                    } else {
                        serializationService
                            .decodeFromString(
                                jsonString,
                                ListSerializer(StudentExportData.serializer())
                            )
                            .fold(
                                onSuccess = { data ->
                                    Result.success(
                                        data.map { ParsedStudentImportData(it) }
                                    )
                                },
                                onFailure = { e -> Result.failure(e) }
                            )
                    }
                },
                onFailure = { e ->
                    Result.failure(e)
                }
            )
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    suspend fun performImport(
        parsedStudents: List<ParsedStudentImportData>,
        selectionMap: Map<Int, Boolean>
    ): Result<ImportResult> {
        return try {
            val studentsToImport =
                parsedStudents.filter { selectionMap[it.id] == true }.map { it.originalData }
            if (studentsToImport.isEmpty()) {
                return Result.failure(IllegalArgumentException())
            }

            var newCount = 0
            var mergedCount = 0
            var datesProcessed = 0
            var datesSkipped = 0

            val existingStudentsResult = studentRepository.getAllStudents().first()
            existingStudentsResult.fold(
                onSuccess = { existingStudents ->
                    val existingMap = existingStudents.associateBy { it.name }

                    studentsToImport.forEach { studentData ->
                        val studentId =
                            existingMap[studentData.name]?.id
                            ?: run {
                                val newId =
                                    studentRepository
                                        .insertStudent(Student(name = studentData.name))
                                        .getOrDefault(0)
                                        .toInt()
                                newCount++
                                newId
                            }

                        if (existingMap[studentData.name] != null) mergedCount++

                        studentData.dates.forEach { dateString ->
                            try {
                                val localDate = LocalDate.parse(dateString)
                                attendanceRepository.insertAttendance(
                                    StudentAttendance(studentId = studentId, date = localDate)
                                )
                                datesProcessed++
                            } catch (e: Exception) {
                                datesSkipped++
                                if (e is CancellationException) throw e
                            }
                        }
                    }
                },
                onFailure = {
                    return Result.failure(it)
                }
            )

            Result.success(
                ImportResult(
                    newCount,
                    mergedCount,
                    datesProcessed,
                    datesSkipped
                )
            )
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }
}
