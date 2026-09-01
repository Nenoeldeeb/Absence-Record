@file:OptIn(ExperimentalMaterial3Api::class)

package dev.nenoeldeeb.education.absencerecord.presentation.screens.students

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.app.AppViewModelProvider
import dev.nenoeldeeb.education.absencerecord.domain.models.SortType
import dev.nenoeldeeb.education.absencerecord.presentation.screens.components.ClassCheckboxFilter
import dev.nenoeldeeb.education.absencerecord.presentation.screens.components.EmptyStateMessage
import dev.nenoeldeeb.education.absencerecord.presentation.screens.components.StudentList
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.components.MultiSelectionHeader
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.components.SortPanel
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.components.StudentsTopBar
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
        uiState.toastMessage?.let {
            snackbarHostState.showSnackbar(it.asString(context))
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

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            if (uiState.isMultiSelectionMode) {
                MultiSelectionHeader(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                    selectedStudentIds = uiState.selectedStudentIds,
                    onCloseSelectionMode = {
                        viewModel.onEvent(StudentsScreenEvent.ToggleSelectionMode)
                    },
                    onToggleSelection = {
                        viewModel.onEvent(StudentsScreenEvent.ToggleStudentsSelection)
                    },
                    onExport = { fileName -> exportLauncher.launch(fileName) },
                    onExportAndDelete = { fileName ->
                        exportAndDeleteLauncher.launch(fileName)
                    },
                    onShowDeleteDialog = {
                        viewModel.onEvent(StudentsScreenEvent.ShowBulkDeleteDialog)
                    }
                )
            }

            if (!uiState.isMultiSelectionMode) {
                StudentsTopBar(
                    title = stringResource(R.string.students_title, uiState.allStudents.size),
                    onAddStudent = {
                        viewModel.onEvent(
                            StudentsScreenEvent.ShowStudentDialog(true)
                        )
                    },
                    onToggleClassFilter = {
                        viewModel.onEvent(
                            StudentsScreenEvent.ToggleClassFilterVisibility
                        )
                    },
                    onToggleSortPanel = {
                        viewModel.onEvent(
                            StudentsScreenEvent.ToggleSortPanelVisible(
                                !uiState.isSortPanelVisible
                            )
                        )
                    }
                )

                AnimatedVisibility(uiState.isSortPanelVisible) {
                    SortPanel(
                        sortType = uiState.sortType,
                        selectedMonth = uiState.selectedMonth,
                        availableMonths = uiState.availableMonths,
                        monthDropdownExpanded = uiState.isMonthDropdownExpanded,
                        onMonthDropdownExpandedChange = {
                            viewModel.onEvent(
                                StudentsScreenEvent.ToggleMonthDropdown(it)
                            )
                        },
                        onMonthSelected = { month ->
                            viewModel.onEvent(
                                StudentsScreenEvent.UpdateSelectedMonth(month)
                            )
                        },
                        onToggleSortType = {
                            viewModel.onEvent(
                                StudentsScreenEvent.UpdateSortType(
                                    if (uiState.sortType == SortType.ByName) {
                                        SortType.ByAttendance
                                    } else {
                                        SortType.ByName
                                    }
                                )
                            )
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                AnimatedVisibility(uiState.isClassFilterVisible) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ClassCheckboxFilter(
                            availableClasses = uiState.availableClasses,
                            selectedClassIds = uiState.selectedClassIds,
                            expanded = uiState.classDropdownExpanded,
                            onExpandedChange = {
                                viewModel.onEvent(StudentsScreenEvent.ToggleClassDropdown(it))
                            },
                            onClassToggle = { classId -> viewModel.onEvent(StudentsScreenEvent.ToggleClassFilter(classId)) },
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.onEvent(StudentsScreenEvent.ShowManageClassesDialog(true)) }
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.outline_edit_24),
                                contentDescription = stringResource(R.string.manage_classes_title),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            if (uiState.allStudents.isEmpty()) {
                EmptyStateMessage(
                    message = R.string.no_students_message,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                StudentList(
                    allStudents = uiState.allStudents,
                    state = listState,
                    selectedStudentIds = uiState.selectedStudentIds,
                    isMultiSelectionMode = uiState.isMultiSelectionMode,
                    onStudentClick = { student ->
                        handleStudentClickBehavior(
                            isMultiSelectionMode = uiState.isMultiSelectionMode,
                            student = student,
                            onNavigateToDetail = onStudentClick,
                            onToggleStudentSelection = { studentId ->
                                viewModel.onEvent(
                                    StudentsScreenEvent.ToggleStudentSelection(studentId)
                                )
                            }
                        )
                    },
                    onStudentLongClick = { studentId ->
                        handleStudentLongPressBehavior(
                            isMultiSelectionMode = uiState.isMultiSelectionMode,
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
                )
            }
        }
    }

    StudentsDialogs(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onImportClick = { importLauncher.launch(arrayOf("application/json", "*/*")) }
    )
}