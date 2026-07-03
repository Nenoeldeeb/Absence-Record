package dev.nenoeldeeb.education.absencerecord.domain.usecases.transfer

import dev.nenoeldeeb.education.absencerecord.domain.models.ImportResult
import dev.nenoeldeeb.education.absencerecord.domain.models.ParsedStudentImportData
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentAttendance
import dev.nenoeldeeb.education.absencerecord.domain.repositories.AttendanceRepository
import dev.nenoeldeeb.education.absencerecord.domain.repositories.StudentClassRepository
import dev.nenoeldeeb.education.absencerecord.domain.repositories.StudentRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import kotlin.coroutines.cancellation.CancellationException

class PerformImportUseCase(
    private val studentRepository: StudentRepository,
    private val attendanceRepository: AttendanceRepository,
    private val studentClassRepository: StudentClassRepository
) {
    suspend operator fun invoke(
        parsedStudents: List<ParsedStudentImportData>,
        selectionMap: Map<Int, Boolean>
    ): Result<ImportResult> {
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
                val classCache = mutableMapOf<String, Int>()

                studentsToImport.forEach { studentData ->
                    val className = studentData.className.trim()
                    val classId =
                        if (className.isNotBlank()) {
                            if (classCache.containsKey(className)) {
                                classCache[className]
                            } else {
                                val newClassId = studentClassRepository.getOrCreateClassByName(className).getOrNull()
                                if (newClassId != null) {
                                    classCache[className] = newClassId
                                }
                                newClassId
                            }
                        } else {
                            null
                        }

                    val studentId =
                        existingMap[studentData.name]?.let { existingStudent ->
                            if (existingStudent.classId != classId) {
                                studentRepository.updateStudent(existingStudent.copy(classId = classId))
                            }
                            existingStudent.id
                        } ?: run {
                            val newId =
                                studentRepository
                                    .insertStudent(Student(name = studentData.name, classId = classId))
                                    .getOrDefault(0)
                                    .toInt()
                            newCount++
                            newId
                        }

                    if (existingMap[studentData.name] != null) mergedCount++

                    studentData.dates.forEach { dateString ->
                        runCatching { LocalDate.parse(dateString) }
                            .onSuccess { localDate ->
                                attendanceRepository.insertAttendance(
                                    StudentAttendance(studentId = studentId, date = localDate)
                                )
                                datesProcessed++
                            }
                            .onFailure { e ->
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

        return Result.success(
            ImportResult(newCount, mergedCount, datesProcessed, datesSkipped)
        )
    }
}