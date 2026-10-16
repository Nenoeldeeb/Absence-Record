package dev.nenoeldeeb.education.absencerecord.domain.usecases.schedule

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

/**
 * What the student sees after a block is re-planned (FR-018, FR-037).
 */
class ScheduleStudentViewAfterReplanTest {
    private val repository: ScheduleRepository = mockk()

    // region the student's own view stays consistent

    @Test
    fun `the student view reports the slot the removal left them with`() =
        runTest {
            val view =
                StudentScheduleView(
                    studentId = 7,
                    lessons =
                        listOf(
                            StudentLessonEntry(
                                DayOfWeek.SATURDAY,
                                540,
                                600,
                                slotStartMinutes = 540,
                                slotEndMinutes = 550
                            )
                        ),
                    busy = emptyList()
                )
            coEvery { repository.observeStudentSchedule(7) } returns flowOf(Result.success(view))

            val lesson =
                ObserveStudentScheduleUseCase(repository)(7)
                    .first()
                    .getOrNull()!!
                    .lessons
                    .single()

            assertEquals(540, lesson.slotStartMinutes)
            assertEquals(550, lesson.slotEndMinutes)
        }

    @Test
    fun `a lesson with no derived slot reports null rather than a guess`() =
        runTest {
            coEvery { repository.observeStudentSchedule(7) } returns
                flowOf(
                    Result.success(
                        StudentScheduleView(
                            studentId = 7,
                            lessons = listOf(StudentLessonEntry(DayOfWeek.SATURDAY, 540, 600)),
                            busy = emptyList()
                        )
                    )
                )

            val lesson =
                ObserveStudentScheduleUseCase(repository)(7)
                    .first()
                    .getOrNull()!!
                    .lessons
                    .single()

            assertNull(lesson.slotStartMinutes)
        }

    // endregion
}