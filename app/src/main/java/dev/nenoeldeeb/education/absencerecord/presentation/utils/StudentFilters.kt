package dev.nenoeldeeb.education.absencerecord.presentation.utils

import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.presentation.screens.components.ClassFilter

fun Iterable<Student>.applyClassFilter(filter: ClassFilter): List<Student> =
    when (filter) {
        ClassFilter.All -> this.toList()
        ClassFilter.Unassigned -> this.filter { it.classId == null }
        is ClassFilter.ByClass -> this.filter { it.classId == filter.studentClass.id }
    }

fun Iterable<Student>.applyMultiClassFilter(classIds: Set<Int>): List<Student> {
    if (classIds.isEmpty()) return this.filter { it.classId == null }
    return this
        .filter { it.classId != null && it.classId in classIds }
        .distinctBy { it.id }
}