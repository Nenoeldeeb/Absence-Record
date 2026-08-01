package dev.nenoeldeeb.education.absencerecord.presentation.screens.students.delegates

import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentClass
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError
import dev.nenoeldeeb.education.absencerecord.domain.usecases.ClassManagementUseCases
import dev.nenoeldeeb.education.absencerecord.domain.usecases.classes.AddClassUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.classes.DeleteClassUseCase
import dev.nenoeldeeb.education.absencerecord.domain.usecases.classes.UpdateClassUseCase
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ClassActionDelegateTest {
    private lateinit var addClassUseCase: AddClassUseCase
    private lateinit var updateClassUseCase: UpdateClassUseCase
    private lateinit var deleteClassUseCase: DeleteClassUseCase
    private lateinit var classManagementUseCases: ClassManagementUseCases
    private lateinit var delegate: ClassActionDelegate

    @BeforeEach
    fun setUp() {
        addClassUseCase = mockk(relaxed = true)
        updateClassUseCase = mockk(relaxed = true)
        deleteClassUseCase = mockk(relaxed = true)

        classManagementUseCases = mockk(relaxed = true)
        every { classManagementUseCases.addClassUseCase } returns addClassUseCase
        every { classManagementUseCases.updateClassUseCase } returns updateClassUseCase
        every { classManagementUseCases.deleteClassUseCase } returns deleteClassUseCase

        delegate = ClassActionDelegate(classManagementUseCases)
    }

    @Test
    fun `addClass returns success toast`() =
        runTest {
            coEvery { addClassUseCase(any()) } returns Result.success(1L)

            val result = delegate.addClass("Class A")

            assertTrue(result.isSuccess)
            val expected = UiText.StringResource(R.string.class_added, "Class A")
            assertEquals(expected, result.getOrNull())
        }

    @Test
    fun `addClass with blank name returns Validation error`() =
        runTest {
            val result = delegate.addClass("  ")

            assertTrue(result.isFailure)
            val error = result.exceptionOrNull()
            assertIs<StudentError.Validation>(error)
        }

    @Test
    fun `addClass with duplicate name returns DuplicateClass error`() =
        runTest {
            coEvery { addClassUseCase(any()) } returns
                Result.failure(StudentError.DuplicateClass)

            val result = delegate.addClass("Existing")

            assertTrue(result.isFailure)
            val error = result.exceptionOrNull()
            assertIs<StudentError.DuplicateClass>(error)
        }

    @Test
    fun `renameClass returns success toast`() =
        runTest {
            val studentClass = StudentClass(id = 1, name = "Old Name")
            coEvery { updateClassUseCase(any()) } returns Result.success(Unit)

            val result = delegate.renameClass(studentClass, "New Name")

            assertTrue(result.isSuccess)
            val expected = UiText.StringResource(R.string.class_updated, "New Name")
            assertEquals(expected, result.getOrNull())
        }

    @Test
    fun `renameClass with blank name returns Validation error`() =
        runTest {
            val studentClass = StudentClass(id = 1, name = "Old Name")

            val result = delegate.renameClass(studentClass, "  ")

            assertTrue(result.isFailure)
            val error = result.exceptionOrNull()
            assertIs<StudentError.Validation>(error)
        }

    @Test
    fun `renameClass with duplicate name returns DuplicateClass error`() =
        runTest {
            val studentClass = StudentClass(id = 1, name = "Old Name")
            coEvery { updateClassUseCase(any()) } returns
                Result.failure(StudentError.DuplicateClass)

            val result = delegate.renameClass(studentClass, "Existing")

            assertTrue(result.isFailure)
            val error = result.exceptionOrNull()
            assertIs<StudentError.DuplicateClass>(error)
        }

    @Test
    fun `deleteClass returns success toast`() =
        runTest {
            val studentClass = StudentClass(id = 1, name = "Class A")
            coEvery { deleteClassUseCase(any()) } returns Result.success(Unit)

            val result = delegate.deleteClass(studentClass)

            assertTrue(result.isSuccess)
            val expected = UiText.StringResource(R.string.class_deleted)
            assertEquals(expected, result.getOrNull())
        }

    @Test
    fun `onDeletedClassFilterFallback removes deleted class id from set`() {
        val deletedClass = StudentClass(id = 1, name = "Class A")

        val result = delegate.onDeletedClassFilterFallback(setOf(1, 2), deletedClass)

        assertEquals(setOf(2), result)
    }

    @Test
    fun `onDeletedClassFilterFallback with non-selected class returns set unchanged`() {
        val deletedClass = StudentClass(id = 1, name = "Class A")

        val result = delegate.onDeletedClassFilterFallback(setOf(2, 3), deletedClass)

        assertEquals(setOf(2, 3), result)
    }
}