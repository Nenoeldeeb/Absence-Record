package dev.nenoeldeeb.education.absencerecord.data.repositories

import dev.nenoeldeeb.education.absencerecord.data.datasources.local.daos.StudentDao
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.entities.StudentEntity
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StudentRepositoryImplTest {
    private val studentDao: StudentDao = mockk()
    private val repository = StudentRepositoryImpl(studentDao)

    @Test
    fun `getAllStudents returns success with mapped students`() =
        runTest {
            // Arrange
            val entity = StudentEntity(id = 1, name = "Test Student")
            every { studentDao.getAllStudents() } returns flowOf(listOf(entity))

            // Act
            val result = repository.getAllStudents().first()

            // Assert
            assertTrue(result.isSuccess)
            val students = result.getOrNull()
            assertEquals(1, students?.size)
            assertEquals(1, students?.first()?.id)
            assertEquals("Test Student", students?.first()?.name)
        }

    @Test
    fun `getStudentById returns success with mapped student when found`() =
        runTest {
            // Arrange
            val entity = StudentEntity(id = 2, name = "Existing", classId = 7)
            every { studentDao.getStudentById(2) } returns flowOf(entity)

            // Act
            val result = repository.getStudentById(2).first()

            // Assert
            assertTrue(result.isSuccess)
            val student = result.getOrNull()
            assertEquals(2, student?.id)
            assertEquals("Existing", student?.name)
            assertEquals(7, student?.classId)
        }

    @Test
    fun `getStudentById returns success with null for unknown id`() =
        runTest {
            // Arrange
            every { studentDao.getStudentById(999) } returns flowOf(null)

            // Act
            val result = repository.getStudentById(999).first()

            // Assert
            assertTrue(result.isSuccess)
            assertNull(result.getOrNull())
        }

    @Test
    fun `getStudentById returns failure when dao fails`() =
        runTest {
            // Arrange
            val exception = RuntimeException("Dao Error")
            val errorFlow =
                kotlinx.coroutines.flow.flow<StudentEntity?> {
                    throw exception
                }
            every { studentDao.getStudentById(1) } returns errorFlow

            // Act
            val result = repository.getStudentById(1).first()

            // Assert
            assertTrue(result.isFailure)
            assertEquals(StudentError.Database, result.exceptionOrNull())
        }

    @Test
    fun `getAllStudents returns failure when dao fails`() =
        runTest {
            // Arrange
            val exception = RuntimeException("Dao Error")
            val errorFlow = kotlinx.coroutines.flow.flow<List<StudentEntity>> { throw exception }
            every { studentDao.getAllStudents() } returns errorFlow

            // Act
            val result = repository.getAllStudents().first()

            // Assert
            assertTrue(result.isFailure)
            assertEquals(StudentError.Database, result.exceptionOrNull())
        }

    @Test
    fun `insertStudent calls dao and returns success`() =
        runTest {
            // Arrange
            val student = Student(name = "New Student")
            coEvery { studentDao.insertStudent(any()) } returns 1L

            // Act
            val result = repository.insertStudent(student)

            // Assert
            assertTrue(result.isSuccess)
            assertEquals(1L, result.getOrThrow())
            coVerify { studentDao.insertStudent(match { it.name == "New Student" }) }
        }

    @Test
    fun `insertStudent returns failure on exception`() =
        runTest {
            // Arrange
            val student = Student(name = "Fail Student")
            val exception = RuntimeException("Insert Fail")
            coEvery { studentDao.insertStudent(any()) } throws exception

            // Act
            val result = repository.insertStudent(student)

            // Assert
            assertTrue(result.isFailure)
            assertEquals(StudentError.Database, result.exceptionOrNull())
        }

    @Test
    fun `deleteStudents calls dao and returns success`() =
        runTest {
            // Arrange
            val student = Student(id = 5, name = "Delete Me")
            coEvery { studentDao.deleteStudents(any()) } returns Unit

            // Act
            val result = repository.deleteStudents(listOf(student))

            // Assert
            assertTrue(result.isSuccess)
            coVerify { studentDao.deleteStudents(match { it.first().id == 5 }) }
        }

    @Test
    fun `deleteStudents returns StudentError_Database on failure`() =
        runTest {
            // Arrange
            val student = Student(id = 5, name = "Delete Me")
            coEvery { studentDao.deleteStudents(any()) } throws RuntimeException("Delete Fail")

            // Act
            val result = repository.deleteStudents(listOf(student))

            // Assert
            assertTrue(result.isFailure)
            assertEquals(StudentError.Database, result.exceptionOrNull())
        }
}