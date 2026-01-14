package dev.nenoeldeeb.education.absencerecord.domain.usecases.student

import dev.nenoeldeeb.education.absencerecord.domain.models.SortType
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.repositories.StudentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number

class GetAllStudentsUseCase(
    private val repository: StudentRepository
) {
    operator fun invoke(
        sortType: SortType = SortType.ByName,
        month: LocalDate? = null
    ): Flow<Result<List<Student>>> {
        val monthYear =
            month?.let {
                "${it.year}-${it.month.number.toString().padStart(2, '0')}"
            }
        return when (sortType) {
            SortType.ByName ->
                monthYear?.let {
                    repository.getStudentsActiveInMonthSortedByName(it)
                } ?: repository.getAllStudents()

            SortType.ByAttendance ->
                monthYear?.let {
                    repository.getAllStudentsSortedByAttendanceForMonth(it)
                } ?: repository.getAllStudentsSortedByAttendance()
        }
    }
}