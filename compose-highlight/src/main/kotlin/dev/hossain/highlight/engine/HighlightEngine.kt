package dev.hossain.highlight.engine

import android.content.Context
import android.webkit.WebView
import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Core engine that manages the hidden WebView and executes Highlight.js highlighting.
 *
 * Thread safety: WebView is always accessed on the Main thread.
 * Concurrent highlight calls are serialized via [mutex].
 *
 * ## Lifecycle
 *
 * The engine holds a hidden WebView resource. Always call [destroy] when the engine is no
 * longer needed. When used inside a Composable, use `rememberHighlightEngine()` which calls
 * [destroy] automatically via `DisposableEffect`.
 *
 * ## Composable usage (recommended)
 *
 * ```kotlin
 * @Composable
 * fun MyCodeBlock(code: String) {
 *     val engine = rememberHighlightEngine()
 *     val highlighted by rememberHighlightedCode(code, "kotlin", HighlightTheme.tomorrow(LocalContext.current))
 *     Text(text = highlighted ?: AnnotatedString(code))
 * }
 * ```
 *
 * ## Manual usage (e.g. ViewModel or background work)
 *
 * ```kotlin
 * val engine = HighlightEngine(context)
 *
 * // Optional: call initialize() to warm up the WebView before the first highlight.
 * // If skipped, the first call to highlight() will initialize it automatically.
 * engine.initialize()
 *
 * val result = engine.highlight(
 *     code     = "val x = 42",
 *     language = "kotlin",
 *     theme    = HighlightTheme.atomOneDark(context),
 * )
 * result.onSuccess { annotated -> /* use AnnotatedString */ }
 *
 * // Release resources when done
 * engine.destroy()
 * ```
 *
 * ## Highlight once, render in two themes
 *
 * ```kotlin
 * val themed = engine.highlightBothThemes(
 *     code       = sourceCode,
 *     language   = "typescript",
 *     lightTheme = HighlightTheme.tomorrow(context),
 *     darkTheme  = HighlightTheme.tomorrowNight(context),
 * )
 * themed.onSuccess { result ->
 *     val display = if (isDark) result.dark else result.light
 * }
 * ```
 */
class HighlightEngine(
    private val context: Context,
) {
    private val manager = WebViewManager(context)

    // Serializes concurrent evaluateJavascript() calls — WebView handles one at a time.
    private val mutex = Mutex()

    /**
     * Warms up the hidden WebView and loads bridge.html.
     *
     * This is an optional optimization — if not called, the first [highlightToHtml] or
     * [highlight] call will initialize automatically. Call this early (e.g. on screen entry)
     * to reduce latency on the first highlight request.
     *
     * Safe to call multiple times — idempotent.
     */
    suspend fun initialize() {
        try {
            manager.initialize()
        } catch (e: Exception) {
            throw HighlightException.WebViewInitFailed(e)
        }
    }

    /**
     * Highlights [code] and returns raw HTML with `<span class="hljs-*">` tokens.
     *
     * Automatically initializes the WebView on the first call.
     * Thread-safe: may be called from any dispatcher.
     *
     * JS escaping fix (PRD §4.2): backslash is escaped first to avoid double-escaping.
     */
    suspend fun highlightToHtml(
        code: String,
        language: String,
    ): Result<String> =
        try {
            manager.initialize()
            val webView = manager.getReadyWebView()

            mutex.withLock {
                withTimeout(HighlightException.TIMEOUT_SECONDS * 1000L) {
                    executeJs(webView, code, language)
                }
            }
        } catch (e: HighlightException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(HighlightException.JsExecutionFailed(e))
        }

    /**
     * Full pipeline: highlight → parse theme → convert to [AnnotatedString].
     *
     * Convenience method combining [highlightToHtml] + [ThemeParser] + [HtmlToAnnotatedString].
     */
    suspend fun highlight(
        code: String,
        language: String,
        theme: HighlightTheme,
    ): Result<AnnotatedString> =
        highlightToHtml(code, language).map { html ->
            try {
                HtmlToAnnotatedString.convert(html, theme.colorMap)
            } catch (e: Exception) {
                throw HighlightException.HtmlParseFailed(e)
            }
        }

    /**
     * Produces both light and dark [AnnotatedString] from a single JS call.
     * The HTML is tokenized once, then converted twice with different color maps,
     * making theme switching instant without an extra JS round-trip.
     */
    suspend fun highlightBothThemes(
        code: String,
        language: String,
        lightTheme: HighlightTheme,
        darkTheme: HighlightTheme,
    ): Result<ThemedHighlightResult> =
        highlightToHtml(code, language).map { html ->
            try {
                val light = HtmlToAnnotatedString.convert(html, lightTheme.colorMap)
                val dark = HtmlToAnnotatedString.convert(html, darkTheme.colorMap)
                ThemedHighlightResult(light, dark)
            } catch (e: Exception) {
                throw HighlightException.HtmlParseFailed(e)
            }
        }

    /** Releases the WebView resources. */
    fun destroy() {
        manager.destroy()
    }

    /**
     * Executes the highlight JS call and returns the resulting HTML.
     *
     * String escaping order:
     * 1. `\` → `\\` (must be first to avoid double-escaping)
     * 2. `'` → `\'`
     * 3. `\n` → `\\n`
     * 4. `\r` → `\\r`
     *
     * The JS callback returns a JSON-encoded string — unescape it before returning.
     */
    private suspend fun executeJs(
        webView: WebView,
        code: String,
        language: String,
    ): Result<String> {
        val escaped =
            code
                .replace("\\", "\\\\") // Must be first
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "\\r")

        val js = "(function() { return highlightCode('$escaped', '$language'); })()"

        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                webView.evaluateJavascript(js) { rawResult ->
                    if (rawResult == null || rawResult == "null") {
                        continuation.resumeWithException(
                            HighlightException.JsExecutionFailed(RuntimeException("JS returned null")),
                        )
                        return@evaluateJavascript
                    }
                    // The result is a JSON-encoded string — strip surrounding quotes and unescape
                    val html = unescapeJsString(rawResult)
                    continuation.resume(Result.success(html))
                }
            }
        }
    }

    /**
     * Unescapes a JSON-encoded string returned by [WebView.evaluateJavascript].
     * The WebView wraps the string in double quotes and escapes special chars.
     */
    private fun unescapeJsString(jsonString: String): String {
        // Strip surrounding double quotes if present
        val inner =
            if (jsonString.startsWith("\"") && jsonString.endsWith("\"")) {
                jsonString.substring(1, jsonString.length - 1)
            } else {
                jsonString
            }
        return inner
            .replace("\\u003C", "<")
            .replace("\\u003c", "<")
            .replace("\\u003E", ">")
            .replace("\\u003e", ">")
            .replace("\\u0026", "&")
            .replace("\\\"", "\"")
            .replace("\\n", "\n")
            .replace("\\t", "\t")
            .replace("\\\\", "\\")
    }
}

/**
 * Holds both light and dark [AnnotatedString] results from a single highlight call.
 * Used by [HighlightEngine.highlightBothThemes].
 */
data class ThemedHighlightResult(
    val light: AnnotatedString,
    val dark: AnnotatedString,
)
