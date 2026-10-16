package dev.nenoeldeeb.education.absencerecord.presentation.screens.calendar

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.nenoeldeeb.education.absencerecord.presentation.theme.AbsenceRecordTheme
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Screen-level behaviour of [CalendarScreenContent].
 *
 * These drive the stateless content composable rather than a ViewModel. The
 * previous version mocked `CalendarViewModel`, which works only on newer ART and
 * so failed on the API 26 emulator while passing on API 35.
 */
@RunWith(AndroidJUnit4::class)
class CalendarScreenContentTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private companion object {
        /** Comfortably past Material's longest snackbar duration (Short = 4s). */
        const val SNACKBAR_SETTLE_MILLIS = 10_000L
    }

    // region CalendarScreen error / snackbar tests

    @Test
    fun calendarScreen_displaysErrorInSnackbar() {
        val errorMessage = "Test error occurred"
        val recordedEvents = mutableListOf<CalendarScreenEvent>()
        val stateFlow = MutableStateFlow(CalendarScreenState(error = UiText.DynamicString(errorMessage)))

        composeTestRule.setContent {
            AbsenceRecordTheme {
                val state = stateFlow.collectAsStateWithLifecycle().value
                CalendarScreenContent(
                    uiState = state,
                    onEvent = { recordedEvents += it }
                )
            }
        }

        composeTestRule.onNodeWithText(errorMessage).assertIsDisplayed()
        // showSnackbar suspends until it dismisses; the error is consumed only
        // then, so the clock has to move past the snackbar's duration first.
        composeTestRule.mainClock.advanceTimeBy(SNACKBAR_SETTLE_MILLIS)
        composeTestRule.waitForIdle()
        assert(recordedEvents.any { it is CalendarScreenEvent.ConsumeError }) {
            "Expected the error to be consumed once the snackbar dismissed"
        }
    }

    @Test
    fun calendarScreen_consumesAttendanceMessageAfterDismissing() {
        val recordedEvents = mutableListOf<CalendarScreenEvent>()
        val stateFlow =
            MutableStateFlow(
                CalendarScreenState(attendanceMessage = UiText.DynamicString("Marked present"))
            )

        composeTestRule.setContent {
            AbsenceRecordTheme {
                val state = stateFlow.collectAsStateWithLifecycle().value
                CalendarScreenContent(uiState = state, onEvent = { recordedEvents += it })
            }
        }

        composeTestRule.onNodeWithText("Marked present").assertIsDisplayed()
        composeTestRule.mainClock.advanceTimeBy(SNACKBAR_SETTLE_MILLIS)
        composeTestRule.waitForIdle()
        assert(recordedEvents.any { it is CalendarScreenEvent.ConsumeAttendanceMessage }) {
            "Expected the attendance message to be consumed"
        }
    }
}