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
import dev.hossain.highlight.engine.HighlightEngine
import dev.hossain.highlight.engine.HighlightException
import dev.hossain.highlight.engine.HighlightTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30])
class SyntaxHighlightedCodeRobolectricTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    // ----- Category 1: tests using the LocalInspectionMode preview fallback -----
    // In inspection mode, SyntaxHighlightedCode renders only Surface + Text (no header).

    @Test
    fun `renders code text in preview mode`() {
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
    fun `has test tag on outer surface`() {
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
    fun `hides language label when content is null`() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                HighlightThemeProvider {
                    SyntaxHighlightedCode(
                        code = "val x = 42",
                        language = "kotlin",
                        languageLabel = null,
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
    fun `hides copy button when content is null`() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                HighlightThemeProvider {
                    SyntaxHighlightedCode(
                        code = "val x = 42",
                        language = "kotlin",
                        copyButton = null,
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
    fun `copy button has accessible content description`() {
        composeTestRule.setContent {
            HighlightThemeProvider {
                SyntaxHighlightedCode(
                    code = "val x = 42",
                    language = "kotlin",
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithContentDescription("Copy code", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `custom copy button content description`() {
        composeTestRule.setContent {
            HighlightThemeProvider {
                SyntaxHighlightedCode(
                    code = "x = 1",
                    language = "python",
                    copyButton = { onClick ->
                        SyntaxHighlightedCodeDefaults.CopyButton(
                            onClick = onClick,
                            contentDescription = "Copiar código",
                        )
                    },
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithContentDescription("Copiar código", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `on copy click callback fires`() {
        var copiedCode: String? = null
        composeTestRule.setContent {
            HighlightThemeProvider {
                SyntaxHighlightedCode(
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

    // ----- Category 3: onError callback tests -----

    @Test
    fun `on error not called in inspection mode`() {
        // In inspection mode the LaunchedEffect is skipped, so the engine is never called
        // and onError must never fire.
        val errors = mutableListOf<HighlightException>()
        composeTestRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                HighlightThemeProvider {
                    SyntaxHighlightedCode(
                        code = "val x = 42",
                        language = "kotlin",
                        onError = { errors.add(it) },
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        assertThat(errors).isEmpty()
    }

    @Test
    fun `on error null does not crash in inspection mode`() {
        // Passing onError = null (the default) must not crash.
        composeTestRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                HighlightThemeProvider {
                    SyntaxHighlightedCode(
                        code = "val x = 42",
                        language = "kotlin",
                        onError = null,
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
    fun `on error callback can be passed without crash`() {
        // Verify that providing onError does not prevent plain-text rendering
        // (inspection mode, so plain-text is shown immediately).
        var errorCallbackRegistered = false
        composeTestRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                HighlightThemeProvider {
                    SyntaxHighlightedCode(
                        code = "print('hello')",
                        language = "python",
                        onError = { errorCallbackRegistered = true },
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        // In inspection mode the engine is never invoked, so the callback is not triggered.
        assertThat(errorCallbackRegistered).isFalse()
        // But the plain-text code is still visible.
        composeTestRule
            .onNodeWithText("print('hello')")
            .assertIsDisplayed()
    }

    @Test
    fun `on error callback fires when JS returns null in non-inspection mode`() {
        // Verify that onError fires when highlighting fails with JsExecutionFailed.
        // Uses ShadowWebView to deterministically trigger the failure without relying on
        // the 5-second engine timeout.
        val errors = mutableListOf<HighlightException>()
        // fromCss with empty CSS: backgroundColor/defaultTextColor resolve without any assets.
        val theme = HighlightTheme.fromCss("", "test-empty-theme")
        // Capture the engine reference via LocalContext inside the composition.
        var capturedEngine: HighlightEngine? = null

        composeTestRule.setContent {
            val context = androidx.compose.ui.platform.LocalContext.current
            val engine =
                androidx.compose.runtime.remember {
                    HighlightEngine(context.applicationContext).also { capturedEngine = it }
                }
            CompositionLocalProvider(LocalHighlightEngine provides engine) {
                SyntaxHighlightedCode(
                    code = "val x = 42",
                    language = "kotlin",
                    theme = theme,
                    onError = { errors.add(it) },
                )
            }
        }
        composeTestRule.waitForIdle()

        val engine = capturedEngine ?: error("Engine was not captured during composition")

        // After waitForIdle(), WebViewManager.initialize() has run on the Main thread.
        // The WebView has been created - retrieve it via the test-only accessor.
        val webView = engine.webViewForTest()
        assertThat(webView).isNotNull()
        val nonNullWebView = requireNotNull(webView)

        // Complete readyDeferred by directly invoking onPageFinished on the registered
        // WebViewClient. Note: ShadowWebView.performSuccessfulPageLoadClientCallbacks()
        // does NOT complete the deferred in Robolectric 4.16 - direct invocation is needed.
        val webViewClient = Shadows.shadowOf(nonNullWebView).getWebViewClient()
        val lastUrl = Shadows.shadowOf(nonNullWebView).getLastLoadedUrl() ?: ""
        webViewClient?.onPageFinished(nonNullWebView, lastUrl)

        // Drain the main looper and advance the compose scheduler to let the coroutine
        // advance past readyDeferred.await() through to evaluateJavascript() in executeJs().
        ShadowLooper.idleMainLooper()
        composeTestRule.waitForIdle()

        // ShadowWebView stores the evaluateJavascript callback without calling it automatically.
        // Invoke it with null to trigger JsExecutionFailed inside the engine.
        val jsCallback = Shadows.shadowOf(nonNullWebView).getLastEvaluatedJavascriptCallback()
        assertThat(jsCallback).isNotNull()
        requireNotNull(jsCallback).onReceiveValue(null)
        ShadowLooper.idleMainLooper()
        composeTestRule.waitForIdle()

        assertThat(errors).hasSize(1)
        assertThat(errors[0]).isInstanceOf(HighlightException.JsExecutionFailed::class.java)

        engine.destroy()
    }
}
