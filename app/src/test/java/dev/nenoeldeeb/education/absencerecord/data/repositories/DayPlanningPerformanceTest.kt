package dev.nenoeldeeb.education.absencerecord.data.repositories

import dev.nenoeldeeb.education.absencerecord.data.datasources.local.daos.StudentNameRow
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.entities.AvailableLessonHourEntity
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.entities.BusyAppointmentEntity
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.entities.LessonAssignmentEntity
import dev.nenoeldeeb.education.absencerecord.domain.services.ScheduleRules
import org.junit.jupiter.api.Test
import kotlin.system.measureNanoTime

/**
 * SC-019: the cost of deriving running orders at the largest size the app allows.
 *
 * A week holds at most 1,008 non-overlapping ten-minute blocks. Because every
 * student occupies ten minutes of a block, a day can hold at most
 * 1,440 / 10 = 144 students however the blocks are sized — so the two extreme
 * shapes are "many short blocks" and "few long ones", and both are measured.
 *
 * Measured on the JVM deliberately: the work is pure Kotlin with no Compose and
 * no instrumentation, so this reports the algorithm's cost rather than a debug
 * build's overhead. On-device render numbers are in the instrumented suite.
 */
class DayPlanningPerformanceTest {
    private val daysInWeek = 7
    private val blocksPerDay = 1440 / 10 // 144 non-overlapping ten-minute blocks per day
    private val commitmentsEach = 3

    /**
     * Builds a week of blocks, each [blockMinutes] long and holding exactly as
     * many students as its length allows.
     */
    private fun weekOf(blockMinutes: Int): DayPlanningInputs {
        val perBlock = blockMinutes / 10
        val blocksPerDayAtThisLength = 1440 / blockMinutes
        val hours =
            (0 until daysInWeek).map { day ->
                (0 until blocksPerDayAtThisLength).map { slot ->
                    AvailableLessonHourEntity(
                        id = day * blocksPerDayAtThisLength + slot + 1,
                        // Days 1..7, all real weekdays
                        weekday = day + 1,
                        startMinutes = slot * blockMinutes,
                        durationMinutes = blockMinutes
                    )
                }
            }.flatten()

        val assignments = mutableListOf<LessonAssignmentEntity>()
        val busy = mutableListOf<BusyAppointmentEntity>()
        val names = mutableListOf<StudentNameRow>()
        var nextStudentId = 1
        hours.forEachIndexed { hourIndex, hour ->
            repeat(perBlock) { seat ->
                val studentId = nextStudentId++
                assignments +=
                    LessonAssignmentEntity(
                        id = assignments.size,
                        availableHourId = hour.id,
                        studentId = studentId,
                        weekday = hour.weekday
                    )
                names += StudentNameRow(studentId, "Student $studentId")
                // Scattered so the comparator actually has to sort: some inside
                // the block, some after it, some students with none at all.
                repeat(commitmentsEach) { commitment ->
                    busy +=
                        BusyAppointmentEntity(
                            id = 0,
                            studentId = studentId,
                            weekday = hour.weekday,
                            startMinutes = (studentId * 7 + commitment * 31 + hourIndex) % 1440,
                            durationMinutes = 30
                        )
                }
            }
        }
        return DayPlanningInputs(hours, assignments, busy, names)
    }

    private fun measure(
        label: String,
        inputs: DayPlanningInputs,
        expectedBlocks: Int
    ) {
        inputs.planAllHours() // warm up: class loading is not the cost under test
        val runs = 10
        val timings =
            LongArray(runs) { _ -> measureNanoTime { inputs.planAllHours() } }.sorted()
        val medianMs = timings[runs / 2] / 1_000_000.0
        val worstMs = timings.last() / 1_000_000.0
        println(
            "$label: ${inputs.hours.size} blocks, ${inputs.assignments.size} students, " +
                "${inputs.busy.size} commitments -> " +
                "median ${"%.1f".format(medianMs)} ms, worst ${"%.1f".format(worstMs)} ms"
        )
        check(inputs.hours.size == expectedBlocks)
        check(timings[runs / 2] < 1_000_000_000L) {
            "$label took ${medianMs}ms, over the 1s SC-019 budget"
        }
    }

    @Test
    fun `a week of maximum-length blocks derives inside the budget`() {
        val inputs = weekOf(blockMinutes = ScheduleRules.MAX_HOUR_DURATION_MINUTES)
        measure("SC-019 worst-case blocks", inputs, expectedBlocks = 84)
        inputs.planAllHours().forEach { hour ->
            check(hour.slots.size == 12) { "a 120-minute block should hold 12, held ${hour.slots.size}" }
        }
    }

    @Test
    fun `a week of minimum-length blocks derives inside the budget`() {
        val inputs = weekOf(blockMinutes = ScheduleRules.MIN_HOUR_DURATION_MINUTES)
        measure("SC-019 maximum blocks", inputs, expectedBlocks = blocksPerDay * daysInWeek)
        inputs.planAllHours().forEach { hour ->
            check(hour.slots.size == 1) { "a 10-minute block should hold 1, held ${hour.slots.size}" }
        }
    }

    @Test
    fun `every derived order is correct and reproducible`() {
        val inputs = weekOf(blockMinutes = 60)
        val first = inputs.planAllHours().map { it.assignedStudentIds }

        check(first.all { it.isNotEmpty() })
        check(first.all { it.size == 6 }) { "a 60-minute block should hold 6 students" }
        repeat(3) {
            check(inputs.planAllHours().map { it.assignedStudentIds } == first) {
                "running order was not reproducible on re-read (FR-060)"
            }
        }
    }
}