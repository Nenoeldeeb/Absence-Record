package dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.components

import android.animation.ValueAnimator
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.presentation.screens.components.EmptyStateMessage
import dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.StudentDetailScreenEvent
import dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.StudentDetailScreenState
import kotlinx.coroutines.launch

@Composable
internal fun StudentDetailBody(
    uiState: StudentDetailScreenState,
    onEvent: (StudentDetailScreenEvent) -> Unit,
    modifier: Modifier = Modifier,
    pagerState: PagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isLandscape = maxWidth > maxHeight
        val scope = rememberCoroutineScope()
        // Stable tab-click handler: keeps CoroutineScope out of DetailContent params
        // (CoroutineScope is unstable and would recompose the whole body on every pass).
        val onTabSelected: (Int) -> Unit =
            remember(scope, pagerState) {
                { page: Int ->
                    scope.launch {
                        if (ValueAnimator.areAnimatorsEnabled()) {
                            pagerState.animateScrollToPage(page)
                        } else {
                            pagerState.scrollToPage(page)
                        }
                    }
                    Unit
                }
            }
        when {
            uiState.isLoading -> {
                val loadingDescription = stringResource(R.string.loading)
                CircularProgressIndicator(
                    modifier =
                        Modifier
                            .align(Alignment.Center)
                            .semantics { contentDescription = loadingDescription }
                )
            }
            uiState.student == null -> {
                EmptyStateMessage(
                    message = R.string.no_students_found,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                DetailContent(
                    uiState = uiState,
                    onEvent = onEvent,
                    pagerState = pagerState,
                    onTabSelected = onTabSelected,
                    isLandscape = isLandscape
                )
            }
        }
    }
}

@Composable
private fun DetailContent(
    uiState: StudentDetailScreenState,
    onEvent: (StudentDetailScreenEvent) -> Unit,
    pagerState: PagerState,
    onTabSelected: (Int) -> Unit,
    isLandscape: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.Start
    ) {
        StudentHeader(
            studentName = uiState.student?.name.orEmpty(),
            assignedClassName = uiState.assignedClassName,
            presentCount = uiState.allAttendanceDates.size,
            compact = isLandscape
        )
        DetailTabRow(
            pagerState = pagerState,
            onTabSelected = onTabSelected,
            compact = isLandscape
        )
        DetailPager(
            uiState = uiState,
            onEvent = onEvent,
            pagerState = pagerState,
            landscape = isLandscape,
            modifier = Modifier.fillMaxWidth().weight(1f)
        )
    }
}