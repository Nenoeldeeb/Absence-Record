package dev.nenoeldeeb.education.absencerecord.presentation.screens.students

import dev.nenoeldeeb.education.absencerecord.domain.models.Student

internal fun handleStudentClickBehavior(
    isMultiSelectionMode: Boolean,
    student: Student,
    onNavigateToDetail: (Int) -> Unit,
    onToggleStudentSelection: (Int) -> Unit
) {
    if (isMultiSelectionMode) {
        onToggleStudentSelection(student.id)
    } else {
        onNavigateToDetail(student.id)
    }
}

internal fun handleStudentLongPressBehavior(
    isMultiSelectionMode: Boolean,
    studentId: Int,
    onEnterSelectionMode: () -> Unit,
    onSelectStudent: (Int) -> Unit
) {
    if (!isMultiSelectionMode) {
        onEnterSelectionMode()
        onSelectStudent(studentId)
    }
}