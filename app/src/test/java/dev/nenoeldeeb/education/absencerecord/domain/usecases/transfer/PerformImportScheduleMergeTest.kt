package dev.nenoeldeeb.education.absencerecord.domain.usecases.transfer

import dev.nenoeldeeb.education.absencerecord.domain.models.AssignmentRemovalReport
import dev.nenoeldeeb.education.absencerecord.domain.models.AvailableHourExportData
import dev.nenoeldeeb.education.absencerecord.domain.models.AvailableLessonHour
import dev.nenoeldeeb.education.absencerecord.domain.models.BusyAppointmentExportData
import dev.nenoeldeeb.education.absencerecord.domain.models.LessonAssignment
import dev.nenoeldeeb.education.absencerecord.domain.models.LessonAssignmentExportData
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

@DisplayName("PerformImportUseCase Student Schedule Merge Tests")
class PerformImportScheduleMergeTest : PerformImportUseCaseTestBase() {
    @Test
    fun `skips lessons duplicating an existing same-weekday lesson`() =
        runTest {
            every { scheduleRepository.observeHours() } returns
                flowOf(
                    Result.success(
                        listOf(
                            AvailableLessonHour(
                                id = 5,
                                weekday = DayOfWeek.MONDAY,
                                startMinutes = 480,
                                durationMinutes = 20
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
                                        LessonAssignmentExportData(DayOfWeek.MONDAY.isoDayNumber, 480)
                                    )
                            )
                    )
                )

            assertTrue(result.isSuccess)
            assertEquals(1, result.getOrNull()?.lessonsSkippedCount)
            assertEquals(0, result.getOrNull()?.lessonsAddedCount)
        }

    // The "a busy appointment costs the lesson" case used to live here, asserted
    // against the merge's own whole-extent pre-filter. That pre-filter is gone:
    // it also refused students whose commitment fell merely LATER inside the
    // block, which FR-027 forbids. Placement is now decided by
    // `ScheduleDao.assignStudent` — which re-plans and judges each student on
    // their own ten minutes — and the merge only has to count the refusals.
    // Covered by `PerformImportSlotRestoreTest`; the refusal itself by
    // `ScheduleDaoSlotValidationTest` (FR-026, FR-027).

    @Test
    fun `adds imported lessons when no conflicts exist`() =
        runTest {
            coEvery { scheduleRepository.insertHour(any(), any(), any()) } returns Result.success(5)
            coEvery { scheduleRepository.assignStudent(any(), any(), any()) } returns Result.success(Unit)

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
                                        LessonAssignmentExportData(DayOfWeek.SATURDAY.isoDayNumber, 480)
                                    )
                            ),
                        hours = listOf(AvailableHourExportData(DayOfWeek.SATURDAY.isoDayNumber, 480, durationMinutes = 20))
                    )
                )

            assertTrue(result.isSuccess)
            assertEquals(1, result.getOrNull()?.hoursAddedCount)
            assertEquals(1, result.getOrNull()?.lessonsAddedCount)
            coVerify(exactly = 1) { scheduleRepository.assignStudent(5, 1, DayOfWeek.SATURDAY) }
        }

    @Test
    fun `imports busy appointments and count wins`() =
        runTest {
            coEvery { scheduleRepository.insertBusyAppointment(any(), any(), any(), any()) } returns
                Result.success(AssignmentRemovalReport(emptyList()))

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
                                        BusyAppointmentExportData(DayOfWeek.SUNDAY.isoDayNumber, 600, 30)
                                    )
                            )
                    )
                )

            assertTrue(result.isSuccess)
            assertEquals(0, result.getOrNull()?.lessonsRemovedBusyWinsCount)
            coVerify(exactly = 1) {
                scheduleRepository.insertBusyAppointment(1, DayOfWeek.SUNDAY, 600, 30)
            }
        }

    @Test
    fun `imported busy removes conflicting lessons and reports wins`() =
        runTest {
            every { scheduleRepository.observeHours() } returns
                flowOf(
                    Result.success(
                        listOf(
                            AvailableLessonHour(
                                id = 5,
                                weekday = DayOfWeek.MONDAY,
                                startMinutes = 480,
                                durationMinutes = 20
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
            coEvery { scheduleRepository.insertBusyAppointment(any(), any(), any(), any()) } returns
                Result.success(AssignmentRemovalReport(listOf(1)))

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
                                        BusyAppointmentExportData(DayOfWeek.MONDAY.isoDayNumber, 480, 60)
                                    )
                            )
                    )
                )

            assertTrue(result.isSuccess)
            assertEquals(1, result.getOrNull()?.lessonsRemovedBusyWinsCount)
            coVerify(exactly = 1) {
                scheduleRepository.insertBusyAppointment(1, DayOfWeek.MONDAY, 480, 60)
            }
        }
}