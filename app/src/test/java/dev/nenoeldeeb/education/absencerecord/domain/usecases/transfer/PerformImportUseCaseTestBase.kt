package dev.nenoeldeeb.education.absencerecord.domain.usecases.transfer

import dev.nenoeldeeb.education.absencerecord.domain.models.AvailableHourExportData
import dev.nenoeldeeb.education.absencerecord.domain.models.ParsedImportData
import dev.nenoeldeeb.education.absencerecord.domain.models.ParsedStudentImportData
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentExportData
import dev.nenoeldeeb.education.absencerecord.domain.repositories.AttendanceRepository
import dev.nenoeldeeb.education.absencerecord.domain.repositories.ScheduleRepository
import dev.nenoeldeeb.education.absencerecord.domain.repositories.StudentClassRepository
import dev.nenoeldeeb.education.absencerecord.domain.repositories.StudentRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.BeforeEach

abstract class PerformImportUseCaseTestBase {
    protected lateinit var studentRepository: StudentRepository
    protected lateinit var attendanceRepository: AttendanceRepository
    protected lateinit var studentClassRepository: StudentClassRepository
    protected lateinit var scheduleRepository: ScheduleRepository
    protected lateinit var useCase: PerformImportUseCase

    @BeforeEach
    fun setup() {
        studentRepository = mockk()
        attendanceRepository = mockk()
        studentClassRepository = mockk()
        scheduleRepository = mockk()
        useCase =
            PerformImportUseCase(
                studentRepository,
                attendanceRepository,
                studentClassRepository,
                scheduleRepository
            )
        every { scheduleRepository.observeHours() } returns flowOf(Result.success(emptyList()))
        every { scheduleRepository.observeAssignments() } returns flowOf(Result.success(emptyList()))
        every { scheduleRepository.observeBusyAppointments() } returns flowOf(Result.success(emptyList()))
        every { studentRepository.getAllStudents() } returns flowOf(Result.success(emptyList()))
        coEvery { studentClassRepository.getOrCreateClassByName(any()) } returns Result.success(1)
        coEvery { studentRepository.updateStudent(any()) } returns Result.success(Unit)
        coEvery { studentRepository.insertStudent(any()) } returns Result.success(1L)
    }

    protected fun parsedData(
        student: StudentExportData = StudentExportData("John", "", emptyList()),
        hours: List<AvailableHourExportData> = emptyList()
    ): ParsedImportData =
        ParsedImportData(
            students = listOf(ParsedStudentImportData(student, 1)),
            availableHours = hours
        )

    protected suspend fun import(
        data: ParsedImportData,
        selectionMap: Map<Int, Boolean> = mapOf(data.students[0].id to true)
    ) = useCase(data, selectionMap)
}