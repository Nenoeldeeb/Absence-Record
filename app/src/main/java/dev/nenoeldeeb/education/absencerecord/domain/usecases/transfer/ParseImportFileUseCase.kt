package dev.nenoeldeeb.education.absencerecord.domain.usecases.transfer

import dev.nenoeldeeb.education.absencerecord.domain.models.BACKUP_VERSION
import dev.nenoeldeeb.education.absencerecord.domain.models.BackupExportData
import dev.nenoeldeeb.education.absencerecord.domain.models.ParsedImportData
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
    suspend operator fun invoke(uriString: String): Result<ParsedImportData> {
        val result = storageRepository.readTextFromUri(uriString)
        return result.fold(
            onSuccess = { jsonString ->
                if (jsonString.isBlank()) {
                    Result.success(ParsedImportData(emptyList(), emptyList()))
                } else {
                    parseBackup(jsonString)
                }
            },
            onFailure = { Result.failure(it) }
        )
    }

    private fun parseBackup(jsonString: String): Result<ParsedImportData> {
        serializationService
            .decodeFromString(jsonString, BackupExportData.serializer())
            .fold(
                onSuccess = { backup ->
                    return if (backup.version != BACKUP_VERSION) {
                        Result.failure(StudentError.UnsupportedBackupVersion(backup.version))
                    } else {
                        Result.success(
                            ParsedImportData(
                                students = backup.students.map { ParsedStudentImportData(it) },
                                availableHours = backup.availableHours
                            )
                        )
                    }
                },
                onFailure = {
                    return serializationService
                        .decodeFromString(jsonString, ListSerializer(StudentExportData.serializer()))
                        .fold(
                            onSuccess = { legacyData ->
                                Result.success(
                                    ParsedImportData(
                                        students = legacyData.map { ParsedStudentImportData(it) },
                                        availableHours = emptyList()
                                    )
                                )
                            },
                            onFailure = { Result.failure(StudentError.ImportParse) }
                        )
                }
            )
    }
}