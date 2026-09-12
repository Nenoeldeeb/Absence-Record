package dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.BusyAppointment
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentLessonEntry
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentScheduleView
import dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.components.StudentScheduleSection
import dev.nenoeldeeb.education.absencerecord.presentation.theme.AbsenceRecordTheme
import dev.nenoeldeeb.education.absencerecord.presentation.utils.TimeFormatter
import kotlinx.datetime.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StudentScheduleSectionTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun time(minutes: Int): String = TimeFormatter.format(minutes, context)

    private fun aState(
        schedule: StudentScheduleView? =
            StudentScheduleView(
                studentId = 1,
                lessons = listOf(StudentLessonEntry(DayOfWeek.SATURDAY, 540, 600)),
                busy =
                    listOf(
                        BusyAppointment(
                            id = 1,
                            studentId = 1,
                            weekday = DayOfWeek.SATURDAY,
                            startMinutes = 510,
                            durationMinutes = 60
                        )
                    )
            ),
        tab: StudentScheduleTab = StudentScheduleTab.Lessons
    ): StudentDetailScreenState =
        StudentDetailScreenState().copy(
            selectedScheduleWeekday = DayOfWeek.SATURDAY,
            selectedScheduleTab = tab,
            studentSchedule = schedule
        )

    private fun setSection(
        state: StudentDetailScreenState,
        onEvent: (StudentDetailScreenEvent) -> Unit = {}
    ) {
        composeTestRule.setContent {
            AbsenceRecordTheme {
                StudentScheduleSection(
                    uiState = state,
                    onEvent = onEvent,
                    modifier = androidx.compose.ui.Modifier
                )
            }
        }
    }

    @Test
    fun section_rendersTitleWeekdaySelectorTabsAndLessons() {
        setSection(aState())

        composeTestRule.onNodeWithText(context.getString(R.string.student_schedule_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.day_short_saturday)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.student_detail_tab_lessons)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.student_detail_tab_busy)).assertIsDisplayed()
        composeTestRule.onNodeWithText("${time(540)} – ${time(600)}").assertIsDisplayed()
    }

    @Test
    fun section_selectingWeekdayDispatchesSelectScheduleWeekday() {
        val recorded = mutableListOf<StudentDetailScreenEvent>()
        setSection(aState()) { recorded.add(it) }

        composeTestRule.onNodeWithText(context.getString(R.string.day_short_sunday)).performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, recorded.size)
        assertEquals(
            StudentDetailScreenEvent.SelectScheduleWeekday(DayOfWeek.SUNDAY),
            recorded[0]
        )
    }

    @Test
    fun section_selectingBusyTabDispatchesSelectScheduleTab() {
        val recorded = mutableListOf<StudentDetailScreenEvent>()
        setSection(aState()) { recorded.add(it) }

        composeTestRule.onNodeWithText(context.getString(R.string.student_detail_tab_busy)).performClick()
        composeTestRule.waitForIdle()

        assertEquals(
            listOf(StudentDetailScreenEvent.SelectScheduleTab(StudentScheduleTab.Busy)),
            recorded
        )
    }

    @Test
    fun section_busyTab_showsBusyAppointments() {
        setSection(aState(tab = StudentScheduleTab.Busy))

        composeTestRule.onNodeWithText("${time(510)} – ${time(570)}").assertIsDisplayed()
    }

    @Test
    fun section_unassignIconDispatchesUnassignLesson() {
        val recorded = mutableListOf<StudentDetailScreenEvent>()
        setSection(aState()) { recorded.add(it) }

        val description =
            context.getString(
                R.string.student_detail_lesson_unassign_description,
                "${time(540)} – ${time(600)}"
            )
        composeTestRule.onNodeWithContentDescription(description).performClick()
        composeTestRule.waitForIdle()

        assertEquals(
            listOf(
                StudentDetailScreenEvent.UnassignLesson(StudentLessonEntry(DayOfWeek.SATURDAY, 540, 600))
            ),
            recorded
        )
    }

    @Test
    fun section_lessonsEmptyShowsAddButtonAndDispatchesOpenAddLessonDialog() {
        val recorded = mutableListOf<StudentDetailScreenEvent>()
        setSection(aState(schedule = StudentScheduleView(1, emptyList(), emptyList()))) { recorded.add(it) }

        composeTestRule.onNodeWithText(context.getString(R.string.student_detail_lessons_empty)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.student_detail_add_lesson)).performClick()
        composeTestRule.waitForIdle()

        assertEquals(
            listOf(StudentDetailScreenEvent.OpenAddLessonDialog),
            recorded
        )
    }

    @Test
    fun section_busyEditAndDeleteDispatchEvents() {
        val recorded = mutableListOf<StudentDetailScreenEvent>()
        setSection(aState(tab = StudentScheduleTab.Busy)) { recorded.add(it) }

        val range = "${time(510)} – ${time(570)}"
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.schedule_edit_busy_description, range))
            .performClick()
        composeTestRule.waitForIdle()

        val expectedBusy =
            BusyAppointment(
                id = 1,
                studentId = 1,
                weekday = DayOfWeek.SATURDAY,
                startMinutes = 510,
                durationMinutes = 60
            )
        assertEquals(1, recorded.size)
        assertEquals(StudentDetailScreenEvent.OpenBusyDialog(expectedBusy), recorded[0])

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.schedule_delete_busy_description, range))
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals(2, recorded.size)
        assertEquals(StudentDetailScreenEvent.DeleteBusyAppointment(1), recorded[1])
    }

    @Test
    fun section_busyEmptyShowsAddButtonAndDispatchesOpenBusyDialog() {
        val recorded = mutableListOf<StudentDetailScreenEvent>()
        setSection(aState(schedule = StudentScheduleView(1, emptyList(), emptyList()), tab = StudentScheduleTab.Busy)) {
            recorded.add(it)
        }

        composeTestRule.onNodeWithText(context.getString(R.string.student_detail_no_busy_for_day)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.schedule_add_busy_appointment)).performClick()
        composeTestRule.waitForIdle()

        assertEquals(
            listOf(StudentDetailScreenEvent.OpenBusyDialog(null)),
            recorded
        )
    }

    @Test
    fun section_busyWithAppointmentsStillShowsAddButtonAndDispatchesOpenBusyDialog() {
        val recorded = mutableListOf<StudentDetailScreenEvent>()
        setSection(aState(tab = StudentScheduleTab.Busy)) { recorded.add(it) }

        composeTestRule.onNodeWithText("${time(510)} – ${time(570)}").assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.schedule_add_busy_appointment)).performClick()
        composeTestRule.waitForIdle()

        assertEquals(
            listOf(StudentDetailScreenEvent.OpenBusyDialog(null)),
            recorded
        )
    }
}