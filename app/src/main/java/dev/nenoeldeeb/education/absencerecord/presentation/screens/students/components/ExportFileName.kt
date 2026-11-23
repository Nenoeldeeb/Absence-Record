package dev.nenoeldeeb.education.absencerecord.presentation.screens.students.components

import android.content.res.Resources
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.presentation.utils.DateFormatter
import kotlinx.datetime.number

internal fun defaultExportFileName(res: Resources): String {
    val today = DateFormatter.today()
    val stamp = "${today.year}${today.month.number.toString().padStart(2, '0')}${today.day.toString().padStart(2, '0')}"
    return res.getString(R.string.absencerecord_export_json, stamp)
}