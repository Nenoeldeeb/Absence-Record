package dev.nenoeldeeb.education.absencerecord.presentation.screens.students.handlers

import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class PanelVisibilityHandler {
    fun handleToggleSortPanel(
        show: Boolean,
        uiState: MutableStateFlow<StudentsScreenState>
    ) {
        uiState.update { state ->
            state.copy(
                isSortPanelVisible = show,
                isClassFilterVisible = if (show) false else state.isClassFilterVisible,
                classDropdownExpanded = if (show) false else state.classDropdownExpanded
            )
        }
    }

    fun handleToggleClassFilterVisibility(uiState: MutableStateFlow<StudentsScreenState>) {
        uiState.update { state ->
            val opening = !state.isClassFilterVisible
            state.copy(
                isClassFilterVisible = opening,
                isSortPanelVisible = if (opening) false else state.isSortPanelVisible,
                isMonthDropdownExpanded = if (opening) false else state.isMonthDropdownExpanded
            )
        }
    }
}