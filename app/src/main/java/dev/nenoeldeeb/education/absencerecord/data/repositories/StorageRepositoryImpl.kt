package dev.nenoeldeeb.education.absencerecord.data.repositories

import android.content.Context
import androidx.core.net.toUri
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentError
import dev.nenoeldeeb.education.absencerecord.domain.repositories.StorageRepository
import dev.nenoeldeeb.education.absencerecord.domain.services.DispatcherProvider
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

class StorageRepositoryImpl(
    private val context: Context,
    private val dispatcherProvider: DispatcherProvider
) : StorageRepository {
    override suspend fun readTextFromUri(uriString: String): Result<String> =
        withContext(dispatcherProvider.io) {
            try {
                val uri = uriString.toUri()
                val content =
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.readBytes().decodeToString()
                    }
                if (content != null) {
                    Result.success(content)
                } else {
                    Result.failure(StudentError.FileRead)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Result.failure(StudentError.FileRead)
            }
        }

    override suspend fun writeTextToUri(
        uriString: String,
        text: String
    ): Result<Unit> =
        withContext(dispatcherProvider.io) {
            try {
                val uri = uriString.toUri()
                // For file URIs, convert to File and use FileOutputStream to ensure truncation
                if (uri.scheme == "file") {
                    val file = File(uri.path ?: return@withContext Result.failure(StudentError.FileWrite))
                    file.writeText(text)
                    Result.success(Unit)
                } else {
                    // For other URI schemes (content://, etc.), use ContentResolver
                    context.contentResolver.openOutputStream(uri, "w")?.use { outputStream ->
                        outputStream.write(text.toByteArray())
                    }
                    Result.success(Unit)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Result.failure(StudentError.FileWrite)
            }
        }
}