package dev.nenoeldeeb.education.absencerecord.data.repositories

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Instrumentation tests for [StorageRepositoryImpl].
 *
 * These tests verify that ContentResolver-based file I/O operations
 * work correctly on a real Android device/emulator.
 */
@RunWith(AndroidJUnit4::class)
class StorageRepositoryImplTest {
    private lateinit var context: Context
    private lateinit var repository: StorageRepositoryImpl
    private lateinit var testDir: File
    private val testFiles = mutableListOf<File>()

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        repository = StorageRepositoryImpl(context)
        testDir = File(context.cacheDir, "storage_test")
        if (!testDir.exists()) testDir.mkdirs()
    }

    @After
    fun tearDown() {
        // Clean up test files
        testFiles.forEach { it.delete() }
        testDir.deleteRecursively()
    }

    private fun createTestFile(
        name: String,
        content: String = ""
    ): File {
        val file = File(testDir, name)
        file.writeText(content)
        testFiles.add(file)
        return file
    }

    private fun getFileUri(file: File): Uri {
        return file.toUri()
    }

    // region readTextFromUri tests

    @Test
    fun readTextFromUri_existingFile_returnsContent() =
        runTest {
            // Arrange
            val expectedContent = "Hello, World!\nThis is test content."
            val file = createTestFile("read_test.txt", expectedContent)
            val uri = getFileUri(file)

            // Act
            val result = repository.readTextFromUri(uri.toString()).first()

            // Assert
            assertTrue(result.isSuccess)
            assertEquals(expectedContent, result.getOrNull())
        }

    @Test
    fun readTextFromUri_emptyFile_returnsEmptyString() =
        runTest {
            // Arrange
            val file = createTestFile("empty.txt", "")
            val uri = getFileUri(file)

            // Act
            val result = repository.readTextFromUri(uri.toString()).first()

            // Assert
            assertTrue(result.isSuccess)
            assertEquals("", result.getOrNull())
        }

    @Test
    fun readTextFromUri_unicodeContent_handlesCorrectly() =
        runTest {
            // Arrange
            val unicodeContent = "مرحبا بالعالم\n日本語\n🎉🎊"
            val file = createTestFile("unicode.txt", unicodeContent)
            val uri = getFileUri(file)

            // Act
            val result = repository.readTextFromUri(uri.toString()).first()

            // Assert
            assertTrue(result.isSuccess)
            assertEquals(unicodeContent, result.getOrNull())
        }

    @Test
    fun readTextFromUri_invalidUri_returnsFailure() =
        runTest {
            // Arrange
            val invalidUri = "file:///non/existent/path/file.txt"

            // Act
            val result = repository.readTextFromUri(invalidUri).first()

            // Assert
            assertTrue(result.isFailure)
        }

    @Test
    fun readTextFromUri_largeFile_handlesCorrectly() =
        runTest {
            // Arrange
            val largeContent = "Line of text\n".repeat(1000)
            val file = createTestFile("large.txt", largeContent)
            val uri = getFileUri(file)

            // Act
            val result = repository.readTextFromUri(uri.toString()).first()

            // Assert
            assertTrue(result.isSuccess)
            assertEquals(largeContent, result.getOrNull())
        }

    // endregion

    // region writeTextToUri tests

    @Test
    fun writeTextToUri_newContent_writesSuccessfully() =
        runTest {
            // Arrange
            val file = createTestFile("write_test.txt")
            val uri = getFileUri(file)
            val contentToWrite = "New content to write"

            // Act
            val result = repository.writeTextToUri(uri.toString(), contentToWrite).first()

            // Assert
            assertTrue(result.isSuccess)
            assertEquals(contentToWrite, file.readText())
        }

    @Test
    fun writeTextToUri_overwritesExistingContent() =
        runTest {
            // Arrange
            val file = createTestFile("overwrite_test.txt", "Original content")
            val uri = getFileUri(file)
            val newContent = "Overwritten content"

            // Act
            val result = repository.writeTextToUri(uri.toString(), newContent).first()

            // Assert
            assertTrue(result.isSuccess)
            assertEquals(newContent, file.readText())
        }

    @Test
    fun writeTextToUri_emptyString_writesEmptyFile() =
        runTest {
            // Arrange
            val file = createTestFile("empty_write.txt", "Will be cleared")
            val uri = getFileUri(file)

            // Act
            val result = repository.writeTextToUri(uri.toString(), "").first()

            // Assert
            assertTrue(result.isSuccess)
            assertEquals("", file.readText())
        }

    @Test
    fun writeTextToUri_unicodeContent_handlesCorrectly() =
        runTest {
            // Arrange
            val file = createTestFile("unicode_write.txt")
            val uri = getFileUri(file)
            val unicodeContent = "العربية\n中文\n🚀✨"

            // Act
            val result = repository.writeTextToUri(uri.toString(), unicodeContent).first()

            // Assert
            assertTrue(result.isSuccess)
            assertEquals(unicodeContent, file.readText())
        }

    // endregion

    // region round-trip tests

    @Test
    fun writeAndRead_preservesContent() =
        runTest {
            // Arrange
            val file = createTestFile("roundtrip.txt")
            val uri = getFileUri(file)
            val content = "Test content for round-trip\nMultiple lines\n\tWith tabs"

            // Act - Write
            val writeResult = repository.writeTextToUri(uri.toString(), content).first()
            assertTrue(writeResult.isSuccess)

            // Act - Read back
            val readResult = repository.readTextFromUri(uri.toString()).first()

            // Assert
            assertTrue(readResult.isSuccess)
            assertEquals(content, readResult.getOrNull())
        }

    // endregion
}