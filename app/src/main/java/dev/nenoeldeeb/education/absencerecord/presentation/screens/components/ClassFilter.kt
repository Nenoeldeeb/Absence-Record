package dev.nenoeldeeb.education.absencerecord.presentation.screens.components

import dev.nenoeldeeb.education.absencerecord.domain.models.StudentClass

sealed interface ClassFilter {
    data object All : ClassFilter

    data object Unassigned : ClassFilter

    data class ByClass(val studentClass: StudentClass) : ClassFilter
}
