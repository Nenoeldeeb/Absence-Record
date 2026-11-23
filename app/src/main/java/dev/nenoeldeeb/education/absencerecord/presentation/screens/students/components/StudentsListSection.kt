package dev.nenoeldeeb.education.absencerecord.presentation.screens.students.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.nenoeldeeb.education.absencerecord.domain.models.SortType
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsScreenState
import kotlinx.datetime.LocalDate

/**
 * The Scaffold body of the Students screen: the collapsible sort/class-filter
 * panels, the selection-scope banner, and the roster itself (list or empty
 * state). Purely display; every interaction is dispatched via callbacks.
 */
@Composable
internal fun StudentsListSection(
    uiState: StudentsScreenState,
    listState: LazyListState,
    isMultiSelectionMode: Boolean,
    isSortPanelVisible: Boolean,
    sortType: SortType,
    expandEnter: EnterTransition,
    shrinkExit: ExitTransition,
    onListStudentClick: (Student) -> Unit,
    onListStudentLongClick: (Int) -> Unit,
    onAddStudent: () -> Unit,
    onMonthDropdownChange: (Boolean) -> Unit,
    onMonthSelected: (LocalDate?) -> Unit,
    onToggleSortType: () -> Unit,
    onClassDropdownChange: (Boolean) -> Unit,
    onClassToggle: (Int) -> Unit,
    onManageClasses: () -> Unit,
    onImportFile: () -> Unit,
    modifier: Modifier = Modifier,
    onClearClassFilter: (() -> Unit)? = null,
    onClearSearch: () -> Unit = {}
) {
    // O(n) roster scan, but only when the roster itself changes — not on
    // every selection/panel state emission.
    val hasUnassignedInRoster =
        remember(uiState.allStudents) { uiState.allStudents.any { it.classId == null } }
    Column(
        modifier = modifier.widthIn(max = 600.dp).fillMaxSize()
    ) {
        if (!isMultiSelectionMode) {
            AnimatedVisibility(
                visible = isSortPanelVisible,
                enter = expandEnter,
                exit = shrinkExit
            ) {
                SortPanel(
                    sortType = sortType,
                    selectedMonth = uiState.selectedMonth,
                    availableMonths = uiState.availableMonths,
                    monthDropdownExpanded = uiState.isMonthDropdownExpanded,
                    onMonthDropdownExpandedChange = onMonthDropdownChange,
                    onMonthSelected = onMonthSelected,
                    onToggleSortType = onToggleSortType,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            AnimatedVisibility(
                visible = uiState.isClassFilterVisible,
                enter = expandEnter,
                exit = shrinkExit
            ) {
                StudentsFilterSection(
                    availableClasses = uiState.availableClasses,
                    selectedClassIds = uiState.selectedClassIds,
                    classDropdownExpanded = uiState.classDropdownExpanded,
                    onExpandedChange = onClassDropdownChange,
                    onClassToggle = onClassToggle,
                    onManageClasses = onManageClasses,
                    onClearFilters = onClearClassFilter
                )
            }
        }

        if (isMultiSelectionMode &&
            uiState.selectedClassIds.isNotEmpty() &&
            hasUnassignedInRoster
        ) {
            SelectionScopeBanner(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        if (uiState.allStudents.isEmpty()) {
            StudentsEmptyState(
                totalStudentsCount = uiState.totalStudentsCount,
                searchQuery = uiState.searchQuery,
                onAddFirstStudent = onAddStudent,
                onImportStudents = onImportFile,
                onClearSearch = onClearSearch,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            StudentList(
                allStudents = uiState.allStudents,
                state = listState,
                selectedStudentIds = uiState.selectedStudentIds,
                isMultiSelectionMode = isMultiSelectionMode,
                availableClasses = uiState.availableClasses,
                selectedMonth = uiState.selectedMonth,
                modifier = Modifier.fillMaxSize(),
                onStudentClick = onListStudentClick,
                onStudentLongClick = onListStudentLongClick
            )
        }
    }
}