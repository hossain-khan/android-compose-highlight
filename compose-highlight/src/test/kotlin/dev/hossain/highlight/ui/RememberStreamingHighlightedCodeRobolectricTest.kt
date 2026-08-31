package dev.hossain.highlight.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.text.AnnotatedString
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import dev.hossain.highlight.engine.HighlightEngine
import dev.hossain.highlight.engine.HighlightException
import dev.hossain.highlight.engine.HighlightResult
import dev.hossain.highlight.engine.HighlightTheme
import dev.hossain.highlight.ui.internal.LocalHighlightEngine
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@OptIn(ExperimentalHighlightApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30])
class RememberStreamingHighlightedCodeRobolectricTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun returnsPlainTextInInspectionMode() {
        var result: AnnotatedString? = null
        composeTestRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                HighlightThemeProvider {
                    result =
                        rememberStreamingHighlightedCode(
                            code = "val streaming = true",
                            language = "kotlin",
                        )
                }
            }
        }
        composeTestRule.waitForIdle()

        assertThat(result?.text).isEqualTo("val streaming = true")
    }

    @Test
    fun onErrorCallbackFiresWhenHighlightFails() {
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
                rememberStreamingHighlightedCode(
                    code = "val x = 42",
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
        val webView = engine.webViewForTest()
        assertThat(webView).isNotNull()
        val nonNullWebView = requireNotNull(webView)

        val webViewClient = Shadows.shadowOf(nonNullWebView).getWebViewClient()
        val lastUrl = Shadows.shadowOf(nonNullWebView).getLastLoadedUrl() ?: ""
        webViewClient?.onPageFinished(nonNullWebView, lastUrl)

        ShadowLooper.idleMainLooper()
        composeTestRule.waitForIdle()

        val jsCallback = Shadows.shadowOf(nonNullWebView).getLastEvaluatedJavascriptCallback()
        assertThat(jsCallback).isNotNull()
        requireNotNull(jsCallback).onReceiveValue(null)
        ShadowLooper.idleMainLooper()
        composeTestRule.waitForIdle()

        assertThat(errors).hasSize(1)
        assertThat(errors.first()).isInstanceOf(HighlightException.JsExecutionFailed::class.java)
    }

    @Test
    fun updatesReturnedTextImmediatelyWhenCodeChanges() {
        var code by mutableStateOf("val a = 1")
        var capturedResult: AnnotatedString? = null

        composeTestRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                HighlightThemeProvider {
                    capturedResult =
                        rememberStreamingHighlightedCode(
                            code = code,
                            language = "kotlin",
                        )
                }
            }
        }
        composeTestRule.waitForIdle()
        assertThat(capturedResult?.text).isEqualTo("val a = 1")

        code = "val a = 1\nval b = 2"
        composeTestRule.waitForIdle()
        assertThat(capturedResult?.text).isEqualTo("val a = 1\nval b = 2")
    }

    @Test
    fun clearingCodeResetsStateToEmptyAnnotatedString() {
        var code by mutableStateOf("val initial = true")
        var capturedResult: AnnotatedString? = null

        composeTestRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                HighlightThemeProvider {
                    capturedResult =
                        rememberStreamingHighlightedCode(
                            code = code,
                            language = "kotlin",
                        )
                }
            }
        }
        composeTestRule.waitForIdle()
        assertThat(capturedResult?.text).isEqualTo("val initial = true")

        code = ""
        composeTestRule.waitForIdle()
        assertThat(capturedResult?.text).isEmpty()
    }

    @Test
    fun newlineTriggersHighlightCallback() {
        val completedResults = mutableListOf<HighlightResult>()
        var code by mutableStateOf("val a = 1")
        val theme = HighlightTheme.fromCss(".hljs-keyword { color: #ff0000; }", "test-theme")
        var capturedEngine: HighlightEngine? = null

        composeTestRule.setContent {
            val context = LocalContext.current
            val engine =
                remember {
                    HighlightEngine(context.applicationContext).also { capturedEngine = it }
                }
            CompositionLocalProvider(LocalHighlightEngine provides engine) {
                rememberStreamingHighlightedCode(
                    code = code,
                    language = "kotlin",
                    theme = theme,
                    debounceMs = 5000L, // long debounce so idle timeout won't fire during test
                    triggerOnNewline = true,
                    minThrottleMs = 0L,
                    onHighlightComplete = { completedResults.add(it) },
                )
            }
        }
        composeTestRule.waitForIdle()

        val engine = capturedEngine ?: error("Engine was not captured")
        ShadowLooper.idleMainLooper()
        composeTestRule.waitForIdle()

        // Emitting a newline should trigger highlight despite 5000ms debounceMs
        code = "val a = 1\nval b = 2"
        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(timeoutMillis = 3000) {
            ShadowLooper.idleMainLooper()
            engine.webViewForTest() != null
        }

        val webView = engine.webViewForTest()
        assertThat(webView).isNotNull()
        val nonNullWebView = requireNotNull(webView)

        val webViewClient = Shadows.shadowOf(nonNullWebView).getWebViewClient()
        val lastUrl = Shadows.shadowOf(nonNullWebView).getLastLoadedUrl() ?: ""
        webViewClient?.onPageFinished(nonNullWebView, lastUrl)
        ShadowLooper.idleMainLooper()
        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(timeoutMillis = 3000) {
            ShadowLooper.idleMainLooper()
            Shadows.shadowOf(nonNullWebView).getLastEvaluatedJavascriptCallback() != null
        }

        val jsCallback = Shadows.shadowOf(nonNullWebView).getLastEvaluatedJavascriptCallback()
        assertThat(jsCallback).isNotNull()
        val resultPayload =
            org.json
                .JSONObject()
                .apply {
                    put("error", false)
                    put("html", "<span class=\"hljs-keyword\">val</span> a = 1\n<span class=\"hljs-keyword\">val</span> b = 2")
                    put("language", "kotlin")
                }.toString()
        requireNotNull(jsCallback).onReceiveValue(org.json.JSONObject.quote(resultPayload))

        composeTestRule.waitUntil(timeoutMillis = 3000) {
            ShadowLooper.idleMainLooper()
            completedResults.isNotEmpty()
        }

        assertThat(completedResults).hasSize(1)
        assertThat(completedResults.first().annotated.text).isEqualTo("val a = 1\nval b = 2")
    }

    @Test
    fun triggerOnNewlineFalseDoesNotTriggerOnNewlineBeforeDebounceMs() {
        val completedResults = mutableListOf<HighlightResult>()
        var code by mutableStateOf("val a = 1")
        val theme = HighlightTheme.fromCss(".hljs-keyword { color: #ff0000; }", "test-theme")
        var capturedEngine: HighlightEngine? = null

        composeTestRule.setContent {
            val context = LocalContext.current
            val engine =
                remember {
                    HighlightEngine(context.applicationContext).also { capturedEngine = it }
                }
            CompositionLocalProvider(LocalHighlightEngine provides engine) {
                rememberStreamingHighlightedCode(
                    code = code,
                    language = "kotlin",
                    theme = theme,
                    debounceMs = 5000L, // long debounce so idle timeout won't fire during test
                    triggerOnNewline = false,
                    onHighlightComplete = { completedResults.add(it) },
                )
            }
        }

        composeTestRule.waitForIdle()

        val engine = capturedEngine ?: error("Engine was not captured")
        ShadowLooper.idleMainLooper()
        composeTestRule.waitForIdle()

        // Emitting a newline when triggerOnNewline is false should not trigger highlight before debounceMs
        code = "val a = 1\nval b = 2"
        composeTestRule.waitForIdle()
        ShadowLooper.idleMainLooper()

        assertThat(engine.webViewForTest()).isNull()
        assertThat(completedResults).isEmpty()
    }

    /**
     * A mid-stream highlight failure (e.g. a transient WebView/JS error) must NOT discard the
     * last successful snapshot. Previously highlighted spans stay applied to the unchanged
     * prefix of the streaming text instead of flashing the whole block back to plain text.
     *
     * Drives two sequential highlight runs through Robolectric's ShadowWebView:
     * run 1 succeeds (spans applied), run 2 fails (null JS result). Asserts the returned
     * AnnotatedString still carries the run-1 spans after the failure.
     */
    @Test
    fun midStreamHighlightFailurePreservesPreviouslyHighlightedSpans() {
        val completedResults = mutableListOf<HighlightResult>()
        val errors = mutableListOf<HighlightException>()
        var code by mutableStateOf("val a = 1")
        val theme = HighlightTheme.fromCss(".hljs-keyword { color: #ff0000; }", "test-theme")
        var capturedEngine: HighlightEngine? = null
        var capturedResult: AnnotatedString? = null

        composeTestRule.setContent {
            val context = LocalContext.current
            val engine =
                remember {
                    HighlightEngine(context.applicationContext).also { capturedEngine = it }
                }
            CompositionLocalProvider(LocalHighlightEngine provides engine) {
                capturedResult =
                    rememberStreamingHighlightedCode(
                        code = code,
                        language = "kotlin",
                        theme = theme,
                        debounceMs = 5000L, // long debounce so idle timeout won't fire during test
                        triggerOnNewline = true,
                        minThrottleMs = 0L,
                        onHighlightComplete = { completedResults.add(it) },
                        onError = { errors.add(it) },
                    )
            }
        }
        composeTestRule.waitForIdle()

        val engine = capturedEngine ?: error("Engine was not captured")
        ShadowLooper.idleMainLooper()
        composeTestRule.waitForIdle()

        // First newline completes line 1 and triggers the first (successful) highlight run.
        code = "val a = 1\nval b = 2"
        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(timeoutMillis = 3000) {
            ShadowLooper.idleMainLooper()
            engine.webViewForTest() != null
        }
        val webView = engine.webViewForTest()
        assertThat(webView).isNotNull()
        val nonNullWebView = requireNotNull(webView)

        val webViewClient = Shadows.shadowOf(nonNullWebView).getWebViewClient()
        val lastUrl = Shadows.shadowOf(nonNullWebView).getLastLoadedUrl() ?: ""
        webViewClient?.onPageFinished(nonNullWebView, lastUrl)
        ShadowLooper.idleMainLooper()
        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(timeoutMillis = 3000) {
            ShadowLooper.idleMainLooper()
            Shadows.shadowOf(nonNullWebView).getLastEvaluatedJavascriptCallback() != null
        }
        val firstCallback = Shadows.shadowOf(nonNullWebView).getLastEvaluatedJavascriptCallback()
        assertThat(firstCallback).isNotNull()
        val successPayload =
            org.json
                .JSONObject()
                .apply {
                    put("error", false)
                    put("html", "<span class=\"hljs-keyword\">val</span> a = 1\n<span class=\"hljs-keyword\">val</span> b = 2")
                    put("language", "kotlin")
                }.toString()
        requireNotNull(firstCallback).onReceiveValue(org.json.JSONObject.quote(successPayload))

        composeTestRule.waitUntil(timeoutMillis = 3000) {
            ShadowLooper.idleMainLooper()
            completedResults.isNotEmpty()
        }
        assertThat(completedResults).hasSize(1)
        assertThat(capturedResult?.text).isEqualTo("val a = 1\nval b = 2")
        assertThat(capturedResult?.spanStyles).isNotEmpty()

        // Second newline triggers a second highlight run; drive it to failure with a null JS result.
        code = "val a = 1\nval b = 2\nval c = 3"
        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(timeoutMillis = 3000) {
            ShadowLooper.idleMainLooper()
            val next = Shadows.shadowOf(nonNullWebView).getLastEvaluatedJavascriptCallback()
            next != null && next !== firstCallback
        }
        val secondCallback = Shadows.shadowOf(nonNullWebView).getLastEvaluatedJavascriptCallback()
        assertThat(secondCallback).isNotNull()
        requireNotNull(secondCallback).onReceiveValue(null)

        composeTestRule.waitUntil(timeoutMillis = 3000) {
            ShadowLooper.idleMainLooper()
            errors.isNotEmpty()
        }
        ShadowLooper.idleMainLooper()
        composeTestRule.waitForIdle()

        assertThat(errors).hasSize(1)
        assertThat(errors.first()).isInstanceOf(HighlightException.JsExecutionFailed::class.java)
        // The new token text renders immediately while spans from the successful first run
        // remain applied to the unchanged prefix - no flash to fully plain text.
        assertThat(capturedResult?.text).isEqualTo("val a = 1\nval b = 2\nval c = 3")
        assertThat(capturedResult?.spanStyles).isNotEmpty()
    }
}
