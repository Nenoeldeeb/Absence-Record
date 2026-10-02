package dev.nenoeldeeb.education.absencerecord.presentation.screens.students.components

import android.content.res.Resources
import dev.nenoeldeeb.education.absencerecord.R
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

internal fun defaultExportFileName(res: Resources): String {
    val stamp =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).run {
            "${year}${month.number.toString().padStart(2, '0')}${day.toString().padStart(2, '0')}"
        }
    return res.getString(R.string.absencerecord_export_json, stamp)
}