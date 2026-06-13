package dev.hossain.highlight.engine

import android.content.Context
import android.webkit.WebView
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import dev.hossain.highlight.engine.internal.HtmlToAnnotatedString
import dev.hossain.highlight.engine.internal.WebViewManager
import dev.hossain.highlight.engine.internal.escapeForJs
import dev.hossain.highlight.engine.internal.unescapeJsString
import dev.hossain.highlight.engine.internal.withEngineErrorHandling
import dev.hossain.highlight.engine.internal.withHtmlParsingErrorHandling
import dev.hossain.highlight.ui.rememberHighlightEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.Closeable
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.Duration
import kotlin.time.TimeSource
import kotlin.time.measureTimedValue

/**
 * Core engine that manages the hidden WebView and executes Highlight.js highlighting.
 *
 * Thread safety: WebView is always accessed on the Main thread.
 * Concurrent highlight calls are serialized via [mutex].
 *
 * ## How highlighting works
 *
 * Highlight.js runs inside a hidden off-screen WebView. When you call [highlight], the engine:
 *
 * 1. **Tokenizes** - calls `highlightCode(code, lang)` via `evaluateJavascript()`, which returns
 *    an HTML string where each token is wrapped in a `<span class="hljs-keyword">` (or similar).
 *    highlight.js only assigns class names - it does not apply any colors itself.
 *
 * 2. **Resolves colors** - [HighlightTheme] lazily parses its CSS file with the theme parser, which
 *    translates CSS rules like `.hljs-keyword { color: #7928a1 }` into a map of
 *    `"hljs-keyword" -> SpanStyle(color=Color(0xFF7928a1))`. This is the bridge between
 *    CSS-based theming and Compose's styling model.
 *
 * 3. **Builds AnnotatedString** - `HtmlToAnnotatedString` walks the HTML token tree with a custom HTML parser,
 *    looks up each span's class name in the theme's color map, and applies the matching
 *    [SpanStyle]. The result is a fully colored [AnnotatedString] ready for Compose `Text`.
 *
 * ## Lifecycle
 *
 * The engine holds a hidden WebView resource and implements [Closeable] for safe resource
 * management. Always call [close] (or [destroy]) when the engine is no longer needed.
 *
 * When used inside a Composable, use `rememberHighlightEngine()` which calls [destroy]
 * automatically via `DisposableEffect`. For manual usage (e.g. in a ViewModel), call [close]
 * or [destroy] in `onCleared()`. Since highlighting APIs are `suspend`, prefer coroutine-friendly
 * `try/finally` cleanup for scoped usage:
 *
 * ```kotlin
 * val engine = HighlightEngine(context.applicationContext)
 * try {
 *     val result = engine.highlight(code, "kotlin", theme)
 * } finally {
 *     engine.close()
 * }
 * ```
 *
 * ## WebView requirement
 *
 * This library requires WebView to be installed and enabled on the device. When WebView is
 * unavailable (e.g. Android Go devices, during a system WebView update, or when disabled by MDM
 * policy), all highlight methods return [Result.failure] wrapping
 * [HighlightException.WebViewInitFailed]. Check for this specific exception type to distinguish
 * WebView availability issues from JavaScript errors.
 *
 * ## Composable usage (lower-level)
 *
 * For most cases, prefer `SyntaxHighlightedCode` inside a `HighlightThemeProvider` - it handles
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
 * val engine = HighlightEngine(context.applicationContext)
 *
 * // Suspend calls must run inside a coroutine (e.g. viewModelScope.launch).
 * viewModelScope.launch {
 *     // Optional: warm up before first use to reduce first-call latency.
 *     engine.initialize().onFailure { /* handle WebViewInitFailed if needed */ }
 *
 *     val result = engine.highlight(
 *         code     = "val x = 42",
 *         language = "kotlin",
 *         theme    = HighlightTheme.atomOneDark(),
 *     )
 *     result.onSuccess { highlighted ->
 *         display(highlighted.annotated)            // AnnotatedString
 *         log("spans: ${highlighted.spanCount}")    // 0 = unsupported language
 *         log("time:  ${highlighted.durationMs} ms")
 *     }
 * }
 *
 * // Release resources when done (e.g. in ViewModel.onCleared())
 * engine.destroy() // or engine.close()
 * ```
 *
 * ## Highlight once, render in two themes
 *
 * ```kotlin
 * // Inside a coroutine (e.g. viewModelScope.launch or LaunchedEffect):
 * engine.highlightBothThemes(
 *     code       = sourceCode,
 *     language   = "typescript",
 *     lightTheme = HighlightTheme.tomorrow(),
 *     darkTheme  = HighlightTheme.tomorrowNight(),
 * ).onSuccess { result ->
 *     val display = if (isDark) result.dark else result.light
 * }
 * ```
 */
class HighlightEngine(
    context: Context,
) : Closeable {
    // Use applicationContext to avoid retaining an Activity context in the long-lived WebView.
    private val manager = WebViewManager(context.applicationContext)

    /**
     * Returns the underlying [WebView] **synchronously** for test-only inspection, or `null`
     * if the engine has not yet been initialized. Tests use this to drive the hidden WebView
     * via Robolectric's `Shadows.shadowOf(...)` without reflection.
     *
     * Production code must not call this. Use [highlight], [highlightToHtml], etc., which
     * all suspend on the engine's normal initialization path.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    internal fun webViewForTest(): WebView? = manager.webViewForTest()

    // Serializes concurrent evaluateJavascript() calls - WebView handles one at a time.
    private val mutex = Mutex()

    // Cached result of supportedLanguages() - list is static for a given bundled hljs version.
    @Volatile
    private var cachedLanguages: List<String>? = null

    // Cached result of highlightJsVersion() - version is static for the bundled hljs build.
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
     *     // WebView is warm - next highlight call has no init latency
     * }
     * ```
     */
    val isInitialized: StateFlow<Boolean> get() = manager.isInitialized

    /**
     * Warms up the hidden WebView and loads bridge.html.
     *
     * This is an optional optimization - if not called, the first [highlightToHtml] or
     * [highlight] call will initialize automatically. Call this early (e.g. on screen entry)
     * to reduce latency on the first highlight request.
     *
     * Safe to call multiple times - idempotent.
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
        } catch (e: HighlightException) {
            Result.failure(e)
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
     *   with a [HighlightException] on error. Returns [HighlightException.Timeout] if the
     *   JavaScript call does not complete within the timeout window.
     */
    suspend fun highlightToHtml(
        code: String,
        language: String,
    ): Result<HtmlHighlightResult> =
        withEngineErrorHandling {
            manager.initialize()
            val webView = manager.getReadyWebView()

            mutex.withLock {
                withTimeout(HighlightException.TIMEOUT_SECONDS * 1000L) {
                    executeJs(webView, code, language)
                }.map { jsResult ->
                    HtmlHighlightResult(
                        html = jsResult.html,
                        durationMs = jsResult.jsBridgeDuration.inWholeMilliseconds,
                        jsBridgeDuration = jsResult.jsBridgeDuration,
                        jsonUnescapeDuration = jsResult.jsonUnescapeDuration,
                    )
                }
            }
        }

    /**
     * Full pipeline: tokenise → apply theme → convert to [HighlightResult].
     *
     * Combines [highlightToHtml] with colour-map application to produce a ready-to-render
     * [AnnotatedString]. A [HighlightResult.spanCount] of `0` indicates a silent failure -
     * the language may be unsupported or the code was empty; [HighlightResult.annotated]
     * still contains plain text so callers can always render something.
     *
     * ```kotlin
     * engine.highlight(code, "kotlin", theme).onSuccess { result ->
     *     display(result.annotated)
     *     if (result.spanCount == 0) log("no tokens - language may be unsupported")
     *     log("highlighted in ${result.durationMs} ms")
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
        val totalStart = TimeSource.Monotonic.markNow()
        // WebView JS call must stay on Dispatchers.Main (handled inside highlightToHtml).
        val htmlResult = highlightToHtml(code, language).getOrElse { return Result.failure(it) }
        // Theme parsing (may include asset I/O on first use) and HTML-to-AnnotatedString conversion
        // are run off the Main thread on Dispatchers.Default.
        return withContext(Dispatchers.Default) {
            withHtmlParsingErrorHandling {
                val (colorMap, themeParseD) = theme.timedColorMap()
                val convertResult = HtmlToAnnotatedString.convertTimed(htmlResult.html, colorMap)
                val totalDuration = totalStart.elapsedNow()
                Result.success(
                    HighlightResult(
                        annotated = convertResult.annotated,
                        spanCount = convertResult.annotated.spanStyles.size,
                        language = language,
                        durationMs = totalDuration.inWholeMilliseconds,
                        timings =
                            HighlightTimings(
                                jsBridge = htmlResult.jsBridgeDuration,
                                jsonUnescape = htmlResult.jsonUnescapeDuration,
                                htmlParse = convertResult.htmlParseDuration,
                                treeWalk = convertResult.treeWalkDuration,
                                themeParse = themeParseD,
                                total = totalDuration,
                            ),
                    ),
                )
            }
        }
    }

    /**
     * Highlights [code] once and produces a [ThemedHighlightResult] with both a light and a dark
     * [androidx.compose.ui.text.AnnotatedString].
     *
     * The JS tokeniser runs **once**; the two colour maps are applied to the same HTML output,
     * so theme switching after the call returns is instant - no extra WebView round-trip.
     *
     * ```kotlin
     * // Inside a coroutine (e.g. viewModelScope.launch or LaunchedEffect):
     * engine.highlightBothThemes(
     *     code       = sourceCode,
     *     language   = "typescript",
     *     lightTheme = HighlightTheme.tomorrow(),
     *     darkTheme  = HighlightTheme.tomorrowNight(),
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
        val totalStart = TimeSource.Monotonic.markNow()
        // WebView JS call must stay on Dispatchers.Main (handled inside highlightToHtml).
        val htmlResult = highlightToHtml(code, language).getOrElse { return Result.failure(it) }
        // Theme parsing (may include asset I/O on first use) and HTML-to-AnnotatedString conversion
        // are run off the Main thread on Dispatchers.Default.
        return withContext(Dispatchers.Default) {
            withHtmlParsingErrorHandling {
                val (lightColorMap, lightThemeParseD) = lightTheme.timedColorMap()
                val (darkColorMap, darkThemeParseD) = darkTheme.timedColorMap()
                val convertResult =
                    HtmlToAnnotatedString.convertBothThemesTimed(
                        htmlResult.html,
                        lightColorMap,
                        darkColorMap,
                    )
                val totalDuration = totalStart.elapsedNow()
                Result.success(
                    ThemedHighlightResult(
                        light = convertResult.light,
                        dark = convertResult.dark,
                        durationMs = totalDuration.inWholeMilliseconds,
                        timings =
                            HighlightTimings(
                                jsBridge = htmlResult.jsBridgeDuration,
                                jsonUnescape = htmlResult.jsonUnescapeDuration,
                                htmlParse = convertResult.htmlParseDuration,
                                treeWalk = convertResult.treeWalkDuration,
                                themeParse = lightThemeParseD + darkThemeParseD,
                                total = totalDuration,
                            ),
                    ),
                )
            }
        }
    }

    /**
     * Returns the list of language identifiers supported by the bundled Highlight.js.
     *
     * The result is fetched from the JS engine on the first call and cached - subsequent calls
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
     *   `"java"`, `"python"`), or [Result.failure] with a [HighlightException] on failure.
     *   Possible failures: [HighlightException.WebViewInitFailed] if the WebView could not be
     *   initialized, [HighlightException.Timeout] if the JS call exceeds the timeout, or
     *   [HighlightException.JsExecutionFailed] for other JS errors.
     */
    suspend fun supportedLanguages(): Result<List<String>> {
        cachedLanguages?.let { return Result.success(it) }
        return withEngineErrorHandling {
            manager.initialize()
            val webView = manager.getReadyWebView()
            mutex.withLock {
                // Double-checked: another coroutine may have populated the cache while we waited.
                val cached = cachedLanguages
                if (cached != null) {
                    Result.success(cached)
                } else {
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
                                    // e.g. ["kotlin","java",...] - parse with JSONArray.
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
            }
        }
    }

    /**
     * Returns the version string of the bundled Highlight.js library (e.g. `"11.11.1"`).
     *
     * The result is fetched from the JS engine on the first call and cached - subsequent calls
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
     *   on failure. Possible failures: [HighlightException.WebViewInitFailed] if the WebView could
     *   not be initialized, [HighlightException.Timeout] if the JS call exceeds the timeout, or
     *   [HighlightException.JsExecutionFailed] for other JS errors.
     */
    suspend fun highlightJsVersion(): Result<String> {
        cachedVersion?.let { return Result.success(it) }
        return withEngineErrorHandling {
            manager.initialize()
            val webView = manager.getReadyWebView()
            mutex.withLock {
                // Double-checked: another coroutine may have populated the cache while we waited.
                val cached = cachedVersion
                if (cached != null) {
                    Result.success(cached)
                } else {
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
                                    // evaluateJavascript returns a JSON-encoded string - strip quotes.
                                    val version = unescapeJsString(rawResult)
                                    cachedVersion = version
                                    continuation.resume(Result.success(version))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns metadata for a Highlight.js language name or alias.
     *
     * This is useful when you want to validate a user-supplied language string or resolve an alias
     * such as `"kt"` to Highlight.js metadata.
     *
     * ```kotlin
     * engine.getLanguage("js").onSuccess { info ->
     *     if (info != null) {
     *         println("${'$'}{info.name}: ${'$'}{info.aliases}")
     *     }
     * }
     * ```
     *
     * @param nameOrAlias Highlight.js language name or alias to look up.
     * @return [Result.success] with [HighlightLanguageInfo] when found, `null` when Highlight.js
     *   does not know the language, or [Result.failure] with a [HighlightException] on error.
     */
    suspend fun getLanguage(nameOrAlias: String): Result<HighlightLanguageInfo?> =
        withEngineErrorHandling {
            manager.initialize()
            val webView = manager.getReadyWebView()
            val escapedNameOrAlias = escapeForJs(nameOrAlias)
            mutex.withLock {
                withTimeout(HighlightException.TIMEOUT_SECONDS * 1000L) {
                    withContext(Dispatchers.Main) {
                        suspendCancellableCoroutine { continuation ->
                            webView.evaluateJavascript("getLanguage('$escapedNameOrAlias')") { rawResult ->
                                if (!continuation.isActive) return@evaluateJavascript
                                if (rawResult == null) {
                                    continuation.resumeWithException(
                                        HighlightException.JsExecutionFailed(RuntimeException("evaluateJavascript returned null")),
                                    )
                                    return@evaluateJavascript
                                }
                                if (rawResult == "null") {
                                    continuation.resume(Result.success(null))
                                    return@evaluateJavascript
                                }
                                try {
                                    val json = org.json.JSONObject(unescapeJsString(rawResult))
                                    if (json.optBoolean("error")) {
                                        continuation.resumeWithException(
                                            HighlightException.JsExecutionFailed(
                                                RuntimeException("highlight.js error: ${json.optString("message")}"),
                                            ),
                                        )
                                        return@evaluateJavascript
                                    }
                                    val aliasesJson = json.optJSONArray("aliases")
                                    val aliases =
                                        buildList {
                                            if (aliasesJson != null) {
                                                for (index in 0 until aliasesJson.length()) {
                                                    add(aliasesJson.getString(index))
                                                }
                                            }
                                        }
                                    continuation.resume(
                                        Result.success(
                                            HighlightLanguageInfo(
                                                name = json.getString("name"),
                                                aliases = aliases,
                                            ),
                                        ),
                                    )
                                } catch (e: Exception) {
                                    continuation.resumeWithException(HighlightException.JsExecutionFailed(e))
                                }
                            }
                        }
                    }
                }
            }
        }

    /**
     * Highlights [code] with Highlight.js automatic language detection.
     *
     * This is convenient when the language is not known ahead of time, but it is typically slower
     * and less accurate than passing an explicit language to [highlight].
     *
     * ```kotlin
     * engine.highlightAuto(code, theme).onSuccess { result ->
     *     println("Detected: ${'$'}{result.detectedLanguage}")
     *     render(result.annotated)
     * }
     * ```
     *
     * @param code The source code to highlight.
     * @param theme The [HighlightTheme] whose colour map will be applied.
     * @return [Result] wrapping an [AutoHighlightResult], or [Result.failure] with a
     *   [HighlightException] on error.
     */
    suspend fun highlightAuto(
        code: String,
        theme: HighlightTheme,
    ): Result<AutoHighlightResult> {
        val totalStart = TimeSource.Monotonic.markNow()
        return withEngineErrorHandling {
            manager.initialize()
            val webView = manager.getReadyWebView()
            // Release the mutex after the WebView call so the lock is not held during CPU work.
            val jsResult =
                mutex
                    .withLock {
                        withTimeout(HighlightException.TIMEOUT_SECONDS * 1000L) {
                            executeJsAuto(webView, code)
                        }
                    }.getOrThrow()
            // Theme parsing (may include asset I/O on first use) and HTML-to-AnnotatedString
            // conversion are run off the Main thread on Dispatchers.Default.
            withContext(Dispatchers.Default) {
                withHtmlParsingErrorHandling {
                    val (colorMap, themeParseD) = theme.timedColorMap()
                    val convertResult = HtmlToAnnotatedString.convertTimed(jsResult.html, colorMap)
                    val totalDuration = totalStart.elapsedNow()
                    Result.success(
                        AutoHighlightResult(
                            annotated = convertResult.annotated,
                            detectedLanguage = jsResult.detectedLanguage,
                            spanCount = convertResult.annotated.spanStyles.size,
                            durationMs = totalDuration.inWholeMilliseconds,
                            timings =
                                HighlightTimings(
                                    jsBridge = jsResult.jsBridgeDuration,
                                    jsonUnescape = jsResult.jsonUnescapeDuration,
                                    htmlParse = convertResult.htmlParseDuration,
                                    treeWalk = convertResult.treeWalkDuration,
                                    themeParse = themeParseD,
                                    total = totalDuration,
                                ),
                        ),
                    )
                }
            }
        }
    }

    /**
     * Releases the WebView resources and clears all internal caches (languages, version).
     *
     * Idempotent - safe to call multiple times. Subsequent calls after the first are no-ops.
     */
    fun destroy() {
        cachedLanguages = null
        cachedVersion = null
        manager.destroy()
    }

    /**
     * Releases the WebView resources by delegating to [destroy]. Implements [Closeable] so this
     * engine participates in IDE resource-leak inspections and supports explicit cleanup through
     * [close]. Safe to call multiple times.
     */
    override fun close() {
        destroy()
    }

    /**
     * Executes the highlight JS call and returns the resulting HTML with timing data.
     *
     * String escaping is delegated to [escapeForJs]; see that function for the full escape order.
     *
     * The JS callback returns a JSON-encoded string - parsed by [unescapeJsString].
     * Returns a [JsResult] containing the HTML, the JS bridge round-trip duration, and the
     * JSON unescape duration.
     */
    private suspend fun executeJs(
        webView: WebView,
        code: String,
        language: String,
    ): Result<JsResult> {
        val escaped = escapeForJs(code)
        val escapedLang = escapeForJs(language)

        val js = "(function() { return highlightCode('$escaped', '$escapedLang'); })()"

        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                val jsStart = TimeSource.Monotonic.markNow()
                webView.evaluateJavascript(js) { rawResult ->
                    val jsBridgeDuration = jsStart.elapsedNow()
                    if (!continuation.isActive) return@evaluateJavascript
                    if (rawResult == null || rawResult == "null") {
                        continuation.resumeWithException(
                            HighlightException.JsExecutionFailed(RuntimeException("JS returned null")),
                        )
                        return@evaluateJavascript
                    }
                    try {
                        // The result is a JSON-encoded string - strip surrounding quotes and unescape
                        val (jsonString, jsonUnescapeDuration) =
                            measureTimedValue { unescapeJsString(rawResult) }
                        val json = org.json.JSONObject(jsonString)
                        if (json.optBoolean("error")) {
                            continuation.resumeWithException(
                                HighlightException.JsExecutionFailed(
                                    RuntimeException("highlight.js error: ${json.optString("message")}"),
                                ),
                            )
                            return@evaluateJavascript
                        }
                        continuation.resume(
                            Result.success(
                                JsResult(
                                    html = json.getString("html"),
                                    jsBridgeDuration = jsBridgeDuration,
                                    jsonUnescapeDuration = jsonUnescapeDuration,
                                ),
                            ),
                        )
                    } catch (e: Exception) {
                        continuation.resumeWithException(HighlightException.JsExecutionFailed(e))
                    }
                }
            }
        }
    }

    /**
     * Executes the auto-detect highlight JS call and returns the resulting HTML with timing data.
     */
    private suspend fun executeJsAuto(
        webView: WebView,
        code: String,
    ): Result<AutoJsResult> {
        val escaped = escapeForJs(code)
        val js = "(function() { return highlightAuto('$escaped'); })()"

        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                val jsStart = TimeSource.Monotonic.markNow()
                webView.evaluateJavascript(js) { rawResult ->
                    val jsBridgeDuration = jsStart.elapsedNow()
                    if (!continuation.isActive) return@evaluateJavascript
                    if (rawResult == null || rawResult == "null") {
                        continuation.resumeWithException(
                            HighlightException.JsExecutionFailed(RuntimeException("JS returned null")),
                        )
                        return@evaluateJavascript
                    }
                    try {
                        val (jsonString, jsonUnescapeDuration) =
                            measureTimedValue { unescapeJsString(rawResult) }
                        val json = org.json.JSONObject(jsonString)
                        // Check if the JS bridge returned an error object from the try/catch in bridge.html
                        if (json.optBoolean("error")) {
                            continuation.resumeWithException(
                                HighlightException.JsExecutionFailed(
                                    RuntimeException("highlight.js error: ${json.optString("message")}"),
                                ),
                            )
                            return@evaluateJavascript
                        }
                        continuation.resume(
                            Result.success(
                                AutoJsResult(
                                    html = json.getString("html"),
                                    detectedLanguage = json.optString("language"),
                                    jsBridgeDuration = jsBridgeDuration,
                                    jsonUnescapeDuration = jsonUnescapeDuration,
                                ),
                            ),
                        )
                    } catch (e: Exception) {
                        continuation.resumeWithException(HighlightException.JsExecutionFailed(e))
                    }
                }
            }
        }
    }
}

/**
 * Internal result of [HighlightEngine.executeJsAuto]: the unescaped HTML string, detected
 * language, and per-stage timing data measured inside the JS callback.
 */
private data class AutoJsResult(
    val html: String,
    val detectedLanguage: String,
    val jsBridgeDuration: Duration,
    val jsonUnescapeDuration: Duration,
)

/**
 * Internal result of [HighlightEngine.executeJs]: the unescaped HTML string together with
 * per-stage timing data measured inside the JS callback.
 */
private data class JsResult(
    val html: String,
    val jsBridgeDuration: Duration,
    val jsonUnescapeDuration: Duration,
)
