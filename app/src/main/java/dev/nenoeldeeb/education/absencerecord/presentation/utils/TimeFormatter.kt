package dev.nenoeldeeb.education.absencerecord.presentation.utils

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import java.util.Calendar

object TimeFormatter {
    fun format(
        minutes: Int,
        context: Context
    ): String {
        val calendar =
            Calendar.getInstance().apply {
                clear()
                set(Calendar.HOUR_OF_DAY, minutes / 60)
                set(Calendar.MINUTE, minutes % 60)
            }
        return DateFormat.getTimeFormat(context).format(calendar.time)
    }

    @Composable
    fun formatTime(minutes: Int): String {
        val context = LocalContext.current
        return format(minutes, context)
    }
}