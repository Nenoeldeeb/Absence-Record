package dev.nenoeldeeb.education.absencerecord.domain.usecases.transfer

import dev.nenoeldeeb.education.absencerecord.domain.models.AvailableHourExportData
import dev.nenoeldeeb.education.absencerecord.domain.models.BACKUP_VERSION
import dev.nenoeldeeb.education.absencerecord.domain.models.BackupExportData
import dev.nenoeldeeb.education.absencerecord.domain.models.BusyAppointmentExportData
import dev.nenoeldeeb.education.absencerecord.domain.models.LessonAssignmentExportData
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentExportData
import dev.nenoeldeeb.education.absencerecord.domain.repositories.AttendanceRepository
import dev.nenoeldeeb.education.absencerecord.domain.repositories.ScheduleRepository
import dev.nenoeldeeb.education.absencerecord.domain.repositories.StorageRepository
import dev.nenoeldeeb.education.absencerecord.domain.repositories.StudentClassRepository
import dev.nenoeldeeb.education.absencerecord.domain.services.SerializationService
import kotlinx.coroutines.flow.first
import kotlinx.datetime.isoDayNumber

/**
 * Use case for exporting selected students and their attendance data to JSON format.
 *
 * @property attendanceRepository Repository for accessing attendance records
 * @property storageRepository Repository for writing data to storage
 * @property serializationService Service for JSON serialization
 * @property studentClassRepository Repository for accessing class information
 * @property scheduleRepository Repository for accessing the weekly availability plan and assignments
 */
class ExportStudentsUseCase(
    private val attendanceRepository: AttendanceRepository,
    private val storageRepository: StorageRepository,
    private val serializationService: SerializationService,
    private val studentClassRepository: StudentClassRepository,
    private val scheduleRepository: ScheduleRepository
) {
    /**
     * Exports selected students with their attendance history and the full availability plan to a JSON file.
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
            return Result.failure(StudentError.Validation("No students selected"))
        }

        val classes = studentClassRepository.getAllClasses().first().getOrDefault(emptyList())
        val classMap = classes.associate { it.id to it.name }

        val allHours = scheduleRepository.observeHours().first().getOrDefault(emptyList())
        val allAssignments = scheduleRepository.observeAssignments().first().getOrDefault(emptyList())
        val allBusyAppointments = scheduleRepository.observeBusyAppointments().first().getOrDefault(emptyList())
        val hourById = allHours.associateBy { it.id }

        val exportStudents =
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
                        className = student.classId?.let { classMap[it] } ?: "",
                        lessonAssignments =
                            allAssignments
                                .filter { it.studentId == studentId }
                                .mapNotNull { assignment ->
                                    hourById[assignment.availableHourId]?.let { hour ->
                                        LessonAssignmentExportData(
                                            weekday = hour.weekday.isoDayNumber,
                                            startMinutes = hour.startMinutes
                                        )
                                    }
                                },
                        busyAppointments =
                            allBusyAppointments
                                .filter { it.studentId == studentId }
                                .map { appointment ->
                                    BusyAppointmentExportData(
                                        weekday = appointment.weekday.isoDayNumber,
                                        startMinutes = appointment.startMinutes,
                                        durationMinutes = appointment.durationMinutes
                                    )
                                }
                    )
                }
            }

        val backup =
            BackupExportData(
                version = BACKUP_VERSION,
                students = exportStudents,
                availableHours =
                    allHours.map { hour ->
                        AvailableHourExportData(
                            weekday = hour.weekday.isoDayNumber,
                            startMinutes = hour.startMinutes,
                            maxStudents = hour.maxStudents
                        )
                    }
            )

        return serializationService.encodeToString(backup, BackupExportData.serializer())
            .fold(
                onSuccess = { jsonString -> storageRepository.writeTextToUri(uriString, jsonString) },
                onFailure = { Result.failure(StudentError.FileWrite) }
            )
    }
}