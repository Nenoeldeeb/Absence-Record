package dev.nenoeldeeb.education.absencerecord.presentation.screens.students.delegates

import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SelectionStateDelegateTest {
    private val delegate = SelectionStateDelegate()

    @Test
    fun `toggleSelection adds item if not present`() {
        val current = setOf(1)
        val result = delegate.toggleSelection(current, 2)
        assertEquals(setOf(1, 2), result)
    }

    @Test
    fun `toggleSelection removes item if present`() {
        val current = setOf(1, 2)
        val result = delegate.toggleSelection(current, 1)
        assertEquals(setOf(2), result)
    }

    @Test
    fun `selectAll selects all student IDs`() {
        val students = listOf(Student(1, "A"), Student(2, "B"))
        val result = delegate.selectAll(students)
        assertEquals(setOf(1, 2), result)
    }

    @Test
    fun `clearSelection returns empty set`() {
        val result = delegate.clearSelection()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `toggleMode switches to true and keeps selection`() {
        val selection = setOf(1)
        val (mode, newSelection) = delegate.toggleMode(false, selection)
        assertEquals(true, mode)
        assertEquals(selection, newSelection)
    }

    @Test
    fun `toggleMode switches to false and clears selection`() {
        val selection = setOf(1)
        val (mode, newSelection) = delegate.toggleMode(true, selection)
        assertEquals(false, mode)
        assertTrue(newSelection.isEmpty())
    }
}