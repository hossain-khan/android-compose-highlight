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
}
