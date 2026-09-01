package dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.AvailableLessonHour
import dev.nenoeldeeb.education.absencerecord.domain.models.BusyAppointment
import dev.nenoeldeeb.education.absencerecord.domain.models.HourWithOccupancy
import dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.dialogs.AddLessonDialog
import dev.nenoeldeeb.education.absencerecord.presentation.theme.AbsenceRecordTheme
import dev.nenoeldeeb.education.absencerecord.presentation.utils.TimeFormatter
import kotlinx.datetime.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AddLessonDialogTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun time(minutes: Int): String = TimeFormatter.format(minutes, context)

    private fun hourWith(
        id: Int,
        start: Int,
        assignedCount: Int = 0,
        max: Int = 5,
        busyAppointments: List<BusyAppointment> = emptyList()
    ): HourWithOccupancy =
        HourWithOccupancy(
            hour = AvailableLessonHour(id, DayOfWeek.SATURDAY, start, max),
            endMinutes = start + 60,
            assignedStudentIds = List(assignedCount) { it + 1 },
            assignedCount = assignedCount,
            maxStudents = max,
            remainingSlots = max - assignedCount,
            isFull = assignedCount >= max
        )

    private fun setDialog(
        hours: List<HourWithOccupancy>,
        busyAppointments: List<BusyAppointment> = emptyList(),
        selectedHourId: Int? = null,
        onHourSelected: (Int) -> Unit = {},
        onConfirm: () -> Unit = {},
        onDismiss: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            AbsenceRecordTheme {
                AddLessonDialog(
                    hours = hours,
                    busyAppointments = busyAppointments,
                    selectedHourId = selectedHourId,
                    onHourSelected = onHourSelected,
                    onConfirm = onConfirm,
                    onDismiss = onDismiss
                )
            }
        }
    }

    @Test
    fun addLessonDialog_emptyHours_showsHintAndDisablesSave() {
        setDialog(hours = emptyList())

        composeTestRule
            .onNodeWithText(context.getString(R.string.student_detail_no_hours_for_day))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.action_save))
            .assertIsNotEnabled()
    }

    @Test
    fun addLessonDialog_selectingEligibleHourInvokesCallback() {
        val selected = mutableListOf<Int>()
        setDialog(hours = listOf(hourWith(id = 3, start = 540)), onHourSelected = { selected.add(it) })

        composeTestRule.onNodeWithText("${time(540)} – ${time(600)}").performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf(3), selected)
    }

    @Test
    fun addLessonDialog_fullHour_showsReasonAndCannotBeSelected() {
        val selected = mutableListOf<Int>()
        setDialog(
            hours = listOf(hourWith(id = 3, start = 540, assignedCount = 5)),
            onHourSelected = { selected.add(it) }
        )

        composeTestRule
            .onNodeWithText(context.getString(R.string.assign_reason_hour_full))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("${time(540)} – ${time(600)}").performClick()
        composeTestRule.waitForIdle()

        assertEquals(emptyList<Int>(), selected)
    }

    @Test
    fun addLessonDialog_busyOverlappingHour_showsReasonAndCannotBeSelected() {
        val selected = mutableListOf<Int>()
        val busy =
            BusyAppointment(
                id = 1,
                studentId = 1,
                weekday = DayOfWeek.SATURDAY,
                startMinutes = 510,
                durationMinutes = 90
            )
        setDialog(
            hours = listOf(hourWith(id = 3, start = 540)),
            busyAppointments = listOf(busy),
            onHourSelected = { selected.add(it) }
        )

        composeTestRule
            .onNodeWithText(context.getString(R.string.assign_reason_busy_conflict))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("${time(540)} – ${time(600)}").performClick()
        composeTestRule.waitForIdle()

        assertEquals(emptyList<Int>(), selected)
    }

    @Test
    fun addLessonDialog_saveDisabledWithoutSelectionAndEnabledAfterSelection() {
        val selectedHourId = mutableStateOf<Int?>(null)
        composeTestRule.setContent {
            AbsenceRecordTheme {
                AddLessonDialog(
                    hours = listOf(hourWith(id = 3, start = 540)),
                    busyAppointments = emptyList(),
                    selectedHourId = selectedHourId.value,
                    onHourSelected = { selectedHourId.value = it },
                    onConfirm = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.action_save)).assertIsNotEnabled()
        composeTestRule.onNodeWithText("${time(540)} – ${time(600)}").performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.action_save)).assertIsEnabled()
    }

    @Test
    fun addLessonDialog_confirmInvokesCallback() {
        var confirmed = false
        setDialog(hours = listOf(hourWith(id = 3, start = 540)), selectedHourId = 3, onConfirm = { confirmed = true })

        composeTestRule.onNodeWithText(context.getString(R.string.action_save)).performClick()
        composeTestRule.waitForIdle()

        assertEquals(true, confirmed)
    }

    @Test
    fun addLessonDialog_cancelInvokesDismiss() {
        var dismissed = false
        setDialog(hours = listOf(hourWith(id = 3, start = 540)), onDismiss = { dismissed = true })

        composeTestRule.onNodeWithText(context.getString(R.string.action_cancel)).performClick()
        composeTestRule.waitForIdle()

        assertEquals(true, dismissed)
    }
}