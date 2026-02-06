package dev.nenoeldeeb.education.absencerecord.data.datasources.local.entities

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class StudentEntityTest {
    @Test
    fun `default values are correct`() {
        val student = StudentEntity(name = "Test Student")
        assertEquals(0, student.id)
        assertEquals("Test Student", student.name)
    }

    @Test
    fun `data class equality works`() {
        val student1 = StudentEntity(id = 1, name = "Alice")
        val student2 = StudentEntity(id = 1, name = "Alice")
        val student3 = StudentEntity(id = 2, name = "Bob")

        assertEquals(student1, student2)
        assertNotEquals(student1, student3)
    }
}