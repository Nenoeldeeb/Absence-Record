package dev.nenoeldeeb.education.absencerecord.domain.usecases.transfer

import dev.nenoeldeeb.education.absencerecord.domain.models.AvailableHourExportData
import dev.nenoeldeeb.education.absencerecord.domain.models.BACKUP_VERSION
import dev.nenoeldeeb.education.absencerecord.domain.models.ParsedImportData
import dev.nenoeldeeb.education.absencerecord.domain.models.ParsedStudentImportData
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentExportData
import dev.nenoeldeeb.education.absencerecord.domain.services.ImportCaps
import dev.nenoeldeeb.education.absencerecord.domain.services.ScheduleRules
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.isoDayNumber
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Skip-and-count behaviour at the import boundary (FR-046, FR-048, FR-056 … FR-059).
 *
 * A file that is not perfectly well-formed is not a failed import: the good
 * records still land, the bad ones are counted, and nothing is reported as a
 * parse failure.
 */
class ImportCapsTest : PerformImportUseCaseTestBase() {
    private val sat = DayOfWeek.SATURDAY.isoDayNumber

    private fun student(
        name: String,
        dateCount: Int = 0
    ) = StudentExportData(
        name = name,
        dates = List(dateCount) { "2026-01-" + ((it % 28) + 1).toString().padStart(2, '0') }
    )

    /** Runs an import and returns the summary the teacher would be shown. */
    private suspend fun run(
        students: List<StudentExportData>,
        hours: List<AvailableHourExportData>
    ) = import(
        ParsedImportData(
            students = students.mapIndexed { index, data -> ParsedStudentImportData(data, index) },
            availableHours = hours
        )
    ).getOrNull()!!

    // region the caps are what research D9 records

    @Test
    fun `documented caps match the derived and judged figures`() {
        assertEquals(10_000, ImportCaps.MAX_IMPORT_STUDENTS)
        // 7 days x 1440 minutes / 10 minutes per block
        assertEquals(7 * 1440 / 10, ImportCaps.MAX_IMPORT_HOURS)
        assertEquals(ImportCaps.MAX_IMPORT_HOURS * 12, ImportCaps.MAX_IMPORT_ASSIGNMENTS)
        assertEquals(3_650, ImportCaps.MAX_IMPORT_ATTENDANCE_DATES_PER_STUDENT)
        assertEquals(32L * 1024 * 1024, ImportCaps.MAX_IMPORT_FILE_BYTES)
    }

    @Test
    fun `the current backup version is three`() {
        assertEquals(3, BACKUP_VERSION)
    }

    // endregion

    // region malformed entries are skipped and counted, never guessed

    @Test
    fun `an entry carrying both length signals is skipped and counted`() =
        runTest {
            coEvery { scheduleRepository.insertHour(any(), any(), any()) } returns Result.success(1)

            val summary =
                run(
                    students = listOf(student("A")),
                    hours =
                        listOf(
                            AvailableHourExportData(sat, 540, durationMinutes = 50, maxStudents = 5),
                            AvailableHourExportData(sat, 660, durationMinutes = 30)
                        )
                )

            assertEquals(1, summary.malformedEntriesSkippedCount)
            assertEquals(1, summary.hoursAddedCount, "only the well-formed block imports")
            coVerify(exactly = 0) { scheduleRepository.insertHour(any(), 540, any()) }
        }

    @Test
    fun `an entry carrying neither signal is skipped and counted`() =
        runTest {
            coEvery { scheduleRepository.insertHour(any(), any(), any()) } returns Result.success(1)

            val summary =
                run(
                    students = listOf(student("A")),
                    hours =
                        listOf(
                            AvailableHourExportData(sat, 540),
                            AvailableHourExportData(sat, 660, durationMinutes = 30)
                        )
                )

            assertEquals(1, summary.malformedEntriesSkippedCount)
            assertEquals(1, summary.hoursAddedCount)
        }

    @Test
    fun `a length that is not selectable is skipped and counted`() =
        runTest {
            coEvery { scheduleRepository.insertHour(any(), any(), any()) } returns Result.success(1)

            val summary =
                run(
                    students = listOf(student("A")),
                    hours =
                        listOf(
                            AvailableHourExportData(sat, 540, durationMinutes = 55),
                            AvailableHourExportData(sat, 660, durationMinutes = 30)
                        )
                )

            assertEquals(1, summary.malformedEntriesSkippedCount)
            assertEquals(1, summary.hoursAddedCount)
        }

    @Test
    fun `a block that would run past midnight is skipped and counted`() =
        runTest {
            coEvery { scheduleRepository.insertHour(any(), any(), any()) } returns Result.success(1)

            val summary =
                run(
                    students = listOf(student("A")),
                    hours = listOf(AvailableHourExportData(sat, 1380, durationMinutes = 120))
                )

            assertEquals(1, summary.malformedEntriesSkippedCount)
            assertEquals(0, summary.hoursAddedCount)
        }

    // endregion

    // region continuity: the previous format still imports

    @Test
    fun `a legacy capacity entry is converted and imports`() =
        runTest {
            coEvery { scheduleRepository.insertHour(any(), any(), any()) } returns Result.success(1)

            val summary =
                run(
                    students = listOf(student("A")),
                    hours = listOf(AvailableHourExportData(sat, 540, maxStudents = 6))
                )

            assertEquals(0, summary.malformedEntriesSkippedCount)
            coVerify(exactly = 1) { scheduleRepository.insertHour(DayOfWeek.SATURDAY, 540, 60) }
        }

    @Test
    fun `a legacy capacity outside the range is clamped and still imports`() =
        runTest {
            coEvery { scheduleRepository.insertHour(any(), any(), any()) } returns Result.success(1)

            run(
                students = listOf(student("A")),
                hours = listOf(AvailableHourExportData(sat, 0, maxStudents = 99))
            )

            coVerify(exactly = 1) { scheduleRepository.insertHour(DayOfWeek.SATURDAY, 0, 120) }
        }

    @Test
    fun `every selectable length round trips at the length it was saved with`() =
        runTest {
            coEvery { scheduleRepository.insertHour(any(), any(), any()) } returns Result.success(1)
            val lengths = ScheduleRules.selectableDurations()

            val summary =
                run(
                    students = listOf(student("A")),
                    hours =
                        lengths.mapIndexed { index, duration ->
                            // Spaced so the longest block still ends by midnight:
                            // 11 x 120 + 120 = 1440 exactly.
                            AvailableHourExportData(sat, index * 120, durationMinutes = duration)
                        }
                )

            assertEquals(0, summary.malformedEntriesSkippedCount)
            assertEquals(lengths.size, summary.hoursAddedCount)
            lengths.forEachIndexed { index, duration ->
                coVerify(exactly = 1) { scheduleRepository.insertHour(DayOfWeek.SATURDAY, index * 120, duration) }
            }
        }

    // endregion

    // region caps are enforced, not merely documented

    @Test
    fun `attendance dates past the per-student cap are counted and skipped`() =
        runTest {
            coEvery { attendanceRepository.insertAttendance(any()) } returns Result.success(1L)
            val oversize = ImportCaps.MAX_IMPORT_ATTENDANCE_DATES_PER_STUDENT + 5

            val summary = run(students = listOf(student("A", dateCount = oversize)), hours = emptyList())

            assertEquals(5, summary.cappedEntriesSkippedCount)
            coVerify(exactly = ImportCaps.MAX_IMPORT_ATTENDANCE_DATES_PER_STUDENT) {
                attendanceRepository.insertAttendance(any())
            }
        }

    @Test
    fun `a capped import succeeds rather than reporting a parse failure`() =
        runTest {
            coEvery { attendanceRepository.insertAttendance(any()) } returns Result.success(1L)

            val summary =
                run(
                    students = listOf(student("A", dateCount = ImportCaps.MAX_IMPORT_ATTENDANCE_DATES_PER_STUDENT + 1)),
                    hours = emptyList()
                )

            assertEquals(1, summary.cappedEntriesSkippedCount)
            assertTrue(summary.studentCapHit.not(), "the student cap is not the one that was hit")
        }

    // endregion
}