@file:OptIn(ExperimentalMaterial3Api::class)

package dev.nenoeldeeb.education.absencerecord.presentation.screens.report

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import dev.nenoeldeeb.education.absencerecord.presentation.screens.components.ClassFilterDropdown
import dev.nenoeldeeb.education.absencerecord.presentation.screens.components.EmptyStateMessage
import dev.nenoeldeeb.education.absencerecord.presentation.screens.components.StudentList
import dev.nenoeldeeb.education.absencerecord.presentation.screens.report.components.ReportControls
import dev.nenoeldeeb.education.absencerecord.presentation.screens.report.dialogs.CalendarPreviewDialog
import dev.nenoeldeeb.education.absencerecord.presentation.screens.report.dialogs.StudentHistoryDialog
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText

@Composable
fun ReportScreen(
    modifier: Modifier = Modifier,
    viewModel: ReportViewModel = viewModel(factory = AppViewModelProvider.factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it.asString(context))
            viewModel.onEvent(ReportScreenEvent.ConsumeError)
        }
    }

    LaunchedEffect(uiState.shareFileUri) {
        uiState.shareFileUri?.let { uri ->
            val shareIntent =
                Intent(Intent.ACTION_VIEW).apply {
                    data = uri
                    flags += Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
            val result =
                runCatching {
                    context.startActivity(
                        Intent.createChooser(
                            shareIntent,
                            context.getString(R.string.view_picture)
                        )
                    )
                }
            viewModel.onEvent(
                ReportScreenEvent.ShareFileResult(
                    uri = uri,
                    error =
                        result.exceptionOrNull()?.let { e ->
                            UiText.StringResource(
                                R.string.sharing_app_not_found,
                                e.message ?: ""
                            )
                        }
                )
            )
        }
    }

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let {
            snackbarHostState.showSnackbar(it.asString(context))
            viewModel.onEvent(ReportScreenEvent.ConsumeToastMessage)
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            TopAppBar(
                title = {},
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.onEvent(ReportScreenEvent.ToggleClassFilterVisibility)
                        }
                    ) {
                        Icon(
                            imageVector =
                                ImageVector.vectorResource(R.drawable.outline_filter_24),
                            contentDescription = stringResource(R.string.filter_description)
                        )
                    }
                    IconButton(
                        onClick = {
                            viewModel.onEvent(ReportScreenEvent.ToggleSortComponentsVisibility)
                        }
                    ) {
                        Icon(
                            imageVector =
                                ImageVector.vectorResource(R.drawable.outline_sort_24),
                            contentDescription = stringResource(R.string.sort_description)
                        )
                    }
                }
            )

            AnimatedVisibility(uiState.isSortComponentsVisible) {
                ReportControls(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    selectedMonth = uiState.selectedMonth,
                    availableMonths = uiState.availableMonths,
                    monthDropdownExpanded = uiState.monthDropdownExpanded,
                    sortType = uiState.sortType,
                    onMonthSelected = { month ->
                        viewModel.onEvent(ReportScreenEvent.UpdateSelectedMonth(month))
                    },
                    onMonthCleared = { viewModel.onEvent(ReportScreenEvent.ClearMonthFilter) },
                    onMonthDropdownToggled = { expanded ->
                        viewModel.onEvent(ReportScreenEvent.ToggleMonthDropdown(expanded))
                    },
                    onSortTypeToggled = { viewModel.onEvent(ReportScreenEvent.ToggleSortType) }
                )
            }

            AnimatedVisibility(uiState.isClassFilterVisible) {
                ClassFilterDropdown(
                    selectedFilter = uiState.selectedClassFilter,
                    availableClasses = uiState.availableClasses,
                    expanded = uiState.classDropdownExpanded,
                    onExpandedChange = {
                        viewModel.onEvent(ReportScreenEvent.ToggleClassDropdown(it))
                    },
                    onFilterSelected = {
                        viewModel.onEvent(ReportScreenEvent.SelectClassFilter(it))
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (uiState.allStudents.isEmpty()) {
                EmptyStateMessage(
                    message = R.string.no_students_found,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                )
            } else {
                StudentList(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                    allStudents = uiState.allStudents,
                    onStudentClick = { student ->
                        viewModel.onEvent(ReportScreenEvent.SelectStudentForHistory(student))
                        viewModel.onEvent(ReportScreenEvent.ShowHistoryDialog(true))
                    },
                    onStudentLongClick = {}
                )
            }
        }
    }

    uiState.selectedStudentForHistory?.let { student ->
        if (uiState.showHistoryDialog) {
            StudentHistoryDialog(
                student = student,
                history = uiState.studentHistory,
                onDismiss = {
                    viewModel.onEvent(ReportScreenEvent.ShowHistoryDialog(false))
                    viewModel.onEvent(ReportScreenEvent.SelectStudentForHistory(null))
                },
                onShareMonth = { month ->
                    viewModel.onEvent(
                        ReportScreenEvent.SelectMonthYearForCalendarPreview(month)
                    )
                    viewModel.onEvent(ReportScreenEvent.ShowCalendarPreviewDialog(true))
                }
            )
        }

        if (uiState.showCalendarPreviewDialog && uiState.selectedMonthYearForCalendarPreview != null) {
            CalendarPreviewDialog(
                student = student,
                monthToPreview = uiState.selectedMonthYearForCalendarPreview!!,
                datesForPreviewMonth =
                    uiState.studentHistory
                        .find {
                            it.first == uiState.selectedMonthYearForCalendarPreview
                        }
                        ?.second
                        ?: emptyList(),
                onDismiss = {
                    viewModel.onEvent(ReportScreenEvent.ShowCalendarPreviewDialog(false))
                },
                onShare = { month, studentId, studentName ->
                    viewModel.onEvent(
                        ReportScreenEvent.PrepareCalendarImageForSharing(
                            month,
                            studentId,
                            studentName
                        )
                    )
                    viewModel.onEvent(ReportScreenEvent.ShowCalendarPreviewDialog(false))
                }
            )
        }
    }
}