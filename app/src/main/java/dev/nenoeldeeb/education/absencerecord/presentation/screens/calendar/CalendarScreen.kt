package dev.nenoeldeeb.education.absencerecord.presentation.screens.calendar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.nenoeldeeb.education.absencerecord.app.AppViewModelProvider

/**
 * State-owning entry point. Deliberately thin: everything testable lives in
 * [CalendarScreenContent], which takes a plain state object and an event sink.
 *
 * Instrumented tests drive that directly — a final ViewModel cannot be mocked
 * on device, so tests must not depend on one.
 */
@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = viewModel(factory = AppViewModelProvider.factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CalendarScreenContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        modifier = modifier
    )
}