package dev.nenoeldeeb.education.absencerecord.domain.usecases.transfer

import dev.nenoeldeeb.education.absencerecord.domain.models.AvailableHourExportData
import dev.nenoeldeeb.education.absencerecord.domain.models.BACKUP_VERSION
import dev.nenoeldeeb.education.absencerecord.domain.models.BackupExportData
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentExportData
import dev.nenoeldeeb.education.absencerecord.domain.repositories.StorageRepository
import dev.nenoeldeeb.education.absencerecord.domain.services.SerializationService
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.KSerializer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DisplayName("ParseImportFileUseCase Tests")
class ParseImportFileUseCaseTest {
    private lateinit var storageRepository: StorageRepository
    private lateinit var serializationService: SerializationService
    private lateinit var useCase: ParseImportFileUseCase

    @BeforeEach
    fun setup() {
        storageRepository = mockk()
        serializationService = mockk()
        useCase = ParseImportFileUseCase(storageRepository, serializationService)
    }

    @Nested
    @DisplayName("invoke Tests")
    inner class InvokeTests {
        @Test
        fun `should parse a versioned backup file`() =
            runTest {
                val uriString = "content://file"
                val jsonString = "{\"version\":2,\"students\":[],\"availableHours\":[]}"
                val exportData = BackupExportData(students = emptyList(), availableHours = emptyList())

                coEvery { storageRepository.readTextFromUri(uriString) } returns Result.success(jsonString)
                every {
                    serializationService.decodeFromString(jsonString, any<KSerializer<BackupExportData>>())
                } returns Result.success(exportData)

                val result = useCase(uriString)

                assertTrue(result.isSuccess)
                assertTrue(result.getOrNull()?.students?.isEmpty() == true)
                assertTrue(result.getOrNull()?.availableHours?.isEmpty() == true)
            }

        @Test
        fun `should parse versioned backup with students and schedule`() =
            runTest {
                val uriString = "content://file"
                val jsonString =
                    "{\"version\":2,\"students\":[{\"name\":\"John\",\"dates\":[]}]," +
                        "\"availableHours\":[{\"weekday\":6,\"startMinutes\":480,\"maxStudents\":2}]}"
                val exportData =
                    BackupExportData(
                        students = listOf(StudentExportData("John", "", emptyList())),
                        availableHours = listOf(AvailableHourExportData(6, 480, 2))
                    )

                coEvery { storageRepository.readTextFromUri(uriString) } returns Result.success(jsonString)
                every {
                    serializationService.decodeFromString(jsonString, any<KSerializer<BackupExportData>>())
                } returns Result.success(exportData)

                val result = useCase(uriString)

                assertTrue(result.isSuccess)
                val parsed = result.getOrNull()
                assertEquals(1, parsed?.students?.size)
                assertEquals("John", parsed?.students?.first()?.originalData?.name)
                assertEquals(1, parsed?.availableHours?.size)
                assertEquals(480, parsed?.availableHours?.first()?.startMinutes)
            }

        @Test
        fun `should reject unsupported backup version`() =
            runTest {
                val uriString = "content://file"
                val jsonString = "{\"version\":9,\"students\":[],\"availableHours\":[]}"
                val exportData = BackupExportData(version = 9, students = emptyList(), availableHours = emptyList())

                coEvery { storageRepository.readTextFromUri(uriString) } returns Result.success(jsonString)
                every {
                    serializationService.decodeFromString(jsonString, any<KSerializer<BackupExportData>>())
                } returns Result.success(exportData)

                val result = useCase(uriString)

                assertTrue(result.isFailure)
                assertEquals(StudentError.UnsupportedBackupVersion(9), result.exceptionOrNull())
            }

        @Test
        fun `should fall back to legacy flat array when object parse fails`() =
            runTest {
                val uriString = "content://file"
                val jsonString = "[{\"name\":\"John\",\"dates\":[\"2024-01-01\"]}]"
                val legacyData = listOf(StudentExportData("John", "", listOf("2024-01-01")))

                coEvery { storageRepository.readTextFromUri(uriString) } returns Result.success(jsonString)
                every {
                    serializationService.decodeFromString(
                        jsonString,
                        any<KSerializer<List<StudentExportData>>>()
                    )
                } returns Result.success(legacyData)
                every {
                    serializationService.decodeFromString(jsonString, BackupExportData.serializer())
                } returns Result.failure(StudentError.ImportParse)

                val result = useCase(uriString)

                assertTrue(result.isSuccess)
                val parsed = result.getOrNull()
                assertEquals(1, parsed?.students?.size)
                assertEquals("John", parsed?.students?.first()?.originalData?.name)
                assertTrue(parsed?.availableHours?.isEmpty() == true)
            }

        @Test
        fun `should fail when both object and legacy parse fail`() =
            runTest {
                val uriString = "content://file"
                val jsonString = "{ malformed"

                coEvery { storageRepository.readTextFromUri(uriString) } returns Result.success(jsonString)
                every {
                    serializationService.decodeFromString(
                        jsonString,
                        any<KSerializer<List<StudentExportData>>>()
                    )
                } returns Result.failure(StudentError.ImportParse)
                every {
                    serializationService.decodeFromString(jsonString, BackupExportData.serializer())
                } returns Result.failure(StudentError.ImportParse)

                val result = useCase(uriString)

                assertTrue(result.isFailure)
                assertEquals(StudentError.ImportParse, result.exceptionOrNull())
            }

        @Test
        fun `should return empty data for blank file content`() =
            runTest {
                val uriString = "content://empty"
                coEvery { storageRepository.readTextFromUri(uriString) } returns
                    Result.success("  ")

                val result = useCase(uriString)

                assertTrue(result.isSuccess)
                assertTrue(result.getOrNull()?.students?.isEmpty() == true)
                assertTrue(result.getOrNull()?.availableHours?.isEmpty() == true)
            }

        @Test
        fun `should handle storage repository failure`() =
            runTest {
                val uriString = "content://error"
                val exception = StudentError.FileRead
                coEvery { storageRepository.readTextFromUri(uriString) } returns
                    Result.failure(exception)

                val result = useCase(uriString)

                assertTrue(result.isFailure)
                assertEquals(exception, result.exceptionOrNull())
            }
    }

    @Test
    fun `backup version constant is 2`() {
        assertEquals(2, BACKUP_VERSION)
    }
}