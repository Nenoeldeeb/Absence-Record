package dev.nenoeldeeb.education.absencerecord.presentation.util

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.presentation.theme.AbsenceRecordTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation tests for [UiText].
 *
 * These tests verify that UiText correctly resolves to actual Android string resources on a real
 * device/emulator, including Composable context usage.
 */
@RunWith(AndroidJUnit4::class)
class UiTextTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    // region DynamicString Tests

    @Test
    fun dynamicString_asString_withContext_returnsValue() {
        val text = UiText.DynamicString("Hello World")
        assertEquals("Hello World", text.asString(context))
    }

    @Test
    fun dynamicString_asString_inComposable_displaysCorrectly() {
        val text = UiText.DynamicString("Composable Test")

        composeTestRule.setContent { AbsenceRecordTheme { Text(text = text.asString()) } }

        composeTestRule.onNodeWithText("Composable Test").assertIsDisplayed()
    }

    @Test
    fun dynamicString_emptyString_returnsEmpty() {
        val text = UiText.DynamicString("")
        assertEquals("", text.asString(context))
    }

    @Test
    fun dynamicString_specialCharacters_returnsCorrectly() {
        val specialText = "Hello 你好 🌍 €100"
        val text = UiText.DynamicString(specialText)
        assertEquals(specialText, text.asString(context))
    }

    // endregion

    // region StringResource Tests

    @Test
    fun stringResource_asString_withContext_resolvesCorrectly() {
        val text = UiText.StringResource(R.string.app_name)
        val expected = context.getString(R.string.app_name)
        assertEquals(expected, text.asString(context))
    }

    @Test
    fun stringResource_withFormatArgs_resolvesCorrectly() {
        // Using error_display which has format: "Error: %1$s"
        val errorMessage = "Test error"
        val text = UiText.StringResource(R.string.error_display, errorMessage)
        val expected = context.getString(R.string.error_display, errorMessage)
        assertEquals(expected, text.asString(context))
    }

    @Test
    fun stringResource_withNestedUiText_resolvesCorrectly() {
        // month_january nested inside date_format_month_year
        val nestedMonth = UiText.StringResource(R.string.month_january)
        val text = UiText.StringResource(R.string.date_format_month_year, nestedMonth, 2026)
        val result = text.asString(context)

        assertTrue("Result should contain 2026", result.contains("2026"))
        assertTrue(
            "Result should contain January",
            result.contains(context.getString(R.string.month_january))
        )
    }

    @Test
    fun stringResource_inComposable_displaysCorrectly() {
        composeTestRule.setContent {
            AbsenceRecordTheme { Text(text = UiText.StringResource(R.string.action_close).asString()) }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.action_close)).assertIsDisplayed()
    }

    // endregion

    // region PluralResource Tests

    @Test
    fun pluralResource_quantityOne_resolvesCorrectly() {
        val text = UiText.PluralResource(R.plurals.days_count, 1, 1)
        val expected = context.resources.getQuantityString(R.plurals.days_count, 1, 1)
        assertEquals(expected, text.asString(context))
    }

    @Test
    fun pluralResource_quantityMany_resolvesCorrectly() {
        val text = UiText.PluralResource(R.plurals.days_count, 5, 5)
        val expected = context.resources.getQuantityString(R.plurals.days_count, 5, 5)
        assertEquals(expected, text.asString(context))
    }

    @Test
    fun pluralResource_quantityZero_resolvesCorrectly() {
        val text = UiText.PluralResource(R.plurals.import_preview_message, 0, 0)
        val expected = context.resources.getQuantityString(R.plurals.import_preview_message, 0, 0)
        assertEquals(expected, text.asString(context))
    }

    @Test
    fun pluralResource_inComposable_displaysCorrectly() {
        val quantity = 3
        val expected = context.resources.getQuantityString(R.plurals.days_count, quantity, quantity)

        composeTestRule.setContent {
            AbsenceRecordTheme {
                Text(
                    text =
                        UiText.PluralResource(R.plurals.days_count, quantity, quantity)
                            .asString()
                )
            }
        }

        composeTestRule.onNodeWithText(expected).assertIsDisplayed()
    }

    // endregion

    // region Joined Tests

    @Test
    fun joined_multipleStrings_joinedCorrectly() {
        val text =
            UiText.Joined(
                parts =
                    listOf(
                        UiText.DynamicString("Hello"),
                        UiText.DynamicString("World")
                    ),
                separator = " "
            )
        assertEquals("Hello World", text.asString(context))
    }

    @Test
    fun joined_withCommaSeparator_joinedCorrectly() {
        val text =
            UiText.Joined(
                parts =
                    listOf(
                        UiText.DynamicString("Alice"),
                        UiText.DynamicString("Bob"),
                        UiText.DynamicString("Charlie")
                    ),
                separator = ", "
            )
        assertEquals("Alice, Bob, Charlie", text.asString(context))
    }

    @Test
    fun joined_mixedTypes_joinedCorrectly() {
        val text =
            UiText.Joined(
                parts =
                    listOf(
                        UiText.DynamicString("Action:"),
                        UiText.StringResource(R.string.action_save)
                    ),
                separator = " "
            )
        val expected = "Action: ${context.getString(R.string.action_save)}"
        assertEquals(expected, text.asString(context))
    }

    @Test
    fun joined_emptySeparator_concatenatesDirectly() {
        val text =
            UiText.Joined(
                parts =
                    listOf(
                        UiText.DynamicString("Hello"),
                        UiText.DynamicString("World")
                    ),
                separator = ""
            )
        assertEquals("HelloWorld", text.asString(context))
    }

    @Test
    fun joined_singlePart_returnsJustThatPart() {
        val text = UiText.Joined(parts = listOf(UiText.DynamicString("Only One")), separator = ", ")
        assertEquals("Only One", text.asString(context))
    }

    @Test
    fun joined_emptyList_returnsEmptyString() {
        val text = UiText.Joined(parts = emptyList(), separator = ", ")
        assertEquals("", text.asString(context))
    }

    @Test
    fun joined_inComposable_displaysCorrectly() {
        composeTestRule.setContent {
            AbsenceRecordTheme {
                Text(
                    text =
                        UiText.Joined(
                            parts =
                                listOf(
                                    UiText.DynamicString("Test"),
                                    UiText.DynamicString("Joined")
                                ),
                            separator = "-"
                        )
                            .asString()
                )
            }
        }

        composeTestRule.onNodeWithText("Test-Joined").assertIsDisplayed()
    }

    // endregion

    // region Equality Tests

    @Test
    fun stringResource_equality_sameResIdAndArgs_areEqual() {
        val res1 = UiText.StringResource(R.string.error_display, "arg")
        val res2 = UiText.StringResource(R.string.error_display, "arg")
        assertEquals(res1, res2)
    }

    @Test
    fun stringResource_equality_differentResId_notEqual() {
        val res1 = UiText.StringResource(R.string.action_save)
        val res2 = UiText.StringResource(R.string.action_close)
        assertNotEquals(res1, res2)
    }

    @Test
    fun stringResource_equality_differentArgs_notEqual() {
        val res1 = UiText.StringResource(R.string.error_display, "error1")
        val res2 = UiText.StringResource(R.string.error_display, "error2")
        assertNotEquals(res1, res2)
    }

    @Test
    fun pluralResource_equality_sameParts_areEqual() {
        val res1 = UiText.PluralResource(R.plurals.days_count, 5, 5)
        val res2 = UiText.PluralResource(R.plurals.days_count, 5, 5)
        assertEquals(res1, res2)
    }

    @Test
    fun pluralResource_equality_differentQuantity_notEqual() {
        val res1 = UiText.PluralResource(R.plurals.days_count, 1, 1)
        val res2 = UiText.PluralResource(R.plurals.days_count, 5, 5)
        assertNotEquals(res1, res2)
    }

    // endregion
}