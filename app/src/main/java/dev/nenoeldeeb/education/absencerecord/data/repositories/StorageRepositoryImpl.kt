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
    override suspend fun readTextFromUri(
        uriString: String,
        maxBytes: Long
    ): Result<String> =
        withContext(dispatcherProvider.io) {
            try {
                val uri = uriString.toUri()
                val stream = context.contentResolver.openInputStream(uri)
                if (stream == null) return@withContext Result.failure(StudentError.FileRead)
                stream.use { inputStream ->
                    val chunk = ByteArray(READ_CHUNK_BYTES)
                    val collected = java.io.ByteArrayOutputStream()
                    var total = 0L
                    while (true) {
                        val read = inputStream.read(chunk)
                        if (read <= 0) break
                        total += read
                        // Bail the moment the limit is crossed, so an oversize
                        // file is never held in memory (FR-063). Reading in
                        // chunks rather than pre-sizing keeps an unlimited read
                        // from allocating the whole file up front.
                        if (total > maxBytes) {
                            return@withContext Result.failure(StudentError.ImportFileTooLarge)
                        }
                        collected.write(chunk, 0, read)
                    }
                    Result.success(collected.toByteArray().decodeToString())
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

    private companion object {
        /** Read granularity; unrelated to the limit, which is enforced cumulatively. */
        const val READ_CHUNK_BYTES = 8 * 1024
    }
}