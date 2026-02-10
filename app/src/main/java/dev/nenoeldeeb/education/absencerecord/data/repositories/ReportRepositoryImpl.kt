package dev.nenoeldeeb.education.absencerecord.data.repositories

import android.content.Context
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import dev.nenoeldeeb.education.absencerecord.data.utils.CalendarImageGenerator
import dev.nenoeldeeb.education.absencerecord.domain.repositories.ReportRepository
import dev.nenoeldeeb.education.absencerecord.domain.services.DispatcherProvider
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

class ReportRepositoryImpl(
    private val context: Context,
    private val dispatcherProvider: DispatcherProvider
) : ReportRepository {
    override suspend fun generateAndSaveReport(
        studentName: String,
        month: LocalDate,
        attendance: List<LocalDate>
    ): Result<String> =
        withContext(dispatcherProvider.io) {
            try {
                val markedDays = attendance.map { it.day }.toSet()
                val bitmap =
                    CalendarImageGenerator.generateCalendarBitmap(
                        context = context,
                        month = month,
                        studentName = studentName,
                        markedDays = markedDays
                    )

                val fileName = "calendar_${studentName}_$month.png"
                val cacheDir = File(context.cacheDir, "shared_images")
                if (!cacheDir.exists()) cacheDir.mkdirs()
                val file = File(cacheDir, fileName)
                file.outputStream().use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                val authority = "dev.nenoeldeeb.education.absencerecord.fileprovider"
                val uri = FileProvider.getUriForFile(context, authority, file)
                Result.success(uri.toString())
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Result.failure(e)
            }
        }
}