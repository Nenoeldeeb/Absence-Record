package dev.nenoeldeeb.education.absencerecord.domain.usecases

import dev.nenoeldeeb.education.absencerecord.domain.repositories.AttendanceRepository
import dev.nenoeldeeb.education.absencerecord.domain.repositories.StorageRepository
import dev.nenoeldeeb.education.absencerecord.domain.repositories.StudentRepository
import dev.nenoeldeeb.education.absencerecord.domain.services.SerializationService
import dev.nenoeldeeb.education.absencerecord.domain.usecases.student.AddStudentUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.student.DeleteStudentsUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.student.GetAllStudentsUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.student.UpdateStudentUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.transfer.ExportStudentsUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.transfer.ImportStudentsUseCase

data class StudentManagementUseCases(
    private val studentRepository: StudentRepository,
    private val attendanceRepository: AttendanceRepository,
    private val storageRepository: StorageRepository,
    private val serializationService: SerializationService
) {
    val addStudentUseCase: AddStudentUseCase = AddStudentUseCase(studentRepository)
    val updateStudentUseCase: UpdateStudentUseCase = UpdateStudentUseCase(studentRepository)
    val deleteStudentsUseCase: DeleteStudentsUseCase = DeleteStudentsUseCase(studentRepository)
    val getAllStudentsUseCase: GetAllStudentsUseCase = GetAllStudentsUseCase(studentRepository)
    val importStudentsUseCase: ImportStudentsUseCase =
        ImportStudentsUseCase(
            studentRepository,
            attendanceRepository,
            storageRepository,
            serializationService
        )
    val exportStudentsUseCase: ExportStudentsUseCase =
        ExportStudentsUseCase(
            attendanceRepository,
            storageRepository,
            serializationService
        )
}