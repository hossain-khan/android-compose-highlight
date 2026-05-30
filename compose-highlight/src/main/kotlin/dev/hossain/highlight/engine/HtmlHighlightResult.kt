package dev.hossain.highlight.engine

import kotlin.time.Duration

/**
 * Result of a successful [HighlightEngine.highlightToHtml] call.
 *
 * Pairs the raw HTML output with the time taken to produce it, so callers that work
 * directly with HTML (e.g. custom renderers, harnesses measuring JS round-trip time)
 * get timing data without having to measure it manually.
 *
 * ## Usage
 *
 * ```kotlin
 * engine.highlightToHtml("val x = 42", "kotlin").onSuccess { result ->
 *     // Raw HTML with <span class="hljs-*"> tokens
 *     renderHtml(result.html)
 *
 *     // JS round-trip time (ms)
 *     log("highlightToHtml took ${result.durationMs} ms")
 *
 *     // Per-stage Duration values
 *     log("JS bridge:     ${result.jsBridgeDuration}")
 *     log("JSON unescape: ${result.jsonUnescapeDuration}")
 * }
 * ```
 *
 * @property html The raw HTML string produced by Highlight.js, containing
 *   `<span class="hljs-*">` tokens that can be styled by a CSS theme.
 * @property durationMs JavaScript round-trip time in milliseconds - measured from immediately
 *   before `evaluateJavascript()` is called (after WebView is ready and the internal mutex is
 *   acquired) through receipt of the result. Excludes WebView warm-up and queue-wait time.
 *   Equals `jsBridgeDuration.inWholeMilliseconds`.
 * @property jsBridgeDuration Time for the `evaluateJavascript()` round-trip as a [Duration].
 *   Measured from immediately before the call to when the JS result callback fires.
 *   Excludes WebView warm-up and mutex-wait time.
 * @property jsonUnescapeDuration Time for [unescapeJsString] - the character-by-character pass
 *   that strips JSON encoding from the string returned by the JS engine.
 */
data class HtmlHighlightResult(
    val html: String,
    val durationMs: Long,
    val jsBridgeDuration: Duration = Duration.ZERO,
    val jsonUnescapeDuration: Duration = Duration.ZERO,
)
