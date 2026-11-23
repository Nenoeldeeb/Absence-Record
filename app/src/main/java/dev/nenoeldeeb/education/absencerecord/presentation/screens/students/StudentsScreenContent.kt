package dev.nenoeldeeb.education.absencerecord.presentation.screens.students

import android.animation.ValueAnimator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.SortType
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.components.MultiSelectionHeader
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.components.StudentsListSection
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.components.StudentsTopBar
import kotlinx.datetime.LocalDate

@Composable
fun StudentsScreenContent(
    uiState: StudentsScreenState,
    onEvent: (StudentsScreenEvent) -> Unit,
    listState: LazyListState,
    isMultiSelectionMode: Boolean,
    isSortPanelVisible: Boolean,
    sortType: SortType,
    snackbarHostState: SnackbarHostState,
    onConfirmExportDelete: () -> Unit,
    onListStudentClick: (Student) -> Unit,
    onListStudentLongClick: (Int) -> Unit,
    onAddStudent: () -> Unit,
    onToggleClassFilter: () -> Unit,
    onToggleSortPanel: () -> Unit,
    onToggleSortType: () -> Unit,
    onExportFile: (String) -> Unit,
    onImportFile: () -> Unit,
    onCloseSelectionMode: () -> Unit,
    onToggleStudentsSelection: () -> Unit,
    onShowExportDeleteDialog: () -> Unit,
    onShowBulkDeleteDialog: () -> Unit,
    onMonthDropdownChange: (Boolean) -> Unit,
    onMonthSelected: (LocalDate?) -> Unit,
    onClassDropdownChange: (Boolean) -> Unit,
    onClassToggle: (Int) -> Unit,
    onManageClasses: () -> Unit,
    modifier: Modifier = Modifier,
    onClearClassFilter: (() -> Unit)? = null,
    onSearchQueryChange: (String) -> Unit = {},
    onToggleSearch: (Boolean) -> Unit = {},
    onClearSearch: () -> Unit = {}
) {
    val context = LocalContext.current
    val animationsEnabled = ValueAnimator.areAnimatorsEnabled()
    // Remembered so unrelated state changes (search keystrokes, dropdown
    // toggles) don't hand fresh transition objects to every AnimatedVisibility
    // and re-trigger parent remeasurement of the list below.
    val expandEnter =
        remember(animationsEnabled) {
            if (animationsEnabled) {
                expandVertically() + fadeIn()
            } else {
                EnterTransition.None
            }
        }
    val shrinkExit =
        remember(animationsEnabled) {
            if (animationsEnabled) {
                shrinkVertically() + fadeOut()
            } else {
                ExitTransition.None
            }
        }
    val fabEnter =
        remember(animationsEnabled) {
            if (animationsEnabled) {
                fadeIn() + scaleIn()
            } else {
                EnterTransition.None
            }
        }
    val fabExit =
        remember(animationsEnabled) {
            if (animationsEnabled) {
                fadeOut() + scaleOut()
            } else {
                ExitTransition.None
            }
        }
    // FAB is hidden in multi-selection mode, where bulk actions own the
    // screen, and on first-run empty state, where "Add first student" is
    // already the primary CTA — a third add path would duplicate it and
    // cover the tip below.
    val isRosterEmpty =
        uiState.totalStudentsCount == 0 && uiState.allStudents.isEmpty()
    // Hide the FAB while the roster scrolls away under the thumb (standard
    // M3 scroll behavior). Sticky once hidden: it comes back ONLY on an
    // upward scroll (or when the list returns to the very top), never
    // just because a scroll frame wasn't downward.
    var fabHiddenForScroll by remember { mutableStateOf(false) }
    var lastFirstVisibleIndex by remember { mutableIntStateOf(listState.firstVisibleItemIndex) }
    var lastFirstVisibleOffset by remember { mutableIntStateOf(listState.firstVisibleItemScrollOffset) }
    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        val index = listState.firstVisibleItemIndex
        val offset = listState.firstVisibleItemScrollOffset
        val scrolledDown =
            index > lastFirstVisibleIndex ||
                (index == lastFirstVisibleIndex && offset - lastFirstVisibleOffset >= 2)
        val scrollingUp =
            index < lastFirstVisibleIndex ||
                (index == lastFirstVisibleIndex && lastFirstVisibleOffset - offset >= 2)
        val atTop = index == 0 && offset <= 56
        when {
            scrolledDown && !atTop -> fabHiddenForScroll = true
            scrollingUp || atTop -> fabHiddenForScroll = false
        }
        lastFirstVisibleIndex = index
        lastFirstVisibleOffset = offset
    }
    // Bulk-selection mode takes over the screen; make sure the FAB comes
    // back once it exits, even if the user isn't scrolling anymore.
    LaunchedEffect(isMultiSelectionMode) {
        if (isMultiSelectionMode) fabHiddenForScroll = false
    }
    val fabVisible = !isMultiSelectionMode && !isRosterEmpty && !fabHiddenForScroll
    val filteredCount = uiState.allStudents.size
    val totalCount = uiState.totalStudentsCount.takeIf { it > 0 } ?: filteredCount
    val undoLabel = stringResource(R.string.action_undo)

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it.asString(context))
            onEvent(StudentsScreenEvent.ConsumeError)
        }
    }
    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { toast ->
            if (uiState.lastDeletedBackup.isNotEmpty()) {
                val result =
                    snackbarHostState.showSnackbar(
                        message = toast.asString(context),
                        actionLabel = undoLabel,
                        duration = SnackbarDuration.Long
                    )
                if (result == SnackbarResult.ActionPerformed) {
                    onEvent(StudentsScreenEvent.UndoDeleteStudents)
                } else {
                    onEvent(StudentsScreenEvent.ConsumeUndoBackup)
                }
            } else {
                snackbarHostState.showSnackbar(toast.asString(context))
            }
            onEvent(StudentsScreenEvent.ConsumeToastMessage)
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            AnimatedVisibility(
                visible = fabVisible,
                enter = fabEnter,
                exit = fabExit
            ) {
                FloatingActionButton(onClick = onAddStudent) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.outline_add_24),
                        contentDescription = stringResource(R.string.new_student_label)
                    )
                }
            }
        },
        topBar = {
            if (isMultiSelectionMode) {
                MultiSelectionHeader(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(bottom = 16.dp),
                    selectedStudentIds = uiState.selectedStudentIds,
                    totalCount = filteredCount,
                    onCloseSelectionMode = onCloseSelectionMode,
                    onToggleSelection = onToggleStudentsSelection,
                    onExport = onExportFile,
                    onExportAndDelete = { onShowExportDeleteDialog() },
                    onShowDeleteDialog = onShowBulkDeleteDialog
                )
            } else {
                val title =
                    if (totalCount > 0 && filteredCount != totalCount) {
                        stringResource(
                            R.string.students_title_filtered,
                            filteredCount,
                            totalCount
                        )
                    } else {
                        stringResource(R.string.students_title, filteredCount)
                    }
                StudentsTopBar(
                    title = title,
                    isFilterActive = uiState.selectedClassIds.isNotEmpty(),
                    isSortActive =
                        sortType == SortType.ByAttendance ||
                            uiState.selectedMonth != null,
                    searchQuery = uiState.searchQuery,
                    isSearchActive = uiState.isSearchActive,
                    onToggleClassFilter = onToggleClassFilter,
                    onToggleSortPanel = onToggleSortPanel,
                    onSearchQueryChange = onSearchQueryChange,
                    onToggleSearch = onToggleSearch
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier.padding(paddingValues).fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            StudentsListSection(
                uiState = uiState,
                listState = listState,
                isMultiSelectionMode = isMultiSelectionMode,
                isSortPanelVisible = isSortPanelVisible,
                sortType = sortType,
                expandEnter = expandEnter,
                shrinkExit = shrinkExit,
                onListStudentClick = onListStudentClick,
                onListStudentLongClick = onListStudentLongClick,
                onAddStudent = onAddStudent,
                onMonthDropdownChange = onMonthDropdownChange,
                onMonthSelected = onMonthSelected,
                onToggleSortType = onToggleSortType,
                onClassDropdownChange = onClassDropdownChange,
                onClassToggle = onClassToggle,
                onManageClasses = onManageClasses,
                onImportFile = onImportFile,
                onClearClassFilter = onClearClassFilter,
                onClearSearch = onClearSearch
            )
        }
    }

    StudentsDialogs(
        uiState = uiState,
        onEvent = onEvent,
        onImportClick = onImportFile,
        onConfirmExportDelete = onConfirmExportDelete
    )
}