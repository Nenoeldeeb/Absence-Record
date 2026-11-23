package dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.nenoeldeeb.education.absencerecord.app.AppViewModelProvider

@Composable
fun ScheduleScreen(
    onStudentClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScheduleViewModel = viewModel(factory = AppViewModelProvider.factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ScheduleScreenContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onStudentClick = onStudentClick,
        modifier = modifier
    )
}