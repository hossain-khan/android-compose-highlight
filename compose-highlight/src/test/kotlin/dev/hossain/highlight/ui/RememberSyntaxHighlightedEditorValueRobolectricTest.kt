package dev.hossain.highlight.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.text.input.TextFieldValue
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

/**
 * Robolectric tests for [rememberSyntaxHighlightedEditorValue] that need deterministic control
 * over the WebView callback queue. The `androidTest/` counterpart covers the happy path against
 * a real device; this file uses Robolectric's `ShadowWebView` to drive the engine through its
 * error path without depending on a 5-second timeout.
 *
 * Mirrors the pattern in [SyntaxHighlightedCodeRobolectricTest] (see
 * `onErrorCallbackFiresWhenJsReturnsNullInNonInspectionMode`).
 */
@OptIn(ExperimentalHighlightApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30])
class RememberSyntaxHighlightedEditorValueRobolectricTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun onErrorCallbackFiresWhenHighlightFails() {
        // Verifies the onError parameter wires correctly: when the WebView's JS callback returns
        // null, HighlightEngine maps it to HighlightException.JsExecutionFailed and the editor
        // helper's onError lambda is invoked exactly once with that typed exception.
        //
        // The fromCss("", "test-empty-theme") trick avoids loading any real CSS assets - the
        // theme's backgroundColor/defaultTextColor resolve to Color.Unspecified, which is fine
        // because this test is about the error path, not visual output.
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
                rememberSyntaxHighlightedEditorValue(
                    value = TextFieldValue("val x = 42"),
                    language = "kotlin",
                    theme = theme,
                    // Zero debounce so the LaunchedEffect proceeds immediately to engine.highlight().
                    // Without this the test would have to advance the test clock past the default
                    // 150 ms window before the JS callback is queued.
                    debounceMs = 0L,
                    onError = { errors.add(it) },
                )
            }
        }
        composeTestRule.waitForIdle()

        val engine = capturedEngine ?: error("Engine was not captured during composition")

        // After waitForIdle() + an extra looper pump, WebViewManager.initialize() has run on
        // the Main thread and the WebView has been created. Retrieve it via the test-only
        // accessor (no reflection - PR #210 introduced this).
        ShadowLooper.idleMainLooper()
        composeTestRule.waitForIdle()
        val webView = engine.webViewForTest()
        assertThat(webView).isNotNull()
        val nonNullWebView = webView!!

        // Complete readyDeferred by directly invoking onPageFinished on the WebViewClient.
        // ShadowWebView.performSuccessfulPageLoadClientCallbacks() does NOT complete the
        // deferred in Robolectric 4.16 - direct invocation is needed.
        val webViewClient = Shadows.shadowOf(nonNullWebView).getWebViewClient()
        val lastUrl = Shadows.shadowOf(nonNullWebView).getLastLoadedUrl() ?: ""
        webViewClient?.onPageFinished(nonNullWebView, lastUrl)

        // Drain the main looper and advance the compose scheduler to let the engine coroutine
        // advance past readyDeferred.await() through to evaluateJavascript() in executeJs().
        ShadowLooper.idleMainLooper()
        composeTestRule.waitForIdle()

        // ShadowWebView stores the evaluateJavascript callback without calling it automatically.
        // Invoke it with null to trigger HighlightException.JsExecutionFailed inside the engine.
        val jsCallback = Shadows.shadowOf(nonNullWebView).getLastEvaluatedJavascriptCallback()
        assertThat(jsCallback).isNotNull()
        requireNotNull(jsCallback).onReceiveValue(null)
        ShadowLooper.idleMainLooper()
        composeTestRule.waitForIdle()

        // The editor helper's onFailure handler should have unwrapped the typed exception and
        // invoked our onError lambda exactly once.
        assertThat(errors).hasSize(1)
        assertThat(errors[0]).isInstanceOf(HighlightException.JsExecutionFailed::class.java)

        engine.destroy()
    }
}
