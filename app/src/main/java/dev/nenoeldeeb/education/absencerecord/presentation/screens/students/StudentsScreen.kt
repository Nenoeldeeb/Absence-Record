@file:OptIn(ExperimentalMaterial3Api::class)

package dev.nenoeldeeb.education.absencerecord.presentation.screens.students

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.app.AppViewModelProvider
import dev.nenoeldeeb.education.absencerecord.domain.models.SortType
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.components.defaultExportFileName
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.components.handleStudentClickBehavior
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.components.handleStudentLongPressBehavior

@Composable
fun StudentsScreen(
    modifier: Modifier = Modifier,
    onStudentClick: (Int) -> Unit = {},
    listState: LazyListState,
    viewModel: StudentsViewModel = viewModel(factory = AppViewModelProvider.factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it.asString(context))
            viewModel.onEvent(StudentsScreenEvent.ConsumeError)
        }
    }
    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { toast ->
            if (uiState.lastDeletedBackup.isNotEmpty()) {
                val result =
                    snackbarHostState.showSnackbar(
                        message = toast.asString(context),
                        actionLabel = context.getString(R.string.action_undo),
                        duration = SnackbarDuration.Long
                    )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.onEvent(StudentsScreenEvent.UndoDeleteStudents)
                } else {
                    viewModel.onEvent(StudentsScreenEvent.ConsumeUndoBackup)
                }
            } else {
                snackbarHostState.showSnackbar(toast.asString(context))
            }
            viewModel.onEvent(StudentsScreenEvent.ConsumeToastMessage)
        }
    }
    BackHandler(uiState.isMultiSelectionMode) {
        viewModel.onEvent(StudentsScreenEvent.ToggleSelectionMode)
    }

    val exportLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/json"),
            onResult = { uri ->
                uri?.let {
                    viewModel.onEvent(StudentsScreenEvent.ExportSelectedStudents(it))
                }
            }
        )

    val exportAndDeleteLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/json"),
            onResult = { uri ->
                if (uri != null) {
                    viewModel.onEvent(
                        StudentsScreenEvent.ExportAndDeleteSelectedStudents(uri)
                    )
                }
            }
        )

    val importLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
            onResult = { uri ->
                viewModel.onEvent(StudentsScreenEvent.PrepareImportSelectionDialog(uri))
            }
        )

    // Stable callbacks keep lists skippable on unrelated state changes.
    val isMultiSelectionMode = uiState.isMultiSelectionMode
    val isSortPanelVisible = uiState.isSortPanelVisible
    val sortType = uiState.sortType
    val onEventStable: (StudentsScreenEvent) -> Unit =
        remember(viewModel) { { event -> viewModel.onEvent(event) } }
    val onListStudentClick =
        remember(isMultiSelectionMode, onStudentClick) {
            { student: Student ->
                handleStudentClickBehavior(
                    isMultiSelectionMode = isMultiSelectionMode,
                    student = student,
                    onNavigateToDetail = onStudentClick,
                    onToggleStudentSelection = { studentId ->
                        viewModel.onEvent(
                            StudentsScreenEvent.ToggleStudentSelection(studentId)
                        )
                    }
                )
            }
        }
    val onListStudentLongClick =
        remember(isMultiSelectionMode) {
            { studentId: Int ->
                handleStudentLongPressBehavior(
                    isMultiSelectionMode = isMultiSelectionMode,
                    studentId = studentId,
                    onEnterSelectionMode = {
                        viewModel.onEvent(StudentsScreenEvent.ToggleSelectionMode)
                    },
                    onSelectStudent = { id ->
                        viewModel.onEvent(
                            StudentsScreenEvent.ToggleStudentSelection(id)
                        )
                    }
                )
            }
        }
    val onAddStudent = remember(viewModel) { { viewModel.onEvent(StudentsScreenEvent.ShowStudentDialog(true)) } }
    val onToggleClassFilter =
        remember(viewModel) { { viewModel.onEvent(StudentsScreenEvent.ToggleClassFilterVisibility) } }
    val onToggleSortPanel =
        remember(viewModel, isSortPanelVisible) {
            {
                viewModel.onEvent(
                    StudentsScreenEvent.ToggleSortPanelVisible(!isSortPanelVisible)
                )
            }
        }
    val onToggleSortType =
        remember(viewModel, sortType) {
            {
                viewModel.onEvent(
                    StudentsScreenEvent.UpdateSortType(
                        if (sortType == SortType.ByName) {
                            SortType.ByAttendance
                        } else {
                            SortType.ByName
                        }
                    )
                )
            }
        }
    val onExportFile = remember(exportLauncher) { { fileName: String -> exportLauncher.launch(fileName) } }
    val onImportFile =
        remember(importLauncher) { { importLauncher.launch(arrayOf("application/json", "*/*")) } }
    val onCloseSelectionMode =
        remember(viewModel) { { viewModel.onEvent(StudentsScreenEvent.ToggleSelectionMode) } }
    val onToggleStudentsSelection =
        remember(viewModel) { { viewModel.onEvent(StudentsScreenEvent.ToggleStudentsSelection) } }
    val onShowExportDeleteDialog =
        remember(viewModel) { { viewModel.onEvent(StudentsScreenEvent.ShowExportDeleteDialog) } }
    val onShowBulkDeleteDialog =
        remember(viewModel) { { viewModel.onEvent(StudentsScreenEvent.ShowBulkDeleteDialog) } }
    val onMonthDropdownChange =
        remember(viewModel) { { expanded: Boolean -> viewModel.onEvent(StudentsScreenEvent.ToggleMonthDropdown(expanded)) } }
    val onMonthSelected =
        remember(viewModel) {
            { month: kotlinx.datetime.LocalDate? ->
                viewModel.onEvent(StudentsScreenEvent.UpdateSelectedMonth(month))
            }
        }
    val onClassDropdownChange =
        remember(viewModel) { { expanded: Boolean -> viewModel.onEvent(StudentsScreenEvent.ToggleClassDropdown(expanded)) } }
    val onClassToggle =
        remember(viewModel) { { classId: Int -> viewModel.onEvent(StudentsScreenEvent.ToggleClassFilter(classId)) } }
    val onClearClassFilter =
        remember(viewModel) { { viewModel.onEvent(StudentsScreenEvent.ClearClassFilter) } }
    val onManageClasses =
        remember(viewModel) { { viewModel.onEvent(StudentsScreenEvent.ShowManageClassesDialog(true)) } }
    val onSearchQueryChange =
        remember(viewModel) { { query: String -> viewModel.onEvent(StudentsScreenEvent.UpdateSearchQuery(query)) } }
    val onToggleSearch =
        remember(viewModel) { { active: Boolean -> viewModel.onEvent(StudentsScreenEvent.ToggleSearch(active)) } }
    val onClearSearch =
        remember(viewModel) { { viewModel.onEvent(StudentsScreenEvent.UpdateSearchQuery("")) } }
    val onConfirmExportDelete =
        remember(exportAndDeleteLauncher, context.resources) {
            {
                exportAndDeleteLauncher.launch(defaultExportFileName(context.resources))
            }
        }

    StudentsScreenContent(
        uiState = uiState,
        listState = listState,
        isMultiSelectionMode = isMultiSelectionMode,
        isSortPanelVisible = isSortPanelVisible,
        sortType = sortType,
        snackbarHostState = snackbarHostState,
        onListStudentClick = onListStudentClick,
        onListStudentLongClick = onListStudentLongClick,
        onAddStudent = onAddStudent,
        onToggleClassFilter = onToggleClassFilter,
        onToggleSortPanel = onToggleSortPanel,
        onToggleSortType = onToggleSortType,
        onExportFile = onExportFile,
        onImportFile = onImportFile,
        onCloseSelectionMode = onCloseSelectionMode,
        onToggleStudentsSelection = onToggleStudentsSelection,
        onShowExportDeleteDialog = onShowExportDeleteDialog,
        onShowBulkDeleteDialog = onShowBulkDeleteDialog,
        onMonthDropdownChange = onMonthDropdownChange,
        onMonthSelected = onMonthSelected,
        onClassDropdownChange = onClassDropdownChange,
        onClassToggle = onClassToggle,
        onManageClasses = onManageClasses,
        onClearClassFilter = onClearClassFilter,
        onSearchQueryChange = onSearchQueryChange,
        onToggleSearch = onToggleSearch,
        onClearSearch = onClearSearch,
        modifier = modifier
    )

    StudentsDialogs(
        uiState = uiState,
        onEvent = onEventStable,
        onImportClick = onImportFile,
        onConfirmExportDelete = onConfirmExportDelete
    )
}