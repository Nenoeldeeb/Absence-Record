package dev.nenoeldeeb.education.absencerecord.domain.usecases.transfer

import dev.nenoeldeeb.education.absencerecord.domain.models.ImportResult
import dev.nenoeldeeb.education.absencerecord.domain.models.ParsedImportData
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentAttendance
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError
import dev.nenoeldeeb.education.absencerecord.domain.repositories.AttendanceRepository
import dev.nenoeldeeb.education.absencerecord.domain.repositories.ScheduleRepository
import dev.nenoeldeeb.education.absencerecord.domain.repositories.StudentClassRepository
import dev.nenoeldeeb.education.absencerecord.domain.repositories.StudentRepository
import dev.nenoeldeeb.education.absencerecord.domain.services.ImportCounters
import dev.nenoeldeeb.education.absencerecord.domain.services.ImportMergeEngine
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlin.coroutines.cancellation.CancellationException

class PerformImportUseCase(
    private val studentRepository: StudentRepository,
    private val attendanceRepository: AttendanceRepository,
    private val studentClassRepository: StudentClassRepository,
    private val scheduleRepository: ScheduleRepository
) {
    private val mergeEngine = ImportMergeEngine(scheduleRepository)

    suspend operator fun invoke(
        parsedData: ParsedImportData,
        selectionMap: Map<Int, Boolean>
    ): Result<ImportResult> {
        val studentsToImport =
            parsedData.students
                .filter { selectionMap[it.id] == true }
                .map { it.originalData }
        if (studentsToImport.isEmpty()) {
            return Result.failure(StudentError.Validation("No students selected for import"))
        }

        val counters = ImportCounters()
        val existingHours =
            scheduleRepository.observeHours().first().getOrElse { return Result.failure(it) }
        val existingBusy =
            scheduleRepository.observeBusyAppointments().first().getOrElse { return Result.failure(it) }

        val hourIdByKey =
            existingHours.associateTo(mutableMapOf()) { hour ->
                (hour.weekday.isoDayNumber to hour.startMinutes) to hour.id
            }
        val mutableHours = existingHours.toMutableList()

        mergeEngine.mergeAvailableHours(parsedData.availableHours, hourIdByKey, mutableHours, counters)

        val existingStudentsResult = studentRepository.getAllStudents().first()
        existingStudentsResult.fold(
            onSuccess = { existingStudents ->
                val existingMap = existingStudents.associateBy { it.name }
                val classCache = mutableMapOf<String, Int>()
                val studentIdByName = mutableMapOf<String, Int>()

                studentsToImport.forEach { studentData ->
                    val classId = resolveClassId(studentData.className, classCache)
                    val existingStudent = existingMap[studentData.name]
                    val studentId =
                        studentIdByName[studentData.name]
                            ?: existingStudent?.let { existing ->
                                if (existing.classId != classId) {
                                    studentRepository.updateStudent(existing.copy(classId = classId))
                                }
                                existing.id
                            }
                            ?: run {
                                val newId =
                                    studentRepository
                                        .insertStudent(Student(name = studentData.name, classId = classId))
                                        .getOrDefault(0)
                                        .toInt()
                                counters.newCount++
                                newId
                            }
                    studentIdByName[studentData.name] = studentId
                    if (existingStudent != null) counters.mergedCount++

                    studentData.dates.forEach { dateString ->
                        runCatching { LocalDate.parse(dateString) }
                            .onSuccess { localDate ->
                                attendanceRepository.insertAttendance(
                                    StudentAttendance(studentId = studentId, date = localDate)
                                )
                                counters.datesProcessed++
                            }
                            .onFailure { e ->
                                counters.datesSkipped++
                                if (e is CancellationException) throw e
                            }
                    }

                    mergeEngine.mergeBusyAppointments(studentId, studentData.busyAppointments, existingBusy, counters)
                }

                val postBusyAssignments =
                    scheduleRepository.observeAssignments().first().getOrElse { return Result.failure(it) }
                val postBusy =
                    scheduleRepository.observeBusyAppointments().first().getOrElse { return Result.failure(it) }

                val plannedByHour =
                    mergeEngine.collectPlannedLessons(
                        studentsToImport,
                        studentIdByName,
                        hourIdByKey,
                        postBusyAssignments,
                        postBusy,
                        counters
                    )

                mergeEngine.raiseExistingHourCapacities(existingHours, postBusyAssignments, plannedByHour)

                mergeEngine.executePlannedLessons(plannedByHour, counters)
            },
            onFailure = {
                return Result.failure(it)
            }
        )

        return Result.success(
            ImportResult(
                newStudentsCount = counters.newCount,
                existingStudentsMergedCount = counters.mergedCount,
                datesProcessedCount = counters.datesProcessed,
                datesSkippedCount = counters.datesSkipped,
                hoursAddedCount = counters.hoursAdded,
                hoursSkippedOverlapCount = counters.hoursOverlapSkipped,
                lessonsAddedCount = counters.lessonsAdded,
                lessonsRemovedBusyWinsCount = counters.lessonsRemovedBusyWins,
                lessonsSkippedCount = counters.lessonsSkipped,
                malformedEntriesSkippedCount = counters.malformedSkipped
            )
        )
    }

    private suspend fun resolveClassId(
        className: String,
        classCache: MutableMap<String, Int>
    ): Int? {
        val trimmed = className.trim()
        if (trimmed.isBlank()) return null
        return classCache[trimmed]
            ?: studentClassRepository.getOrCreateClassByName(trimmed).getOrNull()
                ?.also { classCache[trimmed] = it }
    }
}