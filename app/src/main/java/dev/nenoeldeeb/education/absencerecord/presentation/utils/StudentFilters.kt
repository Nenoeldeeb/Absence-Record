package dev.nenoeldeeb.education.absencerecord.presentation.utils

import dev.nenoeldeeb.education.absencerecord.domain.models.Student

fun Iterable<Student>.applyMultiClassFilter(classIds: Set<Int>): List<Student> {
    if (classIds.isEmpty()) return this.filter { it.classId == null }
    return this
        .filter { it.classId != null && it.classId in classIds }
        .distinctBy { it.id }
}

/**
 * Selection-mode scope for the Students bulk-action layout.
 *
 * Browse semantics stay untouched: an empty filter shows unassigned students
 * only and a partial filter shows enrolled students of the checked classes.
 * In multi-selection mode, when the checked classes cover every assigned
 * student in the roster (i.e. the filter hides nothing), unassigned students
 * are appended in roster order so the whole roster stays selectable for
 * deletion or export. Otherwise the browse filter applies unchanged.
 */
fun Iterable<Student>.applySelectionScopeFilter(
    classIds: Set<Int>,
    inSelectionMode: Boolean
): List<Student> {
    if (!inSelectionMode || classIds.isEmpty()) return applyMultiClassFilter(classIds)
    if (any { it.classId != null && it.classId !in classIds }) return applyMultiClassFilter(classIds)
    return this
        .filter { it.classId == null || it.classId in classIds }
        .distinctBy { it.id }
}

/**
 * Name-contains search applied AFTER class/scope filtering.
 * Trimmed, case-insensitive (Arabic + English). Blank query returns input unchanged.
 */
fun Iterable<Student>.applyNameSearchFilter(query: String): List<Student> {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return this.toList()
    return this.filter { it.name.contains(trimmed, ignoreCase = true) }
}

/** Class/scope filter followed by name search. Search never widens the class scope. */
fun Iterable<Student>.applyStudentListFilters(
    classIds: Set<Int>,
    inSelectionMode: Boolean,
    query: String
): List<Student> = applySelectionScopeFilter(classIds, inSelectionMode).applyNameSearchFilter(query)