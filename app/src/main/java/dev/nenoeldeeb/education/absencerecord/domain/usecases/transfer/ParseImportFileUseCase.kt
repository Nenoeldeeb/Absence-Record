package dev.nenoeldeeb.education.absencerecord.domain.usecases.transfer

import dev.nenoeldeeb.education.absencerecord.domain.models.ParsedStudentImportData
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentExportData
import dev.nenoeldeeb.education.absencerecord.domain.repositories.StorageRepository
import dev.nenoeldeeb.education.absencerecord.domain.services.SerializationService
import kotlinx.serialization.builtins.ListSerializer

class ParseImportFileUseCase(
    private val storageRepository: StorageRepository,
    private val serializationService: SerializationService
) {
    suspend operator fun invoke(uriString: String): Result<List<ParsedStudentImportData>> {
        val result = storageRepository.readTextFromUri(uriString)
        return result.fold(
            onSuccess = { jsonString ->
                if (jsonString.isBlank()) {
                    Result.success(emptyList())
                } else {
                    serializationService
                        .decodeFromString(
                            jsonString,
                            ListSerializer(StudentExportData.serializer())
                        )
                        .fold(
                            onSuccess = { data ->
                                Result.success(
                                    data.map { ParsedStudentImportData(it) }
                                )
                            },
                            onFailure = { Result.failure(StudentError.ImportParse) }
                        )
                }
            },
            onFailure = { Result.failure(it) }
        )
    }
}