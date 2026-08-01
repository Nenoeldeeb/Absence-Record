package dev.nenoeldeeb.education.absencerecord.presentation.utils

import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class StudentFiltersTest {
    private val unassigned = Student(id = 1, name = "Unassigned")
    private val class1Student = Student(id = 2, name = "Class 1 Student", classId = 1)
    private val class2Student = Student(id = 3, name = "Class 2 Student", classId = 2)
    private val class3Student = Student(id = 4, name = "Class 3 Student", classId = 3)

    @Test
    fun `empty set returns only unassigned students`() {
        val students = listOf(unassigned, class1Student, class2Student)

        val result = students.applyMultiClassFilter(emptySet())

        assertEquals(listOf(unassigned), result)
    }

    @Test
    fun `empty set returns empty list when no unassigned students exist`() {
        val students = listOf(class1Student, class2Student)

        val result = students.applyMultiClassFilter(emptySet())

        assertEquals(emptyList<Student>(), result)
    }

    @Test
    fun `single class selection returns only students of that class`() {
        val students = listOf(unassigned, class1Student, class2Student, class3Student)

        val result = students.applyMultiClassFilter(setOf(1))

        assertEquals(listOf(class1Student), result)
    }

    @Test
    fun `multi class selection merges students across classes and deduplicates`() {
        val duplicate = Student(id = 2, name = "Class 1 Student", classId = 1)
        val students = listOf(class1Student, class2Student, duplicate, class1Student)

        val result = students.applyMultiClassFilter(setOf(1, 2))

        assertEquals(listOf(class1Student, class2Student), result)
    }

    @Test
    fun `unassigned students excluded when non empty set is given`() {
        val students = listOf(unassigned, class1Student, class2Student)

        val result = students.applyMultiClassFilter(setOf(2))

        assertEquals(listOf(class2Student), result)
    }

    @Test
    fun `classId not present in student list yields no students for it`() {
        val students = listOf(class1Student, class2Student)

        val result = students.applyMultiClassFilter(setOf(3))

        assertEquals(emptyList<Student>(), result)
    }
}