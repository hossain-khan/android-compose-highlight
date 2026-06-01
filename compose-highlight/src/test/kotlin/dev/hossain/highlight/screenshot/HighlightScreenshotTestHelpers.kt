package dev.hossain.highlight.screenshot

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import dev.hossain.highlight.engine.HighlightEngine
import dev.hossain.highlight.ui.internal.LocalHighlightEngine
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
 * Wall-clock budget for the post-JS-callback drain inside [captureHighlightedScreenshot].
 * Five seconds is generous: in practice the engine's four-stage dispatcher hop sequence
 * settles within ~50 ms. The cap exists only to surface a clear timeout error instead of a
 * confusing "spans = 0" symptom if a future Compose BOM adds an additional hop.
 */
private const val DRAIN_TIMEOUT_MS = 5_000L

/**
 * Number of additional looper-pump cycles to run after `engine.isInitialized` flips to
 * `true`. Covers the State<AnnotatedString?> write + recomposition + AnimatedContent fade
 * settle. The previous `repeat(200) { ... }` cap (PR #207) was empirically known to produce
 * stable goldens; we keep that same number here as the post-init budget so existing
 * goldens don't shift. Most cycles are no-ops (idle returns immediately) so cost is small.
 */
private const val POST_INIT_STABILITY_TURNS = 200

/**
 * What to feed into the WebView's pending `evaluateJavascript` callback during a screenshot
 * capture. Each variant exercises a different rendering path of `SyntaxHighlightedCode`.
 */
internal sealed interface JsResponse {
    /**
     * Inject [fixtureHtml] as the successful highlight result (the default path that all
     * theme/layout/language tests use).
     */
    data class Success(
        val fixtureHtml: String,
    ) : JsResponse

    /**
     * Inject `null` to trigger `HighlightException.JsExecutionFailed`. The composable falls
     * back to plain `AnnotatedString(code)` text inside the Surface.
     */
    object Failure : JsResponse

    /**
     * Do not invoke the JS callback at all. The composable's `LaunchedEffect` stays in the
     * pre-result state, exposing the placeholder (or the bare `AnnotatedString(code)` if
     * none is provided) for the duration of the capture.
     */
    object NoResponse : JsResponse
}

/**
 * Default testTag used to find the node to capture. Matches the tag set inside
 * `SyntaxHighlightedCode`. Editor screenshot tests pass [SYNTAX_HIGHLIGHTED_TEXT_EDITOR_TEST_TAG]
 * instead.
 */
internal const val SYNTAX_HIGHLIGHTED_CODE_TEST_TAG: String = "syntax-highlighted-code"

/** testTag used by `SyntaxHighlightedTextEditor`. */
internal const val SYNTAX_HIGHLIGHTED_TEXT_EDITOR_TEST_TAG: String = "syntax-highlighted-text-editor"

/**
 * Convenience overload for the success path: injects [fixtureHtml] as the highlight result.
 * All theme/layout/language screenshot tests use this.
 */
internal fun ComposeContentTestRule.captureHighlightedScreenshot(
    name: String,
    fixtureHtml: String,
    testTag: String = SYNTAX_HIGHLIGHTED_CODE_TEST_TAG,
    content: @Composable () -> Unit,
) = captureHighlightedScreenshot(
    name = name,
    jsResponse = JsResponse.Success(fixtureHtml),
    testTag = testTag,
    content = content,
)

/**
 * Robolectric-driven screenshot test helper. Renders [content] inside a `LocalHighlightEngine`
 * provider scope, deterministically drives the hidden WebView via [jsResponse], waits for the
 * downstream coroutine + recomposition + animation pipeline to settle, then captures the
 * `syntax-highlighted-code` testTag node to a Roborazzi golden.
 *
 * The async-driving recipe mirrors the proven pattern in
 * `SyntaxHighlightedCodeRobolectricTest.onErrorCallbackFiresWhenJsReturnsNullInNonInspectionMode`.
 * The only thing that varies between callers is the `JsResponse` strategy.
 *
 * ## Capture flow
 *
 * 1. `setContent` injects a fresh `HighlightEngine` and runs [content].
 * 2. `waitForIdle()` lets `LaunchedEffect` start `WebViewManager.initialize()` and create the
 *    hidden WebView.
 * 3. The WebView is pulled out via the test-only `HighlightEngine.webViewForTest()` accessor.
 * 4. `WebViewClient.onPageFinished(...)` is invoked manually to complete the engine's
 *    `readyDeferred`.
 * 5. The looper is drained so the coroutine advances to `evaluateJavascript()`.
 * 6. [jsResponse] dictates what (if anything) is fed into the pending JS callback. See
 *    [JsResponse] for the variants.
 * 7. The looper is drained until `engine.isInitialized` flips to `true` (5s wall-clock cap),
 *    plus a fixed budget of follow-on cycles for State write + recomposition + AnimatedContent
 *    fade settle.
 * 8. `captureRoboImage` writes the PNG to `<roborazzi-outputDir>/<name>.png`.
 *
 * @param name Filename stem (no `.png` suffix). Should match the test method.
 * @param jsResponse Strategy for the WebView's pending `evaluateJavascript` callback.
 * @param testTag testTag of the node to capture. Defaults to [SYNTAX_HIGHLIGHTED_CODE_TEST_TAG]
 *   so existing read-only viewer tests need no changes; editor tests pass
 *   [SYNTAX_HIGHLIGHTED_TEXT_EDITOR_TEST_TAG].
 * @param content Composable rendering the actual UI under test. Must include exactly one node
 *   carrying [testTag].
 */
internal fun ComposeContentTestRule.captureHighlightedScreenshot(
    name: String,
    jsResponse: JsResponse,
    testTag: String = SYNTAX_HIGHLIGHTED_CODE_TEST_TAG,
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
        engine.webViewForTest()
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

    // ShadowWebView captures the evaluateJavascript callback without invoking it. The
    // strategy depends on which rendering path we want to capture: success drives the
    // happy path, failure exercises the AnnotatedString(code) fallback, and no-response
    // leaves the LaunchedEffect suspended so the placeholder stays visible.
    when (jsResponse) {
        is JsResponse.Success -> {
            val jsCallback =
                Shadows.shadowOf(webView).getLastEvaluatedJavascriptCallback()
                    ?: error("No pending evaluateJavascript callback; engine did not reach JS bridge")
            val resultPayload =
                JSONObject()
                    .apply {
                        put("error", false)
                        put("html", jsResponse.fixtureHtml)
                    }.toString()
            // evaluateJavascript returns a JSON-encoded *string*. Wrap with JSONObject.quote
            // so the engine's unescapeJsString() strips the outer quotes correctly.
            val wireValue = JSONObject.quote(resultPayload)
            jsCallback.onReceiveValue(wireValue)
        }

        JsResponse.Failure -> {
            val jsCallback =
                Shadows.shadowOf(webView).getLastEvaluatedJavascriptCallback()
                    ?: error("No pending evaluateJavascript callback; engine did not reach JS bridge")
            // Invoking with null triggers HighlightException.JsExecutionFailed inside the
            // engine. The composable's onError fires, highlightFailed flips to true, and
            // the rendered output is the AnnotatedString(code) fallback styled by Surface.
            jsCallback.onReceiveValue(null)
        }

        JsResponse.NoResponse -> {
            // Skip the callback entirely. The LaunchedEffect stays suspended on
            // engine.highlight(...) and the composable shows its placeholder slot (or, if
            // the caller passed no placeholder, the raw `AnnotatedString(code)` fallback).
        }
    }

    // After invoking the JS callback the engine still has 4 stages to walk:
    //   (1) resume the suspended coroutine on Dispatchers.Main,
    //   (2) hop to Dispatchers.Default for jsoup parse + AnnotatedString build,
    //   (3) hop back to Main and write State<AnnotatedString?>,
    //   (4) trigger recomposition + AnimatedContent fade frames.
    // Under v2 createComposeRule's StandardTestDispatcher each hop is its own scheduler
    // turn, so one waitForIdle() is not enough.
    //
    // The pragmatic strategy is to drain the looper repeatedly until the engine signals it
    // has finished its hot path. We use `engine.isInitialized` (StateFlow<Boolean>) which
    // flips to `true` once WebViewManager has completed bridge.html loading and the engine
    // is past the WebView round-trip stages. After it flips, we keep pumping for a small
    // additional budget so the State write + recomposition + AnimatedContent fade settle.
    //
    // This replaces an earlier `repeat(200) { idleMainLooper + waitForIdle }` cap. The
    // wall-clock timeout makes failures legible: if a future Compose BOM adds another
    // dispatcher hop, the test fails with a clear "ComposeTimeoutException after Xms"
    // instead of a confusing "spans = 0" symptom.
    val deadlineMs = System.currentTimeMillis() + DRAIN_TIMEOUT_MS
    var sawInitialized = false
    var stableTurns = 0
    while (System.currentTimeMillis() < deadlineMs) {
        ShadowLooper.idleMainLooper()
        waitForIdle()
        if (!sawInitialized) {
            sawInitialized = engine.isInitialized.value
            if (sawInitialized) stableTurns = 0
        } else {
            // Once the engine reports ready, pump a few more turns for downstream Compose
            // work (State write + recomposition + AnimatedContent settle), then exit.
            stableTurns++
            if (stableTurns >= POST_INIT_STABILITY_TURNS) break
        }
    }
    check(sawInitialized) {
        "engine.isInitialized never flipped to true within ${DRAIN_TIMEOUT_MS}ms after the JS " +
            "callback was invoked. A future Compose BOM may have added a dispatcher hop the " +
            "drain loop does not anticipate; bump DRAIN_TIMEOUT_MS or revisit the wait condition."
    }

    onNodeWithTag(testTag).captureRoboImage(
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
