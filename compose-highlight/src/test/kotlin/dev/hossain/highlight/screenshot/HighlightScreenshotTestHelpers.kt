package dev.hossain.highlight.screenshot

import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import dev.hossain.highlight.engine.HighlightEngine
import dev.hossain.highlight.ui.LocalHighlightEngine
import org.json.JSONObject
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowLooper
import java.io.File

/**
 * Pixel-diff threshold applied to every screenshot capture. Picked at 0.05% to absorb minor
 * font-hinting jitter while still catching real color and layout regressions. If goldens
 * recorded on one machine fail to verify on another at this threshold, the right move is to
 * re-record on the canonical platform (Linux for CI), not to relax the threshold.
 */
private const val SCREENSHOT_CHANGE_THRESHOLD = 0.0005f

/**
 * Robolectric-driven screenshot test helper. Renders [content] inside a `LocalHighlightEngine`
 * provider scope, deterministically drives the hidden WebView through a successful highlight
 * round-trip using [fixtureHtml], skips the `AnimatedContent` fade-in via the test main clock,
 * and finally captures the `syntax-highlighted-code` testTag node to a Roborazzi golden.
 *
 * The async-driving recipe mirrors the proven pattern in
 * `SyntaxHighlightedCodeRobolectricTest.onErrorCallbackFiresWhenJsReturnsNullInNonInspectionMode`.
 * The only difference is the `onReceiveValue` payload: success path here, error path there.
 *
 * ## Capture flow
 *
 * 1. `setContent` injects a fresh `HighlightEngine` and runs [content].
 * 2. `waitForIdle()` lets `LaunchedEffect` start `WebViewManager.initialize()` and create the
 *    hidden WebView.
 * 3. The WebView is pulled out via reflection (private field `webView` on `WebViewManager`).
 * 4. `WebViewClient.onPageFinished(...)` is invoked manually to complete the engine's
 *    `readyDeferred`.
 * 5. The looper is drained so the coroutine advances to `evaluateJavascript()`.
 * 6. The pending JS callback is retrieved via `Shadows.shadowOf(webView).getLastEvaluatedJavascriptCallback()`
 *    and invoked with a JSON-encoded `{ error: false, html: ... }` payload built from [fixtureHtml].
 * 7. The looper is drained again so the coroutine surfaces the parsed result back to Compose.
 * 8. `mainClock.autoAdvance` is disabled and the clock is advanced past the
 *    `AnimatedContent` fade-in so the captured frame is stable.
 * 9. `captureRoboImage` writes the PNG to the file-name derived path (`outputDir` + [name] + `.png`).
 *
 * @param name Filename stem (no `.png` suffix). Should be descriptive and match the test method,
 *   e.g. `"theme_tomorrow"`. Becomes `<roborazzi-outputDir>/<name>.png`.
 * @param fixtureHtml The HTML token tree to inject. See [TestHljsFixtures].
 * @param content Composable rendering the actual UI under test. Must include exactly one
 *   `SyntaxHighlightedCode` (or anything else carrying the `syntax-highlighted-code` testTag).
 */
internal fun ComposeContentTestRule.captureHighlightedScreenshot(
    name: String,
    fixtureHtml: String,
    content: @Composable () -> Unit,
) {
    var capturedEngine: HighlightEngine? = null

    setContent {
        val context = LocalContext.current
        val engine =
            remember {
                HighlightEngine(context.applicationContext).also { capturedEngine = it }
            }
        CompositionLocalProvider(LocalHighlightEngine provides engine) {
            content()
        }
    }
    waitForIdle()
    // Drain the main looper so the LaunchedEffect inside SyntaxHighlightedCode actually
    // dispatches its withContext(Dispatchers.Main) hop into WebViewManager.initialize().
    // Under @GraphicsMode(NATIVE) + StandardTestDispatcher (the v2 createComposeRule default)
    // an additional pump is needed beyond what waitForIdle provides on its own.
    ShadowLooper.idleMainLooper()
    waitForIdle()

    val engine = capturedEngine ?: error("HighlightEngine was not captured during composition")
    val webView =
        extractWebViewFromEngine(engine)
            ?: error("WebView was not created by HighlightEngine; check that LocalInspectionMode is false")

    // Complete readyDeferred by directly invoking onPageFinished on the WebViewClient.
    // ShadowWebView.performSuccessfulPageLoadClientCallbacks() does NOT complete the deferred
    // in Robolectric 4.16; direct invocation is needed.
    val webViewClient = Shadows.shadowOf(webView).getWebViewClient()
    val lastUrl = Shadows.shadowOf(webView).getLastLoadedUrl().orEmpty()
    webViewClient?.onPageFinished(webView, lastUrl)

    // Drain the main looper so the engine coroutine reaches evaluateJavascript().
    ShadowLooper.idleMainLooper()
    waitForIdle()

    // ShadowWebView captures the evaluateJavascript callback without invoking it. Drive a
    // successful response by feeding it the wire format the engine expects: a JSON-encoded
    // string that, once unescaped, is a JSONObject with `error: false` and `html: <fixture>`.
    val jsCallback =
        Shadows.shadowOf(webView).getLastEvaluatedJavascriptCallback()
            ?: error("No pending evaluateJavascript callback; engine did not reach JS bridge")

    val resultPayload =
        JSONObject()
            .apply {
                put("error", false)
                put("html", fixtureHtml)
            }.toString()
    // evaluateJavascript returns a JSON-encoded *string*. Wrap with JSONObject.quote so the
    // engine's unescapeJsString() strips the outer quotes correctly.
    val wireValue = JSONObject.quote(resultPayload)
    jsCallback.onReceiveValue(wireValue)

    // Drain repeatedly so the engine: (1) resumes from JS callback on Main, (2) hops to
    // Dispatchers.Default for jsoup parse + AnnotatedString build, (3) hops back to Main and
    // writes State<AnnotatedString?>, (4) recomposes. Each Dispatcher hop is its own
    // scheduler turn under v2 StandardTestDispatcher; one idle isn't enough. Letting
    // mainClock auto-advance throughout means the AnimatedContent fade-in animation also
    // settles to its final frame before capture.
    repeat(200) {
        ShadowLooper.idleMainLooper()
        waitForIdle()
    }

    onNodeWithTag("syntax-highlighted-code").captureRoboImage(
        filePath = "src/test/snapshots/images/$name.png",
        roborazziOptions =
            RoborazziOptions(
                compareOptions =
                    RoborazziOptions.CompareOptions(
                        changeThreshold = SCREENSHOT_CHANGE_THRESHOLD,
                    ),
            ),
    )

    engine.destroy()
}

/**
 * Extracts the hidden [WebView] from a [HighlightEngine] for test-only manipulation.
 *
 * `HighlightEngine` holds a `private val manager: WebViewManager` which itself holds a private
 * `@Volatile var webView: WebView?`. Both are reachable from the same module via reflection.
 *
 * Adapted verbatim from `SyntaxHighlightedCodeRobolectricTest.extractWebViewFromEngine`. A
 * future audit item replaces both copies with a `@VisibleForTesting internal` accessor on
 * `WebViewManager`; until then the reflection helper is centralised here.
 */
private fun extractWebViewFromEngine(engine: HighlightEngine): WebView? =
    try {
        val managerField = HighlightEngine::class.java.getDeclaredField("manager")
        managerField.isAccessible = true
        val manager = managerField.get(engine) ?: return null
        val webViewField = manager.javaClass.getDeclaredField("webView")
        webViewField.isAccessible = true
        webViewField.get(manager) as? WebView
    } catch (_: ReflectiveOperationException) {
        null
    }
