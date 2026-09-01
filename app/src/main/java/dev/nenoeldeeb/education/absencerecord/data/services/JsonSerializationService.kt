package dev.nenoeldeeb.education.absencerecord.data.services

import dev.nenoeldeeb.education.absencerecord.domain.services.SerializationService
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

class JsonSerializationService : SerializationService {
    val json =
        Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            isLenient = true
        }

    override fun <T> encodeToString(
        data: T,
        serializer: KSerializer<T>
    ): Result<String> {
        return runCatching {
            json.encodeToString(serializer, data)
        }
    }

    override fun <T> decodeFromString(
        jsonString: String,
        deserializer: KSerializer<T>
    ): Result<T> {
        return runCatching {
            json.decodeFromString(deserializer, jsonString)
        }
    }
}