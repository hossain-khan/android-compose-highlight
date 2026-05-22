package dev.hossain.highlight.ui

import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
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
                    languageLabelContent = null,
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

    @Test
    fun copyButtonHasAccessibleContentDescription() {
        composeTestRule.setContent {
            HighlightThemeProvider {
                SyntaxHighlightedCode(
                    code = sampleCode,
                    language = "kotlin",
                )
            }
        }
        // The copy button must be findable by its content description for TalkBack / a11y
        composeTestRule
            .onNodeWithContentDescription("Copy code", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun composableHasTestTag() {
        composeTestRule.setContent {
            HighlightThemeProvider {
                SyntaxHighlightedCode(
                    code = sampleCode,
                    language = "kotlin",
                )
            }
        }
        // The outer surface must carry the "syntax-highlighted-code" test tag
        composeTestRule
            .onNodeWithTag("syntax-highlighted-code")
            .assertIsDisplayed()
    }

    @Test
    fun placeholderIsShownDuringLoading() {
        var placeholderComposed = false
        var placeholderVisibleInTree = false
        composeTestRule.setContent {
            HighlightThemeProvider {
                SyntaxHighlightedCode(
                    code = sampleCode,
                    language = "python",
                    placeholder = { _ ->
                        SideEffect { placeholderComposed = true }
                        androidx.compose.material3.Text(text = "loading-placeholder")
                    },
                )
            }
        }
        composeTestRule.waitUntil(timeoutMillis = 10_000L) {
            val isVisible =
                composeTestRule
                    .onAllNodesWithText("loading-placeholder", useUnmergedTree = true)
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            if (isVisible) {
                placeholderVisibleInTree = true
            }
            placeholderComposed && placeholderVisibleInTree
        }
        composeTestRule.runOnIdle {
            assertThat(placeholderComposed).isTrue()
            assertThat(placeholderVisibleInTree).isTrue()
        }
    }

    @Test
    fun placeholderDisappearsAfterHighlightCompletes() {
        var capturedResult: dev.hossain.highlight.engine.HighlightResult? = null
        var placeholderVisibleBeforeCompletion = false

        composeTestRule.setContent {
            HighlightThemeProvider(
                lightHighlightTheme = HighlightTheme.tomorrow(context),
                darkHighlightTheme = HighlightTheme.tomorrowNight(context),
            ) {
                SyntaxHighlightedCode(
                    code = sampleCode,
                    language = "python",
                    placeholder = { _ ->
                        androidx.compose.material3.Text(text = "loading-placeholder")
                    },
                    onHighlightComplete = { result -> capturedResult = result },
                )
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 10_000L) {
            val placeholderVisible =
                composeTestRule
                    .onAllNodesWithText("loading-placeholder", useUnmergedTree = true)
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            if (placeholderVisible && capturedResult == null) {
                placeholderVisibleBeforeCompletion = true
            }
            placeholderVisibleBeforeCompletion
        }

        // Wait until highlighting is done
        composeTestRule.waitUntil(timeoutMillis = 10_000L) { capturedResult != null }
        composeTestRule.waitForIdle()

        assertThat(placeholderVisibleBeforeCompletion).isTrue()
        // Placeholder must be gone after highlighting completes
        composeTestRule.onNodeWithText("loading-placeholder", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun copyButtonWithContentDescriptionIsClickable() {
        var copyCalled = false
        composeTestRule.setContent {
            HighlightThemeProvider {
                SyntaxHighlightedCode(
                    code = sampleCode,
                    language = "python",
                    onCopyClick = { copyCalled = true },
                )
            }
        }
        // The copy button can be found and clicked via its content description
        composeTestRule
            .onNodeWithContentDescription("Copy code", useUnmergedTree = true)
            .performClick()
        composeTestRule.waitForIdle()
        assertThat(copyCalled).isTrue()
    }

    @Test
    fun defaultThemesAreStableAcrossRecomposition() {
        var recomposeTick by mutableStateOf(0)
        val seenLightThemes = mutableSetOf<Int>()
        val seenDarkThemes = mutableSetOf<Int>()

        composeTestRule.setContent {
            HighlightThemeProvider {
                // Read state so this subtree recomposes when tick changes.
                recomposeTick
                val lightTheme = LocalLightHighlightTheme.current
                val darkTheme = LocalDarkHighlightTheme.current
                SideEffect {
                    seenLightThemes += System.identityHashCode(lightTheme)
                    seenDarkThemes += System.identityHashCode(darkTheme)
                }
            }
        }

        composeTestRule.runOnIdle { recomposeTick++ }
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle { recomposeTick++ }
        composeTestRule.waitForIdle()

        assertThat(seenLightThemes).hasSize(1)
        assertThat(seenDarkThemes).hasSize(1)
    }
}
