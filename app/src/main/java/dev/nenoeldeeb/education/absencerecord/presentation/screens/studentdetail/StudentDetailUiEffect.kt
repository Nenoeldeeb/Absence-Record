package dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail

sealed interface StudentDetailUiEffect {
    data object NavigateBack : StudentDetailUiEffect
}