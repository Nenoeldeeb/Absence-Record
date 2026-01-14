package dev.nenoeldeeb.education.absencerecord.domain.repositories

import kotlinx.coroutines.flow.Flow

interface StorageRepository {
    fun readTextFromUri(uriString: String): Flow<Result<String>>

    fun writeTextToUri(
        uriString: String,
        text: String
    ): Flow<Result<Unit>>
}