package dev.nenoeldeeb.education.absencerecord.presentation.screens.students

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.presentation.screens.components.EmptyStateMessage
import dev.nenoeldeeb.education.absencerecord.presentation.theme.AbsenceRecordTheme
import org.junit.Test
import org.junit.runner.RunWith

/** The "no students yet" placeholder shown on an empty list (SC-018). */
@RunWith(AndroidJUnit4::class)
class StudentsEmptyStateTest : StudentsScreenTestBase() {
    @Test
    fun emptyStateMessage_isDisplayed() {
        composeTestRule.setContent { AbsenceRecordTheme { EmptyStateMessage(R.string.no_students_message) } }

        composeTestRule.onNodeWithText(getString(R.string.no_students_message)).assertIsDisplayed()
    }
}