package dev.nenoeldeeb.education.absencerecord.presentation.utils

import dev.nenoeldeeb.education.absencerecord.domain.models.Student

fun Iterable<Student>.applyMultiClassFilter(classIds: Set<Int>): List<Student> {
    if (classIds.isEmpty()) return this.filter { it.classId == null }
    return this
        .filter { it.classId != null && it.classId in classIds }
        .distinctBy { it.id }
}