package dev.nenoeldeeb.education.absencerecord.domain.usecases.transfer

import dev.nenoeldeeb.education.absencerecord.domain.models.StudentExportData
import dev.nenoeldeeb.education.absencerecord.domain.repositories.StorageRepository
import dev.nenoeldeeb.education.absencerecord.domain.services.SerializationService
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
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
        fun `should successfully parse file`() =
            runTest {
                val uriString = "content://file"
                val jsonString = "[{\"name\":\"John\",\"dates\":[\"2024-01-01\"]}]"
                val exportDataList = listOf(StudentExportData("John", "", listOf("2024-01-01")))

                coEvery { storageRepository.readTextFromUri(uriString) } returns
                    Result.success(jsonString)
                every {
                    serializationService.decodeFromString(
                        jsonString,
                        any<kotlinx.serialization.KSerializer<List<StudentExportData>>>()
                    )
                } returns Result.success(exportDataList)

                val result = useCase(uriString)

                assertTrue(result.isSuccess)
                val parsedList = result.getOrNull()
                assertEquals(1, parsedList?.size)
                assertEquals("John", parsedList?.first()?.originalData?.name)
            }

        @Test
        fun `should return empty list for blank file content`() =
            runTest {
                val uriString = "content://empty"
                coEvery { storageRepository.readTextFromUri(uriString) } returns
                    Result.success("  ")

                val result = useCase(uriString)

                assertTrue(result.isSuccess)
                assertEquals(result.getOrNull()?.isEmpty(), true)
            }

        @Test
        fun `should handle storage repository failure`() =
            runTest {
                val uriString = "content://error"
                val exception = Exception("Read error")
                coEvery { storageRepository.readTextFromUri(uriString) } returns
                    Result.failure(exception)

                val result = useCase(uriString)

                assertTrue(result.isFailure)
                assertEquals(exception, result.exceptionOrNull())
            }

        @Test
        fun `should handle serialization failure`() =
            runTest {
                val uriString = "content://malformed"
                val jsonString = "{ malformed json"
                val exception = Exception("JSON Parse error")

                coEvery { storageRepository.readTextFromUri(uriString) } returns
                    Result.success(jsonString)
                every {
                    serializationService.decodeFromString(
                        jsonString,
                        any<kotlinx.serialization.KSerializer<List<StudentExportData>>>()
                    )
                } returns Result.failure(exception)

                val result = useCase(uriString)

                assertTrue(result.isFailure)
                assertEquals(exception, result.exceptionOrNull())
            }
    }
}