package dev.nenoeldeeb.education.absencerecord.domain.repositories

import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import kotlinx.coroutines.flow.Flow

interface StudentRepository {
    fun getAllStudents(): Flow<Result<List<Student>>>

    fun getAllStudentsSortedByAttendance(): Flow<Result<List<Student>>>

    suspend fun insertStudent(student: Student): Result<Long>

    suspend fun updateStudent(student: Student): Result<Unit>

    suspend fun unassignStudentsFromClass(classId: Int): Result<Unit>

    suspend fun deleteStudents(students: List<Student>): Result<Unit>

    fun getStudentsActiveInMonthSortedByName(monthYear: String): Flow<Result<List<Student>>>

    fun getAllStudentsSortedByAttendanceForMonth(monthYear: String): Flow<Result<List<Student>>>
}