package dev.hossain.highlight.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import dev.hossain.highlight.engine.HighlightTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SyntaxHighlightedCodeTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val sampleCode = "def hello():\n    print('world')"

    @Test
    fun composableRendersWithoutCrash() {
        composeTestRule.setContent {
            HighlightThemeProvider(
                lightHighlightTheme = HighlightTheme.tomorrow(context),
                darkHighlightTheme = HighlightTheme.tomorrowNight(context),
            ) {
                SyntaxHighlightedCode(
                    code = sampleCode,
                    language = "python",
                )
            }
        }
        // Plain fallback text should be visible immediately
        composeTestRule.onNodeWithText("python", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun languageLabelIsDisplayed() {
        composeTestRule.setContent {
            HighlightThemeProvider {
                SyntaxHighlightedCode(
                    code = "val x = 1",
                    language = "kotlin",
                    showLanguageLabel = true,
                )
            }
        }
        composeTestRule.onNodeWithText("kotlin", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun languageLabelCanBeHidden() {
        composeTestRule.setContent {
            HighlightThemeProvider {
                SyntaxHighlightedCode(
                    code = "val x = 1",
                    language = "kotlin",
                    showLanguageLabel = false,
                )
            }
        }
        composeTestRule.onNodeWithText("kotlin", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun copyButtonIsClickable() {
        var copyCalled = false
        composeTestRule.setContent {
            HighlightThemeProvider {
                SyntaxHighlightedCode(
                    code = sampleCode,
                    language = "python",
                    showCopyButton = true,
                    onCopyClick = { copyCalled = true },
                )
            }
        }
        // Copy button shows the ⧉ icon
        composeTestRule.onNodeWithText("⧉", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()
        assert(copyCalled) { "Expected onCopyClick to be called" }
    }

    @Test
    fun emptyCodeRendersWithoutCrash() {
        composeTestRule.setContent {
            HighlightThemeProvider {
                SyntaxHighlightedCode(code = "", language = "python")
            }
        }
    }

    @Test
    fun lineNumbersDisplayWhenEnabled() {
        composeTestRule.setContent {
            HighlightThemeProvider {
                SyntaxHighlightedCode(
                    code = "line1\nline2\nline3",
                    language = "plaintext",
                    showLineNumbers = true,
                )
            }
        }
        composeTestRule.waitForIdle()
        // Line numbers are rendered as a single Text composable "1\n2\n3" (gutter joined by \n)
        composeTestRule.onNodeWithText("1\n2\n3", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun onHighlightCompleteCallbackFiresWithCorrectResult() {
        var capturedResult: dev.hossain.highlight.engine.HighlightResult? = null

        composeTestRule.setContent {
            HighlightThemeProvider(
                lightHighlightTheme = HighlightTheme.tomorrow(context),
                darkHighlightTheme = HighlightTheme.tomorrowNight(context),
            ) {
                SyntaxHighlightedCode(
                    code = sampleCode,
                    language = "python",
                    onHighlightComplete = { result -> capturedResult = result },
                )
            }
        }

        // Wait until the callback fires (highlighting happens asynchronously)
        composeTestRule.waitUntil(timeoutMillis = 10_000L) { capturedResult != null }

        val result = capturedResult!!
        assertThat(result.language).isEqualTo("python")
        assertThat(result.spanCount).isGreaterThan(0)
        assertThat(result.durationMs).isAtLeast(0L)
        assertThat(result.annotated.text).isNotEmpty()
    }
}
