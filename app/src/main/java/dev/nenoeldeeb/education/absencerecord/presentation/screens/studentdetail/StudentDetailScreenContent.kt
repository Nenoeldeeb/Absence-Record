@file:OptIn(ExperimentalMaterial3Api::class)

package dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail

import android.content.Intent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.components.StudentDetailBody
import dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.components.StudentDetailTopBar
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText

/**
 * Stateless content: renders the given state and dispatches events. All
 * state-driven snackbars and the share intent live here; VM-initiated one-off
 * navigation ([StudentDetailUiEffect]) stays in the Screen, which owns the
 * ViewModel.
 */
@Composable
internal fun StudentDetailScreenContent(
    uiState: StudentDetailScreenState,
    onEvent: (StudentDetailScreenEvent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val undoLabel = stringResource(R.string.action_undo)
    val unassignedLessonMessage = stringResource(R.string.student_detail_lesson_unassigned)
    val busyDeletedMessage = stringResource(R.string.student_detail_busy_deleted)
    val deletedStudentMessage = stringResource(R.string.student_detail_student_deleted)
    val shareTitle = stringResource(R.string.student_detail_share)
    val studentName = uiState.student?.name
    val studentFirstName =
        remember(studentName) {
            studentName?.trim()?.substringBefore(" ")?.takeIf { it.isNotBlank() }
        }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it.asString(context))
            onEvent(StudentDetailScreenEvent.ConsumeError)
        }
    }

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let {
            snackbarHostState.showSnackbar(it.asString(context))
            onEvent(StudentDetailScreenEvent.ConsumeToastMessage)
        }
    }

    LaunchedEffect(uiState.deletedLesson) {
        uiState.deletedLesson?.let {
            val result =
                snackbarHostState.showSnackbar(
                    message = unassignedLessonMessage,
                    actionLabel = undoLabel,
                    duration = SnackbarDuration.Long
                )
            if (result == SnackbarResult.ActionPerformed) {
                onEvent(StudentDetailScreenEvent.UndoUnassignLesson)
            } else {
                onEvent(StudentDetailScreenEvent.ConsumeDeletedLesson)
            }
        }
    }

    LaunchedEffect(uiState.deletedBusy) {
        uiState.deletedBusy?.let {
            val result =
                snackbarHostState.showSnackbar(
                    message = busyDeletedMessage,
                    actionLabel = undoLabel,
                    duration = SnackbarDuration.Long
                )
            if (result == SnackbarResult.ActionPerformed) {
                onEvent(StudentDetailScreenEvent.UndoDeleteBusy)
            } else {
                onEvent(StudentDetailScreenEvent.ConsumeDeletedBusy)
            }
        }
    }

    LaunchedEffect(uiState.deletedStudentBackup) {
        uiState.deletedStudentBackup?.let {
            val result =
                snackbarHostState.showSnackbar(
                    message = deletedStudentMessage,
                    actionLabel = undoLabel,
                    duration = SnackbarDuration.Long
                )
            if (result == SnackbarResult.ActionPerformed) {
                onEvent(StudentDetailScreenEvent.UndoDeleteStudent)
            } else {
                onEvent(StudentDetailScreenEvent.DismissDeletedStudent)
            }
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
                            shareTitle
                        )
                    )
                }
            onEvent(
                StudentDetailScreenEvent.ShareFileResult(
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

    val onEditName = remember(onEvent) { { onEvent(StudentDetailScreenEvent.ToggleEditNameDialog) } }
    val onChangeClass =
        remember(onEvent) { { onEvent(StudentDetailScreenEvent.ToggleChangeClassDialog) } }
    val onDeleteStudent =
        remember(onEvent) { { onEvent(StudentDetailScreenEvent.ToggleDeleteConfirmationDialog) } }
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            StudentDetailTopBar(
                studentFirstName = studentFirstName,
                hasStudent = uiState.student != null,
                onBack = onNavigateBack,
                onEditName = onEditName,
                onChangeClass = onChangeClass,
                onDelete = onDeleteStudent
            )
        }
    ) { paddingValues ->
        StudentDetailBody(
            uiState = uiState,
            onEvent = onEvent,
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        )
    }

    StudentDetailDialogs(
        uiState = uiState,
        onEvent = onEvent
    )
}