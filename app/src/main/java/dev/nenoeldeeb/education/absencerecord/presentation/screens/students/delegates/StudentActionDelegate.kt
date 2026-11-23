package dev.nenoeldeeb.education.absencerecord.presentation.screens.students.delegates

import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError
import dev.nenoeldeeb.education.absencerecord.domain.usecases.StudentManagementUseCases
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText

/**
 * Student CRUD wrappers returning `Result<UiText?>`; the ViewModel (or a bulk
 * delegate) owns the state updates for them. Import/export orchestration lives
 * in [ImportExportDelegate].
 */
class StudentActionDelegate(
    private val studentManagementUseCases: StudentManagementUseCases
) {
    suspend fun addStudent(
        name: String,
        classId: Int?
    ): Result<UiText?> {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            return Result.failure(StudentError.Validation("Name must not be blank"))
        }
        return studentManagementUseCases.addStudentUseCase(Student(name = trimmed, classId = classId))
            .fold(
                onSuccess = { Result.success(UiText.StringResource(R.string.student_added, trimmed) as UiText?) },
                onFailure = { Result.failure(it) }
            )
    }

    suspend fun updateStudent(
        student: Student,
        newName: String,
        newClassId: Int?
    ): Result<UiText?> {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) {
            return Result.failure(StudentError.Validation("Name must not be blank"))
        }
        return studentManagementUseCases.updateStudentUseCase(student.copy(name = trimmed, classId = newClassId))
            .fold(
                onSuccess = { Result.success(UiText.StringResource(R.string.student_updated, trimmed) as UiText?) },
                onFailure = { Result.failure(it) }
            )
    }

    suspend fun deleteSelectedStudents(
        selectedIds: Set<Int>,
        allStudents: List<Student>
    ): Result<UiText?> {
        val studentsToDelete = allStudents.filter { it.id in selectedIds }
        if (studentsToDelete.isEmpty()) {
            return Result.success(UiText.StringResource(R.string.students_deleted_successfully) as UiText?)
        }
        return studentManagementUseCases.deleteStudentsUseCase(studentsToDelete)
            .fold(
                onSuccess = { Result.success(UiText.StringResource(R.string.students_deleted_successfully) as UiText?) },
                onFailure = { Result.failure(it) }
            )
    }

    suspend fun exportSelectedStudents(
        uriString: String,
        selectedIds: Set<Int>,
        allStudents: List<Student>
    ): Result<UiText?> {
        val filteredStudents = allStudents.filter { it.id in selectedIds }
        return studentManagementUseCases.exportStudentsUseCase(uriString, selectedIds, filteredStudents)
            .fold(
                onSuccess = { Result.success(UiText.StringResource(R.string.data_exported_successfully) as UiText?) },
                onFailure = { e -> Result.failure(e) }
            )
    }

    suspend fun exportAndDeleteSelectedStudents(
        uriString: String,
        selectedIds: Set<Int>,
        allStudents: List<Student>
    ): Result<UiText?> {
        val filteredStudents = allStudents.filter { it.id in selectedIds }
        return studentManagementUseCases.exportStudentsUseCase(uriString, selectedIds, filteredStudents)
            .fold(
                onSuccess = {
                    studentManagementUseCases.deleteStudentsUseCase(filteredStudents)
                        .fold(
                            onSuccess = {
                                Result.success(
                                    UiText.StringResource(R.string.data_exported_and_deleted_successfully) as UiText?
                                )
                            },
                            onFailure = { e -> Result.failure(e) }
                        )
                },
                onFailure = { e -> Result.failure(e) }
            )
    }
}