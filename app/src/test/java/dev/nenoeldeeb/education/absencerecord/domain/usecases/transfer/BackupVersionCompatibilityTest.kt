package dev.nenoeldeeb.education.absencerecord.domain.usecases.transfer

import dev.nenoeldeeb.education.absencerecord.domain.models.AvailableHourExportData
import dev.nenoeldeeb.education.absencerecord.domain.models.BACKUP_VERSION
import dev.nenoeldeeb.education.absencerecord.domain.models.PREVIOUS_BACKUP_VERSION
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError
import dev.nenoeldeeb.education.absencerecord.domain.repositories.StorageRepository
import dev.nenoeldeeb.education.absencerecord.domain.services.ImportCaps
import dev.nenoeldeeb.education.absencerecord.domain.services.SerializationService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Reading both backup formats, and refusing everything else (FR-042 … FR-048).
 *
 * Decoding runs through the real [Json] here rather than a mock, so these
 * assert what a file on disk actually yields — including whether a legacy
 * `maxStudents` key survives a decode alongside `durationMinutes`.
 */
class BackupVersionCompatibilityTest {
    private val storageRepository: StorageRepository = mockk()
    private val json =
        Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            isLenient = true
        }
    private val serializationService: SerializationService = mockk()
    private lateinit var useCase: ParseImportFileUseCase

    private val uri = "content://backup"

    @BeforeEach
    fun setUp() {
        useCase = ParseImportFileUseCase(storageRepository, serializationService)
    }

    private fun serve(content: String) {
        coEvery { storageRepository.readTextFromUri(uri, any()) } returns Result.success(content)
        coEvery {
            serializationService.decodeFromString(content, any<kotlinx.serialization.KSerializer<*>>())
        } answers {
            val serializer = secondArg<kotlinx.serialization.KSerializer<*>>()
            runCatching { json.decodeFromString(serializer, content) }
        }
    }

    private fun block(
        weekday: Int = 6,
        start: Int = 540,
        duration: String? = "50",
        capacity: String? = null
    ): String {
        val parts =
            buildList {
                duration?.let { add("\"durationMinutes\":$it") }
                capacity?.let { add("\"maxStudents\":$it") }
            }
        return "{\"weekday\":$weekday,\"startMinutes\":$start,${parts.joinToString(",")}}"
    }

    // region FR-045 / FR-044: both formats load

    @Test
    fun `a v3 file is read without conversion`() =
        runTest {
            serve("""{"version":3,"students":[],"availableHours":[${block()}]}""")

            val hour = useCase(uri).getOrNull()!!.availableHours.single()

            assertEquals(50, hour.resolvedDurationMinutes())
        }

    @Test
    fun `a v2 file is read and its capacity converted`() =
        runTest {
            serve("""{"version":2,"students":[],"availableHours":[${block(duration = null, capacity = "6")}]}""")

            val hour = useCase(uri).getOrNull()!!.availableHours.single()

            assertEquals(60, hour.resolvedDurationMinutes())
        }

    @Test
    fun `a legacy capacity outside the selectable range is clamped`() =
        runTest {
            serve("""{"version":2,"students":[],"availableHours":[{"weekday":6,"startMinutes":0,"maxStudents":99}]}""")

            val hour = useCase(uri).getOrNull()!!.availableHours.single()

            assertEquals(120, hour.resolvedDurationMinutes())
        }

    @Test
    fun `a legacy capacity below the minimum is clamped up`() =
        runTest {
            serve("""{"version":2,"students":[],"availableHours":[{"weekday":6,"startMinutes":0,"maxStudents":0}]}""")

            assertEquals(10, useCase(uri).getOrNull()!!.availableHours.single().resolvedDurationMinutes())
        }

    // endregion

    // region FR-046: both signals, or neither, is malformed

    @Test
    fun `an entry carrying both fields is malformed`() {
        assertNull(AvailableHourExportData(6, 540, durationMinutes = 50, maxStudents = 5).resolvedDurationMinutes())
    }

    @Test
    fun `an entry carrying neither field is malformed`() {
        assertNull(AvailableHourExportData(6, 540).resolvedDurationMinutes())
    }

    @Test
    fun `malformed flags exactly the ambiguous and incomplete entries`() {
        assertTrue(AvailableHourExportData(6, 540, durationMinutes = 50, maxStudents = 5).isMalformed)
        assertTrue(AvailableHourExportData(6, 540).isMalformed)
        assertTrue(!AvailableHourExportData(6, 540, durationMinutes = 50).isMalformed)
        assertTrue(!AvailableHourExportData(6, 540, maxStudents = 5).isMalformed)
    }

    @Test
    fun `a file whose entry carries both fields still parses, leaving the entry unusable`() =
        runTest {
            serve("""{"version":3,"students":[],"availableHours":[${block(capacity = "5")}]}""")

            // The file is structurally valid, so it parses; the entry is what
            // gets skipped and counted later, never guessed at (FR-046, SC-009).
            val hour = useCase(uri).getOrNull()!!.availableHours.single()
            assertNull(hour.resolvedDurationMinutes())
            assertTrue(hour.isMalformed)
        }

    // endregion

    // region FR-048: a length that is not a selectable value is malformed

    @Test
    fun `a length outside ten to one hundred twenty is malformed`() {
        assertTrue(AvailableHourExportData(6, 0, durationMinutes = 5).isMalformed)
        assertTrue(AvailableHourExportData(6, 0, durationMinutes = 130).isMalformed)
    }

    @Test
    fun `a length that is not a whole ten-minute step is malformed`() {
        assertTrue(AvailableHourExportData(6, 0, durationMinutes = 55).isMalformed)
    }

    @Test
    fun `every selectable length is accepted`() {
        for (duration in listOf(10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120)) {
            assertTrue(!AvailableHourExportData(6, 0, durationMinutes = duration).isMalformed, "rejected $duration")
        }
    }

    // endregion

    // region FR-047: unknown or newer versions are refused, writing nothing

    @Test
    fun `a version newer than this build is refused`() =
        runTest {
            serve("""{"version":99,"students":[],"availableHours":[]}""")

            val result = useCase(uri)

            assertTrue(result.isFailure)
            assertEquals(StudentError.UnsupportedBackupVersion(99), result.exceptionOrNull())
        }

    @Test
    fun `a version older than the immediately preceding one is refused`() =
        runTest {
            serve("""{"version":1,"students":[],"availableHours":[]}""")

            assertEquals(StudentError.UnsupportedBackupVersion(1), useCase(uri).exceptionOrNull())
        }

    @Test
    fun `a refused file is never handed on for merging`() =
        runTest {
            serve("""{"version":99,"students":[],"availableHours":[]}""")

            useCase(uri)

            // One read, and nothing else: the file is refused at the boundary.
            coVerify(exactly = 1) { storageRepository.readTextFromUri(uri, any()) }
        }

    @Test
    fun `the two supported versions are the current and previous ones`() {
        assertEquals(3, BACKUP_VERSION)
        assertEquals(2, PREVIOUS_BACKUP_VERSION)
    }

    // endregion

    // region FR-063: the size guard is asked for, before decoding

    @Test
    fun `reading asks the storage layer to refuse an oversize file`() =
        runTest {
            serve("""{"version":3,"students":[],"availableHours":[]}""")

            useCase(uri)

            coVerify { storageRepository.readTextFromUri(uri, ImportCaps.MAX_IMPORT_FILE_BYTES) }
        }

    @Test
    fun `an oversize refusal surfaces as its own error, not a parse failure`() =
        runTest {
            coEvery { storageRepository.readTextFromUri(uri, any()) } returns
                Result.failure(StudentError.ImportFileTooLarge)

            val result = useCase(uri)

            assertEquals(StudentError.ImportFileTooLarge, result.exceptionOrNull())
            assertTrue(result.exceptionOrNull() !is StudentError.ImportParse)
        }

    // endregion
}