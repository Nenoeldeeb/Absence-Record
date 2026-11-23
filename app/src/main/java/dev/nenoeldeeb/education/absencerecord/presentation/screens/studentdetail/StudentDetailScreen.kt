package dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.nenoeldeeb.education.absencerecord.app.AppViewModelProvider

@Composable
fun StudentDetailScreen(
    studentId: Int,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StudentDetailViewModel =
        viewModel(
            key = "student_detail_$studentId",
            factory = AppViewModelProvider.studentDetailFactory(studentId)
        )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Stable event dispatcher: viewModel::onEvent allocates per recomposition and would
    // invalidate every downstream lambda (pager callbacks, dialog handlers) each pass.
    val onEvent =
        remember(viewModel) {
            { event: StudentDetailScreenEvent ->
                viewModel.onEvent(event)
            }
        }

    // VM-initiated one-off navigation effect; needs the ViewModel, so it stays here.
    LaunchedEffect(viewModel.uiEffect) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is StudentDetailUiEffect.NavigateBack -> onNavigateBack()
            }
        }
    }

    StudentDetailScreenContent(
        uiState = uiState,
        onEvent = onEvent,
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}