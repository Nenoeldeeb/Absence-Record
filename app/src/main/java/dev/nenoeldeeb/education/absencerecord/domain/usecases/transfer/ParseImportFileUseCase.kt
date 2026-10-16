package dev.nenoeldeeb.education.absencerecord.domain.usecases.transfer

import dev.nenoeldeeb.education.absencerecord.domain.models.BackupExportData
import dev.nenoeldeeb.education.absencerecord.domain.models.ParsedImportData
import dev.nenoeldeeb.education.absencerecord.domain.models.ParsedStudentImportData
import dev.nenoeldeeb.education.absencerecord.domain.models.SUPPORTED_BACKUP_VERSIONS
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentExportData
import dev.nenoeldeeb.education.absencerecord.domain.repositories.StorageRepository
import dev.nenoeldeeb.education.absencerecord.domain.services.ImportCaps
import dev.nenoeldeeb.education.absencerecord.domain.services.SerializationService
import kotlinx.serialization.builtins.ListSerializer

class ParseImportFileUseCase(
    private val storageRepository: StorageRepository,
    private val serializationService: SerializationService
) {
    suspend operator fun invoke(uriString: String): Result<ParsedImportData> {
        // The size guard runs here, before anything is decoded: a file that is
        // too large must be refused without being materialised (FR-063, SC-010).
        val result = storageRepository.readTextFromUri(uriString, ImportCaps.MAX_IMPORT_FILE_BYTES)
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
                    // Current and immediately-preceding versions both load; a
                    // legacy count is converted per entry rather than here, so
                    // one shape serves both formats (FR-044, FR-045). Anything
                    // else — older, or newer than this build — is refused
                    // without writing anything (FR-047).
                    return if (backup.version !in SUPPORTED_BACKUP_VERSIONS) {
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