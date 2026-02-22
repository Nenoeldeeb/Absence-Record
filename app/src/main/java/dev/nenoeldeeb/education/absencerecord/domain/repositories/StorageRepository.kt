package dev.nenoeldeeb.education.absencerecord.domain.repositories

interface StorageRepository {
    suspend fun readTextFromUri(uriString: String): Result<String>

    suspend fun writeTextToUri(
        uriString: String,
        text: String
    ): Result<Unit>
}