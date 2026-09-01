package dev.nenoeldeeb.education.absencerecord.app

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.app.navigation.AppNavigation
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.AppDatabase
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.entities.AvailableLessonHourEntity
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.entities.LessonAssignmentEntity
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.entities.StudentEntity
import dev.nenoeldeeb.education.absencerecord.presentation.utils.TimeFormatter
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.isoDayNumber
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end navigation test for the Schedule page tap-to-profile flow.
 *
 * Seeds the app's real Room database with a student + Saturday hour +
 * assignment, then verifies that tapping the assigned student's name on the
 * Schedule page opens the Student Detail screen and that pressing back
 * restores the Schedule page with the hour still expanded (the
 * activity-scoped [ScheduleViewModel] survives the round trip).
 */
@RunWith(AndroidJUnit4::class)
class ScheduleNavigationTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val database = AppDatabase.getDatabase(context)

    private val studentName = "Nav Test Student"

    private fun time(minutes: Int): String = TimeFormatter.format(minutes, context)

    private fun seedData(): Pair<Long, Long> =
        runBlocking {
            val studentId = database.studentDao().insertStudent(StudentEntity(name = studentName))
            val hourId =
                database.scheduleDao().insertAvailableHour(
                    AvailableLessonHourEntity(
                        weekday = DayOfWeek.SATURDAY.isoDayNumber,
                        startMinutes = 540,
                        maxStudents = 5
                    )
                )
            database.scheduleDao().insertAssignmentEntity(
                LessonAssignmentEntity(
                    availableHourId = hourId.toInt(),
                    studentId = studentId.toInt(),
                    weekday = DayOfWeek.SATURDAY.isoDayNumber
                )
            )
            studentId to hourId
        }

    private fun cleanup(
        studentId: Long,
        hourId: Long
    ) {
        runBlocking {
            database.scheduleDao().deleteAssignment(hourId.toInt(), studentId.toInt())
            database.scheduleDao().deleteHourById(hourId.toInt())
            database.studentDao().deleteStudents(listOf(StudentEntity(id = studentId.toInt(), name = studentName)))
        }
    }

    private fun waitForSchedulePage() {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Saturday").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun tappingStudentName_navigatesToDetail_andBackRestoresExpandedHour() {
        val (studentId, hourId) = seedData()
        try {
            composeTestRule.setContent {
                AppNavigation(contentPadding = PaddingValues(0.dp))
            }
            composeTestRule.waitForIdle()

            composeTestRule.onRoot().performTouchInput { swipeLeft() }
            waitForSchedulePage()

            composeTestRule
                .onNodeWithContentDescription(
                    context.getString(R.string.schedule_expand_hour_description, time(540))
                )
                .performClick()
            composeTestRule.onNodeWithText(studentName).assertIsDisplayed().performClick()
            composeTestRule.waitForIdle()

            composeTestRule
                .onNodeWithText(context.getString(R.string.student_detail_tab_lessons))
                .assertIsDisplayed()

            Espresso.pressBack()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText("Saturday").assertIsDisplayed()
            composeTestRule.onAllNodesWithText(studentName).onFirst().assertIsDisplayed()
        } finally {
            cleanup(studentId, hourId)
        }
    }
}