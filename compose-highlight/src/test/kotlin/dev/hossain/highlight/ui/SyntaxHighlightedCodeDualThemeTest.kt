package dev.hossain.highlight.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.text.SpanStyle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import dev.hossain.highlight.engine.HighlightEngine
import dev.hossain.highlight.engine.HighlightResult
import dev.hossain.highlight.engine.HighlightTheme
import dev.hossain.highlight.engine.HljsSelectors
import dev.hossain.highlight.ui.internal.LocalHighlightEngine
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

/**
 * Robolectric unit tests for dual-theme pre-highlighting in [SyntaxHighlightedCode] (#459).
 *
 * Verifies that:
 * 1. Inside [HighlightThemeProvider], toggling dark mode changes the active theme without
 *    making another WebView JavaScript bridge call (0 ms re-tokenization latency).
 * 2. Explicit single theme overrides bypass dual-theme pre-highlighting and remain fixed.
 * 3. Setting [SyntaxHighlightedCode.prehighlightBothThemes] to `false` disables dual
 *    pre-highlighting and triggers a re-highlight on theme toggles.
 * 4. Observability callback [SyntaxHighlightedCode.onHighlightComplete] fires with the
 *    active theme variant.
 * 5. Calling [SyntaxHighlightedCode] without [HighlightThemeProvider] and without an explicit
 *    theme throws an [IllegalStateException].
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30])
class SyntaxHighlightedCodeDualThemeTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val lightTheme =
        HighlightTheme.fromColorMap(
            name = "custom-light",
            colorMap = mapOf(HljsSelectors.KEYWORD to SpanStyle(color = Color.Blue)),
            backgroundColor = Color.White,
            defaultTextColor = Color.Black,
        )

    private val darkTheme =
        HighlightTheme.fromColorMap(
            name = "custom-dark",
            colorMap = mapOf(HljsSelectors.KEYWORD to SpanStyle(color = Color.Red)),
            backgroundColor = Color.Black,
            defaultTextColor = Color.White,
        )

    private val sampleHtmlPayload =
        JSONObject()
            .apply {
                put("error", false)
                put("html", "<span class=\"hljs-keyword\">fun</span> hello()")
                put("language", "kotlin")
            }.toString()

    @Test
    fun insideProvider_themeToggleDoesNotReInvokeWebViewJs() {
        var isDark by mutableStateOf(false)
        var capturedEngine: HighlightEngine? = null
        val highlightResults = mutableListOf<HighlightResult>()

        composeTestRule.setContent {
            HighlightThemeProvider(
                darkTheme = isDark,
                lightHighlightTheme = lightTheme,
                darkHighlightTheme = darkTheme,
            ) {
                capturedEngine = LocalHighlightEngine.current
                SyntaxHighlightedCode(
                    code = "fun hello()",
                    language = "kotlin",
                    onHighlightComplete = { highlightResults.add(it) },
                )
            }
        }
        composeTestRule.waitForIdle()

        val engine = requireNotNull(capturedEngine)
        val webView = requireNotNull(engine.webViewForTest())

        // Complete initial page load and dispatch the JS highlight response
        val webViewClient = Shadows.shadowOf(webView).getWebViewClient()
        val lastUrl = Shadows.shadowOf(webView).getLastLoadedUrl().orEmpty()
        webViewClient?.onPageFinished(webView, lastUrl)
        ShadowLooper.idleMainLooper()
        composeTestRule.waitForIdle()

        val firstCallback = Shadows.shadowOf(webView).getLastEvaluatedJavascriptCallback()
        assertThat(firstCallback).isNotNull()
        requireNotNull(firstCallback).onReceiveValue(JSONObject.quote(sampleHtmlPayload))

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            ShadowLooper.idleMainLooper()
            highlightResults.isNotEmpty()
        }

        assertThat(highlightResults).hasSize(1)
        assertThat(highlightResults[0].spanCount).isEqualTo(1)

        // Toggle from Light to Dark mode
        isDark = true
        composeTestRule.waitForIdle()
        ShadowLooper.idleMainLooper()
        composeTestRule.waitForIdle()

        // Verify that NO additional evaluateJavascript call was made
        val callbackAfterToggle = Shadows.shadowOf(webView).getLastEvaluatedJavascriptCallback()
        assertThat(callbackAfterToggle).isSameInstanceAs(firstCallback)

        // No additional highlight complete callback because no re-tokenization was required
        assertThat(highlightResults).hasSize(1)

        engine.destroy()
    }

    @Test
    fun insideProvider_withPrehighlightDisabled_themeToggleReInvokesWebViewJs() {
        var isDark by mutableStateOf(false)
        var capturedEngine: HighlightEngine? = null
        val highlightResults = mutableListOf<HighlightResult>()

        composeTestRule.setContent {
            HighlightThemeProvider(
                darkTheme = isDark,
                lightHighlightTheme = lightTheme,
                darkHighlightTheme = darkTheme,
            ) {
                capturedEngine = LocalHighlightEngine.current
                SyntaxHighlightedCode(
                    code = "fun hello()",
                    language = "kotlin",
                    prehighlightBothThemes = false,
                    onHighlightComplete = { highlightResults.add(it) },
                )
            }
        }
        composeTestRule.waitForIdle()

        val engine = requireNotNull(capturedEngine)
        val webView = requireNotNull(engine.webViewForTest())

        // Complete initial page load and respond to first JS highlight run
        val webViewClient = Shadows.shadowOf(webView).getWebViewClient()
        val lastUrl = Shadows.shadowOf(webView).getLastLoadedUrl().orEmpty()
        webViewClient?.onPageFinished(webView, lastUrl)
        ShadowLooper.idleMainLooper()
        composeTestRule.waitForIdle()

        val firstCallback = Shadows.shadowOf(webView).getLastEvaluatedJavascriptCallback()
        assertThat(firstCallback).isNotNull()
        requireNotNull(firstCallback).onReceiveValue(JSONObject.quote(sampleHtmlPayload))

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            ShadowLooper.idleMainLooper()
            highlightResults.isNotEmpty()
        }

        assertThat(highlightResults).hasSize(1)

        // Toggle to dark mode: with prehighlightBothThemes = false, it must re-highlight
        isDark = true
        composeTestRule.waitForIdle()
        ShadowLooper.idleMainLooper()
        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            ShadowLooper.idleMainLooper()
            val next = Shadows.shadowOf(webView).getLastEvaluatedJavascriptCallback()
            next != null && next !== firstCallback
        }

        val secondCallback = Shadows.shadowOf(webView).getLastEvaluatedJavascriptCallback()
        assertThat(secondCallback).isNotNull()
        assertThat(secondCallback).isNotSameInstanceAs(firstCallback)

        // Fulfill the second highlight run
        requireNotNull(secondCallback).onReceiveValue(JSONObject.quote(sampleHtmlPayload))

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            ShadowLooper.idleMainLooper()
            highlightResults.size == 2
        }

        assertThat(highlightResults).hasSize(2)

        engine.destroy()
    }

    @Test
    fun explicitTheme_bypassesDualThemeAndRetainsExplicitTheme() {
        var isDark by mutableStateOf(false)
        var capturedEngine: HighlightEngine? = null
        val highlightResults = mutableListOf<HighlightResult>()
        val explicitTheme =
            HighlightTheme.fromColorMap(
                name = "explicit-theme",
                colorMap = mapOf(HljsSelectors.KEYWORD to SpanStyle(color = Color.Green)),
            )

        composeTestRule.setContent {
            HighlightThemeProvider(
                darkTheme = isDark,
                lightHighlightTheme = lightTheme,
                darkHighlightTheme = darkTheme,
            ) {
                capturedEngine = LocalHighlightEngine.current
                SyntaxHighlightedCode(
                    code = "fun hello()",
                    language = "kotlin",
                    theme = explicitTheme,
                    onHighlightComplete = { highlightResults.add(it) },
                )
            }
        }
        composeTestRule.waitForIdle()

        val engine = requireNotNull(capturedEngine)
        val webView = requireNotNull(engine.webViewForTest())

        // Complete initial page load and respond to first JS highlight run
        val webViewClient = Shadows.shadowOf(webView).getWebViewClient()
        val lastUrl = Shadows.shadowOf(webView).getLastLoadedUrl().orEmpty()
        webViewClient?.onPageFinished(webView, lastUrl)
        ShadowLooper.idleMainLooper()
        composeTestRule.waitForIdle()

        val firstCallback = Shadows.shadowOf(webView).getLastEvaluatedJavascriptCallback()
        assertThat(firstCallback).isNotNull()
        requireNotNull(firstCallback).onReceiveValue(JSONObject.quote(sampleHtmlPayload))

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            ShadowLooper.idleMainLooper()
            highlightResults.isNotEmpty()
        }

        assertThat(highlightResults).hasSize(1)

        // Provider toggles dark mode, but composable uses explicit theme so it should not re-run
        isDark = true
        composeTestRule.waitForIdle()
        ShadowLooper.idleMainLooper()
        composeTestRule.waitForIdle()

        val callbackAfterToggle = Shadows.shadowOf(webView).getLastEvaluatedJavascriptCallback()
        assertThat(callbackAfterToggle).isSameInstanceAs(firstCallback)
        assertThat(highlightResults).hasSize(1)

        engine.destroy()
    }

    @Test
    fun withoutProviderAndWithoutExplicitTheme_throwsIllegalStateException() {
        val result =
            runCatching {
                composeTestRule.setContent {
                    SyntaxHighlightedCode(
                        code = "val x = 1",
                        language = "kotlin",
                    )
                }
                composeTestRule.waitForIdle()
            }

        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
        assertThat(result.exceptionOrNull()?.message).contains("No HighlightTheme provided")
    }

    @Test
    fun withoutProviderWithExplicitTheme_rendersWithoutThrowing() {
        var capturedEngine: HighlightEngine? = null

        composeTestRule.setContent {
            val context = LocalContext.current
            val engine =
                remember {
                    HighlightEngine(context.applicationContext).also { capturedEngine = it }
                }
            CompositionLocalProvider(
                LocalHighlightEngine provides engine,
            ) {
                SyntaxHighlightedCode(
                    code = "val x = 1",
                    language = "kotlin",
                    theme = lightTheme,
                )
            }
        }
        composeTestRule.waitForIdle()

        val engine = requireNotNull(capturedEngine)
        val webView = requireNotNull(engine.webViewForTest())

        val webViewClient = Shadows.shadowOf(webView).getWebViewClient()
        val lastUrl = Shadows.shadowOf(webView).getLastLoadedUrl().orEmpty()
        webViewClient?.onPageFinished(webView, lastUrl)
        ShadowLooper.idleMainLooper()
        composeTestRule.waitForIdle()

        val callback = Shadows.shadowOf(webView).getLastEvaluatedJavascriptCallback()
        assertThat(callback).isNotNull()
        requireNotNull(callback).onReceiveValue(JSONObject.quote(sampleHtmlPayload))
        ShadowLooper.idleMainLooper()
        composeTestRule.waitForIdle()

        engine.destroy()
    }
}
