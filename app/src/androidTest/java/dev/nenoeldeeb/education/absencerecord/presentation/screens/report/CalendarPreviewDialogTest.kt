package dev.nenoeldeeb.education.absencerecord.presentation.screens.report

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.presentation.screens.report.dialogs.CalendarPreviewDialog
import dev.nenoeldeeb.education.absencerecord.presentation.theme.AbsenceRecordTheme
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CalendarPreviewDialogTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun getString(
        resId: Int,
        vararg formatArgs: Any
    ): String {
        return context.getString(resId, *formatArgs)
    }

    // region CalendarPreviewDialog Tests

    @Test
    fun calendarPreviewDialog_displaysShareButton() {
        val student = Student(id = 1, name = "Alice")
        val month = LocalDate(2026, 1, 1)

        composeTestRule.setContent {
            AbsenceRecordTheme {
                CalendarPreviewDialog(
                    student = student,
                    monthToPreview = month,
                    datesForPreviewMonth = emptyList(),
                    onDismiss = {},
                    onShare = { _, _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithText(getString(R.string.action_share_image)).assertIsDisplayed()
    }

    @Test
    fun calendarPreviewDialog_clickingShareInvokesCallback() {
        var sharedMonth: LocalDate? = null
        var sharedId: Int? = null
        var sharedName: String? = null

        val student = Student(id = 1, name = "Alice")
        val month = LocalDate(2026, 1, 1)

        composeTestRule.setContent {
            AbsenceRecordTheme {
                CalendarPreviewDialog(
                    student = student,
                    monthToPreview = month,
                    datesForPreviewMonth = emptyList(),
                    onDismiss = {},
                    onShare = { m, id, name ->
                        sharedMonth = m
                        sharedId = id
                        sharedName = name
                    }
                )
            }
        }

        composeTestRule.onNodeWithText(getString(R.string.action_share_image)).performClick()

        assert(sharedMonth == month)
        assert(sharedId == student.id)
        assert(sharedName == student.name)
    }

    // endregion
}
