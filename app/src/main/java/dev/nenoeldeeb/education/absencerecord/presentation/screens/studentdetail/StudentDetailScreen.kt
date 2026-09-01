@file:OptIn(ExperimentalMaterial3Api::class)

package dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.app.AppViewModelProvider
import dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.components.StudentDetailBody
import dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.components.StudentDetailDialogs
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText

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
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it.asString(context))
            viewModel.onEvent(StudentDetailScreenEvent.ConsumeError)
        }
    }

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let {
            snackbarHostState.showSnackbar(it.asString(context))
            viewModel.onEvent(StudentDetailScreenEvent.ConsumeToastMessage)
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
                            context.getString(R.string.student_detail_share)
                        )
                    )
                }
            viewModel.onEvent(
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

    LaunchedEffect(viewModel.uiEffect) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is StudentDetailUiEffect.NavigateBack -> onNavigateBack()
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            StudentDetailTopBar(
                title = uiState.student?.name ?: stringResource(R.string.student_detail_title_placeholder),
                hasStudent = uiState.student != null,
                onBack = onNavigateBack,
                onEditName = {
                    viewModel.onEvent(StudentDetailScreenEvent.ToggleEditNameDialog)
                },
                onChangeClass = {
                    viewModel.onEvent(StudentDetailScreenEvent.ToggleChangeClassDialog)
                },
                onDelete = {
                    viewModel.onEvent(StudentDetailScreenEvent.ToggleDeleteConfirmationDialog)
                }
            )
        }
    ) { paddingValues ->
        StudentDetailBody(
            uiState = uiState,
            onEvent = viewModel::onEvent,
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        )
    }

    StudentDetailDialogs(
        uiState = uiState,
        onEvent = viewModel::onEvent
    )
}

@Composable
private fun StudentDetailTopBar(
    title: String,
    hasStudent: Boolean,
    onBack: () -> Unit,
    onEditName: () -> Unit,
    onChangeClass: () -> Unit,
    onDelete: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                modifier =
                    if (hasStudent) {
                        Modifier.clickable(onClick = onEditName)
                    } else {
                        Modifier
                    }
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector =
                        ImageVector.vectorResource(
                            R.drawable.outline_chevron_left_24
                        ),
                    contentDescription =
                        stringResource(R.string.student_detail_back_description)
                )
            }
        },
        actions = {
            if (hasStudent) {
                IconButton(onClick = onEditName) {
                    Icon(
                        imageVector =
                            ImageVector.vectorResource(R.drawable.outline_edit_24),
                        contentDescription =
                            stringResource(R.string.student_detail_edit_name_title)
                    )
                }
                IconButton(onClick = onChangeClass) {
                    Icon(
                        imageVector =
                            ImageVector.vectorResource(R.drawable.outline_filter_24),
                        contentDescription =
                            stringResource(
                                R.string.student_detail_change_class_icon_description
                            )
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector =
                            ImageVector.vectorResource(R.drawable.outline_delete_24),
                        contentDescription =
                            stringResource(
                                R.string.student_detail_delete_icon_description
                            )
                    )
                }
            }
        }
    )
}