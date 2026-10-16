package dev.nenoeldeeb.education.absencerecord.data.repositories

import android.content.Context
import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError
import dev.nenoeldeeb.education.absencerecord.domain.repositories.StorageRepository
import dev.nenoeldeeb.education.absencerecord.domain.services.ImportCaps
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * The size guard refuses an oversize file without materialising it (FR-063, SC-010).
 *
 * The real question is not "is a big file rejected" but "is it rejected without
 * being held in memory" — so the largest file here is still small enough to
 * write quickly, while sitting far past a lowered limit.
 */
@RunWith(AndroidJUnit4::class)
class StorageRepositorySizeGuardTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val testFiles = mutableListOf<File>()

    @After
    fun tearDown() {
        testFiles.forEach { it.delete() }
    }

    /** Bound to the runTest scheduler so the IO hop is driven by the test. */
    private fun repository(scheduler: TestCoroutineScheduler): StorageRepository =
        StorageRepositoryImpl(
            context = context,
            dispatcherProvider = TestDispatcherProvider(StandardTestDispatcher(scheduler))
        )

    private fun tempFile(content: String): String {
        val file = File(context.cacheDir, "size-guard-${content.length}.json")
        file.writeText(content)
        testFiles += file
        return file.toUri().toString()
    }

    @Test
    fun aFileWithinTheLimitIsRead() =
        runTest {
            val repo = repository(testScheduler)
            val uri = tempFile("""{"version":3,"students":[],"availableHours":[]}""")

            val result = repo.readTextFromUri(uri, 1024)

            assertTrue(result.isSuccess)
            assertTrue(result.getOrNull()!!.contains("version"))
        }

    @Test
    fun aFileOneByteOverTheLimitIsRefused() =
        runTest {
            val repo = repository(testScheduler)
            val uri = tempFile("x".repeat(1025))

            val result = repo.readTextFromUri(uri, 1024)

            assertEquals(StudentError.ImportFileTooLarge, result.exceptionOrNull())
        }

    @Test
    fun aFileExactlyAtTheLimitIsAccepted() =
        runTest {
            val repo = repository(testScheduler)
            val uri = tempFile("x".repeat(1024))

            assertTrue(repo.readTextFromUri(uri, 1024).isSuccess)
        }

    @Test
    fun anEmptyFileIsNotAnOversizeFailure() =
        runTest {
            val repo = repository(testScheduler)
            val uri = tempFile("")

            val result = repo.readTextFromUri(uri, 1024)

            // An empty file reads as empty content, which the caller treats as
            // "nothing to import" — it is not a size refusal.
            assertTrue(result.isSuccess)
            assertEquals("", result.getOrNull())
        }

    @Test
    fun anUnreadableUriIsAReadFailureNotASizeFailure() =
        runTest {
            val repo = repository(testScheduler)
            val result = repo.readTextFromUri("file:///does/not/exist.json", 1024)

            assertEquals(StudentError.FileRead, result.exceptionOrNull())
        }

    @Test
    fun theDocumentedLimitIsThirtyTwoMegabytes() {
        assertEquals(33_554_432L, ImportCaps.MAX_IMPORT_FILE_BYTES)
    }

    @Test
    fun multiByteContentIsMeasuredInBytesNotCharacters() =
        runTest {
            // Arabic characters are two UTF-8 bytes each, so 600 of them are
            // ~1200 bytes: past a 1024-byte limit despite being short.
            val repo = repository(testScheduler)
            val uri = tempFile("ط".repeat(600))

            val result = repo.readTextFromUri(uri, 1024)

            assertEquals(StudentError.ImportFileTooLarge, result.exceptionOrNull())
        }
}