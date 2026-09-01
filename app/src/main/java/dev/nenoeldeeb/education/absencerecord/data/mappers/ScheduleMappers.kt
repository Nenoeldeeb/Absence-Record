package dev.nenoeldeeb.education.absencerecord.data.mappers

import dev.nenoeldeeb.education.absencerecord.data.datasources.local.entities.AvailableLessonHourEntity
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.entities.BusyAppointmentEntity
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.entities.LessonAssignmentEntity
import dev.nenoeldeeb.education.absencerecord.domain.models.AvailableLessonHour
import dev.nenoeldeeb.education.absencerecord.domain.models.BusyAppointment
import dev.nenoeldeeb.education.absencerecord.domain.models.LessonAssignment
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.isoDayNumber

fun AvailableLessonHourEntity.toAvailableLessonHour(): AvailableLessonHour =
    AvailableLessonHour(
        id = id,
        weekday = DayOfWeek(weekday),
        startMinutes = startMinutes,
        maxStudents = maxStudents
    )

fun AvailableLessonHour.toAvailableLessonHourEntity(): AvailableLessonHourEntity =
    AvailableLessonHourEntity(
        id = id,
        weekday = weekday.isoDayNumber,
        startMinutes = startMinutes,
        maxStudents = maxStudents
    )

fun LessonAssignmentEntity.toLessonAssignment(): LessonAssignment =
    LessonAssignment(
        id = id,
        availableHourId = availableHourId,
        studentId = studentId,
        weekday = DayOfWeek(weekday)
    )

fun LessonAssignment.toLessonAssignmentEntity(): LessonAssignmentEntity =
    LessonAssignmentEntity(
        id = id,
        availableHourId = availableHourId,
        studentId = studentId,
        weekday = weekday.isoDayNumber
    )

fun BusyAppointmentEntity.toBusyAppointment(): BusyAppointment =
    BusyAppointment(
        id = id,
        studentId = studentId,
        weekday = DayOfWeek(weekday),
        startMinutes = startMinutes,
        durationMinutes = durationMinutes
    )

fun BusyAppointment.toBusyAppointmentEntity(): BusyAppointmentEntity =
    BusyAppointmentEntity(
        id = id,
        studentId = studentId,
        weekday = weekday.isoDayNumber,
        startMinutes = startMinutes,
        durationMinutes = durationMinutes
    )