package dev.nenoeldeeb.education.absencerecord.data.repositories

import android.content.Context
import androidx.core.net.toUri
import dev.nenoeldeeb.education.absencerecord.domain.repositories.StorageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class StorageRepositoryImpl(
    private val context: Context
) : StorageRepository {
    override fun readTextFromUri(uriString: String): Flow<Result<String>> =
        flow {
            try {
                val uri = uriString.toUri()
                val content =
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.readBytes().decodeToString()
                    }
                if (content != null) {
                    emit(Result.success(content))
                } else {
                    emit(Result.failure(Exception("Could not open input stream for URI: $uriString")))
                }
            } catch (e: Exception) {
                emit(Result.failure(e))
            }
        }.flowOn(Dispatchers.IO)

    override fun writeTextToUri(
        uriString: String,
        text: String
    ): Flow<Result<Unit>> =
        flow {
            try {
                val uri = uriString.toUri()
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(text.toByteArray())
                }
                emit(Result.success(Unit))
            } catch (e: Exception) {
                emit(Result.failure(e))
            }
        }.flowOn(Dispatchers.IO)
}