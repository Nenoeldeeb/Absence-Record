package dev.nenoeldeeb.education.absencerecord.presentation.screens.students

import dev.nenoeldeeb.education.absencerecord.MainDispatcherRule
import dev.nenoeldeeb.education.absencerecord.domain.usecases.ClassManagementUseCases
import dev.nenoeldeeb.education.absencerecord.domain.usecases.StudentManagementUseCases
import dev.nenoeldeeb.education.absencerecord.domain.usecases.student.AddStudentUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.student.DeleteStudentsUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.student.GetAllStudentsUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.student.UpdateStudentUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.transfer.ExportStudentsUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.transfer.ParseImportFileUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.transfer.PerformImportUseCase
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.delegates.ImportExportDelegate
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.delegates.SelectionStateDelegate
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
open class StudentsViewModelTestBase {
    companion object {
        @JvmStatic
        @RegisterExtension
        val mainDispatcherRule = MainDispatcherRule()
    }

    protected lateinit var addStudentUseCase: AddStudentUseCase
    protected lateinit var updateStudentUseCase: UpdateStudentUseCase
    protected lateinit var deleteStudentsUseCase: DeleteStudentsUseCase
    protected lateinit var getAllStudentsUseCase: GetAllStudentsUseCase
    protected lateinit var parseImportFileUseCase: ParseImportFileUseCase
    protected lateinit var performImportUseCase: PerformImportUseCase
    protected lateinit var exportStudentsUseCase: ExportStudentsUseCase
    protected lateinit var studentManagementUseCases: StudentManagementUseCases
    protected lateinit var selectionDelegate: SelectionStateDelegate
    protected lateinit var importExportDelegate: ImportExportDelegate
    protected lateinit var viewModel: StudentsViewModel
    protected lateinit var classManagementUseCases: ClassManagementUseCases

    fun commonSetUp() {
        addStudentUseCase = mockk(relaxed = true)
        updateStudentUseCase = mockk(relaxed = true)
        deleteStudentsUseCase = mockk(relaxed = true)
        getAllStudentsUseCase = mockk(relaxed = true)
        parseImportFileUseCase = mockk(relaxed = true)
        performImportUseCase = mockk(relaxed = true)
        exportStudentsUseCase = mockk(relaxed = true)

        studentManagementUseCases = mockk(relaxed = true)
        every { studentManagementUseCases.addStudentUseCase } returns addStudentUseCase
        every { studentManagementUseCases.updateStudentUseCase } returns updateStudentUseCase
        every { studentManagementUseCases.deleteStudentsUseCase } returns deleteStudentsUseCase
        every { studentManagementUseCases.getAllStudentsUseCase } returns getAllStudentsUseCase
        every { studentManagementUseCases.parseImportFileUseCase } returns parseImportFileUseCase
        every { studentManagementUseCases.performImportUseCase } returns performImportUseCase
        every { studentManagementUseCases.exportStudentsUseCase } returns exportStudentsUseCase

        selectionDelegate = SelectionStateDelegate()
        importExportDelegate = ImportExportDelegate()

        classManagementUseCases = mockk<ClassManagementUseCases>(relaxed = true)
        every { classManagementUseCases.getAllClassesUseCase() } returns flowOf(Result.success(emptyList()))
    }

    protected fun createViewModel() {
        viewModel =
            StudentsViewModel(
                studentManagementUseCases,
                classManagementUseCases,
                selectionDelegate,
                importExportDelegate
            )
    }
}