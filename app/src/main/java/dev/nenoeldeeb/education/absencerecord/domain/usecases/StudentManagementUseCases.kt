package dev.nenoeldeeb.education.absencerecord.domain.usecases

import dev.nenoeldeeb.education.absencerecord.domain.usecases.student.AddStudentUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.student.DeleteStudentsUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.student.GetAllStudentsUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.student.UpdateStudentUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.transfer.ExportStudentsUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.transfer.ImportStudentsUseCase

data class StudentManagementUseCases(
    val addStudentUseCase: AddStudentUseCase,
    val updateStudentUseCase: UpdateStudentUseCase,
    val deleteStudentsUseCase: DeleteStudentsUseCase,
    val getAllStudentsUseCase: GetAllStudentsUseCase,
    val importStudentsUseCase: ImportStudentsUseCase,
    val exportStudentsUseCase: ExportStudentsUseCase
)