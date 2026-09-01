package dev.nenoeldeeb.education.absencerecord.domain.usecases.transfer

import dev.nenoeldeeb.education.absencerecord.domain.models.AssignmentRemovalReport
import dev.nenoeldeeb.education.absencerecord.domain.models.AvailableHourExportData
import dev.nenoeldeeb.education.absencerecord.domain.models.AvailableLessonHour
import dev.nenoeldeeb.education.absencerecord.domain.models.BusyAppointmentExportData
import dev.nenoeldeeb.education.absencerecord.domain.models.LessonAssignment
import dev.nenoeldeeb.education.absencerecord.domain.models.LessonAssignmentExportData
import dev.nenoeldeeb.education.absencerecord.domain.models.ParsedImportData
import dev.nenoeldeeb.education.absencerecord.domain.models.ParsedStudentImportData
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentExportData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.isoDayNumber
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DisplayName("PerformImportUseCase Hours Merge Tests")
class PerformImportHoursMergeTest : PerformImportUseCaseTestBase() {
    @Test
    fun `imports availability plan into empty app`() =
        runTest {
            coEvery { scheduleRepository.insertHour(any(), any(), any()) } returns Result.success(5)
            coEvery { scheduleRepository.assignStudent(any(), any(), any()) } returns Result.success(Unit)

            val result =
                import(
                    parsedData(
                        hours = listOf(AvailableHourExportData(DayOfWeek.SATURDAY.isoDayNumber, 480, 2))
                    )
                )

            assertTrue(result.isSuccess)
            assertEquals(1, result.getOrNull()?.hoursAddedCount)
            assertEquals(0, result.getOrNull()?.hoursSkippedOverlapCount)
            coVerify(exactly = 1) {
                scheduleRepository.insertHour(DayOfWeek.SATURDAY, 480, 2)
            }
        }

    @Test
    fun `skips imported hours overlapping an existing hour`() =
        runTest {
            every { scheduleRepository.observeHours() } returns
                flowOf(
                    Result.success(
                        listOf(
                            AvailableLessonHour(
                                id = 5,
                                weekday = DayOfWeek.SATURDAY,
                                startMinutes = 480,
                                maxStudents = 2
                            )
                        )
                    )
                )
            coEvery { scheduleRepository.insertHour(any(), any(), any()) } returns Result.success(6)

            val result =
                import(
                    parsedData(
                        hours =
                            listOf(
                                AvailableHourExportData(DayOfWeek.SATURDAY.isoDayNumber, 480, 3),
                                AvailableHourExportData(DayOfWeek.SATURDAY.isoDayNumber, 420, 3),
                                AvailableHourExportData(DayOfWeek.SATURDAY.isoDayNumber, 540, 3)
                            )
                    )
                )

            assertTrue(result.isSuccess)
            assertEquals(2, result.getOrNull()?.hoursAddedCount)
            assertEquals(1, result.getOrNull()?.hoursSkippedOverlapCount)
            coVerify(exactly = 1) { scheduleRepository.insertHour(DayOfWeek.SATURDAY, 420, 3) }
            coVerify(exactly = 1) { scheduleRepository.insertHour(DayOfWeek.SATURDAY, 540, 3) }
            coVerify(exactly = 0) { scheduleRepository.insertHour(DayOfWeek.SATURDAY, 480, any()) }
        }

    @Test
    fun `drops lessons referencing an absent hour`() =
        runTest {
            every { scheduleRepository.observeHours() } returns
                flowOf(
                    Result.success(
                        listOf(
                            AvailableLessonHour(
                                id = 5,
                                weekday = DayOfWeek.MONDAY,
                                startMinutes = 480,
                                maxStudents = 2
                            )
                        )
                    )
                )

            val result =
                import(
                    parsedData(
                        student =
                            StudentExportData(
                                "John",
                                "",
                                emptyList(),
                                lessonAssignments =
                                    listOf(
                                        LessonAssignmentExportData(DayOfWeek.MONDAY.isoDayNumber, 600)
                                    )
                            )
                    )
                )

            assertTrue(result.isSuccess)
            assertEquals(1, result.getOrNull()?.lessonsSkippedCount)
            assertEquals(0, result.getOrNull()?.lessonsAddedCount)
        }

    @Test
    fun `raises existing hour capacity to fit imported lessons`() =
        runTest {
            every { scheduleRepository.observeHours() } returns
                flowOf(
                    Result.success(
                        listOf(
                            AvailableLessonHour(
                                id = 5,
                                weekday = DayOfWeek.MONDAY,
                                startMinutes = 480,
                                maxStudents = 1
                            )
                        )
                    )
                )
            every { scheduleRepository.observeAssignments() } returns
                flowOf(
                    Result.success(
                        listOf(
                            LessonAssignment(
                                id = 1,
                                availableHourId = 5,
                                studentId = 1,
                                weekday = DayOfWeek.MONDAY
                            )
                        )
                    )
                )
            every { studentRepository.getAllStudents() } returns
                flowOf(Result.success(listOf(Student(id = 1, name = "John"))))
            coEvery { scheduleRepository.updateHour(any(), any(), any(), any()) } returns
                Result.success(AssignmentRemovalReport(emptyList()))
            coEvery { studentRepository.insertStudent(any()) } returns Result.success(2L)
            coEvery { scheduleRepository.assignStudent(any(), any(), any()) } returns Result.success(Unit)

            val john = ParsedStudentImportData(StudentExportData("John", "", emptyList()), 1)
            val jane =
                ParsedStudentImportData(
                    StudentExportData(
                        "Jane",
                        "",
                        emptyList(),
                        lessonAssignments =
                            listOf(LessonAssignmentExportData(DayOfWeek.MONDAY.isoDayNumber, 480))
                    ),
                    2
                )
            val data = ParsedImportData(listOf(john, jane), emptyList())

            val result = import(data, mapOf(john.id to true, jane.id to true))

            assertTrue(result.isSuccess)
            assertEquals(1, result.getOrNull()?.lessonsAddedCount)
            coVerify(exactly = 1) { scheduleRepository.updateHour(5, DayOfWeek.MONDAY, 480, 2) }
            coVerify(exactly = 1) { scheduleRepository.assignStudent(5, 2, DayOfWeek.MONDAY) }
        }

    @Test
    fun `counts malformed entries and skips them`() =
        runTest {
            val result =
                import(
                    parsedData(
                        student =
                            StudentExportData(
                                "John",
                                "",
                                emptyList(),
                                busyAppointments =
                                    listOf(
                                        BusyAppointmentExportData(DayOfWeek.MONDAY.isoDayNumber, 480, 10)
                                    ),
                                lessonAssignments =
                                    listOf(
                                        LessonAssignmentExportData(9, 480),
                                        LessonAssignmentExportData(DayOfWeek.MONDAY.isoDayNumber, 1385)
                                    )
                            ),
                        hours =
                            listOf(
                                AvailableHourExportData(DayOfWeek.MONDAY.isoDayNumber, 480, 0),
                                AvailableHourExportData(0, 480, 2)
                            )
                    )
                )

            assertTrue(result.isSuccess)
            assertEquals(5, result.getOrNull()?.malformedEntriesSkippedCount)
            assertEquals(0, result.getOrNull()?.hoursAddedCount)
            assertEquals(0, result.getOrNull()?.lessonsAddedCount)
        }

    @Test
    fun `legacy flat array import restores students and dates only`() =
        runTest {
            coEvery { attendanceRepository.insertAttendance(any()) } returns Result.success(1L)

            val result = import(parsedData(StudentExportData("John", "", listOf("2024-01-01"))))

            assertTrue(result.isSuccess)
            val importResult = result.getOrNull()
            assertEquals(1, importResult?.newStudentsCount)
            assertEquals(1, importResult?.datesProcessedCount)
            assertEquals(0, importResult?.hoursAddedCount)
            assertEquals(0, importResult?.lessonsAddedCount)
            coVerify(exactly = 0) { scheduleRepository.insertHour(any(), any(), any()) }
            coVerify(exactly = 0) { scheduleRepository.assignStudent(any(), any(), any()) }
            coVerify(exactly = 0) { scheduleRepository.insertBusyAppointment(any(), any(), any(), any()) }
        }

    @Test
    fun `round trip on empty app reproduces the backup`() =
        runTest {
            coEvery { scheduleRepository.insertHour(any(), any(), any()) } returns Result.success(5)
            coEvery { scheduleRepository.insertBusyAppointment(any(), any(), any(), any()) } returns
                Result.success(AssignmentRemovalReport(emptyList()))
            coEvery { scheduleRepository.assignStudent(any(), any(), any()) } returns Result.success(Unit)

            val result =
                import(
                    ParsedImportData(
                        students =
                            listOf(
                                ParsedStudentImportData(
                                    StudentExportData(
                                        "John",
                                        "",
                                        emptyList(),
                                        lessonAssignments =
                                            listOf(
                                                LessonAssignmentExportData(
                                                    DayOfWeek.SATURDAY.isoDayNumber,
                                                    480
                                                )
                                            ),
                                        busyAppointments =
                                            listOf(
                                                BusyAppointmentExportData(DayOfWeek.SUNDAY.isoDayNumber, 600, 30)
                                            )
                                    ),
                                    1
                                )
                            ),
                        availableHours =
                            listOf(AvailableHourExportData(DayOfWeek.SATURDAY.isoDayNumber, 480, 2))
                    )
                )

            assertTrue(result.isSuccess)
            assertEquals(1, result.getOrNull()?.newStudentsCount)
            assertEquals(1, result.getOrNull()?.hoursAddedCount)
            assertEquals(1, result.getOrNull()?.lessonsAddedCount)
            coVerify(exactly = 1) { scheduleRepository.insertHour(DayOfWeek.SATURDAY, 480, 2) }
            coVerify(exactly = 1) { scheduleRepository.insertBusyAppointment(1, DayOfWeek.SUNDAY, 600, 30) }
            coVerify(exactly = 1) { scheduleRepository.assignStudent(5, 1, DayOfWeek.SATURDAY) }
        }
}