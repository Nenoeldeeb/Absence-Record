package dev.nenoeldeeb.education.absencerecord.presentation.screens.students

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule

/**
 * Shared plumbing for the students screen's instrumented tests.
 *
 * Extracted when `StudentsScreenTest` crossed the constitution's 300-line cap.
 * The tests themselves are unchanged — this only removes the duplicated rule,
 * instrumentation context and string lookup that every class would otherwise
 * repeat.
 */
abstract class StudentsScreenTestBase {
    @get:Rule
    val composeTestRule = createComposeRule()

    protected val context = InstrumentationRegistry.getInstrumentation().targetContext

    protected fun getString(
        resId: Int,
        vararg formatArgs: Any
    ): String {
        return context.getString(resId, *formatArgs)
    }
}