package dev.nenoeldeeb.education.absencerecord.domain.usecases.transfer

import dev.nenoeldeeb.education.absencerecord.domain.models.AvailableLessonHour
import dev.nenoeldeeb.education.absencerecord.domain.models.BackupExportData
import dev.nenoeldeeb.education.absencerecord.domain.models.BusyAppointment
import dev.nenoeldeeb.education.absencerecord.domain.models.LessonAssignment
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.repositories.AttendanceRepository
import dev.nenoeldeeb.education.absencerecord.domain.repositories.ScheduleRepository
import dev.nenoeldeeb.education.absencerecord.domain.repositories.StorageRepository
import dev.nenoeldeeb.education.absencerecord.domain.repositories.StudentClassRepository
import dev.nenoeldeeb.education.absencerecord.domain.services.SerializationService
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.isoDayNumber
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DisplayName("ExportStudentsUseCase Schedule Tests")
class ExportStudentsUseCaseScheduleTest {
    private lateinit var attendanceRepository: AttendanceRepository
    private lateinit var storageRepository: StorageRepository
    private lateinit var serializationService: SerializationService
    private lateinit var studentClassRepository: StudentClassRepository
    private lateinit var scheduleRepository: ScheduleRepository
    private lateinit var useCase: ExportStudentsUseCase

    @BeforeEach
    fun setup() {
        attendanceRepository = mockk()
        storageRepository = mockk()
        serializationService = mockk()
        studentClassRepository = mockk()
        scheduleRepository = mockk()
        useCase =
            ExportStudentsUseCase(
                attendanceRepository, storageRepository, serializationService, studentClassRepository, scheduleRepository
            )
        every { studentClassRepository.getAllClasses() } returns flowOf(Result.success(emptyList()))
        every { scheduleRepository.observeHours() } returns flowOf(Result.success(emptyList()))
        every { scheduleRepository.observeAssignments() } returns flowOf(Result.success(emptyList()))
        every { scheduleRepository.observeBusyAppointments() } returns flowOf(Result.success(emptyList()))
        every { serializationService.encodeToString<BackupExportData>(any(), any()) } returns Result.success("json")
        coEvery { storageRepository.writeTextToUri(any(), any()) } returns Result.success(Unit)
    }

    private fun captureBackup(): BackupExportData {
        val capturedBackup = slot<BackupExportData>()
        verify {
            serializationService.encodeToString(
                capture(capturedBackup),
                any()
            )
        }
        return capturedBackup.captured
    }

    @Test
    fun `should export full availability plan and per-student schedule`() =
        runTest {
            // Arrange
            val selectedIds = setOf(1)
            val allStudents = listOf(Student(id = 1, name = "John"))
            val hour =
                AvailableLessonHour(
                    id = 5,
                    weekday = DayOfWeek.SATURDAY,
                    startMinutes = 480,
                    maxStudents = 3
                )
            val assignment =
                LessonAssignment(
                    id = 1,
                    availableHourId = 5,
                    studentId = 1,
                    weekday = DayOfWeek.SATURDAY
                )
            val busy =
                BusyAppointment(
                    id = 1,
                    studentId = 1,
                    weekday = DayOfWeek.SUNDAY,
                    startMinutes = 600,
                    durationMinutes = 30
                )

            every { attendanceRepository.getStudentAttendanceDates(1) } returns flowOf(Result.success(emptyList()))
            every { scheduleRepository.observeHours() } returns flowOf(Result.success(listOf(hour)))
            every { scheduleRepository.observeAssignments() } returns flowOf(Result.success(listOf(assignment)))
            every { scheduleRepository.observeBusyAppointments() } returns flowOf(Result.success(listOf(busy)))

            // Act
            useCase("content://export", selectedIds, allStudents)

            // Assert
            val backup = captureBackup()
            assertEquals(1, backup.availableHours.size)
            assertEquals(480, backup.availableHours[0].startMinutes)
            assertEquals(3, backup.availableHours[0].maxStudents)
            assertEquals(DayOfWeek.SATURDAY.isoDayNumber, backup.availableHours[0].weekday)
            assertEquals(1, backup.students[0].lessonAssignments.size)
            assertEquals(480, backup.students[0].lessonAssignments[0].startMinutes)
            assertEquals(DayOfWeek.SATURDAY.isoDayNumber, backup.students[0].lessonAssignments[0].weekday)
            assertEquals(1, backup.students[0].busyAppointments.size)
            assertEquals(30, backup.students[0].busyAppointments[0].durationMinutes)
        }

    @Test
    fun `should only export schedule of selected students`() =
        runTest {
            // Arrange
            val selectedIds = setOf(1)
            val allStudents = listOf(Student(id = 1, name = "John"), Student(id = 2, name = "Jane"))
            val hour =
                AvailableLessonHour(
                    id = 5,
                    weekday = DayOfWeek.MONDAY,
                    startMinutes = 480,
                    maxStudents = 2
                )
            val johnAssignment =
                LessonAssignment(
                    id = 1,
                    availableHourId = 5,
                    studentId = 1,
                    weekday = DayOfWeek.MONDAY
                )
            val janeAssignment =
                LessonAssignment(
                    id = 2,
                    availableHourId = 5,
                    studentId = 2,
                    weekday = DayOfWeek.MONDAY
                )

            every { attendanceRepository.getStudentAttendanceDates(1) } returns flowOf(Result.success(emptyList()))
            every { scheduleRepository.observeHours() } returns flowOf(Result.success(listOf(hour)))
            every { scheduleRepository.observeAssignments() } returns
                flowOf(Result.success(listOf(johnAssignment, janeAssignment)))

            // Act
            useCase("content://export", selectedIds, allStudents)

            // Assert
            val backup = captureBackup()
            assertEquals(1, backup.students.size)
            assertEquals(1, backup.students[0].lessonAssignments.size)
        }

    @Test
    fun `should drop assignments whose hour no longer exists`() =
        runTest {
            // Arrange
            val selectedIds = setOf(1)
            val allStudents = listOf(Student(id = 1, name = "John"))
            val orphanedAssignment =
                LessonAssignment(
                    id = 1,
                    availableHourId = 999,
                    studentId = 1,
                    weekday = DayOfWeek.MONDAY
                )

            every { attendanceRepository.getStudentAttendanceDates(1) } returns flowOf(Result.success(emptyList()))
            every { scheduleRepository.observeAssignments() } returns flowOf(Result.success(listOf(orphanedAssignment)))

            // Act
            useCase("content://export", selectedIds, allStudents)

            // Assert
            val backup = captureBackup()
            assertTrue(backup.students[0].lessonAssignments.isEmpty())
        }

    @Test
    fun `should succeed when schedule repository fails`() =
        runTest {
            // Arrange
            val selectedIds = setOf(1)
            val allStudents = listOf(Student(id = 1, name = "John"))

            every { attendanceRepository.getStudentAttendanceDates(1) } returns flowOf(Result.success(emptyList()))
            every { scheduleRepository.observeHours() } returns flowOf(Result.failure(RuntimeException("db")))
            every { scheduleRepository.observeAssignments() } returns flowOf(Result.failure(RuntimeException("db")))
            every { scheduleRepository.observeBusyAppointments() } returns flowOf(Result.failure(RuntimeException("db")))

            // Act
            val result = useCase("content://export", selectedIds, allStudents)

            // Assert
            assertTrue(result.isSuccess)
            val backup = captureBackup()
            assertTrue(backup.availableHours.isEmpty())
            assertTrue(backup.students[0].lessonAssignments.isEmpty())
        }
}