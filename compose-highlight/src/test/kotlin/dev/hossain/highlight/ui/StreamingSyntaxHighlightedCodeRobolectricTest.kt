package dev.hossain.highlight.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import dev.hossain.highlight.engine.HighlightTheme
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@OptIn(ExperimentalHighlightApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30])
class StreamingSyntaxHighlightedCodeRobolectricTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `renders code text in preview mode`() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                HighlightThemeProvider {
                    StreamingSyntaxHighlightedCode(
                        code = "val x = 42",
                        language = "kotlin",
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("val x = 42")
            .assertIsDisplayed()
    }

    @Test
    fun `has test tag on outer surface`() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                HighlightThemeProvider {
                    StreamingSyntaxHighlightedCode(
                        code = "print('streaming')",
                        language = "python",
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithTag("streaming-syntax-highlighted-code")
            .assertIsDisplayed()
    }

    @Test
    fun `hides language label when content is null`() {
        composeTestRule.setContent {
            HighlightThemeProvider {
                StreamingSyntaxHighlightedCode(
                    code = "val x = 42",
                    language = "kotlin",
                    languageLabel = null,
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("kotlin")
            .assertDoesNotExist()
    }

    @Test
    fun `hides copy button when content is null`() {
        composeTestRule.setContent {
            HighlightThemeProvider {
                StreamingSyntaxHighlightedCode(
                    code = "val x = 42",
                    language = "kotlin",
                    copyButton = null,
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithContentDescription("Copy code", useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun `copy button triggers onCopyClick callback`() {
        var copiedCode: String? = null
        composeTestRule.setContent {
            HighlightThemeProvider {
                StreamingSyntaxHighlightedCode(
                    code = "val x = 42",
                    language = "kotlin",
                    onCopyClick = { copiedCode = it },
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithContentDescription("Copy code", useUnmergedTree = true)
            .performClick()
        composeTestRule.waitForIdle()
        assertThat(copiedCode).isEqualTo("val x = 42")
    }

    @Test
    fun `shows language label by default`() {
        composeTestRule.setContent {
            HighlightThemeProvider {
                StreamingSyntaxHighlightedCode(
                    code = "SELECT 1",
                    language = "sql",
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("sql")
            .assertIsDisplayed()
    }

    @Test
    fun `renders with custom triggerOnNewline and minThrottleMs`() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                HighlightThemeProvider {
                    StreamingSyntaxHighlightedCode(
                        code = "fun stream() = true",
                        language = "kotlin",
                        triggerOnNewline = false,
                        minThrottleMs = 250L,
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("fun stream() = true")
            .assertIsDisplayed()
    }

    @Test
    fun `horizontal scroll position is preserved across state restoration`() {
        val restorationTester = StateRestorationTester(composeTestRule)
        var scrollState: ScrollState? = null
        val theme = HighlightTheme.fromCss("", "test-empty-theme")

        restorationTester.setContent {
            val state = rememberScrollState()
            scrollState = state
            HighlightThemeProvider {
                StreamingSyntaxHighlightedCode(
                    code = "val longLine = " + "x".repeat(500),
                    language = "kotlin",
                    theme = theme,
                    scrollState = state,
                    modifier = Modifier.width(100.dp),
                )
            }
        }
        composeTestRule.waitForIdle()
        assertThat(scrollState?.maxValue).isGreaterThan(0)

        runBlocking {
            scrollState?.scrollTo(42)
        }
        composeTestRule.waitForIdle()
        assertThat(scrollState?.value).isEqualTo(42)

        restorationTester.emulateSavedInstanceStateRestore()
        composeTestRule.waitForIdle()

        assertThat(scrollState?.value).isEqualTo(42)
    }

    @Test
    fun `horizontal scroll position is preserved during streaming appends and resets on new stream`() {
        var code by mutableStateOf("val longLine = " + "x".repeat(500))
        var scrollState: ScrollState? = null
        val theme = HighlightTheme.fromCss("", "test-empty-theme")

        composeTestRule.setContent {
            val state = rememberScrollState()
            scrollState = state
            HighlightThemeProvider {
                StreamingSyntaxHighlightedCode(
                    code = code,
                    language = "kotlin",
                    theme = theme,
                    scrollState = state,
                    modifier = Modifier.width(100.dp),
                )
            }
        }
        composeTestRule.waitForIdle()
        assertThat(scrollState?.maxValue).isGreaterThan(0)

        runBlocking {
            scrollState?.scrollTo(42)
        }
        composeTestRule.waitForIdle()
        assertThat(scrollState?.value).isEqualTo(42)

        // Append to existing code (simulating streaming tokens)
        code += " // appended"
        composeTestRule.waitForIdle()

        assertThat(scrollState?.value).isEqualTo(42)

        // Replace with brand new code stream
        code = "class NewStream { " + "y".repeat(500) + " }"
        composeTestRule.waitForIdle()

        assertThat(scrollState?.value).isEqualTo(0)
    }
}
