package dev.nenoeldeeb.education.absencerecord.app.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.nenoeldeeb.education.absencerecord.app.MainScreen
import dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.StudentDetailScreen

private val AppDestinationListSaver =
    listSaver<List<AppDestination>, Any>(
        save = { destinations ->
            destinations.flatMap { destination ->
                when (destination) {
                    AppDestination.MainPager -> listOf("MainPager")
                    is AppDestination.StudentDetail -> listOf("StudentDetail", destination.studentId)
                }
            }
        },
        restore = { saved ->
            buildList {
                var index = 0
                while (index < saved.size) {
                    when (saved[index]) {
                        "StudentDetail" -> {
                            add(AppDestination.StudentDetail(saved[index + 1] as Int))
                            index += 2
                        }
                        else -> {
                            add(AppDestination.MainPager)
                            index += 1
                        }
                    }
                }
            }
        }
    )

@Composable
fun AppNavigation(contentPadding: PaddingValues) {
    var backStack by rememberSaveable(
        stateSaver = AppDestinationListSaver
    ) { mutableStateOf(listOf(AppDestination.MainPager)) }

    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 2 })
    val studentsListState = rememberLazyListState()

    BackHandler(enabled = backStack.size > 1) {
        backStack = backStack.dropLast(1)
    }

    AnimatedContent(
        targetState = backStack.last(),
        transitionSpec = {
            if (targetState is AppDestination.StudentDetail) {
                (slideInHorizontally { it } + fadeIn()) togetherWith
                    (slideOutHorizontally { -it / 3 } + fadeOut())
            } else {
                (slideInHorizontally { -it / 3 } + fadeIn()) togetherWith
                    (slideOutHorizontally { it } + fadeOut())
            }
        },
        label = "app_navigation"
    ) { destination ->
        when (destination) {
            AppDestination.MainPager ->
                MainScreen(
                    contentPadding = contentPadding,
                    pagerState = pagerState,
                    studentsListState = studentsListState,
                    onStudentClick = { id ->
                        backStack = backStack + AppDestination.StudentDetail(id)
                    }
                )
            is AppDestination.StudentDetail ->
                StudentDetailScreen(
                    studentId = destination.studentId,
                    onNavigateBack = { backStack = backStack.dropLast(1) }
                )
        }
    }
}