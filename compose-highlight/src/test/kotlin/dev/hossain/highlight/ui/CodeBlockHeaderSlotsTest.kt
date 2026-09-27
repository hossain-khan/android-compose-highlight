package dev.hossain.highlight.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Verifies the header slot precedence rules shared by [SyntaxHighlightedCode] and
 * [StreamingSyntaxHighlightedCode] (implemented in the internal `CodeBlockHeader`):
 *
 * 1. `header` explicitly provided - render it alone (custom chrome owns the row).
 * 2. `header` explicitly `null` - render no header at all.
 * 3. `header` unset - default header layout, resolving `languageLabel` and `actions`:
 *    - `actions` explicitly provided - render it (including `actions = null` to hide all).
 *    - `actions` unset - render the default copy button.
 */
@OptIn(ExperimentalHighlightApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30])
class CodeBlockHeaderSlotsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    // ----- actions slot -----

    @Test
    fun `actions slot renders multiple buttons in a row`() {
        composeTestRule.setContent {
            HighlightThemeProvider {
                SyntaxHighlightedCode(
                    code = "val x = 42",
                    language = "kotlin",
                    actions = { onCopy ->
                        TextButton(onClick = {}, modifier = Modifier.testTag("edit-action")) { Text("Edit") }
                        TextButton(onClick = onCopy, modifier = Modifier.testTag("copy-action")) { Text("Copy") }
                    },
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("edit-action").assertIsDisplayed()
        composeTestRule.onNodeWithTag("copy-action").assertIsDisplayed()
    }

    @Test
    fun `actions slot receives working onCopy`() {
        var copiedCode: String? = null
        composeTestRule.setContent {
            HighlightThemeProvider {
                SyntaxHighlightedCode(
                    code = "val x = 42",
                    language = "kotlin",
                    onCopyClick = { copiedCode = it },
                    actions = { onCopy ->
                        TextButton(onClick = onCopy, modifier = Modifier.testTag("copy-action")) { Text("Copy") }
                    },
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("copy-action").performClick()
        composeTestRule.waitForIdle()
        assertThat(copiedCode).isEqualTo("val x = 42")
    }

    @Test
    fun `actions null hides all trailing actions`() {
        composeTestRule.setContent {
            HighlightThemeProvider {
                SyntaxHighlightedCode(
                    code = "val x = 42",
                    language = "kotlin",
                    actions = null,
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithContentDescription("Copy code", useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun `actions null keeps language label`() {
        composeTestRule.setContent {
            HighlightThemeProvider {
                SyntaxHighlightedCode(
                    code = "val x = 42",
                    language = "kotlin",
                    actions = null,
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("kotlin").assertIsDisplayed()
    }

    // ----- header slot -----

    @Test
    fun `custom header replaces default chrome`() {
        composeTestRule.setContent {
            HighlightThemeProvider {
                SyntaxHighlightedCode(
                    code = "val x = 42",
                    language = "kotlin",
                    header = { onCopy ->
                        Row {
                            Text("MainActivity.kt", modifier = Modifier.testTag("file-title"))
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(onClick = onCopy, modifier = Modifier.testTag("header-copy")) { Text("Copy") }
                        }
                    },
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("file-title").assertIsDisplayed()
        composeTestRule.onNodeWithTag("header-copy").assertIsDisplayed()
        // Default chrome is gone when header is customized.
        composeTestRule.onNodeWithText("kotlin").assertDoesNotExist()
        composeTestRule
            .onNodeWithContentDescription("Copy code", useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun `custom header receives working onCopy`() {
        var copiedCode: String? = null
        composeTestRule.setContent {
            HighlightThemeProvider {
                SyntaxHighlightedCode(
                    code = "val x = 42",
                    language = "kotlin",
                    onCopyClick = { copiedCode = it },
                    header = { onCopy ->
                        TextButton(onClick = onCopy, modifier = Modifier.testTag("header-copy")) { Text("Copy") }
                    },
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("header-copy").performClick()
        composeTestRule.waitForIdle()
        assertThat(copiedCode).isEqualTo("val x = 42")
    }

    @Test
    fun `header null renders no header chrome`() {
        composeTestRule.setContent {
            HighlightThemeProvider {
                SyntaxHighlightedCode(
                    code = "val x = 42",
                    language = "kotlin",
                    header = null,
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("kotlin").assertDoesNotExist()
        composeTestRule
            .onNodeWithContentDescription("Copy code", useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun `header null still renders code`() {
        composeTestRule.setContent {
            HighlightThemeProvider {
                SyntaxHighlightedCode(
                    code = "val x = 42",
                    language = "kotlin",
                    header = null,
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("val x = 42").assertIsDisplayed()
    }

    // ----- SyntaxHighlightedCodeDefaults.Header -----

    @Test
    fun `defaults Header renders label and actions`() {
        composeTestRule.setContent {
            HighlightThemeProvider {
                SyntaxHighlightedCode(
                    code = "val x = 42",
                    language = "kotlin",
                    header = { onCopy ->
                        SyntaxHighlightedCodeDefaults.Header(
                            languageLabel = { Text("custom-label", modifier = Modifier.testTag("label")) },
                            actions = { copy: () -> Unit ->
                                TextButton(onClick = copy, modifier = Modifier.testTag("action")) { Text("Copy") }
                            },
                            onCopy = onCopy,
                        )
                    },
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("label").assertIsDisplayed()
        composeTestRule.onNodeWithTag("action").assertIsDisplayed()
    }

    @Test
    fun `defaults Header renders nothing when both slots null`() {
        composeTestRule.setContent {
            HighlightThemeProvider {
                SyntaxHighlightedCode(
                    code = "val x = 42",
                    language = "kotlin",
                    header = { _ ->
                        SyntaxHighlightedCodeDefaults.Header(
                            languageLabel = null,
                            actions = null,
                            onCopy = {},
                        )
                    },
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("kotlin").assertDoesNotExist()
        composeTestRule
            .onNodeWithContentDescription("Copy code", useUnmergedTree = true)
            .assertDoesNotExist()
    }

    // ----- StreamingSyntaxHighlightedCode parity -----

    @Test
    fun `streaming actions slot renders and copies`() {
        var copiedCode: String? = null
        composeTestRule.setContent {
            HighlightThemeProvider {
                StreamingSyntaxHighlightedCode(
                    code = "val x = 42",
                    language = "kotlin",
                    onCopyClick = { copiedCode = it },
                    actions = { onCopy ->
                        TextButton(onClick = onCopy, modifier = Modifier.testTag("copy-action")) { Text("Copy") }
                    },
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("copy-action").performClick()
        composeTestRule.waitForIdle()
        assertThat(copiedCode).isEqualTo("val x = 42")
    }

    @Test
    fun `streaming custom header replaces default chrome`() {
        composeTestRule.setContent {
            HighlightThemeProvider {
                StreamingSyntaxHighlightedCode(
                    code = "val x = 42",
                    language = "kotlin",
                    header = { _ ->
                        Text("MainActivity.kt", modifier = Modifier.testTag("file-title"))
                    },
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("file-title").assertIsDisplayed()
        composeTestRule.onNodeWithText("kotlin").assertDoesNotExist()
    }

    @Test
    fun `streaming header null renders no header chrome`() {
        composeTestRule.setContent {
            HighlightThemeProvider {
                StreamingSyntaxHighlightedCode(
                    code = "val x = 42",
                    language = "kotlin",
                    header = null,
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("kotlin").assertDoesNotExist()
        composeTestRule
            .onNodeWithContentDescription("Copy code", useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun `streaming header slot`() {
        composeTestRule.setContent {
            HighlightThemeProvider {
                StreamingSyntaxHighlightedCode(
                    code = "val x = 42",
                    language = "kotlin",
                    header = { onCopy ->
                        Row {
                            Text("MainActivity.kt", modifier = Modifier.testTag("file-title"))
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(onClick = onCopy, modifier = Modifier.testTag("header-copy")) { Text("Copy") }
                        }
                    },
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("file-title").assertIsDisplayed()
        composeTestRule.onNodeWithText("kotlin").assertDoesNotExist()
        composeTestRule.onNodeWithTag("header-copy").assertIsDisplayed()
    }
}
