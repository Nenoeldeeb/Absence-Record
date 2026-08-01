package dev.nenoeldeeb.education.absencerecord.data.repositories

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClassFilterRepositoryImplTest {
    private val repository = ClassFilterRepositoryImpl()

    @Test
    fun `initial value is emptySet`() =
        runTest {
            assertEquals(emptySet(), repository.selectedClassIds.value)
        }

    @Test
    fun `toggleClass adds a classId`() =
        runTest {
            repository.toggleClass(1)

            assertEquals(setOf(1), repository.selectedClassIds.value)
        }

    @Test
    fun `toggleClass removes an already-present classId`() =
        runTest {
            repository.toggleClass(1)
            repository.toggleClass(1)

            assertEquals(emptySet(), repository.selectedClassIds.value)
        }

    @Test
    fun `toggling multiple ids accumulates them`() =
        runTest {
            repository.toggleClass(1)
            repository.toggleClass(2)
            repository.toggleClass(3)

            assertEquals(setOf(1, 2, 3), repository.selectedClassIds.value)
        }

    @Test
    fun `toggling multiple ids then removing one keeps the others`() =
        runTest {
            repository.toggleClass(1)
            repository.toggleClass(2)
            repository.toggleClass(3)
            repository.toggleClass(2)

            assertEquals(setOf(1, 3), repository.selectedClassIds.value)
        }

    @Test
    fun `setSelectedClassIds replaces the set`() =
        runTest {
            repository.toggleClass(1)
            repository.setSelectedClassIds(setOf(2, 3))

            assertEquals(setOf(2, 3), repository.selectedClassIds.value)
            assertTrue(1 !in repository.selectedClassIds.value)
        }

    @Test
    fun `setSelectedClassIds with empty set clears selection`() =
        runTest {
            repository.toggleClass(1)
            repository.setSelectedClassIds(emptySet())

            assertEquals(emptySet(), repository.selectedClassIds.value)
        }

    @Test
    fun `clearFilter resets to emptySet`() =
        runTest {
            repository.toggleClass(1)
            repository.toggleClass(2)
            repository.clearFilter()

            assertEquals(emptySet(), repository.selectedClassIds.value)
        }

    @Test
    fun `clearFilter on empty state stays empty`() =
        runTest {
            repository.clearFilter()

            assertEquals(emptySet(), repository.selectedClassIds.value)
        }

    @Test
    fun `toggleClass does not mutate the emitted set in place`() =
        runTest {
            val initial = repository.selectedClassIds.value
            repository.toggleClass(1)

            assertEquals(emptySet(), initial)
            assertEquals(setOf(1), repository.selectedClassIds.value)
        }
}