package dev.nenoeldeeb.education.absencerecord.domain.usecases

import dev.nenoeldeeb.education.absencerecord.domain.repositories.StudentClassRepository
import dev.nenoeldeeb.education.absencerecord.domain.repositories.StudentRepository
import dev.nenoeldeeb.education.absencerecord.domain.usecases.classes.AddClassUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.classes.DeleteClassUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.classes.GetAllClassesUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.classes.UpdateClassUseCase

data class ClassManagementUseCases(
    private val studentClassRepository: StudentClassRepository,
    private val studentRepository: StudentRepository
) {
    val getAllClassesUseCase: GetAllClassesUseCase = GetAllClassesUseCase(studentClassRepository)
    val addClassUseCase: AddClassUseCase = AddClassUseCase(studentClassRepository)
    val updateClassUseCase: UpdateClassUseCase = UpdateClassUseCase(studentClassRepository)
    val deleteClassUseCase: DeleteClassUseCase = DeleteClassUseCase(studentClassRepository, studentRepository)
}