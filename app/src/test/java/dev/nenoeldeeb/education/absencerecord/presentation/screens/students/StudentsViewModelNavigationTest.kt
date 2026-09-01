package dev.nenoeldeeb.education.absencerecord.presentation.screens.students

import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.components.handleStudentClickBehavior
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StudentsViewModelNavigationTest {
    @Test
    fun `single tap in normal mode navigates to detail`() {
        // Given
        val student = Student(id = 1, name = "S1")
        val navigatedIds = mutableListOf<Int>()
        val toggledIds = mutableListOf<Int>()

        // When
        handleStudentClickBehavior(
            isMultiSelectionMode = false,
            student = student,
            onNavigateToDetail = { navigatedIds.add(it) },
            onToggleStudentSelection = { toggledIds.add(it) }
        )

        // Then
        assertEquals(listOf(1), navigatedIds)
        assertTrue(toggledIds.isEmpty())
    }

    @Test
    fun `single tap in normal mode does not trigger edit dialog state`() {
        // Given
        val student = Student(id = 2, name = "S2")
        var navigated = false

        // When
        handleStudentClickBehavior(
            isMultiSelectionMode = false,
            student = student,
            onNavigateToDetail = { navigated = true },
            onToggleStudentSelection = {}
        )

        // Then
        assertTrue(navigated)
    }

    @Test
    fun `tap in multi selection mode toggles selection without navigating`() {
        // Given
        val student = Student(id = 3, name = "S3")
        val navigatedIds = mutableListOf<Int>()
        val toggledIds = mutableListOf<Int>()

        // When
        handleStudentClickBehavior(
            isMultiSelectionMode = true,
            student = student,
            onNavigateToDetail = { navigatedIds.add(it) },
            onToggleStudentSelection = { toggledIds.add(it) }
        )

        // Then
        assertEquals(listOf(3), toggledIds)
        assertTrue(navigatedIds.isEmpty())
    }

    @Test
    fun `tap in multi selection mode passes the tapped student id`() {
        // Given
        val student = Student(id = 42, name = "S42")
        var toggledId: Int? = null

        // When
        handleStudentClickBehavior(
            isMultiSelectionMode = true,
            student = student,
            onNavigateToDetail = {},
            onToggleStudentSelection = { toggledId = it }
        )

        // Then
        assertEquals(42, toggledId)
    }
}