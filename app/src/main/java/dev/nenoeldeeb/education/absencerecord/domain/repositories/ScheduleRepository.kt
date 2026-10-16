package dev.nenoeldeeb.education.absencerecord.domain.repositories

import dev.nenoeldeeb.education.absencerecord.domain.models.AssignmentRemovalReport
import dev.nenoeldeeb.education.absencerecord.domain.models.AvailableLessonHour
import dev.nenoeldeeb.education.absencerecord.domain.models.BusyAppointment
import dev.nenoeldeeb.education.absencerecord.domain.models.HourReplanResult
import dev.nenoeldeeb.education.absencerecord.domain.models.HourWithOccupancy
import dev.nenoeldeeb.education.absencerecord.domain.models.LessonAssignment
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentScheduleView
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.DayOfWeek

interface ScheduleRepository {
    fun observeHours(): Flow<Result<List<AvailableLessonHour>>>

    fun observeHoursForWeekday(weekday: DayOfWeek): Flow<Result<List<HourWithOccupancy>>>

    fun observeAssignments(): Flow<Result<List<LessonAssignment>>>

    fun observeBusyAppointments(): Flow<Result<List<BusyAppointment>>>

    fun observeStudentSchedule(studentId: Int): Flow<Result<StudentScheduleView>>

    suspend fun insertHour(
        weekday: DayOfWeek,
        startMinutes: Int,
        durationMinutes: Int
    ): Result<Int>

    /**
     * Edits a block's length or start time.
     *
     * The first call returns [HourReplanResult.NeedsConfirmation] without writing
     * anything, so the teacher can see the resulting order and who would be
     * dropped. A second call with `confirmed = true` commits and returns
     * [HourReplanResult.Applied] (FR-011, FR-013).
     */
    suspend fun updateHour(
        id: Int,
        weekday: DayOfWeek,
        startMinutes: Int,
        durationMinutes: Int,
        confirmed: Boolean = false
    ): Result<HourReplanResult>

    suspend fun deleteHour(id: Int): Result<Unit>

    suspend fun assignStudent(
        hourId: Int,
        studentId: Int,
        weekday: DayOfWeek
    ): Result<Unit>

    suspend fun unassignStudent(
        hourId: Int,
        studentId: Int
    ): Result<Unit>

    suspend fun insertBusyAppointment(
        studentId: Int,
        weekday: DayOfWeek,
        startMinutes: Int,
        durationMinutes: Int
    ): Result<AssignmentRemovalReport>

    suspend fun updateBusyAppointment(
        id: Int,
        startMinutes: Int,
        durationMinutes: Int
    ): Result<AssignmentRemovalReport>

    suspend fun deleteBusyAppointment(id: Int): Result<Unit>
}