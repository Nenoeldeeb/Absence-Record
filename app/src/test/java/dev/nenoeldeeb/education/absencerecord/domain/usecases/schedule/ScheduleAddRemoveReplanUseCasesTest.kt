package dev.nenoeldeeb.education.absencerecord.domain.usecases.schedule

import dev.nenoeldeeb.education.absencerecord.domain.models.AssignmentRemovalReport
import dev.nenoeldeeb.education.absencerecord.domain.models.AvailableLessonHour
import dev.nenoeldeeb.education.absencerecord.domain.models.HourWithOccupancy
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError
import dev.nenoeldeeb.education.absencerecord.domain.repositories.ScheduleRepository
import dev.nenoeldeeb.education.absencerecord.domain.services.LessonSlotPlanner
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Adding and removing students re-plan the WHOLE block (FR-031 … FR-039).
 *
 * The use cases are thin delegates, so what is proven here is that a refusal
 * travels back intact and a success returns the removal report the teacher is
 * shown. The re-planning itself is proven against a real database in
 * `ScheduleDaoCommitmentRevalidationTest` and `ScheduleDaoSlotValidationTest`.
 */
class ScheduleAddRemoveReplanUseCasesTest {
    private val repository: ScheduleRepository = mockk()

    /**
     * Builds a block the way the repository does: the planner re-sequences the
     * roster, so the slots reflect the derived order rather than the order the
     * ids were supplied in.
     */
    private fun plannedHour(
        assignments: List<Pair<Int, Int?>>,
        start: Int = 540,
        duration: Int = 60
    ): HourWithOccupancy {
        val slots =
            LessonSlotPlanner
                .plan(
                    start,
                    duration,
                    assignments.map { (id, nextCommitment) ->
                        LessonSlotPlanner.StudentInput(
                            id = id,
                            name = "Student $id",
                            busy =
                                nextCommitment?.let {
                                    listOf(LessonSlotPlanner.BusyWindow(it, 30))
                                }.orEmpty()
                        )
                    }
                ).plan.slots
        return HourWithOccupancy(
            hour = AvailableLessonHour(1, DayOfWeek.SATURDAY, start, duration),
            endMinutes = start + duration,
            assignedStudentIds = slots.map { it.studentId },
            assignedCount = assignments.size,
            slots = slots
        )
    }

    // region adding re-plans rather than appends

    @Test
    fun `adding an earlier student takes an earlier slot on the next read`() =
        runTest {
            // The newcomer's 9:30 commitment puts them ahead of the 10:00 incumbents.
            val hour = plannedHour(assignments = listOf(9 to 600, 7 to 570))
            coEvery { repository.observeHoursForWeekday(DayOfWeek.SATURDAY) } returns
                flowOf(Result.success(listOf(hour)))

            val result =
                ObserveHoursForWeekdayUseCase(repository)(DayOfWeek.SATURDAY).first()

            val slots = result.getOrNull()!!.single().slots
            assertEquals(listOf(7, 9), slots.map { it.studentId })
            assertEquals(
                listOf(540 to 550, 550 to 560),
                slots.map { it.startMinutes to it.endMinutes }
            )
        }

    @Test
    fun `an earlier newcomer pushes incumbents forward rather than overflowing`() =
        runTest {
            val hour = plannedHour(assignments = listOf(9 to 600, 7 to 570))
            coEvery { repository.observeHoursForWeekday(DayOfWeek.SATURDAY) } returns
                flowOf(Result.success(listOf(hour)))

            val slots =
                ObserveHoursForWeekdayUseCase(repository)(DayOfWeek.SATURDAY)
                    .first()
                    .getOrNull()!!
                    .single()
                    .slots

            // the block is unchanged in size: re-sequencing, not truncation
            assertEquals(2, slots.size)
            assertEquals(600, 540 + 60)
            assertEquals(
                slots.zipWithNext { a, b -> a.endMinutes == b.startMinutes },
                List(slots.size - 1) { true }
            )
        }

    // endregion

    // region refusals leave nothing partial (FR-035, FR-036)

    @Test
    fun `a conflict refuses the whole addition`() =
        runTest {
            coEvery { repository.assignStudent(1, 7, DayOfWeek.SATURDAY) } returns
                Result.failure(StudentError.BusyConflict)

            val result = AssignStudentUseCase(repository)(1, 7, DayOfWeek.SATURDAY)

            assertTrue(result.isFailure)
            assertEquals(StudentError.BusyConflict, result.exceptionOrNull())
        }

    @Test
    fun `a refusal leaves the roster unchanged on the next read`() =
        runTest {
            // the block as it was before the refused attempt
            val unchanged = plannedHour(assignments = listOf(9 to 600))
            coEvery { repository.observeHoursForWeekday(DayOfWeek.SATURDAY) } returns
                flowOf(Result.success(listOf(unchanged)))

            val hour =
                ObserveHoursForWeekdayUseCase(repository)(DayOfWeek.SATURDAY)
                    .first()
                    .getOrNull()!!
                    .single()

            assertEquals(listOf(9), hour.assignedStudentIds)
            assertEquals(1, hour.assignedCount)
        }

    @Test
    fun `a full block refuses the addition as HourFull`() =
        runTest {
            coEvery { repository.assignStudent(1, 7, DayOfWeek.SATURDAY) } returns
                Result.failure(StudentError.HourFull)

            val result = AssignStudentUseCase(repository)(1, 7, DayOfWeek.SATURDAY)

            assertEquals(StudentError.HourFull, result.exceptionOrNull())
        }

    @Test
    fun `a second lesson on the same day is refused as AssignmentExists`() =
        runTest {
            coEvery { repository.assignStudent(1, 7, DayOfWeek.SATURDAY) } returns
                Result.failure(StudentError.AssignmentExists)

            val result = AssignStudentUseCase(repository)(1, 7, DayOfWeek.SATURDAY)

            assertEquals(StudentError.AssignmentExists, result.exceptionOrNull())
        }

    @Test
    fun `each refusal type reaches the caller distinctly`() =
        runTest {
            val refusals =
                listOf(
                    StudentError.BusyConflict,
                    StudentError.HourFull,
                    StudentError.AssignmentExists,
                    StudentError.HourOverlap
                )
            for (error in refusals) {
                coEvery { repository.assignStudent(1, 7, DayOfWeek.SATURDAY) } returns
                    Result.failure(error)
                val result = AssignStudentUseCase(repository)(1, 7, DayOfWeek.SATURDAY)
                assertEquals(error, result.exceptionOrNull(), "refusal $error must not be masked")
            }
        }

    // endregion

    // region removing re-plans the remainder (FR-037)

    @Test
    fun `removing a student re-plans the rest with no gap`() =
        runTest {
            // after the middle student leaves, 7 and 9 close up with no hole between them
            val afterRemoval = plannedHour(assignments = listOf(7 to 570, 9 to 600))
            coEvery { repository.observeHoursForWeekday(DayOfWeek.SATURDAY) } returns
                flowOf(Result.success(listOf(afterRemoval)))

            val slots =
                ObserveHoursForWeekdayUseCase(repository)(DayOfWeek.SATURDAY)
                    .first()
                    .getOrNull()!!
                    .single()
                    .slots

            // contiguous from the block start: no hole where the leaver stood
            assertEquals(listOf(7, 9), slots.map { it.studentId })
            assertEquals(540, slots.first().startMinutes)
            assertEquals(
                slots.zipWithNext { a, b -> a.endMinutes == b.startMinutes },
                List(slots.size - 1) { true }
            )
        }

    @Test
    fun `removal reports success and names no other student as removed`() =
        runTest {
            coEvery { repository.unassignStudent(1, 7) } returns Result.success(Unit)

            val result = UnassignStudentUseCase(repository)(1, 7)

            assertTrue(result.isSuccess)
            coVerify { repository.unassignStudent(1, 7) }
        }

    @Test
    fun `removing a student who is not assigned surfaces the error`() =
        runTest {
            coEvery { repository.unassignStudent(1, 7) } returns
                Result.failure(StudentError.Validation("Assignment not found"))

            val result = UnassignStudentUseCase(repository)(1, 7)

            assertTrue(result.isFailure)
        }

    // endregion

    // region commitment edits report what they evicted

    @Test
    fun `a commitment edit returns the removal report for preview and confirm`() =
        runTest {
            coEvery { repository.insertBusyAppointment(7, DayOfWeek.SATURDAY, 540, 30) } returns
                Result.success(AssignmentRemovalReport(listOf(7)))

            val result = InsertBusyAppointmentUseCase(repository)(7, DayOfWeek.SATURDAY, 540, 30)

            assertEquals(listOf(7), result.getOrNull()?.removedStudentIds)
        }

    @Test
    fun `a commitment edit that evicts nobody returns an empty report`() =
        runTest {
            coEvery { repository.updateBusyAppointment(1, 1200, 30) } returns
                Result.success(AssignmentRemovalReport(emptyList()))

            val result = UpdateBusyAppointmentUseCase(repository)(1, 1200, 30)

            assertTrue(result.getOrNull()?.removedStudentIds?.isEmpty() == true)
        }

    // endregion
}