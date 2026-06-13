package dev.hossain.highlight.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyPress
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import dev.hossain.highlight.engine.HighlightEngine
import dev.hossain.highlight.engine.HighlightException
import dev.hossain.highlight.engine.HighlightTheme
import dev.hossain.highlight.ui.internal.LocalHighlightEngine
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

/**
 * Robolectric tests for [SyntaxHighlightedTextEditor]. Mirrors the parity layer that
 * [SyntaxHighlightedCodeRobolectricTest] provides for the read-only viewer:
 * - `LocalInspectionMode` short-circuit (no `LaunchedEffect` fires in @Preview).
 * - Test tag on the outer Surface.
 * - No-provider error path (throws when accessed without an explicit theme).
 * - Deterministic JS-callback driving for the success and failure paths via `ShadowWebView`.
 *
 * The async-driving recipe follows the same pattern as
 * [SyntaxHighlightedCodeRobolectricTest.onErrorCallbackFiresWhenJsReturnsNullInNonInspectionMode].
 *
 * @see RememberSyntaxHighlightedEditorValueRobolectricTest for the helper-level coverage
 *   (this class targets the editor composable specifically: the Surface chrome, the test
 *   tag, and the editor's onError forwarding to the helper).
 */
@OptIn(ExperimentalHighlightApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30])
class SyntaxHighlightedTextEditorRobolectricTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    // ----- Inspection mode -----

    @Test
    fun `renders text in preview mode`() {
        // In LocalInspectionMode the helper short-circuits its LaunchedEffect, so no engine
        // is ever created and the BasicTextField just renders the raw text. The editor must
        // remain visible (no crash, no blank Surface) in @Preview composables.
        composeTestRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                HighlightThemeProvider {
                    SyntaxHighlightedTextEditor(
                        value = TextFieldValue("val x = 42"),
                        onValueChange = {},
                        language = "kotlin",
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("syntax-highlighted-text-editor").assertIsDisplayed()
    }

    // ----- Test infrastructure -----

    @Test
    fun `has test tag on outer surface`() {
        // The "syntax-highlighted-text-editor" testTag is the canonical handle for screenshot
        // tests and other test infrastructure. Pinning it here protects against a refactor
        // that accidentally moves the tag onto an inner element or removes it.
        composeTestRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                HighlightThemeProvider {
                    SyntaxHighlightedTextEditor(
                        value = TextFieldValue("print('hi')"),
                        onValueChange = {},
                        language = "python",
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("syntax-highlighted-text-editor").assertIsDisplayed()
    }

    // ----- No-provider error path -----

    @Test
    fun `throws without ThemeProvider`() {
        // Without a HighlightThemeProvider ancestor and without an explicit `theme` argument,
        // the default `theme = LocalHighlightTheme.current` triggers the staticCompositionLocalOf
        // error("No HighlightTheme provided..."). Mirrors the read-only viewer's behaviour.
        val thrown =
            runCatching {
                composeTestRule.setContent {
                    SyntaxHighlightedTextEditor(
                        value = TextFieldValue("val x = 42"),
                        onValueChange = {},
                        language = "kotlin",
                    )
                }
                composeTestRule.waitForIdle()
            }
        assertThat(thrown.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
    }

    // ----- onError forwarding through the editor -----

    @Test
    fun `on error callback fires when JS returns null`() {
        // RememberSyntaxHighlightedEditorValueRobolectricTest covers the helper's onError
        // path directly. This test asserts the editor's onError parameter is wired to the
        // helper - a regression that broke the forwarding (e.g. a refactor that accidentally
        // dropped the `onError = onError` argument) wouldn't be caught by the helper test.
        val errors = mutableListOf<HighlightException>()
        val theme = HighlightTheme.fromCss("", "test-empty-theme")
        var capturedEngine: HighlightEngine? = null

        composeTestRule.setContent {
            val context = LocalContext.current
            val engine =
                remember {
                    HighlightEngine(context.applicationContext).also { capturedEngine = it }
                }
            CompositionLocalProvider(LocalHighlightEngine provides engine) {
                SyntaxHighlightedTextEditor(
                    value = TextFieldValue("val x = 42"),
                    onValueChange = {},
                    language = "kotlin",
                    theme = theme,
                    debounceMs = 0L,
                    onError = { errors.add(it) },
                )
            }
        }
        composeTestRule.waitForIdle()

        val engine = capturedEngine ?: error("Engine was not captured during composition")
        ShadowLooper.idleMainLooper()
        composeTestRule.waitForIdle()
        val webView = engine.webViewForTest() ?: error("WebView was not created by HighlightEngine")
        val webViewClient = Shadows.shadowOf(webView).getWebViewClient()
        val lastUrl = Shadows.shadowOf(webView).getLastLoadedUrl() ?: ""
        webViewClient?.onPageFinished(webView, lastUrl)
        ShadowLooper.idleMainLooper()
        composeTestRule.waitForIdle()

        val jsCallback =
            Shadows.shadowOf(webView).getLastEvaluatedJavascriptCallback()
                ?: error("No pending evaluateJavascript callback; engine did not reach JS bridge")
        jsCallback.onReceiveValue(null)
        ShadowLooper.idleMainLooper()
        composeTestRule.waitForIdle()

        assertThat(errors).hasSize(1)
        assertThat(errors[0]).isInstanceOf(HighlightException.JsExecutionFailed::class.java)

        engine.destroy()
    }

    @Test
    fun `auto indent copies indentation on newline`() {
        var value = TextFieldValue("    val x = 42", selection = TextRange(14))
        var calledValue: TextFieldValue? = null

        composeTestRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                HighlightThemeProvider {
                    SyntaxHighlightedTextEditor(
                        value = value,
                        onValueChange = {
                            value = it
                            calledValue = it
                        },
                        language = "kotlin",
                        autoIndentEnabled = true,
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        // Focus the text field
        composeTestRule.onNode(hasSetTextAction(), useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()

        // Simulate user typing a newline character at the end of the text
        composeTestRule.onNode(hasSetTextAction(), useUnmergedTree = true).performTextInput("\n")
        composeTestRule.waitForIdle()

        // The expected value should have the indentation "    " automatically appended
        assertThat(calledValue?.text).isEqualTo("    val x = 42\n    ")
        assertThat(calledValue?.selection?.start).isEqualTo(19)
    }

    @Test
    fun `tab key inserts spaces`() {
        var value = TextFieldValue("val x = 42", selection = TextRange(0))
        var calledValue: TextFieldValue? = null

        composeTestRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                HighlightThemeProvider {
                    SyntaxHighlightedTextEditor(
                        value = value,
                        onValueChange = {
                            value = it
                            calledValue = it
                        },
                        language = "kotlin",
                        indentation = "    ",
                        tabKeyInterceptionEnabled = true,
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        // Focus the text field
        composeTestRule.onNode(hasSetTextAction(), useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()

        // Simulate pressing Tab on hardware keyboard
        composeTestRule
            .onNode(hasSetTextAction(), useUnmergedTree = true)
            .performKeyPress(
                KeyEvent(
                    android.view.KeyEvent(
                        android.view.KeyEvent.ACTION_DOWN,
                        android.view.KeyEvent.KEYCODE_TAB,
                    ),
                ),
            )
        composeTestRule.waitForIdle()

        assertThat(calledValue?.text).isEqualTo("    val x = 42")
        assertThat(calledValue?.selection?.start).isEqualTo(4)
    }

    @Test
    fun `auto indent is ignored when disabled`() {
        var value = TextFieldValue("    val x = 42", selection = TextRange(14))
        var calledValue: TextFieldValue? = null

        composeTestRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                HighlightThemeProvider {
                    SyntaxHighlightedTextEditor(
                        value = value,
                        onValueChange = {
                            value = it
                            calledValue = it
                        },
                        language = "kotlin",
                        autoIndentEnabled = false,
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNode(hasSetTextAction(), useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNode(hasSetTextAction(), useUnmergedTree = true).performTextInput("\n")
        composeTestRule.waitForIdle()

        assertThat(calledValue?.text).isEqualTo("    val x = 42\n")
        assertThat(calledValue?.selection?.start).isEqualTo(15)
    }

    @Test
    fun `tab key interception is ignored when disabled`() {
        var value = TextFieldValue("val x = 42", selection = TextRange(0))
        var calledValue: TextFieldValue? = null

        composeTestRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                HighlightThemeProvider {
                    SyntaxHighlightedTextEditor(
                        value = value,
                        onValueChange = {
                            value = it
                            calledValue = it
                        },
                        language = "kotlin",
                        tabKeyInterceptionEnabled = false,
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNode(hasSetTextAction(), useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()

        // Reset calledValue to null since performClick() triggers an initial onValueChange event (focus gain/selection change)
        calledValue = null

        // Tab key press should not be consumed, and calledValue remains null because onValueChange wasn't called by us
        composeTestRule
            .onNode(hasSetTextAction(), useUnmergedTree = true)
            .performKeyPress(
                KeyEvent(
                    android.view.KeyEvent(
                        android.view.KeyEvent.ACTION_DOWN,
                        android.view.KeyEvent.KEYCODE_TAB,
                    ),
                ),
            )
        composeTestRule.waitForIdle()

        assertThat(calledValue?.text).isEqualTo("\tval x = 42")
    }

    @Test
    fun `tab key inserts custom indentation string`() {
        var value = TextFieldValue("val x = 42", selection = TextRange(0))
        var calledValue: TextFieldValue? = null

        composeTestRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                HighlightThemeProvider {
                    SyntaxHighlightedTextEditor(
                        value = value,
                        onValueChange = {
                            value = it
                            calledValue = it
                        },
                        language = "kotlin",
                        indentation = "\t",
                        tabKeyInterceptionEnabled = true,
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNode(hasSetTextAction(), useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()

        composeTestRule
            .onNode(hasSetTextAction(), useUnmergedTree = true)
            .performKeyPress(
                KeyEvent(
                    android.view.KeyEvent(
                        android.view.KeyEvent.ACTION_DOWN,
                        android.view.KeyEvent.KEYCODE_TAB,
                    ),
                ),
            )
        composeTestRule.waitForIdle()

        assertThat(calledValue?.text).isEqualTo("\tval x = 42")
        assertThat(calledValue?.selection?.start).isEqualTo(1)
    }

    @Test
    fun `auto indent copies tab characters`() {
        var value = TextFieldValue("\t\tval x = 42", selection = TextRange(12))
        var calledValue: TextFieldValue? = null

        composeTestRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                HighlightThemeProvider {
                    SyntaxHighlightedTextEditor(
                        value = value,
                        onValueChange = {
                            value = it
                            calledValue = it
                        },
                        language = "kotlin",
                        autoIndentEnabled = true,
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNode(hasSetTextAction(), useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNode(hasSetTextAction(), useUnmergedTree = true).performTextInput("\n")
        composeTestRule.waitForIdle()

        assertThat(calledValue?.text).isEqualTo("\t\tval x = 42\n\t\t")
        assertThat(calledValue?.selection?.start).isEqualTo(15)
    }

    @Test
    fun `auto indent handles middle of text insertion`() {
        var value = TextFieldValue("  valx = 42", selection = TextRange(5)) // Cursor is between 'l' and 'x'
        var calledValue: TextFieldValue? = null

        composeTestRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                HighlightThemeProvider {
                    SyntaxHighlightedTextEditor(
                        value = value,
                        onValueChange = {
                            value = it
                            calledValue = it
                        },
                        language = "kotlin",
                        autoIndentEnabled = true,
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNode(hasSetTextAction(), useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNode(hasSetTextAction(), useUnmergedTree = true).performTextInput("\n")
        composeTestRule.waitForIdle()

        assertThat(calledValue?.text).isEqualTo("  val\n  x = 42")
        assertThat(calledValue?.selection?.start).isEqualTo(8)
    }

    @Test
    fun `auto indent does nothing when previous line has no indentation`() {
        var value = TextFieldValue("val x = 42", selection = TextRange(10))
        var calledValue: TextFieldValue? = null

        composeTestRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                HighlightThemeProvider {
                    SyntaxHighlightedTextEditor(
                        value = value,
                        onValueChange = {
                            value = it
                            calledValue = it
                        },
                        language = "kotlin",
                        autoIndentEnabled = true,
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNode(hasSetTextAction(), useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNode(hasSetTextAction(), useUnmergedTree = true).performTextInput("\n")
        composeTestRule.waitForIdle()

        assertThat(calledValue?.text).isEqualTo("val x = 42\n")
        assertThat(calledValue?.selection?.start).isEqualTo(11)
    }

    @Test
    fun `hardware enter key copies indentation`() {
        var value = TextFieldValue("    val x = 42", selection = TextRange(14))
        var calledValue: TextFieldValue? = null

        composeTestRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                HighlightThemeProvider {
                    SyntaxHighlightedTextEditor(
                        value = value,
                        onValueChange = {
                            value = it
                            calledValue = it
                        },
                        language = "kotlin",
                        autoIndentEnabled = true,
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNode(hasSetTextAction(), useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()

        calledValue = null

        composeTestRule
            .onNode(hasSetTextAction(), useUnmergedTree = true)
            .performKeyPress(
                KeyEvent(
                    android.view.KeyEvent(
                        android.view.KeyEvent.ACTION_DOWN,
                        android.view.KeyEvent.KEYCODE_ENTER,
                    ),
                ),
            )
        composeTestRule.waitForIdle()

        assertThat(calledValue?.text).isEqualTo("    val x = 42\n    ")
        assertThat(calledValue?.selection?.start).isEqualTo(19)
    }

    @Test
    fun `hardware enter key is ignored when disabled`() {
        var value = TextFieldValue("    val x = 42", selection = TextRange(14))
        var calledValue: TextFieldValue? = null

        composeTestRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                HighlightThemeProvider {
                    SyntaxHighlightedTextEditor(
                        value = value,
                        onValueChange = {
                            value = it
                            calledValue = it
                        },
                        language = "kotlin",
                        autoIndentEnabled = false,
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNode(hasSetTextAction(), useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()

        calledValue = null

        composeTestRule
            .onNode(hasSetTextAction(), useUnmergedTree = true)
            .performKeyPress(
                KeyEvent(
                    android.view.KeyEvent(
                        android.view.KeyEvent.ACTION_DOWN,
                        android.view.KeyEvent.KEYCODE_ENTER,
                    ),
                ),
            )
        composeTestRule.waitForIdle()

        assertThat(calledValue?.text).isEqualTo("    val x = 42\n")
        assertThat(calledValue?.selection?.start).isEqualTo(15)
    }

    @Test
    fun `hardware enter key copies tab characters`() {
        var value = TextFieldValue("\t\tval x = 42", selection = TextRange(12))
        var calledValue: TextFieldValue? = null

        composeTestRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                HighlightThemeProvider {
                    SyntaxHighlightedTextEditor(
                        value = value,
                        onValueChange = {
                            value = it
                            calledValue = it
                        },
                        language = "kotlin",
                        autoIndentEnabled = true,
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNode(hasSetTextAction(), useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()

        calledValue = null

        composeTestRule
            .onNode(hasSetTextAction(), useUnmergedTree = true)
            .performKeyPress(
                KeyEvent(
                    android.view.KeyEvent(
                        android.view.KeyEvent.ACTION_DOWN,
                        android.view.KeyEvent.KEYCODE_ENTER,
                    ),
                ),
            )
        composeTestRule.waitForIdle()

        assertThat(calledValue?.text).isEqualTo("\t\tval x = 42\n\t\t")
        assertThat(calledValue?.selection?.start).isEqualTo(15)
    }

    @Test
    fun `tab key replaces selection`() {
        var value = TextFieldValue("val x = 42", selection = TextRange(4, 9)) // selects "x = 4"
        var calledValue: TextFieldValue? = null

        composeTestRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                HighlightThemeProvider {
                    SyntaxHighlightedTextEditor(
                        value = value,
                        onValueChange = {
                            value = it
                            calledValue = it
                        },
                        language = "kotlin",
                        indentation = "    ",
                        tabKeyInterceptionEnabled = true,
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNode(hasSetTextAction(), useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()

        calledValue = null

        composeTestRule
            .onNode(hasSetTextAction(), useUnmergedTree = true)
            .performKeyPress(
                KeyEvent(
                    android.view.KeyEvent(
                        android.view.KeyEvent.ACTION_DOWN,
                        android.view.KeyEvent.KEYCODE_TAB,
                    ),
                ),
            )
        composeTestRule.waitForIdle()

        assertThat(calledValue?.text).isEqualTo("val     2")
        assertThat(calledValue?.selection?.start).isEqualTo(8)
    }

    // Note: a Robolectric test asserting selection preservation across a successful highlight
    // cycle (issue #230's `selectionPreservedAcrossNonCollapsedRange` item) was attempted but
    // proved flaky based on test execution ordering - the engine's coroutine pipeline doesn't
    // always complete within the bounded drain window when the test runs in the full suite.
    // The non-collapsed-selection assertion belongs in the instrumented test file (see
    // RememberSyntaxHighlightedEditorValueTest.preservesNonCollapsedSelectionAcrossHighlightCycle)
    // where the real WebView and real coroutine timing make the assertion deterministic.
}
