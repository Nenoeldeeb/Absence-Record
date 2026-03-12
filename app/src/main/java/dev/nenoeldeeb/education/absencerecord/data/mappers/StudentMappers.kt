package dev.nenoeldeeb.education.absencerecord.data.mappers

import dev.nenoeldeeb.education.absencerecord.data.datasources.local.entities.AttendanceHistoryItemEntity
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.entities.StudentAttendanceEntity
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.entities.StudentClassEntity
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.entities.StudentEntity
import dev.nenoeldeeb.education.absencerecord.domain.models.AttendanceHistoryItem
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentAttendance
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentClass

fun StudentEntity.toStudent(): Student =
    Student(id = this.id, name = this.name, classId = this.classId)

fun Student.toStudentEntity(): StudentEntity =
    StudentEntity(id = this.id, name = this.name, classId = this.classId)

fun StudentAttendanceEntity.toStudentAttendance(): StudentAttendance =
    StudentAttendance(id = this.id, studentId = this.studentId, date = this.date)

fun StudentAttendance.toStudentAttendanceEntity(): StudentAttendanceEntity =
    StudentAttendanceEntity(id = this.id, studentId = this.studentId, date = this.date)

fun AttendanceHistoryItemEntity.toAttendanceHistoryItem(): AttendanceHistoryItem =
    AttendanceHistoryItem(date = this.date, name = this.studentName)

fun StudentClassEntity.toStudentClass(): StudentClass = StudentClass(id = this.id, name = this.name)

fun StudentClass.toStudentClassEntity(): StudentClassEntity =
    StudentClassEntity(id = this.id, name = this.name)
