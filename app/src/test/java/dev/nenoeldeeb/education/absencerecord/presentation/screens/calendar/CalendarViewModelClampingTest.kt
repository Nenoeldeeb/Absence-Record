package dev.nenoeldeeb.education.absencerecord.presentation.screens.calendar

import dev.nenoeldeeb.education.absencerecord.data.repositories.ClassFilterRepositoryImpl
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentClass
import dev.nenoeldeeb.education.absencerecord.domain.usecases.ClassManagementUseCases
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelClampingTest : CalendarViewModelTestBase() {
    @BeforeEach
    fun setUp() {
        commonSetUp()
    }

    @Test
    fun `attendance count is clamped when filter removes students`() =
        runTest {
            val class1 = StudentClass(1, "Class A")
            val class2 = StudentClass(2, "Class B")
            val date = LocalDate(2026, Month.MARCH, 1)
            val students =
                listOf(
                    Student(1, "S1", classId = 1),
                    Student(2, "S2", classId = 1),
                    Student(3, "S3", classId = 2)
                )
            val classes = listOf(class1, class2)
            val attendance =
                listOf(
                    dev.nenoeldeeb.education.absencerecord.domain.models.StudentAttendance(studentId = 1, date = date),
                    dev.nenoeldeeb.education.absencerecord.domain.models.StudentAttendance(studentId = 2, date = date)
                )

            coEvery { studentManagementUseCases.getAllStudentsUseCase() } returns flowOf(Result.success(students))
            val classManagementUseCases = mockk<ClassManagementUseCases>(relaxed = true)
            coEvery { classManagementUseCases.getAllClassesUseCase() } returns flowOf(Result.success(classes))
            coEvery { getAttendanceForDateUseCase(date) } returns flowOf(Result.success(attendance))

            viewModel =
                CalendarViewModel(
                    attendanceUseCases,
                    studentManagementUseCases,
                    classManagementUseCases,
                    ClassFilterRepositoryImpl()
                )
            advanceUntilIdle()

            viewModel.onEvent(CalendarScreenEvent.SelectDateForDialog(date))
            advanceUntilIdle()

            viewModel.onEvent(CalendarScreenEvent.ToggleClassSelection(1))
            advanceUntilIdle()

            assertEquals(2, viewModel.uiState.value.allStudents.size)
            assertEquals(2, viewModel.uiState.value.studentsForSelectedDate.size)

            viewModel.onEvent(CalendarScreenEvent.ToggleClassSelection(2))
            advanceUntilIdle()

            assertEquals(3, viewModel.uiState.value.allStudents.size)
            assertEquals(2, viewModel.uiState.value.studentsForSelectedDate.size)
        }

    @Test
    fun `present count never exceeds visible count when classes are unchecked`() =
        runTest {
            val class1 = StudentClass(1, "Class A")
            val class2 = StudentClass(2, "Class B")
            val date = LocalDate(2026, Month.MARCH, 1)
            val students =
                listOf(
                    Student(1, "S1", classId = 1),
                    Student(2, "S2", classId = 1),
                    Student(3, "S3", classId = 2)
                )
            val classes = listOf(class1, class2)
            val attendance =
                listOf(
                    dev.nenoeldeeb.education.absencerecord.domain.models.StudentAttendance(studentId = 1, date = date),
                    dev.nenoeldeeb.education.absencerecord.domain.models.StudentAttendance(studentId = 2, date = date),
                    dev.nenoeldeeb.education.absencerecord.domain.models.StudentAttendance(studentId = 3, date = date)
                )

            coEvery { studentManagementUseCases.getAllStudentsUseCase() } returns flowOf(Result.success(students))
            val classManagementUseCases = mockk<ClassManagementUseCases>(relaxed = true)
            coEvery { classManagementUseCases.getAllClassesUseCase() } returns flowOf(Result.success(classes))
            coEvery { getAttendanceForDateUseCase(date) } returns flowOf(Result.success(attendance))

            viewModel =
                CalendarViewModel(
                    attendanceUseCases,
                    studentManagementUseCases,
                    classManagementUseCases,
                    ClassFilterRepositoryImpl()
                )
            advanceUntilIdle()

            viewModel.onEvent(CalendarScreenEvent.SelectDateForDialog(date))
            advanceUntilIdle()
            viewModel.onEvent(CalendarScreenEvent.ToggleClassSelection(1))
            advanceUntilIdle()
            viewModel.onEvent(CalendarScreenEvent.ToggleClassSelection(2))
            advanceUntilIdle()

            assertEquals(3, viewModel.uiState.value.allStudents.size)
            assertEquals(3, viewModel.uiState.value.studentsForSelectedDate.size)

            viewModel.onEvent(CalendarScreenEvent.ToggleClassSelection(2))
            advanceUntilIdle()

            assertEquals(2, viewModel.uiState.value.allStudents.size)
            assertEquals(2, viewModel.uiState.value.studentsForSelectedDate.size)
        }

    @Test
    fun `attendance records persist when student is filtered out and back in`() =
        runTest {
            val class1 = StudentClass(1, "Class A")
            val date = LocalDate(2026, Month.MARCH, 1)
            val students =
                listOf(
                    Student(1, "S1", classId = 1)
                )
            val classes = listOf(class1)
            val attendance =
                listOf(
                    dev.nenoeldeeb.education.absencerecord.domain.models.StudentAttendance(studentId = 1, date = date)
                )

            coEvery { studentManagementUseCases.getAllStudentsUseCase() } returns flowOf(Result.success(students))
            val classManagementUseCases = mockk<ClassManagementUseCases>(relaxed = true)
            coEvery { classManagementUseCases.getAllClassesUseCase() } returns flowOf(Result.success(classes))
            coEvery { getAttendanceForDateUseCase(date) } returns flowOf(Result.success(attendance))

            viewModel =
                CalendarViewModel(
                    attendanceUseCases,
                    studentManagementUseCases,
                    classManagementUseCases,
                    ClassFilterRepositoryImpl()
                )
            advanceUntilIdle()

            viewModel.onEvent(CalendarScreenEvent.SelectDateForDialog(date))
            advanceUntilIdle()
            viewModel.onEvent(CalendarScreenEvent.ToggleClassSelection(1))
            advanceUntilIdle()

            assertEquals(1, viewModel.uiState.value.studentsForSelectedDate.size)

            viewModel.onEvent(CalendarScreenEvent.ToggleClassSelection(1))
            advanceUntilIdle()

            assertEquals(0, viewModel.uiState.value.allStudents.size)
            assertEquals(0, viewModel.uiState.value.studentsForSelectedDate.size)

            viewModel.onEvent(CalendarScreenEvent.ToggleClassSelection(1))
            advanceUntilIdle()

            assertEquals(1, viewModel.uiState.value.studentsForSelectedDate.size)
        }
}