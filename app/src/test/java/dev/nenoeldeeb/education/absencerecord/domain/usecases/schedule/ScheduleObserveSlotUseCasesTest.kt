package dev.nenoeldeeb.education.absencerecord.domain.usecases.schedule

import dev.nenoeldeeb.education.absencerecord.domain.models.AvailableLessonHour
import dev.nenoeldeeb.education.absencerecord.domain.models.HourWithOccupancy
import dev.nenoeldeeb.education.absencerecord.domain.models.PlannedSlot
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentLessonEntry
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentScheduleView
import dev.nenoeldeeb.education.absencerecord.domain.repositories.ScheduleRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Slot derivation as it crosses the use-case layer (FR-016 … FR-018).
 *
 * These assert the use case carries the derived slots through untouched rather
 * than re-deriving or reshaping them.
 */
class ScheduleObserveSlotUseCasesTest {
    private val repository: ScheduleRepository = mockk()

    private fun plannedHour(slotTimes: List<Pair<Int, Int>> = listOf(540 to 550, 550 to 560)) =
        HourWithOccupancy(
            hour = AvailableLessonHour(1, DayOfWeek.SATURDAY, 540, 50),
            endMinutes = 590,
            assignedStudentIds = slotTimes.indices.map { it + 1 },
            assignedCount = slotTimes.size,
            slots =
                slotTimes.mapIndexed { index, (start, end) ->
                    PlannedSlot(
                        studentId = index + 1,
                        studentName = "Student ${index + 1}",
                        position = index + 1,
                        startMinutes = start,
                        endMinutes = end
                    )
                }
        )

    @Test
    fun `observeHoursForWeekday carries derived slots to the caller`() =
        runTest {
            val hours = listOf(plannedHour())
            coEvery { repository.observeHoursForWeekday(DayOfWeek.SATURDAY) } returns
                flowOf(Result.success(hours))

            val result =
                ObserveHoursForWeekdayUseCase(repository)(DayOfWeek.SATURDAY).first()

            val hour = result.getOrNull()!!.single()
            assertEquals(listOf(540 to 550, 550 to 560), hour.slots.map { it.startMinutes to it.endMinutes })
            assertEquals(listOf(1, 2), hour.slots.map { it.studentId })
        }

    @Test
    fun `slotFor resolves the slot belonging to a student`() =
        runTest {
            coEvery { repository.observeHoursForWeekday(DayOfWeek.SATURDAY) } returns
                flowOf(Result.success(listOf(plannedHour())))

            val hour =
                ObserveHoursForWeekdayUseCase(repository)(DayOfWeek.SATURDAY)
                    .first()
                    .getOrNull()!!
                    .single()

            assertEquals(550, hour.slotFor(2)?.startMinutes)
            assertNull(hour.slotFor(99))
        }

    @Test
    fun `observeStudentSchedule exposes the student's own slot`() =
        runTest {
            val view =
                StudentScheduleView(
                    studentId = 7,
                    lessons =
                        listOf(
                            StudentLessonEntry(
                                weekday = DayOfWeek.SATURDAY,
                                startMinutes = 540,
                                endMinutes = 590,
                                slotStartMinutes = 550,
                                slotEndMinutes = 560
                            )
                        ),
                    busy = emptyList()
                )
            coEvery { repository.observeStudentSchedule(7) } returns flowOf(Result.success(view))

            val result = ObserveStudentScheduleUseCase(repository)(7).first()

            val lesson = result.getOrNull()!!.lessons.single()
            assertEquals(550, lesson.slotStartMinutes)
            assertEquals(560, lesson.slotEndMinutes)
            // the block still spans the whole run; only the slot is the student's own
            assertEquals(540, lesson.startMinutes)
            assertEquals(590, lesson.endMinutes)
        }

    @Test
    fun `a lesson without a derived slot reports null rather than guessing`() =
        runTest {
            val view =
                StudentScheduleView(
                    studentId = 7,
                    lessons = listOf(StudentLessonEntry(DayOfWeek.SATURDAY, 540, 590)),
                    busy = emptyList()
                )
            coEvery { repository.observeStudentSchedule(7) } returns flowOf(Result.success(view))

            val lesson =
                ObserveStudentScheduleUseCase(repository)(7)
                    .first()
                    .getOrNull()!!
                    .lessons
                    .single()

            assertNull(lesson.slotStartMinutes)
            assertNull(lesson.slotEndMinutes)
        }

    @Test
    fun `a database failure on the observe path is propagated`() =
        runTest {
            coEvery { repository.observeHoursForWeekday(DayOfWeek.SATURDAY) } returns
                flowOf(Result.failure(StudentError.Database))

            val result =
                ObserveHoursForWeekdayUseCase(repository)(DayOfWeek.SATURDAY).first()

            assertTrue(result.isFailure)
            assertEquals(StudentError.Database, result.exceptionOrNull())
        }
}