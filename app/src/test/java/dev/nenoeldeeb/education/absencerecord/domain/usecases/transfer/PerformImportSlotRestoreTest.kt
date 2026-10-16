package dev.nenoeldeeb.education.absencerecord.domain.usecases.transfer

import dev.nenoeldeeb.education.absencerecord.domain.models.AvailableLessonHour
import dev.nenoeldeeb.education.absencerecord.domain.models.BusyAppointment
import dev.nenoeldeeb.education.absencerecord.domain.models.BusyAppointmentExportData
import dev.nenoeldeeb.education.absencerecord.domain.models.LessonAssignmentExportData
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentExportData
import io.mockk.coEvery
import io.mockk.every
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.isoDayNumber
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * A student's own ten minutes, not the whole block, decides whether an imported
 * lesson can be restored (FR-027).
 *
 * The merge used to test the student's commitments against the block's **full
 * extent**, so a commitment falling later inside a block threw away a lesson that
 * was perfectly valid — and, because a backup is exported from lessons that were
 * themselves valid, that silently broke the export/import round trip for exactly
 * the students the feature exists to accommodate.
 *
 * Placement is decided by `ScheduleDao.assignStudent`, which re-plans the whole
 * roster and judges each student against their own slot. That is the single
 * authority; this file checks the merge stops pre-rejecting ahead of it.
 */
@DisplayName("PerformImportUseCase FR-027 Per-Slot Restore Tests")
class PerformImportSlotRestoreTest : PerformImportUseCaseTestBase() {
    private val monday = DayOfWeek.MONDAY.isoDayNumber

    /**
     * A 08:00–10:00 block with a single student committed at 09:30.
     *
     * Their slot is 08:00–08:10, which is clear, so the lesson is restorable. The
     * whole-extent check saw 09:30 sitting inside the block and refused it.
     */
    @Test
    fun `restores a lesson whose commitment falls later inside the block`() =
        runTest {
            every { scheduleRepository.observeHours() } returns
                flowOf(
                    Result.success(
                        listOf(
                            AvailableLessonHour(
                                id = 5,
                                weekday = DayOfWeek.MONDAY,
                                startMinutes = 480,
                                durationMinutes = 120
                            )
                        )
                    )
                )
            every { scheduleRepository.observeBusyAppointments() } returns
                flowOf(
                    Result.success(
                        listOf(
                            BusyAppointment(
                                id = 1,
                                studentId = 1,
                                weekday = DayOfWeek.MONDAY,
                                startMinutes = 570,
                                durationMinutes = 30
                            )
                        )
                    )
                )

            val student =
                StudentExportData(
                    name = "John",
                    className = "",
                    dates = emptyList(),
                    lessonAssignments =
                        listOf(LessonAssignmentExportData(weekday = monday, startMinutes = 480)),
                    busyAppointments =
                        listOf(
                            BusyAppointmentExportData(
                                weekday = monday,
                                startMinutes = 570,
                                durationMinutes = 30
                            )
                        )
                )

            val result = import(parsedData(student))

            assertEquals(
                0,
                result.getOrNull()?.lessonsSkippedCount,
                "the commitment at 09:30 must not cost this student their 08:00 slot"
            )
        }

    /**
     * The mirror image, at the layer where the decision now lives.
     *
     * The merge no longer judges placement, so it cannot report a conflict of its
     * own accord — but it must still count a refusal honestly rather than dropping
     * the lesson silently. `ScheduleDaoSlotValidationTest` covers the DAO side of
     * FR-026/FR-027; this pins that the teacher's summary stays truthful.
     */
    @Test
    fun `counts a lesson the placement pass refuses rather than dropping it silently`() =
        runTest {
            every { scheduleRepository.observeHours() } returns
                flowOf(
                    Result.success(
                        listOf(
                            AvailableLessonHour(
                                id = 5,
                                weekday = DayOfWeek.MONDAY,
                                startMinutes = 480,
                                durationMinutes = 120
                            )
                        )
                    )
                )
            coEvery { scheduleRepository.assignStudent(any(), any(), any()) } returns
                Result.failure(StudentError.BusyConflict)

            val student =
                StudentExportData(
                    name = "John",
                    className = "",
                    dates = emptyList(),
                    lessonAssignments =
                        listOf(LessonAssignmentExportData(weekday = monday, startMinutes = 480))
                )

            val result = import(parsedData(student))

            assertEquals(
                1,
                result.getOrNull()?.lessonsSkippedCount,
                "a refused placement must be reported to the teacher"
            )
            assertEquals(
                0,
                result.getOrNull()?.lessonsAddedCount,
                "a refused lesson must not be counted as restored"
            )
        }
}