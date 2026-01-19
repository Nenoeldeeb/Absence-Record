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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.LocalDate
import kotlinx.serialization.builtins.ListSerializer
import kotlin.coroutines.cancellation.CancellationException

class ImportStudentsUseCase(
    private val studentRepository: StudentRepository,
    private val attendanceRepository: AttendanceRepository,
    private val storageRepository: StorageRepository,
    private val serializationService: SerializationService
) {
    fun parseFile(uriString: String): Flow<Result<List<ParsedStudentImportData>>> =
        flow {
            storageRepository.readTextFromUri(uriString).collect { result ->
                if (result.isSuccess) {
                    val jsonString = result.getOrThrow()
                    if (jsonString.isBlank()) {
                        emit(Result.success(emptyList()))
                    } else {
                        serializationService
                            .decodeFromString(
                                jsonString,
                                ListSerializer(StudentExportData.serializer())
                            )
                            .fold(
                                onSuccess = { data ->
                                    emit(
                                        Result.success(
                                            data.map { ParsedStudentImportData(it) }
                                        )
                                    )
                                },
                                onFailure = { e -> emit(Result.failure(e)) }
                            )
                    }
                } else {
                    emit(
                        Result.failure(
                            result.exceptionOrNull() ?: Exception("Unknown error reading file")
                        )
                    )
                }
            }
        }

    fun performImport(
        parsedStudents: List<ParsedStudentImportData>,
        selectionMap: Map<Int, Boolean>
    ): Flow<Result<ImportResult>> =
        flow {
            val studentsToImport =
                parsedStudents.filter { selectionMap[it.id] == true }.map { it.originalData }
            if (studentsToImport.isEmpty()) {
                emit(Result.failure(IllegalArgumentException("No students selected for import")))
                return@flow
            }

            var newCount = 0
            var mergedCount = 0
            var datesProcessed = 0
            var datesSkipped = 0

            try {
                val existingStudentsResult = studentRepository.getAllStudents().first()
                if (existingStudentsResult.isFailure) throw existingStudentsResult.exceptionOrNull()!!
                val existingStudents = existingStudentsResult.getOrNull()!!
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
                        } catch (_: Exception) {
                            datesSkipped++
                        }
                    }
                }
                emit(Result.success(ImportResult(newCount, mergedCount, datesProcessed, datesSkipped)))
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                emit(Result.failure(e))
            }
        }
}