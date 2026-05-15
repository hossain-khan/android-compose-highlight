package dev.hossain.highlight.engine

import android.content.Context
import android.webkit.WebView
import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
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
 * ## Composable usage (lower-level)
 *
 * For most cases, prefer `SyntaxHighlightedCode` inside a `HighlightThemeProvider` — it handles
 * the engine lifecycle automatically. Use [rememberHighlightEngine] directly only when you need
 * lower-level control, such as calling [highlightBothThemes] or building a custom UI.
 *
 * ```kotlin
 * @Composable
 * fun MyCodeBlock(code: String) {
 *     val engine = rememberHighlightEngine()
 *     val theme = rememberTomorrowTheme()
 *     var highlighted by remember(code) { mutableStateOf<AnnotatedString?>(null) }
 *     LaunchedEffect(code) {
 *         engine.highlight(code, "kotlin", theme).onSuccess { highlighted = it.annotated }
 *     }
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
 * engine.initialize().onFailure { /* handle WebViewInitFailed if needed */ }
 *
 * val result = engine.highlight(
 *     code     = "val x = 42",
 *     language = "kotlin",
 *     theme    = HighlightTheme.atomOneDark(context),
 * )
 * result.onSuccess { highlighted ->
 *     display(highlighted.annotated)            // AnnotatedString
 *     log("spans: ${highlighted.spanCount}")    // 0 = unsupported language
 *     log("time:  ${highlighted.durationMs} ms")
 * }
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
    context: Context,
) {
    // Use applicationContext to avoid retaining an Activity context in the long-lived WebView.
    private val manager = WebViewManager(context.applicationContext)

    // Serializes concurrent evaluateJavascript() calls — WebView handles one at a time.
    private val mutex = Mutex()

    // Cached result of supportedLanguages() — list is static for a given bundled hljs version.
    @Volatile
    private var cachedLanguages: List<String>? = null

    // Cached result of highlightJsVersion() — version is static for the bundled hljs build.
    @Volatile
    private var cachedVersion: String? = null

    /**
     * `true` once [initialize] has completed successfully (or the first [highlight] /
     * [highlightToHtml] call has finished warming up the WebView).
     *
     * This is a [StateFlow] so Composables can observe initialization reactively without
     * a separate `var engineReady` flag:
     *
     * ```kotlin
     * val isReady by engine.isInitialized.collectAsState()
     * if (isReady) {
     *     // WebView is warm — next highlight call has no init latency
     * }
     * ```
     */
    val isInitialized: StateFlow<Boolean> get() = manager.isInitialized

    /**
     * Warms up the hidden WebView and loads bridge.html.
     *
     * This is an optional optimization — if not called, the first [highlightToHtml] or
     * [highlight] call will initialize automatically. Call this early (e.g. on screen entry)
     * to reduce latency on the first highlight request.
     *
     * Safe to call multiple times — idempotent.
     *
     * @return [Result.success] when the WebView load has been started (full readiness is
     *   signalled asynchronously via [isInitialized]), or [Result.failure] wrapping a
     *   [HighlightException.WebViewInitFailed] if initialization fails.
     */
    suspend fun initialize(): Result<Unit> =
        try {
            manager.initialize()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(HighlightException.WebViewInitFailed(e))
        }

    /**
     * Highlights [code] and returns raw HTML with `<span class="hljs-*">` tokens, together
     * with the time taken for the JavaScript round-trip.
     *
     * Lower-level alternative to [highlight]: use this when you need the raw HTML string rather
     * than a theme-applied [AnnotatedString]. Automatically initializes the WebView on the first
     * call. Thread-safe: may be called from any dispatcher.
     *
     * ```kotlin
     * engine.highlightToHtml("val x = 42", "kotlin").onSuccess { result ->
     *     // result.html contains e.g. <span class="hljs-keyword">val</span> x = ...
     *     renderRawHtml(result.html)
     *     log("JS round-trip: ${result.durationMs} ms")
     * }
     * ```
     *
     * @param code The source code to highlight.
     * @param language Highlight.js language identifier (e.g. `"kotlin"`, `"python"`).
     * @return [Result] wrapping an [HtmlHighlightResult] (html + timing), or [Result.failure]
     *   with a [HighlightException] on error.
     */
    suspend fun highlightToHtml(
        code: String,
        language: String,
    ): Result<HtmlHighlightResult> =
        try {
            manager.initialize()
            val webView = manager.getReadyWebView()

            mutex.withLock {
                val start = System.nanoTime()
                withTimeout(HighlightException.TIMEOUT_SECONDS * 1000L) {
                    executeJs(webView, code, language)
                }.map { html ->
                    HtmlHighlightResult(
                        html = html,
                        durationMs = (System.nanoTime() - start) / 1_000_000L,
                    )
                }
            }
        } catch (e: HighlightException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(HighlightException.JsExecutionFailed(e))
        }

    /**
     * Full pipeline: tokenise → apply theme → convert to [HighlightResult].
     *
     * Combines [highlightToHtml] with colour-map application to produce a ready-to-render
     * [AnnotatedString]. A [HighlightResult.spanCount] of `0` indicates a silent failure —
     * the language may be unsupported or the code was empty; [HighlightResult.annotated]
     * still contains plain text so callers can always render something.
     *
     * ```kotlin
     * engine.highlight(code, "kotlin", theme).onSuccess { result ->
     *     display(result.annotated)
     *     if (result.spanCount == 0) log("no tokens — language may be unsupported")
     *     log("highlighted in \${result.durationMs} ms")
     * }
     * ```
     *
     * @param code The source code to highlight.
     * @param language Highlight.js language identifier (e.g. `"kotlin"`, `"python"`).
     * @param theme The [HighlightTheme] whose colour map will be applied.
     * @return [Result] wrapping a [HighlightResult], or [Result.failure] with a
     *   [HighlightException] on error.
     */
    suspend fun highlight(
        code: String,
        language: String,
        theme: HighlightTheme,
    ): Result<HighlightResult> {
        val start = System.nanoTime()
        return highlightToHtml(code, language).map { htmlResult ->
            try {
                val annotated = HtmlToAnnotatedString.convert(htmlResult.html, theme.colorMap)
                val durationMs = (System.nanoTime() - start) / 1_000_000L
                HighlightResult(
                    annotated = annotated,
                    spanCount = annotated.spanStyles.size,
                    language = language,
                    durationMs = durationMs,
                )
            } catch (e: Exception) {
                throw HighlightException.HtmlParseFailed(e)
            }
        }
    }

    /**
     * Highlights [code] once and produces a [ThemedHighlightResult] with both a light and a dark
     * [androidx.compose.ui.text.AnnotatedString].
     *
     * The JS tokeniser runs **once**; the two colour maps are applied to the same HTML output,
     * so theme switching after the call returns is instant — no extra WebView round-trip.
     *
     * ```kotlin
     * engine.highlightBothThemes(
     *     code       = sourceCode,
     *     language   = "typescript",
     *     lightTheme = HighlightTheme.tomorrow(context),
     *     darkTheme  = HighlightTheme.tomorrowNight(context),
     * ).onSuccess { result ->
     *     val display = if (isDark) result.dark else result.light
     * }
     * ```
     *
     * @param code The source code to highlight.
     * @param language Highlight.js language identifier (e.g. `"kotlin"`, `"typescript"`).
     * @param lightTheme Theme applied to produce [ThemedHighlightResult.light].
     * @param darkTheme Theme applied to produce [ThemedHighlightResult.dark].
     * @return [Result] wrapping a [ThemedHighlightResult], or [Result.failure] with a
     *   [HighlightException] on error.
     */
    suspend fun highlightBothThemes(
        code: String,
        language: String,
        lightTheme: HighlightTheme,
        darkTheme: HighlightTheme,
    ): Result<ThemedHighlightResult> {
        val start = System.nanoTime()
        return highlightToHtml(code, language).map { htmlResult ->
            try {
                val (light, dark) =
                    HtmlToAnnotatedString.convertBothThemes(
                        htmlResult.html,
                        lightTheme.colorMap,
                        darkTheme.colorMap,
                    )
                ThemedHighlightResult(
                    light = light,
                    dark = dark,
                    durationMs = (System.nanoTime() - start) / 1_000_000L,
                )
            } catch (e: Exception) {
                throw HighlightException.HtmlParseFailed(e)
            }
        }
    }

    /** Releases the WebView resources and clears all internal caches (languages, version). */
    fun destroy() {
        cachedLanguages = null
        cachedVersion = null
        manager.destroy()
    }

    /**
     * Returns the list of language identifiers supported by the bundled Highlight.js.
     *
     * The result is fetched from the JS engine on the first call and cached — subsequent calls
     * return the cached list immediately without a WebView round-trip.
     *
     * Automatically initializes the WebView if not yet ready.
     *
     * ```kotlin
     * val languages = engine.supportedLanguages()
     * languages.onSuccess { list ->
     *     val isKotlinSupported = "kotlin" in list  // true
     * }
     * ```
     *
     * @return [Result] wrapping a sorted [List] of language name strings (e.g. `"kotlin"`,
     *   `"java"`, `"python"`), or [Result.failure] with a [HighlightException] if the WebView
     *   could not be initialized.
     */
    suspend fun supportedLanguages(): Result<List<String>> {
        cachedLanguages?.let { return Result.success(it) }
        return try {
            manager.initialize()
            val webView = manager.getReadyWebView()
            mutex.withLock {
                // Double-checked: another coroutine may have populated the cache while we waited.
                cachedLanguages?.let { return Result.success(it) }
                withTimeout(HighlightException.TIMEOUT_SECONDS * 1000L) {
                    withContext(Dispatchers.Main) {
                        suspendCancellableCoroutine { continuation ->
                            webView.evaluateJavascript("listLanguages()") { rawResult ->
                                if (!continuation.isActive) return@evaluateJavascript
                                if (rawResult == null || rawResult == "null") {
                                    continuation.resumeWithException(
                                        HighlightException.JsExecutionFailed(
                                            RuntimeException("listLanguages() returned null"),
                                        ),
                                    )
                                    return@evaluateJavascript
                                }
                                // evaluateJavascript serializes a JS array to a JSON array string,
                                // e.g. ["kotlin","java",...] — parse with JSONArray.
                                try {
                                    val jsonArray = org.json.JSONArray(rawResult)
                                    val languages =
                                        (0 until jsonArray.length())
                                            .map { jsonArray.getString(it) }
                                            .sorted()
                                    cachedLanguages = languages
                                    continuation.resume(Result.success(languages))
                                } catch (e: Exception) {
                                    continuation.resumeWithException(
                                        HighlightException.JsExecutionFailed(e),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: HighlightException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(HighlightException.JsExecutionFailed(e))
        }
    }

    /**
     * Returns the version string of the bundled Highlight.js library (e.g. `"11.11.1"`).
     *
     * The result is fetched from the JS engine on the first call and cached — subsequent calls
     * return the cached value immediately without a WebView round-trip.
     *
     * Automatically initializes the WebView if not yet ready.
     *
     * ```kotlin
     * engine.highlightJsVersion().onSuccess { version ->
     *     println("Using Highlight.js $version")
     * }
     * ```
     *
     * @return [Result] wrapping the version string, or [Result.failure] with a [HighlightException]
     *   if the WebView could not be initialized.
     */
    suspend fun highlightJsVersion(): Result<String> {
        cachedVersion?.let { return Result.success(it) }
        return try {
            manager.initialize()
            val webView = manager.getReadyWebView()
            mutex.withLock {
                // Double-checked: another coroutine may have populated the cache while we waited.
                cachedVersion?.let { return Result.success(it) }
                withTimeout(HighlightException.TIMEOUT_SECONDS * 1000L) {
                    withContext(Dispatchers.Main) {
                        suspendCancellableCoroutine { continuation ->
                            webView.evaluateJavascript("hljsVersion()") { rawResult ->
                                if (!continuation.isActive) return@evaluateJavascript
                                if (rawResult == null || rawResult == "null") {
                                    continuation.resumeWithException(
                                        HighlightException.JsExecutionFailed(
                                            RuntimeException("hljsVersion() returned null"),
                                        ),
                                    )
                                    return@evaluateJavascript
                                }
                                // evaluateJavascript returns a JSON-encoded string — strip quotes.
                                val version = unescapeJsString(rawResult)
                                cachedVersion = version
                                continuation.resume(Result.success(version))
                            }
                        }
                    }
                }
            }
        } catch (e: HighlightException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(HighlightException.JsExecutionFailed(e))
        }
    }

    /**
     * Executes the highlight JS call and returns the resulting HTML.
     *
     * String escaping is delegated to [escapeForJs]; see that function for the full escape order.
     *
     * The JS callback returns a JSON-encoded string — parsed by [unescapeJsString].
     */
    private suspend fun executeJs(
        webView: WebView,
        code: String,
        language: String,
    ): Result<String> {
        val escaped = escapeForJs(code)
        val escapedLang = escapeForJs(language)

        val js = "(function() { return highlightCode('$escaped', '$escapedLang'); })()"

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
}

/**
 * Unescapes a JSON-encoded string returned by [WebView.evaluateJavascript].
 *
 * Uses a single character-by-character pass to correctly handle all escape sequences,
 * including cases like `\\n` (JSON for a literal backslash followed by 'n') that sequential
 * [String.replace] calls cannot handle correctly (the `\\` and `\n` replacements interfere).
 *
 * Supported escape sequences: `\"`, `\\`, `\/`, `\n`, `\r`, `\t`, `\uXXXX`.
 */
internal fun unescapeJsString(jsonString: String): String {
    // Strip surrounding double quotes if present
    val inner =
        if (jsonString.startsWith("\"") && jsonString.endsWith("\"")) {
            jsonString.substring(1, jsonString.length - 1)
        } else {
            jsonString
        }
    val sb = StringBuilder(inner.length)
    var i = 0
    while (i < inner.length) {
        val c = inner[i]
        if (c == '\\' && i + 1 < inner.length) {
            when (inner[i + 1]) {
                '"' -> {
                    sb.append('"')
                    i += 2
                }

                '\\' -> {
                    sb.append('\\')
                    i += 2
                }

                '/' -> {
                    sb.append('/')
                    i += 2
                }

                'n' -> {
                    sb.append('\n')
                    i += 2
                }

                'r' -> {
                    sb.append('\r')
                    i += 2
                }

                't' -> {
                    sb.append('\t')
                    i += 2
                }

                'u' -> {
                    // \uXXXX — exactly 4 hex digits required
                    if (i + 5 < inner.length) {
                        val hex = inner.substring(i + 2, i + 6)
                        val codePoint = hex.toIntOrNull(16)
                        if (codePoint != null) {
                            sb.append(codePoint.toChar())
                            i += 6
                        } else {
                            sb.append(c)
                            i++
                        }
                    } else {
                        sb.append(c)
                        i++
                    }
                }

                else -> {
                    sb.append(c)
                    i++
                }
            }
        } else {
            sb.append(c)
            i++
        }
    }
    return sb.toString()
}

/**
 * Escapes a string for safe interpolation into a single-quoted JavaScript string literal.
 *
 * Escape order:
 * 1. `\` → `\\` (must be first to avoid double-escaping subsequent replacements)
 * 2. `'` → `\'`
 * 3. `\n` (LF, U+000A) → `\n`
 * 4. `\r` (CR, U+000D) → `\r`
 * 5. U+2028 (Line Separator) → `\u2028` (pre-ES2019 JS treats this as a line terminator)
 * 6. U+2029 (Paragraph Separator) → `\u2029` (pre-ES2019 JS treats this as a line terminator)
 *
 * Steps 5–6 are required for compatibility with WebView on pre-Android 10 devices (pre-ES2019
 * V8). Without these escapes, a string containing U+2028 or U+2029 would produce an unterminated
 * string literal in the JS engine, resulting in a [HighlightException.JsExecutionFailed].
 */
internal fun escapeForJs(str: String): String =
    str
        .replace("\\", "\\\\") // Must be first
        .replace("'", "\\'")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\u2028", "\\u2028")
        .replace("\u2029", "\\u2029")

/**
 * Holds both light and dark [AnnotatedString] results from a single highlight call.
 * Used by [HighlightEngine.highlightBothThemes].
 *
 * ```kotlin
 * val result by rememberHighlightedCodeBothThemes(
 *     code       = code,
 *     language   = "kotlin",
 *     lightTheme = remember { HighlightTheme.tomorrow(context.applicationContext) },
 *     darkTheme  = remember { HighlightTheme.tomorrowNight(context.applicationContext) },
 * )
 * val text = if (isDark) result?.dark else result?.light
 * Text(text = text ?: AnnotatedString(code))
 * ```
 *
 * @property light Syntax-highlighted [AnnotatedString] styled with the light theme.
 * @property dark Syntax-highlighted [AnnotatedString] styled with the dark theme.
 * @property durationMs Pure highlight time in milliseconds — covers the JS call and a single
 *   HTML conversion pass (light and dark outputs are produced together in one pass). Excludes
 *   coroutine-scheduling overhead.
 */
data class ThemedHighlightResult(
    val light: AnnotatedString,
    val dark: AnnotatedString,
    val durationMs: Long,
)
