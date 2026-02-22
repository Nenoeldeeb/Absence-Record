package dev.nenoeldeeb.education.absencerecord.data.repositories

import android.content.Context
import android.graphics.BitmapFactory
import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.nenoeldeeb.education.absencerecord.domain.repositories.ReportRepository
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Instrumentation tests for [ReportRepositoryImpl].
 *
 * These tests verify that report generation, bitmap compression, and FileProvider URI creation work
 * correctly on a real Android device.
 */
@RunWith(AndroidJUnit4::class)
class ReportRepositoryImplTest {
    private lateinit var context: Context
    private lateinit var cacheDir: File

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        cacheDir = File(context.cacheDir, "shared_images")
    }

    @After
    fun tearDown() {
        // Clean up generated files
        cacheDir.deleteRecursively()
    }

    private fun createRepository(scheduler: TestCoroutineScheduler): ReportRepository {
        val testDispatcher = StandardTestDispatcher(scheduler)
        return ReportRepositoryImpl(
            context = context, dispatcherProvider = TestDispatcherProvider(testDispatcher)
        )
    }

    // region generateAndSaveReport tests

    @Test
    fun generateAndSaveReport_createsValidPngFile() = runTest {
        val repository = createRepository(testScheduler)

        // Arrange
        val studentName = "Test Student"
        val month = LocalDate(2023, 10, 1)
        val attendance = listOf(LocalDate(2023, 10, 5), LocalDate(2023, 10, 15), LocalDate(2023, 10, 25))

        // Act
        val result = repository.generateAndSaveReport(studentName, month, attendance)

        // Assert
        assertTrue("Result should be success", result.isSuccess)

        val uriString = result.getOrNull()
        assertNotNull("URI should not be null", uriString)
        assertTrue("URI should be content:// scheme", uriString!!.startsWith("content://"))

        // Verify file exists
        val expectedFileName = "calendar_${studentName}_$month.png"
        val file = File(cacheDir, expectedFileName)
        assertTrue("File should exist", file.exists())
        assertTrue("File should have content", file.length() > 0)
    }

    @Test
    fun generateAndSaveReport_generatesValidBitmap() = runTest {
        val repository = createRepository(testScheduler)

        // Arrange
        val studentName = "Student"
        val month = LocalDate(2023, 10, 1)
        val attendance = listOf(LocalDate(2023, 10, 10))

        // Act
        val result = repository.generateAndSaveReport(studentName, month, attendance)
        assertTrue(result.isSuccess)

        // Read the generated file and verify it's a valid bitmap
        val expectedFileName = "calendar_${studentName}_$month.png"
        val file = File(cacheDir, expectedFileName)

        val bitmap = BitmapFactory.decodeFile(file.absolutePath)

        // Assert
        assertNotNull("Should decode to valid bitmap", bitmap)
        assertEquals("Width should be 1080", 1080, bitmap.width)
        assertEquals("Height should be 1920", 1920, bitmap.height)
    }

    @Test
    fun generateAndSaveReport_handlesEmptyAttendance() = runTest {
        val repository = createRepository(testScheduler)

        // Arrange
        val studentName = "No Attendance"
        val month = LocalDate(2023, 11, 1)
        val attendance = emptyList<LocalDate>()

        // Act
        val result = repository.generateAndSaveReport(studentName, month, attendance)

        // Assert
        assertTrue("Should succeed with empty attendance", result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    @Test
    fun generateAndSaveReport_handlesAllDaysMarked() = runTest {
        val repository = createRepository(testScheduler)

        // Arrange - October 2023 has 31 days
        val studentName = "Full Attendance"
        val month = LocalDate(2023, 10, 1)
        val attendance = (1..31).map { LocalDate(2023, 10, it) }

        // Act
        val result = repository.generateAndSaveReport(studentName, month, attendance)

        // Assert
        assertTrue("Should succeed with all days marked", result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    @Test
    fun generateAndSaveReport_handlesFebruary28Days() = runTest {
        val repository = createRepository(testScheduler)

        // Arrange - February 2023 has 28 days (non-leap year)
        val studentName = "February Student"
        val month = LocalDate(2023, 2, 1)
        val attendance = listOf(LocalDate(2023, 2, 14), LocalDate(2023, 2, 28))

        // Act
        val result = repository.generateAndSaveReport(studentName, month, attendance)

        // Assert
        assertTrue("Should succeed for February", result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    @Test
    fun generateAndSaveReport_handlesLeapYearFebruary() = runTest {
        val repository = createRepository(testScheduler)

        // Arrange - February 2024 has 29 days (leap year)
        val studentName = "Leap Year Student"
        val month = LocalDate(2024, 2, 1)
        val attendance = listOf(LocalDate(2024, 2, 29))

        // Act
        val result = repository.generateAndSaveReport(studentName, month, attendance)

        // Assert
        assertTrue("Should succeed for leap year February", result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    @Test
    fun generateAndSaveReport_handlesSpecialCharactersInName() = runTest {
        val repository = createRepository(testScheduler)

        // Arrange
        val studentName = "محمد أحمد"
        val month = LocalDate(2023, 10, 1)
        val attendance = listOf(LocalDate(2023, 10, 1))

        // Act
        val result = repository.generateAndSaveReport(studentName, month, attendance)

        // Assert
        assertTrue("Should handle Arabic names", result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    @Test
    fun generateAndSaveReport_overwritesExistingFile() = runTest {
        val repository = createRepository(testScheduler)

        // Arrange
        val studentName = "Overwrite Test"
        val month = LocalDate(2023, 10, 1)
        val attendance1 = listOf(LocalDate(2023, 10, 1))
        val attendance2 = listOf(LocalDate(2023, 10, 15), LocalDate(2023, 10, 16))

        // Act - Generate first report
        val result1 = repository.generateAndSaveReport(studentName, month, attendance1)
        assertTrue(result1.isSuccess)

        val expectedFileName = "calendar_${studentName}_$month.png"
        val file = File(cacheDir, expectedFileName)

        // Act - Generate second report (should overwrite)
        val result2 = repository.generateAndSaveReport(studentName, month, attendance2)

        // Assert
        assertTrue(result2.isSuccess)
        assertTrue("File should still exist after overwrite", file.exists())
    }

    @Test
    fun generateAndSaveReport_returnsContentUri() = runTest {
        val repository = createRepository(testScheduler)

        // Arrange
        val studentName = "URI Test"
        val month = LocalDate(2023, 10, 1)
        val attendance = listOf(LocalDate(2023, 10, 1))

        // Act
        val result = repository.generateAndSaveReport(studentName, month, attendance)

        // Assert
        assertTrue(result.isSuccess)
        val uriString = result.getOrNull()!!
        val uri = uriString.toUri()

        assertEquals("Scheme should be content", "content", uri.scheme)
        assertTrue(
            "Authority should contain fileprovider", uri.authority?.contains("fileprovider") == true
        )
    }

    // endregion
}