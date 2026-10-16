package dev.nenoeldeeb.education.absencerecord.data.repositories

import dev.nenoeldeeb.education.absencerecord.data.datasources.local.daos.ScheduleDao
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.daos.StudentNameRow
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.entities.AvailableLessonHourEntity
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.entities.BusyAppointmentEntity
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.entities.LessonAssignmentEntity
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The derived running order as it reaches the UI (FR-019 … FR-025, FR-060).
 *
 * These cover the read path specifically: the planner's own ordering rules are
 * proven in [dev.nenoeldeeb.education.absencerecord.domain.services], so what
 * matters here is that the repository actually DERIVES on every read rather than
 * passing stored row order through.
 */
class ScheduleRepositoryDerivedOrderTest {
    private val scheduleDao: ScheduleDao = mockk()
    private val repository = ScheduleRepositoryImpl(scheduleDao)

    private fun stub(
        hours: List<AvailableLessonHourEntity> = listOf(hour()),
        assignments: List<LessonAssignmentEntity>,
        busy: List<BusyAppointmentEntity> = emptyList(),
        names: List<StudentNameRow> = emptyList()
    ) {
        every { scheduleDao.observeHoursForWeekday(6) } returns flowOf(hours)
        every { scheduleDao.observeAssignments() } returns flowOf(assignments)
        every { scheduleDao.observeBusyAppointmentsForWeekday(6) } returns flowOf(busy)
        every { scheduleDao.observeStudentNames() } returns flowOf(names)
    }

    private fun hour(
        id: Int = 1,
        start: Int = 540,
        duration: Int = 50
    ) = AvailableLessonHourEntity(id = id, weekday = 6, startMinutes = start, durationMinutes = duration)

    private fun assignment(
        studentId: Int,
        hourId: Int = 1,
        rowId: Int = 0
    ) = LessonAssignmentEntity(id = rowId, availableHourId = hourId, studentId = studentId, weekday = 6)

    private fun busy(
        studentId: Int,
        start: Int,
        duration: Int = 30
    ) = BusyAppointmentEntity(
        id = 0,
        studentId = studentId,
        weekday = 6,
        startMinutes = start,
        durationMinutes = duration
    )

    private fun name(
        id: Int,
        name: String
    ) = StudentNameRow(id = id, name = name)

    @Test
    fun `earliest next commitment runs first`() =
        runTest {
            stub(
                assignments = listOf(assignment(1), assignment(2), assignment(3)),
                busy = listOf(busy(1, 600), busy(2, 630), busy(3, 660)),
                names = listOf(name(1, "Zoe"), name(2, "Adam"), name(3, "Mia"))
            )

            val hour = repository.observeHoursForWeekday(DayOfWeek.SATURDAY).first().getOrNull()!!.single()

            // earliest commitment first, even though "Zoe" is alphabetically last
            assertEquals(listOf(1, 2, 3), hour.assignedStudentIds)
        }

    @Test
    fun `students with no commitment run last, alphabetically`() =
        runTest {
            stub(
                assignments = listOf(assignment(1), assignment(2), assignment(3)),
                busy = listOf(busy(2, 630)),
                names = listOf(name(1, "Zoe"), name(2, "Adam"), name(3, "Mia"))
            )

            val hour = repository.observeHoursForWeekday(DayOfWeek.SATURDAY).first().getOrNull()!!.single()

            assertEquals(listOf(2, 3, 1), hour.assignedStudentIds)
        }

    @Test
    fun `equal commitment times break alphabetically`() =
        runTest {
            stub(
                assignments = listOf(assignment(1), assignment(2)),
                busy = listOf(busy(1, 600), busy(2, 600)),
                names = listOf(name(1, "Zoe"), name(2, "Adam"))
            )

            val hour = repository.observeHoursForWeekday(DayOfWeek.SATURDAY).first().getOrNull()!!.single()

            assertEquals(listOf(2, 1), hour.assignedStudentIds)
        }

    @Test
    fun `stored row order does not affect the derived order`() =
        runTest {
            val forward = listOf(assignment(1, rowId = 1), assignment(2, rowId = 2), assignment(3, rowId = 3))
            val reversed = listOf(assignment(3, rowId = 1), assignment(2, rowId = 2), assignment(1, rowId = 3))
            val busyRows = listOf(busy(1, 660), busy(2, 600), busy(3, 630))
            val names = listOf(name(1, "Zoe"), name(2, "Adam"), name(3, "Mia"))

            stub(assignments = forward, busy = busyRows, names = names)
            val a = repository.observeHoursForWeekday(DayOfWeek.SATURDAY).first().getOrNull()!!.single()
            stub(assignments = reversed, busy = busyRows, names = names)
            val b = repository.observeHoursForWeekday(DayOfWeek.SATURDAY).first().getOrNull()!!.single()

            assertEquals(a.assignedStudentIds, b.assignedStudentIds)
            assertEquals(listOf(2, 3, 1), b.assignedStudentIds)
        }

    @Test
    fun `repeated reads yield an identical order`() =
        runTest {
            stub(
                assignments = listOf(assignment(1), assignment(2)),
                busy = listOf(busy(1, 660), busy(2, 600)),
                names = listOf(name(1, "Zoe"), name(2, "Adam"))
            )

            val flow = repository.observeHoursForWeekday(DayOfWeek.SATURDAY)
            val first = flow.first().getOrNull()!!.single().assignedStudentIds
            val second = flow.first().getOrNull()!!.single().assignedStudentIds
            val third = flow.first().getOrNull()!!.single().assignedStudentIds

            assertEquals(listOf(2, 1), first)
            assertEquals(first, second)
            assertEquals(second, third)
        }

    @Test
    fun `slots are contiguous ten minutes from the block start`() =
        runTest {
            stub(
                assignments = listOf(assignment(1), assignment(2), assignment(3)),
                busy = listOf(busy(1, 660), busy(2, 630), busy(3, 600)),
                names = listOf(name(1, "Zoe"), name(2, "Adam"), name(3, "Mia"))
            )

            val hour = repository.observeHoursForWeekday(DayOfWeek.SATURDAY).first().getOrNull()!!.single()

            assertEquals(
                listOf(540 to 550, 550 to 560, 560 to 570),
                hour.slots.map { it.startMinutes to it.endMinutes }
            )
            assertEquals(listOf(1, 2, 3), hour.slots.map { it.position })
            assertEquals(hour.assignedStudentIds, hour.slots.map { it.studentId })
        }

    @Test
    fun `a rename re-derives the order`() =
        runTest {
            stub(
                assignments = listOf(assignment(1), assignment(2)),
                busy = listOf(busy(1, 600), busy(2, 600)),
                names = listOf(name(1, "Zoe"), name(2, "Adam"))
            )
            val before = repository.observeHoursForWeekday(DayOfWeek.SATURDAY).first().getOrNull()!!.single()

            stub(
                assignments = listOf(assignment(1), assignment(2)),
                busy = listOf(busy(1, 600), busy(2, 600)),
                names = listOf(name(1, "Aaron"), name(2, "Adam"))
            )
            val after = repository.observeHoursForWeekday(DayOfWeek.SATURDAY).first().getOrNull()!!.single()

            assertEquals(listOf(2, 1), before.assignedStudentIds)
            assertEquals(listOf(1, 2), after.assignedStudentIds)
        }

    @Test
    fun `a commitment before the block does not drive the order`() =
        runTest {
            // A commitment that is over before 9:00 is not "next" (FR-019).
            stub(
                assignments = listOf(assignment(1), assignment(2)),
                busy = listOf(busy(1, 480, duration = 30), busy(2, 630)),
                names = listOf(name(1, "Zoe"), name(2, "Adam"))
            )

            val hour = repository.observeHoursForWeekday(DayOfWeek.SATURDAY).first().getOrNull()!!.single()

            assertEquals(listOf(2, 1), hour.assignedStudentIds)
        }

    @Test
    fun `occupied count survives ordering`() =
        runTest {
            stub(
                assignments = listOf(assignment(1), assignment(2)),
                busy = listOf(busy(1, 600), busy(2, 600)),
                names = listOf(name(1, "Zoe"), name(2, "Adam"))
            )

            val hour = repository.observeHoursForWeekday(DayOfWeek.SATURDAY).first().getOrNull()!!.single()

            assertEquals(2, hour.assignedCount)
            assertEquals(3, hour.remainingSlots)
        }

    @Test
    fun `an empty block plans to no slots`() =
        runTest {
            stub(assignments = emptyList())

            val hour = repository.observeHoursForWeekday(DayOfWeek.SATURDAY).first().getOrNull()!!.single()

            assertEquals(emptyList(), hour.assignedStudentIds)
            assertEquals(emptyList(), hour.slots)
            assertEquals(0, hour.assignedCount)
        }

    @Test
    fun `a missing name still yields a deterministic order`() =
        runTest {
            stub(
                assignments = listOf(assignment(1), assignment(2)),
                busy = listOf(busy(1, 600), busy(2, 600)),
                names = emptyList()
            )

            val hour = repository.observeHoursForWeekday(DayOfWeek.SATURDAY).first().getOrNull()!!.single()

            assertEquals(listOf(1, 2), hour.assignedStudentIds)
        }

    @Test
    fun `a dao failure still surfaces as Database error`() =
        runTest {
            every { scheduleDao.observeHoursForWeekday(6) } returns
                flow { throw RuntimeException("boom") }
            every { scheduleDao.observeAssignments() } returns flowOf(emptyList())
            every { scheduleDao.observeBusyAppointmentsForWeekday(6) } returns flowOf(emptyList())
            every { scheduleDao.observeStudentNames() } returns flowOf(emptyList())

            val result = repository.observeHoursForWeekday(DayOfWeek.SATURDAY).first()

            assertTrue(result.isFailure)
            assertEquals(StudentError.Database, result.exceptionOrNull())
        }
}