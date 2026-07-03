package dev.nenoeldeeb.education.absencerecord.domain.usecases

import dev.nenoeldeeb.education.absencerecord.domain.repositories.AttendanceRepository
import dev.nenoeldeeb.education.absencerecord.domain.repositories.StorageRepository
import dev.nenoeldeeb.education.absencerecord.domain.repositories.StudentClassRepository
import dev.nenoeldeeb.education.absencerecord.domain.repositories.StudentRepository
import dev.nenoeldeeb.education.absencerecord.domain.services.SerializationService
import dev.nenoeldeeb.education.absencerecord.domain.usecases.student.AddStudentUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.student.DeleteStudentsUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.student.GetAllStudentsUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.student.UpdateStudentUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.transfer.ExportStudentsUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.transfer.ParseImportFileUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.transfer.PerformImportUseCase

data class StudentManagementUseCases(
    private val studentRepository: StudentRepository,
    private val attendanceRepository: AttendanceRepository,
    private val storageRepository: StorageRepository,
    private val serializationService: SerializationService,
    private val studentClassRepository: StudentClassRepository
) {
    val addStudentUseCase: AddStudentUseCase = AddStudentUseCase(studentRepository)
    val updateStudentUseCase: UpdateStudentUseCase = UpdateStudentUseCase(studentRepository)
    val deleteStudentsUseCase: DeleteStudentsUseCase = DeleteStudentsUseCase(studentRepository)
    val getAllStudentsUseCase: GetAllStudentsUseCase = GetAllStudentsUseCase(studentRepository)
    val parseImportFileUseCase: ParseImportFileUseCase = ParseImportFileUseCase(storageRepository, serializationService)
    val performImportUseCase: PerformImportUseCase =
        PerformImportUseCase(studentRepository, attendanceRepository, studentClassRepository)
    val exportStudentsUseCase: ExportStudentsUseCase =
        ExportStudentsUseCase(
            attendanceRepository,
            storageRepository,
            serializationService,
            studentClassRepository
        )
}