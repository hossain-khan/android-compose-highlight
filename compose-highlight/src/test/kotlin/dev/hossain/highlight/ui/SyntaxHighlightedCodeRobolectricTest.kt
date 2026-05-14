package dev.hossain.highlight.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30])
class SyntaxHighlightedCodeRobolectricTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    // ----- Category 1: tests using the LocalInspectionMode preview fallback -----
    // In inspection mode, SyntaxHighlightedCode renders only Surface + Text (no header).

    @Test
    fun rendersCodeTextInPreviewMode() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                HighlightThemeProvider {
                    SyntaxHighlightedCode(
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
    fun hasTestTagOnOuterSurface() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                HighlightThemeProvider {
                    SyntaxHighlightedCode(
                        code = "print('hi')",
                        language = "python",
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithTag("syntax-highlighted-code")
            .assertIsDisplayed()
    }

    @Test
    fun hidesLanguageLabelWhenFlagIsFalse() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                HighlightThemeProvider {
                    SyntaxHighlightedCode(
                        code = "val x = 42",
                        language = "kotlin",
                        showLanguageLabel = false,
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("kotlin")
            .assertDoesNotExist()
    }

    @Test
    fun hidesCopyButtonWhenFlagIsFalse() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                HighlightThemeProvider {
                    SyntaxHighlightedCode(
                        code = "val x = 42",
                        language = "kotlin",
                        showCopyButton = false,
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithContentDescription("Copy code", useUnmergedTree = true)
            .assertDoesNotExist()
    }

    // ----- Category 2: tests using the full composable (no inspection mode) -----
    // These tests exercise the header row (language label + copy button).

    @Test
    fun copyButtonHasAccessibleContentDescription() {
        composeTestRule.setContent {
            HighlightThemeProvider {
                SyntaxHighlightedCode(
                    code = "val x = 42",
                    language = "kotlin",
                    showCopyButton = true,
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithContentDescription("Copy code", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun customCopyButtonContentDescription() {
        composeTestRule.setContent {
            HighlightThemeProvider {
                SyntaxHighlightedCode(
                    code = "x = 1",
                    language = "python",
                    showCopyButton = true,
                    copyButtonContentDescription = "Copiar código",
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithContentDescription("Copiar código", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun onCopyClickCallbackFires() {
        var copiedCode: String? = null
        composeTestRule.setContent {
            HighlightThemeProvider {
                SyntaxHighlightedCode(
                    code = "val x = 42",
                    language = "kotlin",
                    showCopyButton = true,
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
    fun showsLanguageLabelByDefault() {
        composeTestRule.setContent {
            HighlightThemeProvider {
                SyntaxHighlightedCode(
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
}
