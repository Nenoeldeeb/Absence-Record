package dev.nenoeldeeb.education.absencerecord.domain.services

import kotlinx.serialization.KSerializer

interface SerializationService {
    fun <T> encodeToString(
        data: T,
        serializer: KSerializer<T>
    ): Result<String>

    fun <T> decodeFromString(
        jsonString: String,
        deserializer: KSerializer<T>
    ): Result<T>
}