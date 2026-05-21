package dev.hossain.highlight.ui

import android.webkit.WebView
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
    fun hidesLanguageLabelWhenContentIsNull() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                HighlightThemeProvider {
                    SyntaxHighlightedCode(
                        code = "val x = 42",
                        language = "kotlin",
                        languageLabelContent = null,
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
    fun hidesCopyButtonWhenContentIsNull() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                HighlightThemeProvider {
                    SyntaxHighlightedCode(
                        code = "val x = 42",
                        language = "kotlin",
                        copyButtonContent = null,
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
                    copyButtonContent = { onClick ->
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
    fun onCopyClickCallbackFires() {
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

    // ----- Category 3: onError callback tests -----

    @Test
    fun onErrorNotCalledInInspectionMode() {
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
    fun onErrorNullDoesNotCrashInInspectionMode() {
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
    fun onErrorCallbackCanBePassedWithoutCrash() {
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
    fun onErrorCallbackFiresWhenJsReturnsNullInNonInspectionMode() {
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
        // The WebView has been created - retrieve it via reflection.
        val webView = extractWebViewFromEngine(engine)
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

    /**
     * Extracts the internal [WebView] from a [HighlightEngine] via reflection.
     *
     * [HighlightEngine] holds a private [WebViewManager], which itself holds a private
     * `@Volatile var webView`. This helper is needed in tests because neither field is
     * exposed publicly. After [HighlightEngine] calls `initialize()` on the Main thread,
     * the `webView` field is populated and can be retrieved here.
     *
     * Returns `null` if the field cannot be found or if the WebView has not been created yet.
     */
    private fun extractWebViewFromEngine(engine: HighlightEngine): WebView? =
        try {
            val managerField = HighlightEngine::class.java.getDeclaredField("manager")
            managerField.isAccessible = true
            val manager = managerField.get(engine) ?: return null
            val webViewField = manager.javaClass.getDeclaredField("webView")
            webViewField.isAccessible = true
            webViewField.get(manager) as? WebView
        } catch (e: ReflectiveOperationException) {
            null
        }
}
